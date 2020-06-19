#!/bin/bash

if [ ! -z "$(pgrep -f order-processor.jar)" ]; then
    echo "Result=success"
else
    echo "Result=failure"
fi
