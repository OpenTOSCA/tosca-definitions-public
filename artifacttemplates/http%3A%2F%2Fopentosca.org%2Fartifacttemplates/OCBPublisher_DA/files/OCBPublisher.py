#!/usr/bin/env python

import requests
import json
import sys
import time
import getopt
import re
import os, fnmatch
from os.path import expanduser
import random

##########################################################
# Constants
###########################################################
CHANNETYPE_IN = "IN"
CHANNETYPE_OUT = "OUT"
STATUSCODE_NOTFOUND = 404
SLEEP_TIME = 10 # in seconds

##########################################################
# Following functions are used to build the structure:
#        /<entityID>/attrs/<attributeName>/value
# types: /Entity             /Atribute
# where topic = "/<entityID>/attrs/<attributeName>/value"
#
##########################################################

# create topic
def createTopic (host, entityID, attributeName, attributeType):
    url = "http://" + host + "/v2/entities"
    try:
        payload = {"id": entityID, "type":"Topic", attributeName: {"value": "", "type": attributeType}}
        response = requests.post (url, data=json.dumps(payload), headers={"Content-Type":"application/json"})
        print ("createTopic() response: ", response)
        #print (response.headers)
    except:
        e = sys.exc_info()[0]
        print ("createTopic(): Error sendind request: " + str(e))

# sends message to topic
def sendMessage (host, msg, topic):
    topic_url = "http://" + host + "/v2/entities/" + topic
    try:
        response = requests.put (topic_url, data=msg, headers={"Content-Type":"text/plain"})
        print ("sendMessage() response: ", response)
        #print (response.headers)
        #print (response.text)
    except:
        e = sys.exc_info()[0]
        print ("sendMessage(): Error sendind request: " + str(e))

# gets last message published on given topic 
def getLastMessage (host, topic):
    topic_url = "http://" + host + "/v2/entities/" + topic
    try:
        response = requests.get (topic_url, headers={"Accept":"text/plain"})
        print ("getLastMessage() response: ", response)
        #print (response.headers)
        #print ("value: ", response.text)
        if (response.status_code == STATUSCODE_NOTFOUND):
            return None
        else:
            return response.text
    except:
        e = sys.exc_info()
        print ("getLastMessage(): Error sendind request: " + str(e))

# checks if topics exist, if not, creates it
def checkTopicAvailability (host, topic, attributeType):
    message = getLastMessage(host, topic)
    if (message == None):
        print ("Topic non-existent, it will be created")
        matchTopic = re.search(r"([A-Za-z0-6-]+)(/attrs/)([A-Za-z0-6]+)(/value)", topic)
        if (matchTopic is not None):
            entityID = matchTopic.group(1)
            attributeName = matchTopic.group(3)
            createTopic(host, entityID, attributeName, attributeType)

##########################################################
# Example Calls for functions:
# topic: /SmartOrchestraDemo/attrs/temperature/value
# types: /Entity                  /Attribute
#
#createTopic ("129.69.209.38:1026", "SmartOrchestraDemo", "temperature")
#sendMessage("129.69.209.38:1026", "22.3", "SmartOrchestraDemo/attrs/temperature/value")
#getLastMessage("129.69.209.38:1026", "SmartOrchestraDemo/attrs/temperature/value")
#checkTopicAvailability("129.69.209.38:1026", "SmartOrchestraDemo/attrs/temperature/value")
##########################################################

############################
# MAIN
############################
def main(argv):

    configFileName = "orion_connections.txt"
    home = '/home' 
    pattern = '*.csar'
    dirList = []
    topics = []
    brokerIps = []
    channelTypes = []
    
    hostname = 'localhost'
    topic_pub = 'test'
    
    # walk through directory
    for root, dirs, files in os.walk(home):
        for dirName in dirs:
            # match search string
            if fnmatch.fnmatch(dirName, pattern):
                dirList.append(os.path.join(root, dirName))
                print (os.path.join(root, dirName))

    if (len(dirList) > 0):
        # check if configuration file exists
        configExists = False
        configFile = os.path.join(dirList[0], configFileName)

        while (not configExists):
            configExists = os.path.exists(configFile)
            time.sleep(1)

        # BEGIN parsing file
        fileObject = open (configFile)
        fileLines = fileObject.readlines()
        fileObject.close()

        for line in fileLines:
            pars = line.split('=')
            topic = pars[0].strip('\n').strip()
            ip = pars[1].strip('\n').strip()
            channelType = pars[2].strip('\n').strip()
            topics.append(topic)
            brokerIps.append(ip)
            channelTypes.append(channelType)
    # END parsing file
    else:
        print("Could not find root directory")
        sys.exit()

    # Condition for mapping
    jsonCondition = None
    if (argv):
        print("Condition: ", argv[0])
        try:
            jsonCondition = json.loads(argv[0])
        except:
            print ("invalid mapping condition: topicIN value will be forwarded to topicOUT")

    topicIN_index = channelTypes.index(CHANNETYPE_IN)
    #topicOUT_index = channelTypes.index(CHANNETYPE_OUT)
    #print("TopicIN index: ", topicIN_index, "topic index out: ", topicOUT_index)

    # Check if topics exist, if not, they will be created
    checkTopicAvailability (brokerIps[topicIN_index], topics[topicIN_index], "Float")

    try:  
        while True:
            outputValue = random.choice([20.0, 20.5, 21.0, 25.5, 30.0, 30.1, 31.5, 29.9, 35.0])
            print ("Ramdom value: ", outputValue)
            sendMessage(brokerIps[topicIN_index], str(outputValue), topics[topicIN_index])

            # sleeps x seconds
            time.sleep(SLEEP_TIME)
    except:
        e = sys.exc_info()[0]
        print ("end due to: ", str(e))

if __name__ == "__main__":
    main(sys.argv[1:])

