# Location of EPSG resources

There is no `epsg` sub-package in this module because such package is defined
in the `optional/src/org.apache.sis.referencing.factory.sql.epsg` module.
The current directory nevertheless contains EPSG-related classes and resources,
which are prefixed by "EPSG" instead of being placed in the `epsg` sub-package.
This mix of two conventions is for licensing reasons: use of `epsg` sub-package
requires acceptance of EPSG terms of use, while the EPSG-related resources in
this directory are under Apache 2 license and can be used without the `epsg`
sub-package if the user got EPSG scripts in another way.
