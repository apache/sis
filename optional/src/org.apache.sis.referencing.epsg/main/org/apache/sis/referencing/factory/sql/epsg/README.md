# Manual installation of EPSG data

This is the directory where EPSG data can be placed.
Those data are not commited in the Apache SIS source code repository because
they are licensed under [EPSG terms of use](https://epsg.org/terms-of-use.html).
For including the EPSG data in the `org.apache.sis.referencing.epsg` artifact,
the following commands must be executed manually with this directory as the
current directory:

```shell
# Execute the following in a separated directory.
svn checkout https://svn.apache.org/repos/asf/sis/data/non-free/
cd non-free
export NON_FREE_DIR=`pwd`

# Execute the following in the directory of this `README.md` file.
ln --symbolic $NON_FREE_DIR/EPSG/LICENSE.txt
ln --symbolic $NON_FREE_DIR/EPSG/LICENSE.html
ln --symbolic $NON_FREE_DIR/EPSG/Tables.sql
ln --symbolic $NON_FREE_DIR/EPSG/Data.sql
ln --symbolic $NON_FREE_DIR/EPSG/FKeys.sql
cd -
```
