# SmartOrchestra_ServiceType3_OCBTinyTodo_Notification [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

> This smart service reads sensor values from a OCB Topic "Topic IN"(https://catalogue.fiware.org/enablers/publishsubscribe-context-broker-orion-context-broker) and creates a Notification in the form a Todo inside the contained ToDo Application MyTinyTodo (www.mytinytodo.net). 

Notes:
- The topic should already exist in the FIWARE Orion before the deployment of this service. It should have the following structure:  
`http://<TopicIN.Host>:<TopicIN.Port>/<TopicIN.EntityID>/attrs/<TopicIN.AttributeName>/value`  
Example: `http://192.168.209.165:1026/Demoraum-switch/attrs/cmd/value`

- Expected data type in TopicIN: "ON"/"OFF"   

- Example values for the input parameters:  

| Input Parameter | Example Value |
| --------------- |:--------------|
|`TopicIN.EntityID`| `Demoraum-switch`|
|`TopicIN.FIWARE-Service`| `Lutz`|
|`TopicIN.Port`| `1026`|
|`TopicIN.AttributeName`| `cmd`|
|`TopicIN.Host`| `192.168.209.165`|

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
