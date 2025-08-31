# Manual installation of EPSG data

This is the directory where EPSG data can be placed.
Those data are not commited in the Apache SIS source code repository because
they are licensed under [EPSG terms of use](https://epsg.org/terms-of-use.html).
For including the EPSG data in the `org.apache.sis.referencing.epsg` module,
the following commands must be executed manually in a separated directory:

```shell
svn checkout https://svn.apache.org/repos/asf/sis/data/non-free/
cd non-free/EPSG
export EPSG_DIR=$PWD
```

Then, the following commands (or something equivalent) should be executed
with the directory of this `README.md` file as the current directory:

```shell
ln --symbolic $EPSG_DIR/LICENSE.* .
ln --symbolic $EPSG_DIR/*.sql .
```
