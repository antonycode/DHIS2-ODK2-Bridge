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

**note** Before running this bridge, make sure you have a DHIS2 server and an ODK 2 Sync Endpoint server.

Run CLI jar with path to your configuration file as the first argument. The configuration should be tuned to match settings of your DHIS2 and Sync Endpoint server.

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

See the *Usage with Malaria App* section below for an example.

## Limitation

 - Sync Endpoint tables and DHIS2 data sets must have the same names
 - Columns of Sync Endpoint tables and DHIS2 data elements must have the same names
 - Sync Endpoint tables must have the columns `period` and `orgUnit`, 
 these columns are necessary to correctly place and locate data values
 - Sync Endpoint tables to be transfered to DHIS2 data sets must have the columns `period` and `orgUnit`. Sync Endpoint tables to be transfered to DHIS2 tracked entities must have the columns `orgUnit` and `trackedEntityInstance`. 
 - DHIS2 servers must have the tracked entity attribute `SyncEndpointRowId` for this tool to copy data to a tracked entity.
 - Data conflict (on either DHIS2 or Sync Endpoint) must be resolved prior to running this utility, 
 or choose to override with available value
 - DHIS2 asynchronous data import is not supported
 - Data sets, data elements, and tables, must be created prior to using this utility

## Note

This tool was tested with a DHIS2 server setup using Docker Compose scripts provided by 
[DHIS2 API Tests](https://github.com/dhis2/api-tests). A standalone installation of DHIS2 would work as well.

## Usage with Malaria App

Make sure you have the prerequisite softwares before proceding.
In addition to the ones in the prerequisite, you will also need
[App Designer](http://opendatakit-dev.cs.washington.edu/2_0_tools/download), [ODK Services](http://opendatakit-dev.cs.washington.edu/2_0_tools/download), [ODK Tables](http://opendatakit-dev.cs.washington.edu/2_0_tools/download)

1. Setup
   1. Deploy a DHIS2 server
   2. Deploy a Sync Endpoint server
2. Configure the DHIS2 instance
   1. Configure your organization units as following:
   ```
   OU
   ├── Province 1
   │   ├── District 1
   │   │   ├── Catchment 1
   │   │   └── Catchment 6
   │   └── District 2
   │       ├── Catchment 2
   │       └── Catchment 7
   └── Province 2
       ├── District 3
       │   └── Catchment 3
       └── District 4
           ├── Catchment 4
           └── Catchment 5
   ```
   2. Configure these tracked entity attributes:
      - SyncEndpointRowId - Text
      - comment - Long Text
      - data_selected - Text
      - exclude - Integer
      - head_name - Text
      - house_number - Integer
      - location_accuracy - Number
      - location_altitude - Number
      - location_latitude - Number
      - location_longitude - Number
      - questionnaire_status - Text
      - random_number - Number
      - sample_frame - Integer
      - selected - Integer
      - valid - Integer  

      All of them should have Aggregation Type set of None and set to display in list without program.
   3. Configure a tracked entity called census
3. Configure the Sync Endpoint instance
   1. Extract `malaria-app.zip` located in `malaria-app` directory to your app-designer's `app/config` directory replacing it. 
   2. Use `grunt adbpush` to upload the malaria app to your Android phone
   3. Configure ODK Services to use your Sync Endpoint instance
   4. Reset app server using ODK Services 
4. Enter data
   1. Enter data using ODK Tables
   2. Sync to Sync Endpoint using ODK Services
5. Use this bridge
   1. `example.properties` is configured with sample values that should work with the malaria demo. Fill in DHIS2 and Sync Endpoint server detail in the file 
   2. Run the bridge using this configuration file, refer to [above](#run) for detail.
