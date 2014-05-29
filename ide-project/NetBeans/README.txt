This is the root directory of NetBeans project configuration for Apache SIS.
This configuration is provided as a convenience for NetBeans users - this is
not a replacement for the Maven build.


==============================================================================
Installation
==============================================================================
The configuration provided in this directory requires a checkout of GeoAPI
source code. The recommended installation steps is as below (from the root
directory of all SIS-related projects):

  mkdir SIS
  svn checkout http://svn.apache.org/repos/asf/sis/branches/JDK7 SIS/JDK7
  mkdir GeoAPI
  svn checkout http://svn.code.sf.net/p/geoapi/code/trunk GeoAPI/trunk

Above commands should create the following directory structure:

  +-- GeoAPI
  |   +-- trunk
  |       +-- README.txt
  |       +-- etc...
  +-- SIS
      +-- JDK7
          +-- README
          +-- etc...

If a different directory layout is desired, this is possible provided that
the following line is added to "nbproject/private/private.properties" file:

  project.GeoAPI = <path to your GeoAPI checkout>/ide-project/NetBeans




==============================================================================
Recommendations for NetBeans project configuration changes
==============================================================================
There is 3 important files that should be edited BY HAND for preserving user-
neutral configuration:

build.xml
---------
This Ant file contains tasks executed in addition to the default NetBeans
tasks during the build process, for:

  - Copying the resources compiled by Maven.


nbproject/project.properties
----------------------------
Contains most of the project configuration. The content of this file is
modified by the NetBeans "Project properties" panel. PLEASE REVIEW MANUALLY
BEFORE COMMITTING ANY CHANGE. Please preserve the formatting for easier
reading. Ensure that all directories are relative to a variable.
If some user-specific properties are desired (e.g. absolute paths),
they can be declared in the "nbproject/private/private.properties" file.


nbproject/project.xml
---------------------
Edited together with nbproject/project.properties for declaring the source
directories.
