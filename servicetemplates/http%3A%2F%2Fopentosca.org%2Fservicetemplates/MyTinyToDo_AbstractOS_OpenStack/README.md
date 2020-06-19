# MyTinyToDo_AbstractOS_OpenStack [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

> Installs the PHP Application MyTinyToDo as a Docker Container on a DockerEngine which is hosted on a node template of the abstract OperatingSystem node type. This abstract OperatingSystem node template gets substituted during deployment with a matching running node template instance.

Notes: This Service Template requires a running instance of a service template that contains a Ubuntu VM 18.4 which is hosted on OpenStack to have at least one option for substitution once in deployment.

## Properties

- `SSHPort`
- `SSHPort`
- `DBUser`
- `DBPassword`
- `DBMSUser`
- `DBMSPassword`
- `DBMSPort`
- `instanceRef` (this property requires the value 'get_input: x' with x being an arbitrary non-empty value)
- `VMIP` (this property requires the value 'get_input: y' with y being an arbitrary non-empty value and x not equal y)

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
