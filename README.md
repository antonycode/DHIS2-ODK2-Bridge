# DHIS2-ODK2-Bridge

This utility copies data between a DHIS2 server and a Sync Endpoint server.

## Build
 
#### Jar

  - Run `mvn validate package` in project root directory

#### Docker Image

  - `docker build --pull -t odk/dhis2-sync .`
  
## Run

Run CLI jar with path to your configuration file as the first argument.

For example, 
```
java -jar dhis2-odk2-bridge-cli-1.0-SNAPSHOT-jar-with-dependencies.jar $HOME/configuration.properties
```

See [example.properties](example.properties) for detail on the configuration file. 

## Limitation

 - Sync Endpoint tables and DHIS2 data sets must have the same names
 - Columns of Sync Endpoint tables and DHIS2 data elements must have the same names
 - Sync Endpoint tables must have the columns `period` and `orgUnit`, 
 these columns are necessary to correctly place and locate data values
 - Data conflict (on either DHIS2 or Sync Endpoint) must be resolved prior to running this utility, 
 or choose to override with available value
 - Asynchronous data import is not supported
 - Data sets, data elements, and tables, must be created prior to using this utility