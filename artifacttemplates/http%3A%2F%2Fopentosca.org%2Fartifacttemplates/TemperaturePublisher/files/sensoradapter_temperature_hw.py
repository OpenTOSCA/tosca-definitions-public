#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys, getopt
import paho.mqtt.client as mqtt
from datetime import datetime
import time
import json
import os, fnmatch
from os.path import expanduser

# hardware imports
import RPi.GPIO as GPIO
import spidev

# global hardware config
GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)
OUTPUT = GPIO.OUT
INPUT = GPIO.IN
LOW = GPIO.LOW
HIGH = GPIO.HIGH
HexDigits = [0x3f,0x06,0x5b,0x4f,0x66,0x6d,0x7d,0x07,0x7f,0x6f,0x77,0x7c,0x39,0x5e,0x79,0x71]
ADDR_AUTO = 0x40
ADDR_FIXED = 0x44
STARTADDR = 0xC0
BRIGHT_DARKEST = 0
BRIGHT_TYPICAL = 2
BRIGHT_HIGHEST = 7
# - Buzzer
useBuzzer = True
buzzer_datapin = 27
# - Display
useDisplay = True
display_clockpin = 23
display_datapin = 24
# - Temperature
temp_adc_channel = int(0)

############################
# MQTT Client
############################
class mqttClient(object):
   hostname = 'localhost'
   port = 1883
   clientid = ''

   def __init__(self, hostname, port, clientid):
      self.hostname = hostname
      self.port = port
      self.clientid = clientid

      # create MQTT client and set user name and password 
      self.client = mqtt.Client(client_id=self.clientid, clean_session=True, userdata=None)
      #client.username_pw_set(username="use-token-auth", password=mq_authtoken)

      # set mqtt client callbacks
      self.client.on_connect = self.on_connect      

   # The callback for when the client receives a CONNACK response from the server.
   def on_connect(self, client, userdata, flags, rc):
      print("[" + datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3] + "]: " + "ClientID: " + self.clientid + "; Connected with result code " + str(rc))

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

############################
# Buzzer
############################
class buzzer:
   pd = 2
   def __init__( self, pd):
      self.pd = pd
      GPIO.setup(self.pd, OUTPUT)
      GPIO.output(self.pd, 0)

   def buzzPwm(self, duration, freq):
      p = GPIO.PWM(self.pd,freq)
      p.start(50)
      time.sleep(float(duration)/1000)
      p.stop()

   # freq in Hz
   def buzz(self, duration, freq):
      GPIO.output(self.pd, 0)
      for i in range(1,duration):
         GPIO.output(self.pd, 1)
         time.sleep(0.001)
         GPIO.output(self.pd, 0)
         time.sleep(1.0/freq)

      GPIO.output(self.pd, 0)
# end class buzzer

############################
# Analog in (on linker-base ADC)
############################
class analogInputReader(object):
        def __init__(self):
                self.spi = spidev.SpiDev()
                self.spi.open(0,0)
 
        def readadc (self, adPin):
                # read SPI data from MCP3004 chip, 4 possible adc’s (0 thru 3)
                if ((adPin > 3) or (adPin < 0)):
                        return -1
                r = self.spi.xfer2([1,8+adPin <<4,0])
                #print(r)
                adcout = ((r[1] &3) <<8)+r[2]
                return adcout
 
        def getLevel (self, adPin):
                value = self.readadc(adPin)
                volts = (value*3.3)/1024
                return (volts, value)
	
        def getTemperature (self, adPin):
                v0 = self.getLevel(adPin)
                temp = (((v0[0] * 1000) - 500)/10) # celsius
                return temp

############################
# Linker Digital Display
############################
class TM1637:
	__doublePoint = False
	__Clkpin = 0
	__Datapin = 0
	__brightnes = BRIGHT_TYPICAL;
	__currentData = [0,0,0,0];
	
	def __init__( self, pinClock, pinData, brightnes ):
		self.__Clkpin = pinClock
		self.__Datapin = pinData
		self.__brightnes = brightnes;
		GPIO.setup(self.__Clkpin,OUTPUT)
		GPIO.setup(self.__Datapin,OUTPUT)
	# end  __init__

	def Clear(self):
		b = self.__brightnes;
		point = self.__doublePoint;
		self.__brightnes = 0;
		self.__doublePoint = False;
		data = [0x7F,0x7F,0x7F,0x7F];
		self.Show(data);
		self.__brightnes = b;				# restore saved brightnes
		self.__doublePoint = point;
	# end  Clear
	
	def ShowInt(self, i):
		s = str(i)
		self.Clear()
		for i in range(0,len(s)):
			self.Show1(i, int(s[i]))

	def Show( self, data ):
		for i in range(0,4):
			self.__currentData[i] = data[i];
		
		self.start();
		self.writeByte(ADDR_AUTO);
		self.stop();
		self.start();
		self.writeByte(STARTADDR);
		for i in range(0,4):
			self.writeByte(self.coding(data[i]));
		self.stop();
		self.start();
		self.writeByte(0x88 + self.__brightnes);
		self.stop();
	# end  Show

	def Show1(self, DigitNumber, data):	# show one Digit (number 0...3)
		if( DigitNumber < 0 or DigitNumber > 3):
			return;	# error
	
		self.__currentData[DigitNumber] = data;
		
		self.start();
		self.writeByte(ADDR_FIXED);
		self.stop();
		self.start();
		self.writeByte(STARTADDR | DigitNumber);
		self.writeByte(self.coding(data));
		self.stop();
		self.start();
		self.writeByte(0x88 + self.__brightnes);
		self.stop();
	# end  Show1
		
	def SetBrightnes(self, brightnes):		# brightnes 0...7
		if( brightnes > 7 ):
			brightnes = 7;
		elif( brightnes < 0 ):
			brightnes = 0;

		if( self.__brightnes != brightnes):
			self.__brightnes = brightnes;
			self.Show(self.__currentData);
		# end if
	# end  SetBrightnes

	def ShowDoublepoint(self, on):			# shows or hides the doublepoint
		if( self.__doublePoint != on):
			self.__doublePoint = on;
			self.Show(self.__currentData);
		# end if
	# end  ShowDoublepoint
			
	def writeByte( self, data ):
		for i in range(0,8):
			GPIO.output( self.__Clkpin, LOW)
			if(data & 0x01):
				GPIO.output( self.__Datapin, HIGH)
			else:
				GPIO.output( self.__Datapin, LOW)
			data = data >> 1
			GPIO.output( self.__Clkpin, HIGH)
		#endfor

		# wait for ACK
		GPIO.output( self.__Clkpin, LOW)
		GPIO.output( self.__Datapin, HIGH)
		GPIO.output( self.__Clkpin, HIGH)
		GPIO.setup(self.__Datapin, INPUT)
		
		while(GPIO.input(self.__Datapin)):
			time.sleep(0.001)
			if( GPIO.input(self.__Datapin)):
				GPIO.setup(self.__Datapin, OUTPUT)
				GPIO.output( self.__Datapin, LOW)
				GPIO.setup(self.__Datapin, INPUT)
			#endif
		# endwhile            
		GPIO.setup(self.__Datapin, OUTPUT)
	# end writeByte
    
	def start(self):
		GPIO.output( self.__Clkpin, HIGH) # send start signal to TM1637
		GPIO.output( self.__Datapin, HIGH)
		GPIO.output( self.__Datapin, LOW) 
		GPIO.output( self.__Clkpin, LOW) 
	# end start
	
	def stop(self):
		GPIO.output( self.__Clkpin, LOW) 
		GPIO.output( self.__Datapin, LOW) 
		GPIO.output( self.__Clkpin, HIGH)
		GPIO.output( self.__Datapin, HIGH)
	# end stop
	
	def coding(self, data):
		if( self.__doublePoint ):
			pointData = 0x80
		else:
			pointData = 0;
		
		if(data == 0x7F):
			data = 0
		else:
			data = HexDigits[data] + pointData;
		return data
	# end coding	
# end class TM1637

############################
# MAIN
############################
def main(argv):

   configFileName = "connections.txt"
   home = '/home/pi' 
   pattern = '*.csar'
   dirList = []
   topics = []
   brokerIps = []

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
           topics.append(topic)
           brokerIps.append(ip)

       # END parsing file
       
   else:
       print("Could not find root directory")
       sys.exit()

   hostname = brokerIps [0]
   topic_pub = topics [0]
   print("Connecting to: " + hostname + " pub on topic: " + topic_pub)
   
   # --- Begin start mqtt client
   id = "id_%s" % (datetime.utcnow().strftime('%H_%M_%S'))
   publisher = mqttClient(hostname, 1883, id)
   publisher.start()
   
   #sensor IDs
   id_temperature_sensor_0 = "temp_T0"

   # Hardware - buzzer
   if (useBuzzer):
      buzzer_act = buzzer(buzzer_datapin)
      for i in range (100, 10000, 100):
         buzzer_act.buzz(10,i)

   # Hardware - init analog input reader
   aiReader = analogInputReader()

   # Hardware - init display
   if (useDisplay): 
      display = TM1637(display_clockpin, display_datapin, BRIGHT_TYPICAL)
      display.ShowDoublepoint(True)
      
   try:  
      while True:
         # messages in json format
         # send message, topic: temperature
         t = datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]

         # read temperature
         measured_temp = aiReader.getTemperature(temp_adc_channel)
         msg_pub = { "timestamp": t, 
                     "value": "%f" % (measured_temp) }

         publisher.sendMessage (topic_pub, json.dumps(msg_pub))
         #publisher.sendMessage (topic_pub, "%f" % (measured_temp))
         if (useDisplay):  
            display.ShowInt(int(measured_temp))
            
         time.sleep(1)
   except:
      print ("end")
      
if __name__ == "__main__":
   main(sys.argv[1:])
