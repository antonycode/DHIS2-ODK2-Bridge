# DHIS2-ODK2-Bridge

This utility copies data between a DHIS2 server and a Sync Endpoint server.

## Prerequisites

  - [DHIS2](https://www.dhis2.org/documentation)
  - [Sync Endpoint](https://github.com/opendatakit/sync-endpoint-default-setup)
  - [Java 8](http://openjdk.java.net/) or [Docker](https://www.docker.com/community-edition)

#### Prerequisites for Building 

 - for building a JAR, 
   - [Git](https://git-scm.com/)
   - [Java 8 JDK](http://openjdk.java.net/)
   - [Maven 3](https://maven.apache.org/)
 - for building a Docker Image
   - [Docker](https://www.docker.com/community-edition) 17.06.1 or newer

## Build
 
#### JAR

  - Download this repository, [DHIS2-ODK2-Bridge.zip](https://github.com/uw-ictd/DHIS2-ODK2-Bridge/archive/master.zip)  
  Or clone it, `git clone https://github.com/uw-ictd/DHIS2-ODK2-Bridge.git`
  - Run `mvn validate package` in project root directory
  - After the command returns, the JAR will be available under `dhis2-odk2-bridge-cli/target`

#### or build as a Docker Image

  - `docker build --pull -t odk/dhis2-odk2-bridge https://github.com/uw-ictd/DHIS2-ODK2-Bridge.git`
  
## Run

Run CLI jar with path to your configuration file as the first argument.

For example, to run this tool with a configuration file located at `$HOME/configuration.properties`
```sh
java -jar dhis2-odk2-bridge-cli-1.0-SNAPSHOT-jar-with-dependencies.jar $HOME/configuration.properties
```

Or using Docker, 
```sh
docker run \
  -v $HOME/configuration.properties:/config \
  odk/dhis2-odk2-bridge /config
```

See [example.properties](example.properties) for detail on the configuration file. 

This bridge utility only performs a one time transfer of data. If you wish to periodically transfer
data, you will need to setup the scheduling using other tools such as [cron](https://wiki.archlinux.org/index.php/cron).

## Limitation

 - Sync Endpoint tables and DHIS2 data sets must have the same names
 - Columns of Sync Endpoint tables and DHIS2 data elements must have the same names
 - Sync Endpoint tables must have the columns `period` and `orgUnit`, 
 these columns are necessary to correctly place and locate data values
 - Data conflict (on either DHIS2 or Sync Endpoint) must be resolved prior to running this utility, 
 or choose to override with available value
 - DHIS2 asynchronous data import is not supported
 - Data sets, data elements, and tables, must be created prior to using this utility

## Note

This tool was tested with a DHIS2 server setup using Docker Compose scripts provided by 
[DHIS2 API Tests](https://github.com/dhis2/api-tests). A standalone installation of DHIS2 would work as well.
