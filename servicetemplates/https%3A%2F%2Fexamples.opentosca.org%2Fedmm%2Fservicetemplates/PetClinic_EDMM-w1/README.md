# Database

> This is an example model which is used to demonstrate the EDMM transformation.

## EDMM Model

The expected output using the EDMM export should be similar to:

```yml
---
components:
  pet_clinic:
    type: https_examples.opentosca.orgedmmnodetypes__Pet_Clinic_w1
    relations:
    - connects_to: db
    - hosted_on: tomcat
    artifacts:
    - war: C:\tosca-definitions-internal/artifacttemplates/https%253A%252F%252Fexamples.opentosca.org%252Fedmm%252Fnodetypeimplementations/Pet-Clinic-DA_w1-wip1/files/petclinic.war
  tomcat:
    relations:
    - hosted_on: pet_clinic_ubuntu
    type: tomcat
  pet_clinic_ubuntu:
    type: compute
    properties:
      key_name: edmm
      public_key: |-
        -----BEGIN PUBLIC KEY-----
        MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCqGKukO1De7zhZj6+H0qtjTkVxwTCpvKe4eCZ0
        FPqri0cb2JZfXJ/DgYSF6vUpwmJG8wVQZKjeGcjDOL5UlsuusFncCzWBQ7RKNUSesmQRMSGkVb1/
        3j+skZ6UtW+5u09lHNsj6tQ51s1SPrCBkedbNf0Tp0GbMJDyR4e9T04ZZwIDAQAB
        -----END PUBLIC KEY-----
      os_family: ''
      machine_image: ubuntu
      instance_type: large
  db_ubuntu:
    type: compute
    properties:
      key_name: edmm
      public_key: |-
        -----BEGIN PUBLIC KEY-----
        MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCqGKukO1De7zhZj6+H0qtjTkVxwTCpvKe4eCZ0
        FPqri0cb2JZfXJ/DgYSF6vUpwmJG8wVQZKjeGcjDOL5UlsuusFncCzWBQ7RKNUSesmQRMSGkVb1/
        3j+skZ6UtW+5u09lHNsj6tQ51s1SPrCBkedbNf0Tp0GbMJDyR4e9T04ZZwIDAQAB
        -----END PUBLIC KEY-----
      os_family: ''
      machine_image: ubuntu
      instance_type: large
  dbms:
    type: mysql_dbms
    relations:
    - hosted_on: db_ubuntu
    properties:
      root_password: petclinic
  db:
    type: mysql_database
    relations:
    - hosted_on: dbms
    properties:
      password: petclinic
      schema_name: petclinic
      user: pc
relation_types:
  depends_on:
    extends: null
  hosted_on:
    extends: depends_on
  connects_to:
    extends: depends_on
component_types:
  compute:
    operations:
      configure: null
    extends: base
    properties:
      public_key:
        type: string
      key_name:
        type: string
      os_family:
        type: string
      machine_image:
        type: string
      instance_type:
        type: string
  web_application:
    operations:
      configure: null
    extends: base
  database:
    extends: base
    properties:
      password:
        type: string
      schema_name:
        type: string
      user:
        type: string
  tomcat:
    operations:
      start: C:\tosca-definitions-internal/artifacttemplates/https%253A%252F%252Fexamples.opentosca.org%252Fedmm%252Fnodetypeimplementations/Tomcat-Create-IA_w1/files/create.sh
      create: C:\tosca-definitions-internal/artifacttemplates/https%253A%252F%252Fexamples.opentosca.org%252Fedmm%252Fnodetypeimplementations/Tomcat-Create-IA_w1/files/create.sh
    extends: web_server
  web_server:
    extends: software_component
    properties:
      port:
        type: integer
  dbms:
    extends: software_component
    properties:
      port:
        type: integer
      root_password:
        type: string
  software_component:
    extends: base
  mysql_dbms:
    operations:
      start: null
      create: null
    extends: mysql_dbms
  mysql_database:
    operations:
      configure: null
    extends: database
  https_examples.opentosca.orgedmmnodetypes__Pet_Clinic_w1:
    operations:
      configure: C:\tosca-definitions-internal/artifacttemplates/https%253A%252F%252Fexamples.opentosca.org%252Fedmm%252Fnodetypeimplementations/Pet-Clinic-Configure-IA_w1/files/configure.sh
    extends: web_application
```

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
