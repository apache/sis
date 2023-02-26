This is the root directory of NetBeans project configuration for Apache SIS.
This configuration is provided as a convenience for NetBeans users - this is
not a replacement for the Maven build.


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
