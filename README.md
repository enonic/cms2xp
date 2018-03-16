# Enonic CMS to XP migration tool
This repository contains the code for the CMS to XP migration tool.

The migration works from ```CMS 4.7``` to ```XP 6.x``` or higher.

<img align="left" alt="Enonic CMS" src="https://rawgithub.com/enonic/cms2xp/master/src/main/resources/icons/enonic-cms-logo.png" width="200">
<img align="right" style="margin-top:10px;" alt="Enonic XP" src="https://rawgithub.com/enonic/cms2xp/master/src/main/resources/icons/enonic-xp-shield-logo.svg" width="200">  
<br/><br/>

## Releases

| CMS2XP version | Required XP version | Download |
| -------------- | ------------------- | -------- |
| 0.7.5 | 6.0.0 | [Download 0.7.5 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.7.5/cms2xp-0.7.5.zip) |
| 0.8.0 | 6.0.0 | [Download 0.8.0 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.8.0/cms2xp-0.8.0.zip) |
| 0.8.1 | 6.0.0 | [Download 0.8.1 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.8.1/cms2xp-0.8.1.zip) |
| 0.9.0 | 6.9.0 | [Download 0.9.0 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.9.0/cms2xp-0.9.0.zip) |
| 0.9.1 | 6.9.0 | [Download 0.9.1 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.9.1/cms2xp-0.9.1.zip) |
| 0.9.2 | 6.9.0 | [Download 0.9.2 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.9.2/cms2xp-0.9.2.zip) |
| 0.10.0 | 6.9.0 | [Download 0.10.0 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.10.0/cms2xp-0.10.0.zip) |
| 0.10.1 | 6.9.0 | [Download 0.10.1 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.10.1/cms2xp-0.10.1.zip) |
| 0.10.2 | 6.9.0 | [Download 0.10.2 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.10.2/cms2xp-0.10.2.zip) |
| 0.10.3 | 6.9.0 | [Download 0.10.3 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.10.3/cms2xp-0.10.3.zip) |
| 0.10.4 | 6.9.0 | [Download 0.10.4 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.10.4/cms2xp-0.10.4.zip) |
| 0.10.5 | 6.9.0 | [Download 0.10.5 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.10.5/cms2xp-0.10.5.zip) |
| 0.10.6 | 6.9.0 | [Download 0.10.6 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.10.6/cms2xp-0.10.6.zip) |
| 0.10.7 | 6.9.0 | [Download 0.10.7 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.10.7/cms2xp-0.10.7.zip) |
| **0.10.8** | **6.9.0** | **[Download 0.10.8 distribution](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.10.8/cms2xp-0.10.8.zip)** |

## Download


[Download the latest version: 0.10.8](http://repo.enonic.com/public/com/enonic/tools/cms2xp/0.10.8/cms2xp-0.10.8.zip)

## Building

Before trying to build the project, you need to verify that the following
software is installed:

* Java 8 for building and running.
* Gradle 2.x build system.

Build all code and run all tests including integration tests:

    gradle build
   
A distribution zip, containing all the required files for running the tool, will be generated in ```build/distributions/cms2xp-0.10.6.zip```
   

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
    <exclude>
      <site>old_site</site>
      <site>42</site>
      <contentPath>/old_content</contentPath>
      <contentPath>/images/not_in_use</contentPath>
    </exclude>
    <include>
      <!-- specify either exclude or include, but not both at the same time -->
    </include>
  </source>

  <!-- Target (XP) -->
  <target>
    <exportDir>./export</exportDir>
    <userExportDir>./export_user</userExportDir>
    <applicationDir>./application</applicationDir>
    <applicationName>com.enonic.xp.app.myApp</applicationName>
    <applicationRepo>starter-vanilla</applicationRepo>
    <exportMenuMixin>true</exportMenuMixin>
    <moveHomeContentToSection>true</moveHomeContentToSection>
    <exportCmsKeyMixin>false</exportCmsKeyMixin>
    <exportCmsMenuKeyMixin>true</exportCmsMenuKeyMixin>
    <logFile>cms2xp.log</logFile>
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
| resourcesDir | Local file path to the resources directory of the CMS. | "./import/home/data/resources" |
| exclude/site | Site key or name in the CMS to be excluded from the export. Optional. | "old_Site", "33" |
| exclude/contentPath | Content path prefix in the CMS to exclude from the export. If the path corresponds with a category, none of its subcategories will be exported. Optional. | "MyContent/old/data" |
| include/site | Site key or name in the CMS to be included in the export. Optional. | "new_Site", "42" |
| include/contentPath | Content path prefix in the CMS to include in the export. If the path corresponds with a category, all its subcategories will be exported. Optional. | "MyContent/new/data" |

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
| ~~exportPublishDateMixin~~ | ~~Whether or not include the publish mixin in the application and content exported.~~ This option is deprecated. CMS publish fields are now exported to map with XP publish fields.  |  |
| exportMenuMixin | Whether or not convert CMS menu properties ('Menu name', 'Show in menu'), and include the menu mixin in the application. Default is "true" | "false" |
| moveHomeContentToSection | Move content that is published on a single section, under the content corresponding to the section in XP. Set to "false" to avoid moving the content. Default is "true" | "false" |
| exportCmsKeyMixin | Whether or not include the cmsContent mixin and add the content key (or category key) as a property in every content exported. Also the *Home* menu-item key where the content is published will be included. Default is "false" | "true" |
| exportCmsMenuKeyMixin | Whether or not include the cmsMenu mixin and add the menu item key as a property in every content exported. Default is "false" | "true" |
| logFile | Path of file where to write export log info. If not set log info will be sent to standard output. |  |



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


See XP documentation for more details about the [import](http://xp.readthedocs.io/en/latest/reference/toolbox/import.html) and [reprocess](http://xp.readthedocs.io/en/latest/reference/toolbox/reprocess.html) commands.


## Notes 

There are some remarks and limitations on the exported data:
- The source CMS data must be from Enonic CMS version 4.7. To convert from an older installation (e.g. 4.5.x, 4.4.x) it is necessary to first upgrade to the latest CMS 4.7 version.
- The Menu tree of the sites is exported maintaining the same structure. CMS page templates are converted to XP page-template, and portlets are converted to XP fragments. 
But the parts and pages generated in the app are placeholders. The xsl logic and datasources from CMS is currently **not** converted to XP.
- CMS section does not have an equivalent in XP. Content published in a section is referenced in a "sectionContents" property of the section content type. 
In addition, if ``<moveHomeContentToSection>`` is set to true in the cms2xp config, the published content will be placed under the section content.
- CMS global groups is currently converted to roles in XP. But due to roles not allowing to be member of other roles in XP, the memberships of global groups are skipped on the exported data.

For questions, feature requests, or reporting issues, please use the [Enonic Discuss forum](https://discuss.enonic.com/).