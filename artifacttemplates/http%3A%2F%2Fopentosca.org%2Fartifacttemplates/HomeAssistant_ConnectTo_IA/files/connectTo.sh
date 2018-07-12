#!/bin/bash
sudo sh -c "echo '127.0.0.1' $(hostname) >> /etc/hosts";
# we need the host and topic as parameters IP and Name
topic="$Name";
echo $topic;
baseURL="$VMIP";
echo $baseURL;

if [ ! -f ~/.homeassistant/configuration.yaml ]; then
mkdir ~/.homeassistant;
echo "homeassistant:
  name: SmartOrchestra Demo
  unit_system: metric

logbook:
history:" > ~/.homeassistant/configuration.yaml;
fi
if [ $SensorType = "temperature" ]; then
unit="°C"
elif [ $SensorType = "pressure" ]; then
unit="\"kPa\""
elif [ $SensorType = "humidity" ]; then
unit="\"%\""
elif [ $SensorType = "moldrisk" ]; then
unit="\"%\""
else
unit=""
fi

if [ $Protocol = "MQTT" ]; then
if [ -z $(grep "mqtt:" ~/.homeassistant/configuration.yaml) ]; then
echo "mqtt:
  broker: $baseURL
  port: 1883
  keepalive: 60
  protocol: 3.1
  " >> ~/.homeassistant/configuration.yaml;
fi

if [ -z $(grep "sensor:" ~/.homeassistant/configuration.yaml) ]; then
echo "sensor:
  - platform: mqtt
    state_topic: $topic
    name: MQTT $SensorType
    value_template: '{{ $ValuePath }}'
    qos: 0
    unit_of_measurement: $unit
    " >> ~/.homeassistant/configuration.yaml;
else
sed -i "
/sensor:/ c\
sensor:\\
  - platform: mqtt \\
    state_topic: $topic \\
    name: MQTT $SensorType \\
    value_template: \'{{ $ValuePath }}\' \\
    qos: 0 \\
    unit_of_measurement: $unit
" ~/.homeassistant/configuration.yaml;

fi


elif [ $Protocol = "HTTP" ]; then
if [ -z $(grep "sensor:" ~/.homeassistant/configuration.yaml) ]; then
echo "sensor:
  - platform: rest
    resource: $baseURL$topic
    name: HTTP $SensorType
    value_template: '{{ $ValuePath }}'
    unit_of_measurement: $unit
    " >> ~/.homeassistant/configuration.yaml;
else
sed -i "
/sensor:/ c\
sensor: \\
  - platform: rest\\
    resource: $baseURL$topic \\
    name: HTTP $SensorType \\
    value_template: \'{{ $ValuePath }}\' \\
    unit_of_measurement: $unit
" ~/.homeassistant/configuration.yaml;


fi
fi
