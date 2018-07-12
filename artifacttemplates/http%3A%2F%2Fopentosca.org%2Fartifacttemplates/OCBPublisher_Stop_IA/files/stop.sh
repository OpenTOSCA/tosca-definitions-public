#!/bin/bash
sudo kill -9 $(ps -ef | grep OCBPublisher.py | grep -v grep | awk '{print $2}')