This directory contains the targets of symbolic links defined in sub-modules.

  * site-module-group.css   target of  "<group>/src/site/resources/css/site.css"  links.
  * site-module.css         target of  "<group>/<module>/src/site/resources/css/site.css"  links.

The above-listed files are used for rendering the Maven-generated web site.
Editing the files in this directory ensures that the changes are applied uniformly in all modules.
Note that the URLs in those files shall be written as if the files were located in the directory
of the link source, not in this directory.
