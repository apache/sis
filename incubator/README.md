# Incubator modules of Apache SIS

The `src` sub-directory contains experimental modules that are not yet included in
Apache SIS releases. Those JAR files are not deployed on Maven Central repository.
Using the JAR files requires compiling the `main` branch from the sources:

    gradle assemble

The JAR files will be in the `build/libs/` directory. Their API may change without
warning at any time, until the modules move to the [endorsed](../endorsed) directory.
