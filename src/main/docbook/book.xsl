<?xml version="1.0" encoding="UTF-8"?>
<!-- ============================================================
     Docbook customization

     This file solves the issue of docbook using the <h2> elements
     both for chapter and for the first level of sections heading.
     ============================================================= -->
<xsl:stylesheet xmlns        = "http://www.w3.org/1999/xhtml"
                xmlns:xsl    = "http://www.w3.org/1999/XSL/Transform"
                xmlns:d      = "http://docbook.org/ns/docbook"
                xmlns:db     = "http://docbook.org/ns/docbook"
                xmlns:ng     = "http://docbook.org/docbook-ng"
                xmlns:exsl   = "http://exslt.org/common"
                xmlns:exslt  = "http://exslt.org/common"
                xmlns:xslthl = "http://xslthl.sf.net"
                exclude-result-prefixes="d db ng exsl exslt xslthl" version="1.0">

  <!-- Symbolic URN specific to docbkx-maven-plugin -->
  <xsl:import href="urn:docbkx:stylesheet/docbook.xsl"/>
  <xsl:import href="urn:docbkx:stylesheet/highlight.xsl"/>

  <!-- Parameters from http://docbook.sourceforge.net/release/xsl/current/doc/fo/  -->
  <xsl:param name="section.autolabel" select="1"/>
  <xsl:param name="section.label.includes.component.label" select="1"/>

  <!--
       Following is copied from "http://docbook.sourceforge.net/release/xsl/current/xhtml/component.xsl".
       Only the lines identified by a comment have been modified.  This file is used only for building
       the web site and is not included in the SIS distributions.
  -->
  <xsl:template name="component.title">
    <xsl:param name="node" select="."/>
    <xsl:variable name="level">
      <xsl:choose>
        <xsl:when test="ancestor::section">
          <xsl:value-of select="count(ancestor::section)+1"/>
        </xsl:when>
        <xsl:when test="ancestor::sect5">6</xsl:when>
        <xsl:when test="ancestor::sect4">5</xsl:when>
        <xsl:when test="ancestor::sect3">4</xsl:when>
        <xsl:when test="ancestor::sect2">3</xsl:when>
        <xsl:when test="ancestor::sect1">2</xsl:when>
        <xsl:when test="ancestor::sect0">1</xsl:when>   <!-- Added -->
        <xsl:otherwise>0</xsl:otherwise>  <!-- Was 1, changed to 0 -->
      </xsl:choose>
    </xsl:variable>
    <xsl:element name="h{$level+1}" namespace="http://www.w3.org/1999/xhtml">
      <xsl:attribute name="class">title</xsl:attribute>
      <xsl:if test="$generate.id.attributes = 0">
        <xsl:call-template name="anchor">
	  <xsl:with-param name="node" select="$node"/>
          <xsl:with-param name="conditional" select="0"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:apply-templates select="$node" mode="object.title.markup">
        <xsl:with-param name="allow-anchors" select="1"/>
      </xsl:apply-templates>
    </xsl:element>
  </xsl:template>


  <!--
       Syntax highlighting partially copied from the "docbook/xhtml-1_1/highlight.xsl" file
       in the "net/sf/docbook/docbook-xsl/docbook-xsl-ns-resources.zip" archive, then edited.
  -->
  <xsl:template match="xslthl:string" mode="xslthl">
    <span class="hl-string">
      <xsl:apply-templates mode="xslthl"/>
    </span>
  </xsl:template>
  <xsl:template match="xslthl:annotation" mode="xslthl">
    <span class="hl-annotation">
      <xsl:apply-templates mode="xslthl"/>
    </span>
  </xsl:template>
  <xsl:template match="xslthl:comment" mode="xslthl">
    <span class="hl-comment">
      <xsl:apply-templates mode="xslthl"/>
    </span>
  </xsl:template>
  <xsl:template match="xslthl:doccomment|xslthl:doctype" mode="xslthl">
    <span class="hl-doccomment">
      <xsl:apply-templates mode="xslthl"/>
    </span>
  </xsl:template>


  <!--
       Map "OGC", "GeoAPI" and "SIS" roles to CSS styles
       for <classname>, <function> and <literal> elements.
  -->
  <xsl:template match="d:classname[@role = 'OGC']"    mode="class.value"> <xsl:value-of select="'OGC'"   /> </xsl:template>
  <xsl:template match="d:classname[@role = 'GeoAPI']" mode="class.value"> <xsl:value-of select="'GeoAPI'"/> </xsl:template>
  <xsl:template match="d:classname[@role = 'SIS']"    mode="class.value"> <xsl:value-of select="'SIS'"   /> </xsl:template>
  <xsl:template match= "d:function[@role = 'OGC']"    mode="class.value"> <xsl:value-of select="'OGC'"   /> </xsl:template>
  <xsl:template match= "d:function[@role = 'GeoAPI']" mode="class.value"> <xsl:value-of select="'GeoAPI'"/> </xsl:template>
  <xsl:template match= "d:function[@role = 'SIS']"    mode="class.value"> <xsl:value-of select="'SIS'"   /> </xsl:template>
  <xsl:template match=  "d:literal[@role = 'OGC']"    mode="class.value"> <xsl:value-of select="'OGC'"   /> </xsl:template>
  <xsl:template match=  "d:literal[@role = 'GeoAPI']" mode="class.value"> <xsl:value-of select="'GeoAPI'"/> </xsl:template>
  <xsl:template match=  "d:literal[@role = 'SIS']"    mode="class.value"> <xsl:value-of select="'SIS'"   /> </xsl:template>

</xsl:stylesheet>
