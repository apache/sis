# GeoAPI snapshot

The Apache SIS source code repository has two branches, named `geoapi-3.1` and `geoapi-4.0`,
which depend on [GeoAPI](https://www.geoapi.org/) versions that are still in development.
Those GeoAPI versions are not deployed on Maven Central, because they are not yet officially approved OGC releases.
The Apache SIS branches that use those versions are never deployed on Maven Central neither.
Official Apache SIS releases are made from the `main` branch, which depends on the standard GeoAPI 3.0.2 release only.

The Apache SIS `geoapi-3.1` and `geoapi-4.0` branches are nevertheless useful for testing latest GeoAPI developments.
The implementation experience gained is used for adjusting the GeoAPI interfaces before submission as an OGC standard.
For making possible to compile against GeoAPI 3.1/4.0 without deployment on Maven Central, GeoAPI must be compiled locally.
This is done in this directory with a Git sub-module, which fetch GeoAPI at a specific commit identified by a SHA1.
The commit SHA1 is updated when needed for keeping Apache SIS `geoapi-xxx` branches in sync with the GeoAPI snapshot they implement.

## Git commands
Following command should be executed once after a fresh checkout of SIS `geoapi-3.1` or `geoapi-4.0` branch:

```bash
git submodule update --init
```

After above initialization, the usual `git pull` command should upgrade GeoAPI snapshot as well when needed.
If a Git error message said that the SHA1 is unknown, running `git fetch --recurse-submodules` first may help.

## Prerequisites
Maven must be available on the classpath.
The GeoAPI snapshot is built by a call to `mvn clean install`.
This call is done automatically by the Gradle build.