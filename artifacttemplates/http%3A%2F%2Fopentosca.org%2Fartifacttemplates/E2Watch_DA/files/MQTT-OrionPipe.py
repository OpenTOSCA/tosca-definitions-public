#!/usr/bin/env python

import paho.mqtt.client as mqtt
import requests
import json
import sys
import time
import getopt
import re
import os, fnmatch
from os.path import expanduser
import random
from datetime import datetime

##########################################################
# Constants
###########################################################
CHANNETYPE_IN = "IN"
CHANNETYPE_OUT = "OUT"
STATUSCODE_NOTFOUND = 404
SLEEP_TIME = 10 # in seconds

topics = []
brokerIps = []
channelTypes = []
fiwareservices = []
topicIN_index=0

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
##########################################################
# Following functions are used to build the structure:
#        /<entityID>/attrs/<attributeName>/value
# types: /Entity             /Atribute
# where topic = "/<entityID>/attrs/<attributeName>/value"
#
##########################################################

# create topic
def createTopic (host, entityID, attributeName, attributeType, fiwareservice):
    url = "http://" + host + "/v2/entities"
    try:
        payload = {"id": entityID, "type":"Topic", attributeName: {"value": "", "type": attributeType}}
        response = requests.post (url, data=json.dumps(payload), headers={"Content-Type":"application/json", "FIWARE-Service":fiwareservice})
        print ("createTopic() response: ", response)
        #print (response.headers)
    except:
        e = sys.exc_info()[0]
        print ("createTopic(): Error sendind request: " + str(e))

# sends message to topic
def orionSendMessage (host, msg, topic, fiwareservice):
    topic_url = "http://" + host + "/v2/entities/" + topic
    print (topic_url)
    try:
        response = requests.put (topic_url, data=msg, headers={"Content-Type":"text/plain", "FIWARE-Service":fiwareservice})
        print ("sendMessage() response: ", response)
        print (response.headers)
        print (response.text)
    except:
        e = sys.exc_info()[0]
        print ("sendMessage(): Error sendind request: " + str(e))

# gets last message published on given topic 
def getLastMessage (host, topic, fiwareservice):
    topic_url = "http://" + host + "/v2/entities/" + topic
    try:
        response = requests.get (topic_url, headers={"Accept":"text/plain", "FIWARE-Service":fiwareservice})
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
def checkTopicAvailability (host, topic, attributeType, fiwareservice):
    message = getLastMessage(host, topic, fiwareservice)
    if (message == None):
        print ("Topic non-existent, it will be created")
        matchTopic = re.search(r"([A-Za-z0-6-]+)(/attrs/)([A-Za-z0-6]+)(/value)", topic)
        if (matchTopic is not None):
            entityID = matchTopic.group(1)
            attributeName = matchTopic.group(3)
            createTopic(host, entityID, attributeName, attributeType, fiwareservice)

############################
# MQTT Client
############################
class mqttClient(object):
   hostname = 'localhost'
   port = 1883
   clientid = ''
   topic_sub = ''
   subscriber = False
   
   def __init__(self, hostname, port, clientid):
      self.hostname = hostname
      self.port = port
      self.clientid = clientid
      
      # create MQTT client and set user name and password 
      self.client = mqtt.Client(client_id=self.clientid, clean_session=True, userdata=None, protocol=mqtt.MQTTv311)
      #client.username_pw_set(username="use-token-auth", password=mq_authtoken)

      # set mqtt client callbacks
      self.client.on_connect = self.on_connect
      self.client.on_message = self.on_message 

   # The callback for when the client receives a CONNACK response from the server.
   def on_connect(self, client, userdata, flags, rc):
      print("[" + datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3] + "]: " + "ClientID: " + self.clientid + "; Connected with result code " + str(rc))
      if (self.subscriber):
         self.client.subscribe(self.topic_sub)

   # The callback for when a PUBLISH message is received from the server.
   def on_message(self, client, userdata, msg):
      #print(msg.topic + " " + str(msg.payload))
      received_senml = json.loads(msg.payload)
      
      print(received_senml["Wasser"]["v"])
      value_wasser = received_senml["Wasser"]["v"]
      print ("Send value to orion: ", value_wasser)
      orionSendMessage(brokerIps[topicIN_index], str(value_wasser), topics[topicIN_index],fiwareservices[topicIN_index])
      print ("on_message")
      
   # publishes message to MQTT broker
   def sendMessage(self, topic, msg):
      self.client.publish(topic=topic, payload=msg, qos=0, retain=False)
      print(msg)

   # connects to IBM IoT MQTT Broker
   def start(self):
      self.client.connect(self.hostname, self.port, 60)

      #runs a thread in the background to call loop() automatically.
      #This frees up the main thread for other work that may be blocking.
      #This call also handles reconnecting to the broker.
      #Call loop_stop() to stop the background thread.
      self.client.loop_start()

   # connects to MQTT Broker
   def startAsSubcriber(self, topic_sub):
      self.subscriber = True
      self.topic_sub = topic_sub

      self.start()

############################



############################
# MAIN
############################
def main(argv):

    configFileName = "orion_connections.txt"
    #home = os.path.expandvars('$HOME/Desktop/spike') #example
    home = os.path.expandvars('$HOME')
    pattern = '*.csar'
    dirList = []

    
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
            fiwareservice = pars[3].strip('\n').strip()
            topics.append(topic)
            brokerIps.append(ip)
            channelTypes.append(channelType)
            fiwareservices.append(fiwareservice)
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
    checkTopicAvailability (brokerIps[topicIN_index], topics[topicIN_index], "Float", fiwareservices[topicIN_index])

    mqtt_hostname="smartorchestra.datenfreunde.net"
    mqtt_topic_sub="e2watch/smartorchestra/hackathon"
    print("Connecting to: " + mqtt_hostname + " pub on topic: " + mqtt_topic_sub)
    
    # --- Begin start mqtt client
    id = "id_%s" % (datetime.utcnow().strftime('%H_%M_%S'))
    publisher = mqttClient(mqtt_hostname, 1883, id)
    publisher.startAsSubcriber(mqtt_topic_sub)
   
    try:  
        while True:
            #outputValue = random.choice([20.0, 20.5, 21.0, 25.5, 30.0, 30.1, 31.5, 29.9, 35.0])
            #print ("Ramdom value: ", outputValue)
            #sendMessage(brokerIps[topicIN_index], str(outputValue), topics[topicIN_index],fiwareservices[topicIN_index])

            # sleeps x seconds
            time.sleep(SLEEP_TIME)
    except:
        e = sys.exc_info()[0]
        print ("end due to: ", str(e))

if __name__ == "__main__":
    main(sys.argv[1:])

