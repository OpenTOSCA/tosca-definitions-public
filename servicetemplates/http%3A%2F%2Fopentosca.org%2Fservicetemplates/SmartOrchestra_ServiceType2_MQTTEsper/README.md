# SmartOrchestra_ServiceType2_MQTTEsper [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

> This smart service reads sensor values from a MQTT "Topic IN" and send the processed value to a MQTT "Topic OUT". These topics are hosted in a MQTT Mosquitto Broker and the data processing is realized using the CEP Engine [Esper](http://www.espertech.com/esper/).

- Example values for the input parameters:

| Input Parameter | Example Value |
| --------------- |:--------------|
|`TopicMapper.Condition`| `create schema TopicOUT (cmd string);create schema TopicIN (value double);insert into TopicOUT select 'ON' as cmd from TopicIN (value >= 30);insert into TopicOUT select 'OFF' as cmd from TopicIN (value < 30)`|
|`TopicIN.Name`| `e2watch/smartorchestra/hackathon`|
|`TopicIN.Host`| `192.168.209.165`|
|`TopicOUT.Name`| `e2watch/smartorchestra/report`|
|`TopicOUT.Host`| `192.168.209.165`|

## Properties

None.

## Haftungsausschluss

Dies ist ein Forschungsprototyp und enthält insbesondere Beiträge von Studenten.
Diese Software enthält möglicherweise Fehler und funktioniert möglicherweise, insbesondere bei variierten oder neuen Anwendungsfällen, nicht richtig.
Insbesondere beim Produktiveinsatz muss 1. die Funktionsfähigkeit geprüft und 2. die Einhaltung sämtlicher Lizenzen geprüft werden.
Die Haftung für entgangenen Gewinn, Produktionsausfall, Betriebsunterbrechung, entgangene Nutzungen, Verlust von Daten und Informationen, Finanzierungsaufwendungen sowie sonstige Vermögens- und Folgeschäden ist, außer in Fällen von grober Fahrlässigkeit, Vorsatz und Personenschäden ausgeschlossen.

## Disclaimer of Warranty

Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor
provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT,
MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for determining the
appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of
permissions under this License.
