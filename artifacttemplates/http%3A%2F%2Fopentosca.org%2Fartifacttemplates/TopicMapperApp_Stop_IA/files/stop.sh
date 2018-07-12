#!/bin/bash
sudo kill -9 $(ps -ef | grep orion_mapper.py | grep -v grep | awk '{print $2}')