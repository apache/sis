=================================================
Welcome to Apache SIS <http://incubator.apache.org/sis/>
=================================================

Apache SIS(TM) is a spatial framework that enables better representation 
of coordinates for searching, data clustering, archiving, or any other 
relevant spatial needs.

Apache SIS is an effort undergoing incubation at The Apache Software Foundation (ASF), 
sponsored by the Apache Incubator PMC. Incubation is required of all newly accepted 
projects until a further review indicates that the infrastructure, communications, 
and decision making process have stabilized in a manner consistent with other successful 
ASF projects. While incubation status is not necessarily a reflection of the completeness 
or stability of the code, it does indicate that the project has yet to be fully endorsed 
by the ASF.

SIS is a project of the Apache Software Foundation <http://www.apache.org/>.

Apache SIS, SIS, Apache, the Apache feather logo, and the Apache SIS
project logo are trademarks of The Apache Software Foundation.

Getting Started
===============

SIS is based on Java 5 and uses the Maven 2 <http://maven.apache.org/>
build system. To build SIS, use the following command in this directory:

    mvn clean install

The build consists of a number of components, including a web-based application
file (WAR) you can use to try out SISfeatures. You can run it on top of Apache Tomcat like this:

    /usr/local/tomcat/bin/shutdown.sh
    mkdir /usr/local/sis
    cd /usr/local/sis
    cp -R /path/to/apache-sis-X.Y-src/sis-webapp/target/sis-webapp-X.Y.war ./
    cp -R /path/to/apache-sis-X.Y-src/sis-webapp/src/main/webapp/META-INF/context.xml ./sis.xml
    edit sis.xml (set the docBase to /usr/local/sis/sis-webapp-X.Y.war and the property 
          org.apache.sis.services.config.filePath to /usr/local/sis/sis-location-config.xml, 
          org.apache.sis.services.config.qIndexPath to /usr/local/sis/qtree, 
          org.apache.sis.services.config.geodataPath to /usr/local/sis/geodata)
    cp -R /path/to/apache-sis-X.Y-src/sis-webapp/src/main/resources/sis-location-config.xml ./
    edit sis-location-config.xml (add or delete GeoRSS URLs)
    ln -s /usr/local/sis/sis.xml /usr/local/tomcat/conf/Catalina/localhost/sis.xml
    /usr/local/tomcat/bin/startup.sh
    Visit http://localhost:8080/sis/demo.jsp and checkout the demo!    
    Try some queries:
    
    Bounding Box:
      0, 50, 50, 100
      
    Point Raidus:
      0, 80, 400km
      

License (see also LICENSE.txt)
==============================

Collective work: Copyright 2010 The Apache Software Foundation.

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Apache SIS includes a number of subcomponents with separate copyright
notices and license terms. Your use of these subcomponents is subject to
the terms and conditions of the licenses listed in the LICENSE.txt file.


Mailing Lists
=============

Discussion about SIS takes place on the following mailing lists:

    sis-user@incubator.apache.org    - About using SIS
    sis-dev@incubator.apache.org     - About developing SIS

Notification on all code changes are sent to the following mailing list:

    sis-commits@incubator.apache.org

The mailing lists are open to anyone and publicly archived.

You can subscribe the mailing lists by sending a message to
sis-<LIST>-subscribe@incubator.apache.org (for example sis-user-subscribe@...).
To unsubscribe, send a message to sis-<LIST>-unsubscribe@incubator.apache.org.
For more instructions, send a message to sis-<LIST>-help@incubator.apache.org.

Issue Tracker
=============

If you encounter errors in SIS or want to suggest an improvement or
a new feature, please visit the SIS issue tracker at
https://issues.apache.org/jira/browse/SIS There you can also find the
latest information on known issues and recent bug fixes and enhancements.
