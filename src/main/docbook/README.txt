This is the root directory of the developer guide in DocBook format.
A documentation about the DocBook format is available on-line there:

    http://www.docbook.org/tdg5/en/html

Direct link to a frequently used chapter:

    http://www.docbook.org/tdg5/en/html/ch02.html#s.inline

XHTML pages are generated automatically from the docbook files when
Maven build the web site. But it is also possible to generate only
the document (which is much faster than generating the whole site)
by executing the following command-line from the SIS project root:

   touch src/main/docbook/fr.xml
   mvn docbkx:generate-xhtml --non-recursive
   ln src/site/resources/book/book.css target/site/book/

The result will be placed in the target/site/book directory.
