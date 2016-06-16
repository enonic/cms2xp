# Enonic CMS to XP migration tool
<img align="right" alt="Enonic Logo" src="https://github.com/enonic/cms2xp/blob/master/src/main/resources/icons/enonic-xp-shield-logo.svg" width="128">

This repository contains the code for the CMS to XP migration tool.

## Building

Before trying to build the project, you need to verify that the following
software is installed:

* Java 8 for building and running.
* Gradle 2.x build system.

Build all code and run all tests including integration tests:

    gradle build
   
A distribution zip, containing all the required files for running the tool, will be generated in ```build/distributions/cms2xp-0.5.0.zip```
   

## Running

There is just one script command to execute the CMS to XP migration tool. 
The script takes one parameter: the path to a *config.xml* file.
 
Note that the tool must be executed from a computer with file access to the CMS *blobs* and *resources* directories, normally located in the *CMS_HOME* directory of the CMS installation.
It also needs direct connection access to the database where the CMS data is stored. The CMS itself does not need to be running.

To run the script, extract the distribution zip file to an empty directory and execute the script from the command line:

- ```./bin/cms2xp <path_to_config.xml> ``` - (on Linux / OS X)
- ```bin\cms2xp.bat <path_to_config.xml> ``` - (on Windows)

If the config.xml parameter is omitted, it will print out an example of config XML. See also Configuration below.

Before running this tool, the **SQL driver** to the CMS database must be copied to the ```lib``` directory. 
This is normally a ```.jar``` file specific to a database version, like  ```postgresql-9.3-1104.jdbc4.jar```.

### Configuration

The configuration is divided in two parts:

- ```<source>```: parameters to access the data from the CMS.
- ```<target>```: parameters to specify the output locations and format for the generated XP data.

Example of config.xml:
```XML
<config>
  <!-- Source (CMS) -->
  <source>
    <jdbcDriver>org.h2.Driver</jdbcDriver>
    <jdbcUrl>jdbc:h2:~/cms-home/data/h2/cms</jdbcUrl>
    <jdbcUser>sa</jdbcUser>
    <jdbcPassword>***</jdbcPassword>
    <blobStoreDir>./cms-home/data/blobs</blobStoreDir>
    <resourcesDir>./cms-home/data/resources</resourcesDir>
  </source>

  <!-- Target (XP) -->
  <target>
    <exportDir>./export</exportDir>
    <userExportDir>./export_user</userExportDir>
    <applicationDir>./application</applicationDir>
    <applicationName>com.enonic.xp.app.myApp</applicationName>
    <applicationRepo>starter-vanilla</applicationRepo>
    <exportPublishDateMixin>false</exportPublishDateMixin>
    <exportMenuMixin>true</exportMenuMixin>
    <moveHomeContentToSection>true</moveHomeContentToSection>
  </target>
</config>
```

#### Source parameters

The source parameters include the details necessary for connecting to the CMS database, and the path to the blobs and resources from the CMS.

| Parameter | Description | Example |
| --- | --- | --- |
| jdbcDriver | JDBC driver class. | "org.postgresql.Driver" |
| jdbcUrl | JDBC URL pointing to the CMS database. | "jdbc:postgresql://localhost:5432/customer" |
| jdbcUser | Username for the CMS database. | "db_user" |
| jdbcPassword | Password for the CMS database. | "password123" |
| blobStoreDir | Local file path to the blobs directory of the CMS. | "./cms-home/data/blobs" |
| resourcesDir | Local file path to the resources directory of the CMS. | "./import/home/data/resources" |

#### Target parameters

The target parameters include the desired path where to generate XP export directories. 
Two different exports will be generated, one with CMS content and the other with user store data.

There are also parameters to specify path and name for generating an _XP application_. This application will contain *content types*, *mixins*, *parts* and *pages*, converted from CMS.
There are also some optional switches for altering the output.

| Parameter | Description | Example |
| --- | --- | --- |
| exportDir | Directory path where to generate the XP export for content. | "./contentExport" |
| userExportDir | Directory path where to generate the XP export for user stores and principals. | "./userExport" |
| applicationDir | Directory path where to generate the XP application. | "./myapp" |
| applicationName | Name for the generated XP application. | "com.acme.myapp" |
| applicationRepo | Name of an XP starter app to be used as a skeleton for the generated app. Default is "starter-vanilla". | "starter-vanilla" |
| exportPublishDateMixin | Whether or not include the publish mixin in the application and content exported. Default is "false" | "true" |
| exportMenuMixin | Whether or not convert CMS menu properties ('Menu name', 'Show in menu'), and include the menu mixin in the application. Default is "true" | "false" |
| moveHomeContentToSection | Move content that is published on a single section, under the content corresponding to the section in XP. Set to "false" to avoid moving the content. Default is "true" | "false" |



## How to use

The process of migrating CMS data to XP includes some additional actions after running the *CMS2XP* tool.

These are the suggested steps for migrating CMS data:

1. Create and fill in a ```config.xml``` file according to the details above.
2. Execute the cms2xp script: 

    ```bash
    ./bin/cms2xp config.xml
    ```

3. If all goes well 3 directories will be created with: content data, user store data, and an XP application.
4. Build and deploy the application: 

    ```bash
    <applicationDir>$ gradle clean deploy
    ```

5. Move the content export to XP export directory:

    ```bash
    mv <exportDir> $XP_HOME/data/export/cms_content
    ```

6. Move the user store export to XP export directory:  

    ```bash
    mv <userExportDir> $XP_HOME/data/export/cms_users
    ```

7. Import user store export into XP: 

    ```bash
    $XP/toolbox/toolbox.sh import -a su:pwd -t system-repo:master:/identity -s cms_users/identity
    ```

8. Import content export into XP: 

    ```bash
    $XP/toolbox/toolbox.sh import -a su:pwd -t cms-repo:draft:/content -s cms_content/content
    ```

9. Reprocess the imported content to update the metadata in media content: 

    ```bash
    $XP/toolbox/toolbox.sh reprocess -a su:pwd -s draft:/
    ```


See XP documentation for more details about the [import](http://xp.readthedocs.io/en/latest/reference/toolbox/import.html) and [reprocess](http://xp.readthedocs.io/en/latest/reference/toolbox/index.html) commands.


## Tips 


## TODO

