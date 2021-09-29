This module requires JavaFX, which is not distributed with Apache SIS
for licensing reasons. For enabling this module, download and install
JavaFX manually then set the following environment variable (replace
"/path/to/my/install/" by the actual JavaFX installation directory):

    export PATH_TO_FX=/path/to/my/install/javafx-sdk/lib

Above is sufficient for compiling with Maven. For editing with the
NetBeans Ant project, open the following file:

    ide-project/NetBeans/nbproject/private/private.properties

And add the following lines:

    javac.source     = 17
    javac.target     = 17
    javac.modulepath = /path/to/my/install/javafx-sdk/lib
    javafx.options   = --add-modules javafx.graphics,javafx.controls,javafx.web
    src.javafx.dir   = ${project.root}/application/sis-javafx/src/main/java
