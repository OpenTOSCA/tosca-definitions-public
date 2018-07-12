#!/usr/bin/env python

import requests
import json
import sys
import time
import getopt
import re
import os, fnmatch
import random
from os.path import expanduser
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
def createTopic (host, entityID, attributeName):
    url = "http://" + host + "/v2/entities"
    try:
        payload = {"id": entityID, "type":"Topic", attributeName: {"value": "", "type": "Float"}}
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
def checkTopicAvailability (host, topic):
    message = getLastMessage(host, topic)
    if (message == None):
        print ("Topic non-existent, it will be created")
        matchTopic = re.search(r"([A-Za-z0-6-]+)(/attrs/)([A-Za-z0-6]+)(/value)", topic)
        if (matchTopic is not None):
            entityID = matchTopic.group(1)
            attributeName = matchTopic.group(3)
            createTopic(host, entityID, attributeName)

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

### TinyTodo functions

def createTask(host, task):
    tinytodoTaskUrl = 'http://' + host + '/ajax.php?newTask'
    payload = {'list':'1', 'title': task, 'tag':''}
    print ('Creating Task ' + task + ' on ' + tinytodoTaskUrl)
    try:
        response = requests.post(tinytodoTaskUrl,data=payload)
	print ('ResponseCode:' + str(response.status_code))
	print ('ResponseText:' + response.text)
    except:
	e = sys.exc_info()
	print ("createTask(): Error sending request: " + str(e))


############################
# MAIN
############################
def main(argv):

    configFileName = "orion_connections.txt"
    configFileName2 = "tinytodo_connections.txt"
    home = '/home'
    pattern = '*.csar'
    dirList = []
    topics = []
    brokerIps = []
    channelTypes = []
    tinytodourl = ''

    hostname = 'localhost'
    topic_pub = 'test'

    random_names = ['Michael', 'Kalle' , 'Ana', 'Uwe', 'Lutz', 'Thomas', 'Niko', 'Ronald']

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
	configExists2 = False
        configFile = os.path.join(dirList[0], configFileName)
	configFile2 = os.path.join(dirList[0], configFileName2)

        while (not configExists and not configExists2):
            configExists = os.path.exists(configFile)
	    configExists2 = os.path.exists(configFile2)
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

	# parsing tinytodo connections
	fileObject = open(configFile2)
	fileLines = fileObject.readlines()
	fileObject.close()

	for line in fileLines:
	    tinytodourl = line.strip()
	    break
    else:
        print("Could not find root directory")
        sys.exit()

    # Condition for mapping
    jsonCondition = None
    #if (argv):
    #    print("Condition: ", argv[0])
    #    try:
    #        jsonCondition = json.loads(argv[0])
    #    except:
    #        print ("invalid mapping condition: topicIN value will be forwarded to topicOUT")

    topicIN_index = channelTypes.index(CHANNETYPE_IN)
    # topicOUT_index = channelTypes.index(CHANNETYPE_OUT)
    print("TopicIN index: ", topicIN_index)

    # Check if topics exist, if not, they will be created
    for i in range(len(topics)):
        checkTopicAvailability (brokerIps[i], topics[i])

    try:
        lastValue = ''
        while True:
            values = []
            # read data from TopicIN
            for i in range(len(topics)):
               lastValue = getLastMessage(brokerIps[i], topics[i])
               values.append(lastValue)
               print ("Topic: ", topics[i], "Last value: ", lastValue)

            currentInTopicValue = None
            try:
                currentInTopicValue = str(values[topicIN_index])
            except:
                 print ("received topic value is empty, no mapping or forwarding possible")

            # Mapping based on condition
            if (currentInTopicValue is not None):
                #for key, value in jsonCondition.items():
                #    print("key: {0} | value: {1}".format(key, value))
                #    matchKey = re.search(r"(TopicIN)([ ><=]+)([0-6. ]+)", key)
                #    matchValue = re.search(r"(TopicOUT)([ =]+)([0-6. ]+)", value)
                print ("Topic value: ", currentInTopicValue)
                if (currentInTopicValue == "ON"):
                    print("Open Window")
                    createTask(tinytodourl, 'Please open the window ' + random.choice(random_names))
                else:
                    print("Close Window")
                    createTask(tinytodourl, 'Please close the window ' + random.choice(random_names))
                #    if ((matchKey is not None) and (matchValue is not None)):
                #        operator = matchKey.group(2).strip()
                #        threshold = float(matchKey.group(3).strip())
                #        outputValue = matchValue.group(3).strip()

                        # send data to TopicOUT
                #        if (operator == ">"):
                #            if (currentInTopicValue > threshold):
                #                sendMessage(brokerIps[topicOUT_index], outputValue, topics[topicOUT_index])
                #        elif (operator == ">="):
                #            if (currentInTopicValue >= threshold):
                #                sendMessage(brokerIps[topicOUT_index], outputValue, topics[topicOUT_index])
                #        elif (operator == "=="):
                #            if (currentInTopicValue == threshold):
                #                sendMessage(brokerIps[topicOUT_index], outputValue, topics[topicOUT_index])
                #        elif (operator == "<"):
                #            if (currentInTopicValue < threshold):
                #                sendMessage(brokerIps[topicOUT_index], outputValue, topics[topicOUT_index])
                #        elif (operator == "<="):
                #            if (currentInTopicValue <= threshold):
                #                sendMessage(brokerIps[topicOUT_index], outputValue, topics[topicOUT_index])

            #elif (currentInTopicValue is not None):
            #    print("No mapping condition found: Forwarding topicIN value to topicOUT")
            #    sendMessage(brokerIps[topicOUT_index], values[topicIN_index], topics[topicOUT_index])

            # sleeps x seconds
            time.sleep(SLEEP_TIME)
    except:
        e = sys.exc_info()[0]
        print ("end due to: ", str(e))

if __name__ == "__main__":
    main(sys.argv[1:])
