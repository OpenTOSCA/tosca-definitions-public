#!/bin/bash
sudo kill -9 $(ps -ef | grep hass | grep -v grep | awk '{print $2}')