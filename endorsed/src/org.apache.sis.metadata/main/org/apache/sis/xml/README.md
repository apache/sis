# Syntax of RenameOnImport/Export files

**WARNING: the syntax documented in this page is not committed API and may change in any future SIS version.**

This package provides two files in the `resources/org/apache/sis/xml/` folder:
`RenameOnImport.lst` and `RenameOnExport.lst`.
Those files are used by `TransformingReader.java` and `TransformingWriter.java` respectively
for converting XML namespaces between new specifications (ISO 19115-3:2016 and ISO 19115-4) and old specifications
(ISO 19139:2007 and ISO 19115-2:2009).
The JAXB annotations in Apache SIS use the newer specifications.
The `Rename*.lst` files are needed only when reading or writing to older specifications.
Those files are used for performing the work of a lightweight XSLT engine.
Both share the same syntax:

* Lines starting with "*" character specify the legacy namespaces containing elements to rename.
* Lines with zero-space indentation are namespace URIs.
* Lines with one-space  indentation are XML type names.
* Lines with two-spaces indentation are property names.
* The "/" character in "_before_/_after_" means that a property name needs to be changed.
  _Before_ is the name before the renaming process and _after_ is the name after the renaming process.
* The ":" character in "_Child_ : _Parent_" means that a subclass inherits all properties from a parent class.
  The _parent_ must be defined before the _child_ (no forward references).
  This is used for avoiding to repeat all super-class properties in sub-classes.
  It has no other meaning, i.e. the class hierarchy is not retained at runtime.
* The "!" character in "_Class_ !_reason_" skips the association of current namespace to that class
  (but namespace will still be associated to the properties). _Reason_ is a free text.
  This is used with deprecated classes that do not exist anymore in the new namespace
  (often because the class has been renamed).

For example, the following snippet from `RenameOnImport.lst` declares that the `Citation.title`,
`Citation.edition` and `Address.country` properties are defined in the **`cit`** namespace,
while the `Extent.description` property is defined in the **`gex`** namespace.
Those information are required when reading a file encoded by the old standards
because almost all properties where in the single `gmd` namespace.
Properties not listed will have their namespace unchanged (e.g. still in the old `gmd` namespace).
Classes that did not existed in old standard should not be listed.

```
# Legacy namespace containing elements to rename:
* http://www.isotc211.org/2005/gmd

# New namespaces:
http://standards.iso.org/iso/19115/-3/**cit**/1.0
 CI_Citation
  title
  edition
 CI_Address
  country
http://standards.iso.org/iso/19115/-3/**gex**/1.0
 EX_Extent
  description
```

In general those information are used for converting only the *namespaces*, the class and property names are unchanged.
But in the special case where the "_before_/_after_" syntax is used, then class and/or property names are also changed.
In the following example, the `DQ_Scope` type is renamed `MD_Scope` but with attributes of the same name.
Then the `Georectified.centerPoint` attribute (from the old standard)
is renamed as `Georectified.centrePoint` (new standard).

```
http://standards.iso.org/iso/19115/-3/**mcc**/1.0
 DQ_Scope/MD_Scope
  extent
  level
  levelDescription
http://standards.iso.org/iso/19115/-3/**msr**/1.0
 MI_Georectified
  centerPoint/centrePoint
```

Conversely, when writing a file, some additional renaming can be applied *after* the namespaces have been renamed to `gmd`.
The following snippet from `RenameOnExport.lst` performs the converse of the property renaming shown in previous example:

```
http://www.isotc211.org/2005/gmd
 MD_Georectified
  centrePoint/centerPoint
```
