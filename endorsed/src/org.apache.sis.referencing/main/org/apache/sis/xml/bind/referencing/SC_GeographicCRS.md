The `SC_GeographicCRS` adapter is defined in the `org.apache.sis.referencing.crs`
package because it needs access to `DefaultGeodeticCRS` package-private methods.
Note also that `GeographicCRS` does not exist in GML, so this is a special case
anyway.
