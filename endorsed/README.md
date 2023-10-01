# Endorsed modules of Apache SIS

The `src` sub-directory contains modules included in Apache SIS releases.
Those modules have no prerequisites other than Java Development Kit (JDK)
and Gradle for building from the sources. The build command is:

    gradle assemble

The JAR files will be in the `build/libs/` directory.
See [the parent directory](../README.md) for more information.

## See also
The [optional](../optional) directory contains modules that are also part
of Apache SIS releases, but requiring prerequisites such as acceptance of
license agreements more restrictive than Apache 2 license.
