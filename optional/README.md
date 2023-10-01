# Optional modules of Apache SIS

The `src` sub-directory contains modules included in Apache SIS releases,
but which are buildable or executable only if some dependencies are added
by the users themselves. Those dependencies are not distributed with Apache
SIS releases because they are under more restrictive licenses than Apache 2.
If those dependencies are not provided, Apache SIS [endorsed](../endorsed)
modules will still work but without Graphical User Interface (GUI).

## Configuration

Paths to dependencies are specified by the following environment variables.
After those variables are set, `gradle assemble` can be executed as usual.

### JavaFX

JavaFX is licensed under GPL with classpath exception. The JavaFX SDK can be
downloaded from the [OpenJFX web site](https://openjfx.io/) and installed in
any directory. The path to the directory containing the JAR files is specified
in Unix shell as below (change the `/usr/lib/jvm/openjfx` path as needed):

    export PATH_TO_FX=/usr/lib/jvm/openjfx


## Running the JavaFX application

A ZIP file containing a subset of Apache SIS modules and dependencies is built
in the `build/bundle/` sub-directory. That ZIP file can be unzipped in any directory.
The application is launched by running the `./bin/sisfx` script.
