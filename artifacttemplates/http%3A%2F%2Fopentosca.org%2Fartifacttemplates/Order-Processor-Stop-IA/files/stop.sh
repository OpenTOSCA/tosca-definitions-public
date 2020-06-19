#!/bin/bash

# stop running processes of the Order-Processor JAR
ps -ef | grep order-processor.jar | grep -v grep | awk '{print $2}' | sudo xargs kill -15