# Class ObjectIdentification

The `org.apache.sis.xml.bind.gco` package conceptually defines two complementary objects:
`ObjectIdentification` and `ObjectReference`. However, only the latter is defined by a Java class,
because the former is implicitly defined by the classes in the public API of SIS.
This page contains the information that we would have put in `ObjectIdentification` Javadoc
if such class existed.

## Overview

The `gco:ObjectIdentification` XML attribute group is implicitly included
by all metadata types defined in the `org.apache.sis.metadata.iso` packages.
The attributes of interest defined in this group are `id` and `uuid`.

This `gco:ObjectIdentification` group is complementary to `gco:ObjectReference`,
which defines the `xlink` and `uuidref` attributes to be supported by all metadata
wrappers in the private `org.apache.sis.xml.bind.metadata` package and sub-packages.

## Difference between `gml:id` and `gco:uuid`

GML identifiers have the following properties:

* `id` is a standard **GML** attribute available on every object-with-identity.
  It has type=`"xs:ID"` - i.e. it is a fragment identifier, unique within document scope only,
  for internal cross-references. It is not useful by itself as a persistent unique identifier.
* `uuid` is an optional attribute available on every object-with-identity, provided in the
  **GCO** schemas that implement ISO 19115 in XML. May be used as a persistent unique identifier,
  but only available within GCO context.

However, according the [OGC/ISO schema](https://www.isotc211.org/2005/gco/gcoBase.xsd),
those identifiers seem to be defined in the GCO schema.
