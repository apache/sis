Do not create an "epsg" sub-package (except for test or maintainance tasks only)
because the "org.apache.sis.referencing.factory.sql.epsg" package name is owned
by the "org.apache.sis.non-free:sis-epsg" module and we should not put anything
in a package owned by another module.

We make this separation for licensing reason since the EPSG geodetic dataset is
subject to different terms of use than Apache 2 license.
