<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:test="http://www.jenitennison.com/xslt/unit-test"
                exclude-result-prefixes="xs test"
                xmlns:pat="http://www.lmnl.org/schema/pattern">
  
<xsl:template match="/">
  <xsl:call-template name="pat:compile-creole">
    <xsl:with-param name="schema" select="." />
  </xsl:call-template>
</xsl:template>  
  
<xsl:variable name="built-in-patterns" as="element()+">
  <pat:define name="#notAllowed" hash="#notAllowed" 
              nullable="false" allows-text="false" 
              allows-annotations="false" only-annotations="false">
    <pat:notAllowed />
  </pat:define>
  <pat:define name="#empty" hash="#empty" 
              nullable="true" allows-text="false" 
              allows-annotations="false" only-annotations="false">
    <pat:empty />
  </pat:define>
  <pat:define name="#text" hash="#text" 
              nullable="true" allows-text="true" 
              allows-annotations="false" only-annotations="false">
    <pat:text />
  </pat:define>
  <pat:define name="#anyName" hash="#anyName" 
              nullable="false" allows-text="false" 
              allows-annotations="false" only-annotations="false">
    <pat:anyName />
  </pat:define>
  <pat:define name="#anyAtoms" hash="#anyAtoms"
              nullable="true" allows-text="false"
              allows-annotations="false" only-annotations="false">
    <pat:choice>
      <pat:ref name="#oneOrMoreAtoms" />
      <pat:ref name="#empty" />
    </pat:choice>
  </pat:define>
  <pat:define name="#oneOrMoreAtoms" hash="#oneOrMoreAtoms"
              nullable="false" allows-text="false"
              allows-annotations="false" only-annotations="false">
    <pat:oneOrMore>
      <pat:ref name="#anyAtom" />
    </pat:oneOrMore>
  </pat:define>
  <pat:define name="#anyAtom" hash="#anyAtom"
              nullable="false" allows-text="false" 
              allows-annotations="false" only-annotations="false">
    <pat:atom>
      <pat:ref name="#anyName" />
      <pat:ref name="#anyAnnotations" />
    </pat:atom>
  </pat:define>
  <pat:define name="#anyAnnotations" hash="#anyAnnotations"
              nullable="true" allows-text="false"
              allows-annotations="true" only-annotations="true">
    <pat:choice>
      <pat:ref name="#oneOrMoreAnnotations" />
      <pat:ref name="#empty" />
    </pat:choice>
  </pat:define>
  <pat:define name="#oneOrMoreAnnotations" hash="#oneOrMoreAnnotations"
              nullable="false" allows-text="false" 
              allows-annotations="true" only-annotations="true">
    <pat:oneOrMore>
      <pat:ref name="#anyAnnotation" />
    </pat:oneOrMore>
  </pat:define>
  <pat:define name="#anyAnnotation" hash="#anyAnnotation"
              nullable="false" allows-text="false"
              allows-annotations="true" only-annotations="true">
    <pat:annotation>
      <pat:ref name="#anyName" />
      <pat:ref name="#any" />
    </pat:annotation>
  </pat:define>
  <pat:define name="#any" hash="#any"
              nullable="true" allows-text="true"
              allows-annotations="true" only-annotations="false">
    <pat:interleave>
      <pat:ref name="#anyAnnotations" />
      <pat:ref name="#limen" />
    </pat:interleave>
  </pat:define>
  <pat:define name="#limen" hash="#limen"
              nullable="true" allows-text="true"
              allows-annotations="false" only-annotations="false">
    <pat:interleave>
      <pat:ref name="#anyContent" />
      <pat:ref name="#anyRanges" />
    </pat:interleave>
  </pat:define>
  <pat:define name="#anyContent" hash="#anyContent"
              nullable="true" allows-text="true" 
              allows-annotations="false" only-annotations="false">
    <pat:interleave>
      <pat:ref name="#text" />
      <pat:ref name="#anyAtoms" />
    </pat:interleave>
  </pat:define>
  <pat:define name="#anyRanges" hash="#anyRanges"
              nullable="true" allows-text="false"
              allows-annotations="false" only-annotations="false">
    <pat:choice>
      <pat:ref name="#oneOrMoreRanges" />
      <pat:ref name="#empty" />
    </pat:choice>
  </pat:define>
  <pat:define name="#oneOrMoreRanges" hash="#oneOrMoreRanges"
              nullable="false" allows-text="false"
              allows-annotations="false" only-annotations="false">
    <pat:oneOrMore>
      <pat:ref name="#anyRange" />
    </pat:oneOrMore>
  </pat:define>
  <pat:define name="#anyRange" hash="#anyRange"
              nullable="false" allows-text="false"
              allows-annotations="false" only-annotations="false">
    <pat:range>
      <pat:ref name="#anyName" />
      <pat:ref name="#any" />
    </pat:range>
  </pat:define>
</xsl:variable>  
  
<xsl:template name="pat:compile-creole">
  <xsl:param name="schema" as="document-node()" required="yes" />
  <xsl:apply-templates select="$schema" mode="pat:compile">
    <xsl:with-param name="patterns" as="element()+" select="$built-in-patterns" />    
  </xsl:apply-templates>
</xsl:template>  
    
<xsl:template match="pat:grammar" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="new-patterns" as="element()+" 
    select="pat:start, pat:define, $patterns" />
  <xsl:variable name="compiled" as="element()+">
    <xsl:call-template name="pat:compile-definitions">
      <xsl:with-param name="patterns" select="$new-patterns" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="start" as="element()" select="$compiled[@name = '#start']" />
  <xsl:sequence select="$start, $compiled except $start" />
</xsl:template>
  
<xsl:template name="pat:compile-definitions" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="uncompiled" as="element()?" 
    select="$patterns[not(@hash)][1]" />
  <xsl:choose>
    <xsl:when test="exists($uncompiled)">
      <xsl:variable name="compiled" as="element()+">
        <xsl:apply-templates select="$uncompiled" mode="pat:compile">
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:apply-templates>
      </xsl:variable>
      <xsl:call-template name="pat:compile-definitions">
        <xsl:with-param name="patterns" select="$compiled" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$patterns" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<xsl:template match="pat:define | pat:start" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="combined" as="element()+">
    <xsl:call-template name="pat:associate">
      <xsl:with-param name="combiner" select="'group'" />
      <xsl:with-param name="combinees" select="pat:*" />
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="id1" as="xs:string" select="$combined[1]/@name" />
  <xsl:variable name="id" as="xs:string" 
    select="if (self::pat:start) then '#start' else @name" />
  <pat:define name="{$id}" hash="{$id} = {$id1}" 
              nullable="{pat:nullable($combined)}"
              allows-text="{pat:allows-text($combined)}"
              allows-annotations="{pat:allows-annotations($combined)}"
              only-annotations="{pat:only-annotations($combined)}">
    <pat:ref name="{$id1}" />
  </pat:define>
  <xsl:sequence select="$combined except ." />
</xsl:template>  
  
<xsl:template match="pat:notAllowed" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="notAllowed" as="element()" select="$patterns[@name = '#notAllowed']" />
  <xsl:sequence select="$notAllowed, $patterns except $notAllowed" />
</xsl:template>  
  
<xsl:template match="pat:empty" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="empty" as="element()" select="$patterns[@name = '#empty']" />
  <xsl:sequence select="$empty, $patterns except $empty" />
</xsl:template>  
  
<xsl:template match="pat:text" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="text" as="element()" select="$patterns[@name = '#text']" />
  <xsl:sequence select="$text, $patterns except $text" />
</xsl:template>  
  
<xsl:template match="pat:ref" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="pattern" as="element()" 
    select="$patterns[@name = current()/@name]" />
  <xsl:sequence select="$pattern, $patterns except $pattern" />
</xsl:template>
  
<!-- Simplification -->  
  
<xsl:template match="pat:optional" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="compiled-content" as="element()+">
    <xsl:call-template name="pat:associate">
      <xsl:with-param name="combiner" select="'group'" />
      <xsl:with-param name="combinees" select="pat:*" />
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:call-template name="pat:choice">
    <xsl:with-param name="pattern1" select="$compiled-content[1]/@name" />
    <xsl:with-param name="pattern2" select="'#empty'" />
    <xsl:with-param name="patterns" select="$compiled-content" />
  </xsl:call-template>
</xsl:template>

<xsl:template match="pat:zeroOrMore" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="grouped" as="element()+">
    <xsl:call-template name="pat:associate">
      <xsl:with-param name="combiner" select="'group'" />
      <xsl:with-param name="combinees" select="pat:*" />
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="repeated" as="element()+">
    <xsl:call-template name="pat:oneOrMore">
      <xsl:with-param name="patterns" select="$grouped" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:call-template name="pat:choice">
    <xsl:with-param name="pattern1" select="$repeated[1]/@name" />
    <xsl:with-param name="pattern2" select="'#empty'" />
    <xsl:with-param name="patterns" select="$repeated" />
  </xsl:call-template>
</xsl:template>  

<xsl:template match="pat:concurZeroOrMore" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="grouped" as="element()+">
    <xsl:call-template name="pat:associate">
      <xsl:with-param name="combiner" select="'group'" />
      <xsl:with-param name="combinees" select="pat:*" />
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="repeated" as="element()+">
    <xsl:call-template name="pat:concurOneOrMore">
      <xsl:with-param name="patterns" select="$grouped" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:call-template name="pat:choice">
    <xsl:with-param name="pattern1" select="$repeated[1]/@name" />
    <xsl:with-param name="pattern2" select="'#empty'" />
    <xsl:with-param name="patterns" select="$repeated" />
  </xsl:call-template>
</xsl:template>  
  
<xsl:template match="pat:mixed" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="grouped" as="element()+">
    <xsl:call-template name="pat:associate">
      <xsl:with-param name="combiner" select="'group'" />
      <xsl:with-param name="combinees" select="pat:*" />
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:call-template name="pat:interleave">
    <xsl:with-param name="pattern1" select="'#text'" />
    <xsl:with-param name="pattern2" select="$grouped[1]/@name" />
    <xsl:with-param name="patterns" select="$grouped" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template match="pat:attribute" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="content" as="element()*" 
    select="if (@name) then pat:* else pat:*[position() > 1]" />
  <xsl:variable name="annotation" as="element()">
    <pat:annotation>
      <xsl:copy-of select="@*" />
      <xsl:choose>
        <xsl:when test="$content">
          <xsl:sequence select="$content" />
        </xsl:when>
        <xsl:otherwise>
          <pat:ref name="#text" />
        </xsl:otherwise>
      </xsl:choose>
    </pat:annotation>
  </xsl:variable>
  <xsl:apply-templates select="$annotation" mode="pat:compile">
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:template>
  
<xsl:template match="pat:element" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="partition" as="element()">
    <pat:partition>
      <pat:range>
        <xsl:copy-of select="@*, pat:*" />
      </pat:range>
    </pat:partition>
  </xsl:variable>
  <xsl:apply-templates select="$partition" mode="pat:compile">
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:template>  
  
<!-- Simplified patterns -->  
  
<xsl:template match="pat:name" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="qname" as="xs:QName" 
    select="if (contains(., ':'))
            then resolve-QName(., .)
            else QName(ancestor-or-self::*[@ns][1]/@ns, .)" />
  <xsl:variable name="ns" as="xs:anyURI" select="namespace-uri-from-QName($qname)" />
  <xsl:variable name="name" as="xs:string" select="local-name-from-QName($qname)" />
  <xsl:variable name="hash" as="xs:string" select="concat('{', $ns, '}', $name)" />
  <xsl:variable name="existing" as="element()?" select="$patterns[@hash = $hash]" />
  <xsl:choose>
    <xsl:when test="exists($existing)">
      <xsl:sequence select="$existing, $patterns except $existing" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="pattern" as="element()">
        <pat:name ns="{$ns}">
          <xsl:value-of select="$name" />
        </pat:name>
      </xsl:variable>
      <xsl:variable name="id" select="generate-id($pattern)" />
      <pat:define name="{$id}" hash="{$hash}" 
                  nullable="false" allows-text="false" 
                  allows-annotations="false" only-annotations="false">
        <xsl:sequence select="$pattern" />
      </pat:define>
      <xsl:sequence select="$patterns" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<xsl:template match="pat:anyName" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="pat:except">
      <xsl:variable name="except" as="element()+">
        <xsl:apply-templates select="pat:except" mode="pat:compile">
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:apply-templates>
      </xsl:variable>
      <xsl:variable name="hash" as="xs:string" select="concat('* - ', $except[1]/@name)" />
      <xsl:variable name="existing" as="element()?" select="$except[@hash = $hash]" />
      <xsl:choose>
        <xsl:when test="exists($existing)">
          <xsl:sequence select="$existing, $except except $existing" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="pattern" as="element()">
            <pat:anyNameExcept>
              <pat:ref name="{$except[1]/@name}" />
            </pat:anyNameExcept>
          </xsl:variable>
          <xsl:variable name="id" as="xs:string" select="generate-id($pattern)" />
          <pat:define name="{$id}" hash="{$hash}" 
                      nullable="false" allows-text="false" 
                      allows-annotations="false" only-annotations="false">
            <xsl:sequence select="$pattern" />
          </pat:define>
          <xsl:sequence select="$except" />
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:when>
    <xsl:otherwise>      
      <xsl:variable name="anyName" as="element()" select="$patterns[@name = '#anyName']" />
      <xsl:sequence select="$anyName, $patterns except $anyName" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<xsl:template match="pat:nsName" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="ns" as="xs:string" select="ancestor-or-self::*[@ns][1]/@ns" />
  <xsl:choose>
    <xsl:when test="pat:except">
      <xsl:variable name="except" as="element()+">
        <xsl:apply-templates select="pat:except" mode="pat:compile">
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:apply-templates>
      </xsl:variable>
      <xsl:variable name="hash" as="xs:string" select="concat('{', $ns, '}* - ', $except[1]/@name)" />
      <xsl:variable name="existing" as="element()?" select="$except[@hash = $hash]" />
      <xsl:choose>
        <xsl:when test="exists($existing)">
          <xsl:sequence select="$existing, $except except $existing" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="pattern" as="element()">
            <pat:nsNameExcept>
              <pat:ref name="{$except[1]/@name}" />
            </pat:nsNameExcept>
          </xsl:variable>
          <xsl:variable name="id" as="xs:string" select="generate-id($pattern)" />
          <pat:define name="{$id}" hash="{$hash}" 
                      nullable="false" allows-text="false" 
                      allows-annotations="false" only-annotations="false">
            <xsl:sequence select="$pattern" />
          </pat:define>
          <xsl:sequence select="$except" />
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="hash" as="xs:string" select="concat('{', $ns, '}*')" />
      <xsl:variable name="existing" as="element()?" select="$patterns[@hash = $hash]" />
      <xsl:choose>
        <xsl:when test="exists($existing)">
          <xsl:sequence select="$existing, $patterns except $existing" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="pattern" as="element()">
            <pat:nsName ns="{$ns}" />
          </xsl:variable>
          <xsl:variable name="id" as="xs:string" select="generate-id($pattern)" />
          <pat:define name="{$id}" hash="{$hash}" 
                      nullable="false" allows-text="false" 
                      allows-annotations="false" only-annotations="false">
            <xsl:sequence select="$pattern" />
          </pat:define>
          <xsl:sequence select="$patterns" />
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
  
<xsl:template match="pat:except" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:associate">
    <xsl:with-param name="combiner" select="'choice'" />
    <xsl:with-param name="combinees" select="pat:*" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template match="pat:end-tag" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="nc" as="element()" select="pat:*[1]" />
  <xsl:variable name="after1" as="element()+">
    <xsl:apply-templates select="$nc" mode="pat:compile">
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:apply-templates>
  </xsl:variable>
  <xsl:variable name="hash" as="xs:string" select="pat:hash(local-name(.), $after1[1]/@name, @id)" />
  <xsl:variable name="existing" as="element()?" select="$patterns[@hash = $hash]" />
  <xsl:choose>
    <xsl:when test="exists($existing)">
      <xsl:sequence select="$existing, $after1 except $existing" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="pattern" as="element()">
        <pat:end-tag id="{@id}">
          <pat:ref name="{$after1[1]/@name}" />
        </pat:end-tag>
      </xsl:variable>
      <xsl:variable name="id" select="generate-id($pattern)" />
      <pat:define name="{$id}" hash="{$hash}" 
                  nullable="false" allows-text="false" 
                  allows-annotations="false" only-annotations="false">
        <xsl:sequence select="$pattern" />
      </pat:define>
      <xsl:sequence select="$after1" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<xsl:template match="pat:end-annotation" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="nc" as="element()" select="pat:*[1]" />
  <xsl:variable name="after1" as="element()+">
    <xsl:apply-templates select="$nc" mode="pat:compile">
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:apply-templates>
  </xsl:variable>
  <xsl:variable name="hash" as="xs:string" select="pat:hash(local-name(.), $after1[1]/@name)" />
  <xsl:variable name="existing" as="element()?" select="$patterns[@hash = $hash]" />
  <xsl:choose>
    <xsl:when test="exists($existing)">
      <xsl:sequence select="$existing, $after1 except $existing" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="pattern" as="element()">
        <pat:end-annotation>
          <pat:ref name="{$after1[1]/@name}" />
        </pat:end-annotation>
      </xsl:variable>
      <xsl:variable name="id" select="generate-id($pattern)" />
      <pat:define name="{$id}" hash="{$hash}" 
                  nullable="false" allows-text="false" 
                  allows-annotations="false" only-annotations="false">
        <xsl:sequence select="$pattern" />
      </pat:define>
      <xsl:sequence select="$after1" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<xsl:template match="pat:end-atom" mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="nc" as="element()" select="pat:*[1]" />
  <xsl:variable name="after1" as="element()+">
    <xsl:apply-templates select="$nc" mode="pat:compile">
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:apply-templates>
  </xsl:variable>
  <xsl:variable name="hash" as="xs:string" select="pat:hash(local-name(.), $after1[1]/@name)" />
  <xsl:variable name="existing" as="element()?" select="$patterns[@hash = $hash]" />
  <xsl:choose>
    <xsl:when test="exists($existing)">
      <xsl:sequence select="$existing, $after1 except $existing" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="pattern" as="element()">
        <pat:end-atom>
          <pat:ref name="{$after1[1]/@name}" />
        </pat:end-atom>
      </xsl:variable>
      <xsl:variable name="id" select="generate-id($pattern)" />
      <pat:define name="{$id}" hash="{$hash}" 
                  nullable="false" allows-text="false" 
                  allows-annotations="false" only-annotations="false">
        <xsl:sequence select="$pattern" />
      </pat:define>
      <xsl:sequence select="$after1" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<xsl:template match="pat:range | pat:annotation | pat:atom" 
              mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="nc" as="element()">
    <xsl:choose>
      <xsl:when test="@name">
        <pat:name ns="{ancestor-or-self::*[@ns][1]/@ns}">
          <xsl:value-of select="@name" />
        </pat:name>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="pat:*[1]" />
      </xsl:otherwise>
    </xsl:choose>    
  </xsl:variable>
  <xsl:variable name="after1" as="element()+">
    <xsl:apply-templates select="$nc" mode="pat:compile">
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:apply-templates>
  </xsl:variable>
  <xsl:variable name="id1" as="xs:string" select="$after1[1]/@name" />
  <xsl:variable name="content" as="element()*" 
    select="if (@name) then pat:* else pat:*[position() > 1]"/>
  <xsl:variable name="after2" as="element()+">
    <xsl:choose>
      <xsl:when test="self::pat:atom and empty($content)">
        <xsl:variable name="empty" as="element()" select="$after1[@name = '#empty']" />
        <xsl:sequence select="$empty, $after1 except $empty" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="pat:associate">
          <xsl:with-param name="combiner" select="'group'" />
          <xsl:with-param name="combinees" select="$content" />
          <xsl:with-param name="patterns" select="$after1" />
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>    
  </xsl:variable>
  <xsl:variable name="id2" as="xs:string" select="$after2[1]/@name" />
  <xsl:variable name="hash" as="xs:string" select="pat:hash(local-name(.), $id1, $id2)" />
  <xsl:variable name="existing" as="element()?" select="$patterns[@hash = $hash]" />
  <xsl:choose>
    <xsl:when test="exists($existing)">
      <xsl:sequence select="$existing, $after2 except $existing" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="pattern" as="element()">
        <xsl:element name="pat:{local-name(.)}">
          <pat:ref name="{$id1}" />
          <pat:ref name="{$id2}" />
        </xsl:element>
      </xsl:variable>
      <xsl:variable name="id" select="generate-id($pattern)" />
      <pat:define name="{$id}" hash="{$hash}" 
                  nullable="false" 
                  allows-text="false" 
                  allows-annotations="{. instance of element(pat:annotation)}"
                  only-annotations="{. instance of element(pat:annotation)}">
        <xsl:sequence select="$pattern" />
      </pat:define>
      <xsl:sequence select="$after2" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  

<xsl:template match="pat:choice" mode="pat:compile"
              name="pat:compile-choice"
              as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:param name="to-compile" as="element()*" select="pat:*" />
  <xsl:param name="to-associate" as="xs:string*" select="()" />
  <xsl:choose>
    <xsl:when test="exists($to-compile)">
      <xsl:variable name="compiled" as="element()+">
        <xsl:apply-templates select="$to-compile[1]" mode="pat:compile">
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:apply-templates>
      </xsl:variable>
      <xsl:call-template name="pat:compile-choice">
        <xsl:with-param name="patterns" select="$compiled" />
        <xsl:with-param name="to-compile" select="$to-compile[position() > 1]" />
        <xsl:with-param name="to-associate" select="$to-associate, $compiled[1]/@name" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="pat:associate-choices">
        <xsl:with-param name="patterns" select="$patterns" />
        <xsl:with-param name="combinees" select="$to-associate" />
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>  
</xsl:template>  
  
<xsl:template match="pat:group 
                     | pat:interleave 
                     | pat:concur"
              name="pat:associate"
              mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:param name="combiner" as="xs:string" select="local-name(.)" />
  <xsl:param name="combinees" as="element()*" select="pat:*" />
  <xsl:choose>
    <xsl:when test="count($combinees) = 1">
      <xsl:apply-templates select="$combinees" mode="pat:compile">
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:apply-templates>
    </xsl:when>
    <xsl:when test="count($combinees) = 2">
      <xsl:call-template name="pat:compile-and-combine">
        <xsl:with-param name="combiner" select="$combiner" />
        <xsl:with-param name="pattern1" select="$combinees[1]" />
        <xsl:with-param name="pattern2" select="$combinees[2]" />
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="combined" as="element()+">
        <xsl:call-template name="pat:associate">
          <xsl:with-param name="combiner" select="$combiner" />
          <xsl:with-param name="combinees" select="$combinees[position() != last()]" />
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:call-template name="pat:compile-and-combine">
        <xsl:with-param name="combiner" select="$combiner" />
        <xsl:with-param name="pattern1" as="element()">
          <pat:ref name="{$combined[1]/@name}" />
        </xsl:with-param>
        <xsl:with-param name="pattern2" select="$combinees[last()]" />
        <xsl:with-param name="patterns" select="$combined" />
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
  
<xsl:template match="pat:oneOrMore | pat:concurOneOrMore | pat:partition" 
              mode="pat:compile" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:compile-and-repeat">
    <xsl:with-param name="repeater" select="local-name(.)" />
    <xsl:with-param name="repeatees" select="pat:*" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>
 
<!-- Constructors -->  
  
<xsl:template name="pat:choice" as="element()+">
  <xsl:param name="pattern1" as="xs:string" required="yes" />
  <xsl:param name="pattern2" as="xs:string" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:associate-choices">
    <xsl:with-param name="combinees" select="pat:expand-compiled-choices(distinct-values(($pattern1, $pattern2)), $patterns)" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template name="pat:associate-choices" as="element()+">
  <xsl:param name="combinees" as="xs:string+" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="count($combinees) = 1">
      <xsl:variable name="pat" as="element()" select="$patterns[@name = $combinees[1]]" />
      <xsl:sequence select="$pat, $patterns except $pat" />
    </xsl:when>
    <xsl:when test="count($combinees) = 2">
      <xsl:call-template name="pat:combine">
        <xsl:with-param name="combiner" select="'choice'" />
        <xsl:with-param name="pattern1" select="$combinees[1]" />
        <xsl:with-param name="pattern2" select="$combinees[2]" />
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="combined" as="element()+">
        <xsl:call-template name="pat:associate-choices">
          <xsl:with-param name="combinees" select="$combinees[position() != last()]" />
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:call-template name="pat:combine">
        <xsl:with-param name="combiner" select="'choice'" />
        <xsl:with-param name="pattern1" select="$combined[1]/@name" />
        <xsl:with-param name="pattern2" select="$combinees[last()]" />
        <xsl:with-param name="patterns" select="$combined" />
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>  
</xsl:template>  
  
<xsl:template name="pat:after" as="element()+">
  <xsl:param name="pattern1" as="xs:string" required="yes" />
  <xsl:param name="pattern2" as="xs:string" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:combine">
    <xsl:with-param name="combiner" select="'after'" />
    <xsl:with-param name="pattern1" select="$pattern1" />
    <xsl:with-param name="pattern2" select="$pattern2" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template name="pat:all" as="element()+">
  <xsl:param name="pattern1" as="xs:string" required="yes" />
  <xsl:param name="pattern2" as="xs:string" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:combine">
    <xsl:with-param name="combiner" select="'all'" />
    <xsl:with-param name="pattern1" select="$pattern1" />
    <xsl:with-param name="pattern2" select="$pattern2" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template name="pat:group" as="element()+">
  <xsl:param name="pattern1" as="xs:string" required="yes" />
  <xsl:param name="pattern2" as="xs:string" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:combine">
    <xsl:with-param name="combiner" select="'group'" />
    <xsl:with-param name="pattern1" select="$pattern1" />
    <xsl:with-param name="pattern2" select="$pattern2" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template name="pat:interleave" as="element()+">
  <xsl:param name="pattern1" as="xs:string" required="yes" />
  <xsl:param name="pattern2" as="xs:string" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:combine">
    <xsl:with-param name="combiner" select="'interleave'" />
    <xsl:with-param name="pattern1" select="$pattern1" />
    <xsl:with-param name="pattern2" select="$pattern2" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template name="pat:concur" as="element()+">
  <xsl:param name="pattern1" as="xs:string" required="yes" />
  <xsl:param name="pattern2" as="xs:string" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:combine">
    <xsl:with-param name="combiner" select="'concur'" />
    <xsl:with-param name="pattern1" select="$pattern1" />
    <xsl:with-param name="pattern2" select="$pattern2" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template name="pat:oneOrMore" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:repeat">
    <xsl:with-param name="repeater" select="'oneOrMore'" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template name="pat:concurOneOrMore" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:repeat">
    <xsl:with-param name="repeater" select="'concurOneOrMore'" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template name="pat:partition" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:call-template name="pat:repeat">
    <xsl:with-param name="repeater" select="'partition'" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:call-template>
</xsl:template>  
  
<xsl:template name="pat:compile-and-repeat" as="element()+">
  <xsl:param name="repeater" as="xs:string" required="yes" />
  <xsl:param name="repeatees" as="element()*" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="grouped" as="element()+">
    <xsl:call-template name="pat:associate">
      <xsl:with-param name="combiner" select="'group'" />
      <xsl:with-param name="combinees" select="$repeatees" />
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:call-template name="pat:repeat">
    <xsl:with-param name="repeater" select="$repeater" />
    <xsl:with-param name="patterns" select="$grouped" />
  </xsl:call-template>
</xsl:template>
  
<xsl:template name="pat:repeat" as="element()+">
  <xsl:param name="repeater" as="xs:string" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="pat" as="xs:string" select="$patterns[1]/@name" />
  <xsl:choose>
    <xsl:when test="$pat = ('#notAllowed', '#empty', '#text')">
      <xsl:sequence select="$patterns" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="hash" as="xs:string" select="pat:hash($repeater, $pat)" />
      <xsl:variable name="existing" as="element()?" select="$patterns[@hash = $hash]" />
      <xsl:choose>
        <xsl:when test="exists($existing)">
          <xsl:sequence select="$existing, $patterns except $existing" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="pattern" as="element()">
            <xsl:element name="pat:{$repeater}">
              <pat:ref name="{$pat}" />
            </xsl:element>
          </xsl:variable>
          <xsl:variable name="id" select="generate-id($pattern)" />
          <!-- return the patterns with the new pattern first -->
          <pat:define name="{$id}" hash="{$hash}" 
                      nullable="{pat:nullable($patterns)}"
                      allows-text="{pat:allows-text($patterns)}"
                      allows-annotations="{pat:allows-annotations($patterns)}"
                      only-annotations="{pat:only-annotations($patterns)}">
            <xsl:sequence select="$pattern" />
          </pat:define>
          <xsl:sequence select="$patterns" />
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="pat:compile-and-combine" as="element()+">
  <xsl:param name="combiner" as="xs:string" required="yes" />
  <xsl:param name="pattern1" as="element()" required="yes" />
  <xsl:param name="pattern2" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />  
  <xsl:variable name="after1" as="element()+">
    <xsl:apply-templates select="$pattern1" mode="pat:compile">
      <xsl:with-param name="patterns" select="$patterns" />
    </xsl:apply-templates>
  </xsl:variable>
  <xsl:variable name="after2" as="element()+">
    <xsl:apply-templates select="$pattern2" mode="pat:compile">
      <xsl:with-param name="patterns" select="$after1" />
    </xsl:apply-templates>
  </xsl:variable>
  <xsl:call-template name="pat:combine">
    <xsl:with-param name="combiner" select="$combiner" />
    <xsl:with-param name="pattern1" select="$after1[1]/@name" />
    <xsl:with-param name="pattern2" select="$after2[1]/@name" />
    <xsl:with-param name="patterns" select="$after2" />
  </xsl:call-template>
</xsl:template>  

<xsl:template name="pat:combine" as="element()+">
  <xsl:param name="combiner" as="xs:string" required="yes" />
  <xsl:param name="pattern1" as="xs:string" required="yes" />
  <xsl:param name="pattern2" as="xs:string" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="pat1" as="element()" select="pat:pattern($pattern1, $patterns)" />
  <xsl:variable name="pat2" as="element()" select="pat:pattern($pattern2, $patterns)" />
  <xsl:choose>
    <xsl:when test="$combiner = 'concur' and
                    $pat1/pat:after and $pat2/pat:after">
      <xsl:variable name="all" as="element()+">
        <xsl:call-template name="pat:all">
          <xsl:with-param name="pattern1" select="$pat1/pat:after/pat:ref[1]/@name" />
          <xsl:with-param name="pattern2" select="$pat2/pat:after/pat:ref[1]/@name" />
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="concur" as="element()+">
        <xsl:call-template name="pat:concur">
          <xsl:with-param name="pattern1" select="$pat1/pat:after/pat:ref[2]/@name" />
          <xsl:with-param name="pattern2" select="$pat2/pat:after/pat:ref[2]/@name" />
          <xsl:with-param name="patterns" select="$all" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:call-template name="pat:after">
        <xsl:with-param name="pattern1" select="$all[1]/@name" />
        <xsl:with-param name="pattern2" select="$concur[1]/@name" />
        <xsl:with-param name="patterns" select="$concur" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$combiner = 'all' and
                    $pat1/pat:after and $pat2/pat:after">
      <xsl:variable name="all1" as="element()+">
        <xsl:call-template name="pat:all">
          <xsl:with-param name="pattern1" select="$pat1/pat:after/pat:ref[1]/@name" />
          <xsl:with-param name="pattern2" select="$pat2/pat:after/pat:ref[1]/@name" />
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="all2" as="element()+">
        <xsl:call-template name="pat:all">
          <xsl:with-param name="pattern1" select="$pat1/pat:after/pat:ref[2]/@name" />
          <xsl:with-param name="pattern2" select="$pat2/pat:after/pat:ref[2]/@name" />
          <xsl:with-param name="patterns" select="$all1" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:call-template name="pat:after">
        <xsl:with-param name="pattern1" select="$all1[1]/@name" />
        <xsl:with-param name="pattern2" select="$all2[1]/@name" />
        <xsl:with-param name="patterns" select="$all2" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="not($combiner = ('choice', 'all')) and 
                    $pat1/pat:after">
      <xsl:call-template name="pat:apply-after">
        <xsl:with-param name="construct" select="$combiner" />
        <xsl:with-param name="arg2" select="$pattern2" />
        <xsl:with-param name="pattern" select="$pattern1" />
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="not($combiner = ('choice', 'after', 'all')) and
                    $pat2/pat:after">
      <xsl:call-template name="pat:apply-after">
        <xsl:with-param name="construct" select="$combiner" />
        <xsl:with-param name="arg1" select="$pattern1" />
        <xsl:with-param name="pattern" select="$pattern2" />
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$pattern1 = '#text' and
                    $pattern2 = '#text'">
      <xsl:sequence select="$pat1, $patterns except $pat1" />
    </xsl:when>
    <xsl:when test="$combiner = 'choice' and
                    $pattern1 = '#notAllowed'">
      <xsl:sequence select="$pat2, $patterns except $pat2" />
    </xsl:when>
    <xsl:when test="$combiner = 'choice' and
                    $pattern2 = '#notAllowed'">
      <xsl:sequence select="$pat1, $patterns except $pat1" />
    </xsl:when>
    <xsl:when test="$pattern1 = '#notAllowed'">
      <xsl:sequence select="$pat1, $patterns except $pat1" />
    </xsl:when>
    <xsl:when test="$pattern2 = '#notAllowed'">
      <xsl:sequence select="$pat2, $patterns except $pat2" />
    </xsl:when>
    <xsl:when test="$pattern1 = '#empty' and $pattern2 = '#empty'">
      <xsl:sequence select="$pat1, $patterns except $pat1" />
    </xsl:when>
    <xsl:when test="$combiner = 'all' and
                    $pattern1 = '#empty'">
      <xsl:choose>
        <xsl:when test="pat:nullable($pat2, $patterns)">
          <xsl:sequence select="$pat1, $patterns except $pat1" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="notAllowed" as="element()" select="$patterns[@name = '#notAllowed']" />
          <xsl:sequence select="$notAllowed, $patterns except $notAllowed" />
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:when>
    <xsl:when test="$combiner = 'all' and
                    $pattern2 = '#empty'">
      <xsl:choose>
        <xsl:when test="pat:nullable($pat1, $patterns)">
          <xsl:sequence select="$pat2, $patterns except $pat2" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="notAllowed" as="element()" select="$patterns[@name = '#notAllowed']" />
          <xsl:sequence select="$notAllowed, $patterns except $notAllowed" />
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:when>
    <xsl:when test="not($combiner = ('choice', 'all')) and
                    $pattern1 = '#empty'">
      <xsl:sequence select="$pat2, $patterns except $pat2" />
    </xsl:when>
    <xsl:when test="not($combiner = ('choice', 'after', 'all')) and
                    $pattern2 = '#empty'">
      <xsl:sequence select="$pat1, $patterns except $pat1" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="hash" as="xs:string" select="pat:hash($combiner, $pattern1, $pattern2)" />
      <xsl:variable name="existing" as="element()?" select="$patterns[@hash = $hash]" />
      <xsl:choose>
        <xsl:when test="exists($existing)">
          <!-- no change, return existing pattern -->
          <xsl:sequence select="$existing, $patterns except $existing" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="pattern" as="element()">
            <xsl:element name="pat:{$combiner}">
              <pat:ref name="{$pattern1}" />
              <pat:ref name="{$pattern2}" />
            </xsl:element>
          </xsl:variable>
          <xsl:variable name="id" select="generate-id($pattern)" />
          <!-- return new pattern, followed by the rest of them -->
          <pat:define name="{$id}" hash="{$hash}" 
                      nullable="{pat:nullable($pattern, $patterns)}"
                      allows-text="{pat:allows-text($pattern, $patterns)}"
                      allows-annotations="{pat:allows-annotations($pattern, $patterns)}"
                      only-annotations="{pat:only-annotations($pattern, $patterns)}">
            <xsl:sequence select="$pattern" />
          </pat:define>
          <xsl:sequence select="$patterns" />
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
      
<!-- *** pat:apply-after *** -->  
  
<!-- This follows James Clark's logic. -->
<xsl:template name="pat:apply-after" as="element()+">
  <xsl:param name="construct" as="xs:string" required="yes" />
  <xsl:param name="arg1" as="xs:string?" select="()" />
  <xsl:param name="arg2" as="xs:string?" select="()" />
  <xsl:param name="pattern" as="xs:string" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="pat" as="element()" select="pat:pattern($pattern, $patterns)" />
  <xsl:choose>
    <xsl:when test="$pat/pat:after">
      <xsl:variable name="group" as="element()+">
        <xsl:variable name="pat1" as="xs:string" 
          select="if (exists($arg1)) then $arg1 else $pat/pat:after/pat:ref[2]/@name"/>
        <xsl:variable name="pat2" as="xs:string" 
          select="if (exists($arg2)) then $arg2 else $pat/pat:after/pat:ref[2]/@name" />
        <xsl:choose>
          <xsl:when test="$construct = 'group'">
            <xsl:call-template name="pat:group">
              <xsl:with-param name="pattern1" select="$pat1" />
              <xsl:with-param name="pattern2" select="$pat2" />
              <xsl:with-param name="patterns" select="$patterns" />
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="$construct = 'interleave'">
            <xsl:call-template name="pat:interleave">
              <xsl:with-param name="pattern1" select="$pat1" />
              <xsl:with-param name="pattern2" select="$pat2" />
              <xsl:with-param name="patterns" select="$patterns" />
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="$construct = 'concur'">
            <xsl:call-template name="pat:concur">
              <xsl:with-param name="pattern1" select="$pat1" />
              <xsl:with-param name="pattern2" select="$pat2" />
              <xsl:with-param name="patterns" select="$patterns" />
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="$construct = 'after'">
            <xsl:call-template name="pat:after">
              <xsl:with-param name="pattern1" select="$pat1" />
              <xsl:with-param name="pattern2" select="$pat2" />
              <xsl:with-param name="patterns" select="$patterns" />
            </xsl:call-template>
          </xsl:when>
        </xsl:choose>
      </xsl:variable>      
      <xsl:call-template name="pat:after">
        <xsl:with-param name="pattern1" select="$pat/pat:after/pat:ref[1]/@name" />
        <xsl:with-param name="pattern2" select="$group[1]/@name" />
        <xsl:with-param name="patterns" select="$group" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$pat/pat:choice">
      <xsl:variable name="after1" as="element()+">
        <xsl:call-template name="pat:apply-after">
          <xsl:with-param name="construct" select="$construct" />
          <xsl:with-param name="arg1" select="$arg1" />
          <xsl:with-param name="arg2" select="$arg2" />
          <xsl:with-param name="pattern" select="$pat/pat:choice/pat:ref[1]/@name" />
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="after2" as="element()+">
        <xsl:call-template name="pat:apply-after">
          <xsl:with-param name="construct" select="$construct" />
          <xsl:with-param name="arg1" select="$arg1" />
          <xsl:with-param name="arg2" select="$arg2" />
          <xsl:with-param name="pattern" select="$pat/pat:choice/pat:ref[2]/@name" />
          <xsl:with-param name="patterns" select="$after1" />
        </xsl:call-template>
      </xsl:variable>      
      <xsl:call-template name="pat:choice">
        <xsl:with-param name="pattern1" select="$after1[1]/@name" />
        <xsl:with-param name="pattern2" select="$after2[1]/@name" />
        <xsl:with-param name="patterns" select="$after2" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$pat/pat:ref">
      <xsl:call-template name="pat:apply-after">
        <xsl:with-param name="construct" select="$construct" />
        <xsl:with-param name="arg1" select="$arg1" />
        <xsl:with-param name="arg2" select="$arg2" />
        <xsl:with-param name="pattern" select="$pat/pat:ref/@name" />
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="notAllowed" as="element()" select="$patterns[@name = '#notAllowed']" />
      <xsl:sequence select="$notAllowed, $patterns except $notAllowed" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  

<xsl:function name="pat:expand-compiled-choices" as="xs:string+">
  <xsl:param name="choices" as="xs:string+" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:variable name="expanded" as="xs:string+">
    <xsl:for-each select="$choices">
      <xsl:sequence select="pat:expand-compiled-choice(., $patterns)" />
    </xsl:for-each>
  </xsl:variable>
  <xsl:sequence select="distinct-values($choices)" />
</xsl:function>  
  
<xsl:function name="pat:expand-compiled-choice" as="xs:string+">
  <xsl:param name="choice" as="xs:string" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:variable name="pat" as="element()" select="pat:pattern($choice, $patterns)" />
  <xsl:choose>
    <xsl:when test="$pat/pat:choice">
      <xsl:sequence select="distinct-values((pat:expand-compiled-choice($pat/pat:choice/pat:ref[1]/@name, $patterns),
                                             pat:expand-compiled-choice($pat/pat:choice/pat:ref[2]/@name, $patterns)))" />
    </xsl:when>
    <xsl:when test="$pat/pat:ref">
      <xsl:sequence select="pat:expand-compiled-choice($pat/pat:ref/@name, $patterns)" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$choice" />
    </xsl:otherwise>
  </xsl:choose>  
</xsl:function>  
  
<!-- *** pat:pattern() *** -->
  
<xsl:function name="pat:pattern" as="element()">
  <xsl:param name="name" as="xs:string" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:variable name="pattern" as="element()*"
    select="$patterns[@name = $name]" />
  <xsl:choose>
    <xsl:when test="count($pattern) = 1">
      <xsl:sequence select="$pattern" />
    </xsl:when>
    <xsl:when test="empty($pattern)">
      <xsl:message terminate="yes">
        ERROR: can't find pattern <xsl:value-of select="$name" /> in grammar:
        <xsl:copy-of select="$patterns" copy-namespaces="no" />
      </xsl:message>
    </xsl:when>
    <xsl:otherwise>
      <xsl:message terminate="yes">
        ERROR: found two patterns named <xsl:value-of select="$name" /> in grammar:
        <xsl:copy-of select="$patterns" copy-namespaces="no" />
      </xsl:message>
    </xsl:otherwise>
  </xsl:choose>  
</xsl:function>
  
<!-- *** pat:hash() *** -->
  
<xsl:function name="pat:hash" as="xs:string">
  <xsl:param name="repeater" as="xs:string" />
  <xsl:param name="pat" as="xs:string" />
  <xsl:choose>
    <xsl:when test="$repeater = 'oneOrMore'">
      <xsl:sequence select="concat($pat, '+')" />
    </xsl:when>
    <xsl:when test="$repeater = 'concurOneOrMore'">
      <xsl:sequence select="concat($pat, '~')" />
    </xsl:when>
    <xsl:when test="$repeater = 'partition'">
      <xsl:sequence select="concat('partition{', $pat, '}')"></xsl:sequence>
    </xsl:when>
    <xsl:when test="$repeater = 'end-annotation'">
      <xsl:sequence select="concat('end-annotation ', $pat)" />
    </xsl:when>
    <xsl:when test="$repeater = 'end-atom'">
      <xsl:sequence select="concat('end-atom ', $pat)" />
    </xsl:when>
  </xsl:choose>
</xsl:function>  
  
<xsl:function name="pat:hash" as="xs:string">
  <xsl:param name="combiner" as="xs:string" />
  <xsl:param name="pat1" as="xs:string" />
  <xsl:param name="pat2" as="xs:string" />
  <xsl:choose>
    <xsl:when test="$combiner = 'choice'">
      <xsl:sequence select="concat('(', $pat1, '|', $pat2, ')')" />
    </xsl:when>
    <xsl:when test="$combiner = 'group'">
      <xsl:sequence select="concat('(', $pat1, ',', $pat2, ')')" />
    </xsl:when>
    <xsl:when test="$combiner = 'interleave'">
      <xsl:sequence select="concat('(', $pat1, '&amp;', $pat2, ')')" />
    </xsl:when>
    <xsl:when test="$combiner = 'concur'">
      <xsl:sequence select="concat('concur{', $pat1, ',', $pat2, '}')" />
    </xsl:when>
    <xsl:when test="$combiner = 'after'">
      <xsl:sequence select="concat('after{', $pat1, ',', $pat2, '}')" />
    </xsl:when>
    <xsl:when test="$combiner = 'all'">
      <xsl:sequence select="concat('all{', $pat1, ',', $pat2, '}')" />
    </xsl:when>
    <xsl:when test="$combiner = 'element'">
      <xsl:sequence select="concat('element ', $pat1, ' {', $pat2, '}')" />
    </xsl:when>
    <xsl:when test="$combiner = 'range'">
      <xsl:sequence select="concat('range ', $pat1, ' {', $pat2, '}')" />
    </xsl:when>
    <xsl:when test="$combiner = 'annotation'">
      <xsl:sequence select="concat('annotation ', $pat1, ' {', $pat2, '}')" />
    </xsl:when>
    <xsl:when test="$combiner = 'atom'">
      <xsl:sequence select="concat('atom ', $pat1, ' {', $pat2, '}')" />
    </xsl:when>
    <xsl:when test="$combiner = 'end-tag'">
      <xsl:sequence select="concat('end-tag ', $pat1, if (string($pat2)) then concat('=', $pat2) else '')" />
    </xsl:when>
  </xsl:choose>
</xsl:function>  
  
<xsl:function name="pat:hash" as="xs:string">
  <xsl:param name="singleton" as="xs:string" />
  <xsl:sequence select="$singleton" />
</xsl:function>  
  
<!-- *** pat:nullable() *** -->  

<xsl:function name="pat:nullable" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" />
  <xsl:sequence select="pat:nullable($patterns[1], $patterns)" />
</xsl:function>

<xsl:function name="pat:nullable" as="xs:boolean">
  <xsl:param name="pattern" as="element()" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:apply-templates select="$pattern" mode="pat:nullable">
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:function>  
  
<xsl:template match="pat:empty | pat:text" 
              mode="pat:nullable" as="xs:boolean">
  <xsl:sequence select="true()" />
</xsl:template>
  
<xsl:template match="pat:choice"
              mode="pat:nullable" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:nullable(pat:*[1], $patterns) or 
                        pat:nullable(pat:*[2], $patterns)" />
</xsl:template>
  
<xsl:template match="pat:group 
                     | pat:interleave 
                     | pat:concur 
                     | pat:after 
                     | pat:all" 
              mode="pat:nullable" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:nullable(pat:*[1], $patterns) and 
                        pat:nullable(pat:*[2], $patterns)" />
</xsl:template>

<xsl:template match="pat:partition" mode="pat:nullable" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:nullable(pat:*, $patterns)" />
</xsl:template>  
  
<xsl:template match="pat:define | pat:start" mode="pat:nullable" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="@nullable">
      <xsl:sequence select="@nullable = 'true'" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="pat:nullable(pat:*[1], $patterns)" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  

<xsl:template match="pat:ref" mode="pat:nullable" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:nullable(pat:pattern(@name, $patterns), $patterns)" />
</xsl:template>  
  
<xsl:template match="pat:*" mode="pat:nullable" as="xs:boolean">
  <xsl:sequence select="false()" />
</xsl:template>  
  
<!-- *** pat:allows-text() *** -->
  
<xsl:function name="pat:allows-text" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" />
  <xsl:sequence select="pat:allows-text($patterns[1], $patterns)" />
</xsl:function>  
  
<xsl:function name="pat:allows-text" as="xs:boolean">
  <xsl:param name="pattern" as="element()" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:apply-templates select="$pattern" mode="pat:allows-text">
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:function>
  
<xsl:template match="pat:text" 
              mode="pat:allows-text" as="xs:boolean">
  <xsl:sequence select="true()" />
</xsl:template>
  
<xsl:template match="pat:choice | pat:interleave"
              mode="pat:allows-text" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:allows-text(pat:*[1], $patterns) or 
                        pat:allows-text(pat:*[2], $patterns)" />
</xsl:template>
  
<xsl:template match="pat:concur | pat:all" 
              mode="pat:allows-text" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:allows-text(pat:*[1], $patterns) and 
                        pat:allows-text(pat:*[2], $patterns)" />
</xsl:template>
  
<xsl:template match="pat:group" 
              mode="pat:allows-text" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="if (pat:nullable(pat:*[1], $patterns) or
                            pat:only-annotations(pat:*[1], $patterns))
                        then (pat:allows-text(pat:*[1], $patterns) or
                              pat:allows-text(pat:*[2], $patterns))
                        else (pat:allows-text(pat:*[1], $patterns))" />
</xsl:template>

<xsl:template match="pat:after" mode="pat:allows-text" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="if (pat:nullable(pat:*[1], $patterns))
                        then (pat:allows-text(pat:*[1], $patterns) or
                              pat:allows-text(pat:*[2], $patterns))
                        else (pat:allows-text(pat:*[1], $patterns))" />
</xsl:template>  
  
<xsl:template match="pat:oneOrMore | pat:concurOneOrMore | pat:partition"
              mode="pat:allows-text" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:allows-text(pat:*, $patterns)" />
</xsl:template>  
  
<xsl:template match="pat:define | pat:start" mode="pat:allows-text" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="@allows-text">
      <xsl:sequence select="@allows-text = 'true'" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="pat:allows-text(pat:*[1], $patterns)" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  

<xsl:template match="pat:ref" mode="pat:allows-text" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:allows-text(pat:pattern(@name, $patterns), $patterns)" />
</xsl:template>  
  
<xsl:template match="pat:*" mode="pat:allows-text" as="xs:boolean">
  <xsl:sequence select="false()" />
</xsl:template>  
  
<!-- *** pat:allows-annotations() *** -->
  
<xsl:function name="pat:allows-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" />
  <xsl:sequence select="pat:allows-annotations($patterns[1], $patterns)" />
</xsl:function>  
  
<xsl:function name="pat:allows-annotations" as="xs:boolean">
  <xsl:param name="pattern" as="element()" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:apply-templates select="$pattern" mode="pat:allows-annotations">
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:function>
  
<xsl:template match="pat:annotation" mode="pat:allows-annotations" as="xs:boolean">
  <xsl:sequence select="true()" />
</xsl:template>  
    
<xsl:template match="pat:choice | pat:interleave | pat:concur | pat:group"
              mode="pat:allows-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:allows-annotations(pat:*[1], $patterns) or 
                        pat:allows-annotations(pat:*[2], $patterns)" />
</xsl:template>

<xsl:template match="pat:all" mode="pat:allows-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:allows-annotations(pat:*[1], $patterns) and 
                        pat:allows-annotations(pat:*[2], $patterns)" />
</xsl:template>  
 
<xsl:template match="pat:after" mode="pat:allows-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="if (pat:nullable(pat:*[1], $patterns))
                        then (pat:allows-annotations(pat:*[1], $patterns) or
                              pat:allows-annotations(pat:*[2], $patterns))
                        else pat:allows-annotations(pat:*[1], $patterns)" />
</xsl:template>
  
<xsl:template match="pat:oneOrMore | pat:concurOneOrMore | pat:partition"
              mode="pat:allows-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:allows-annotations(pat:*[1], $patterns)" />
</xsl:template>  
  
<xsl:template match="pat:define | pat:start" mode="pat:allows-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="@allows-annotations">
      <xsl:sequence select="@allows-annotations = 'true'" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="pat:allows-annotations(pat:*[1], $patterns)" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  

<xsl:template match="pat:ref" mode="pat:allows-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:allows-annotations(pat:pattern(@name, $patterns), $patterns)" />
</xsl:template>  
  
<xsl:template match="pat:*" mode="pat:allows-annotations" as="xs:boolean">
  <xsl:sequence select="false()" />
</xsl:template>  
  
<!-- *** pat:only-annotations() *** -->
  
<xsl:function name="pat:only-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" />
  <xsl:sequence select="pat:only-annotations($patterns[1], $patterns)" />
</xsl:function>  
  
<xsl:function name="pat:only-annotations" as="xs:boolean">
  <xsl:param name="pattern" as="element()" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:apply-templates select="$pattern" mode="pat:only-annotations">
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:function>
  
<xsl:template match="pat:annotation" mode="pat:only-annotations" as="xs:boolean">
  <xsl:sequence select="true()" />
</xsl:template>    
  
<xsl:template match="pat:choice 
                     | pat:interleave 
                     | pat:concur 
                     | pat:group 
                     | pat:after"
              mode="pat:only-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:only-annotations(pat:*[1], $patterns) and 
                        pat:only-annotations(pat:*[2], $patterns)" />
</xsl:template>
  
<xsl:template match="pat:all" mode="pat:only-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:only-annotations(pat:*[1], $patterns) or 
                        pat:only-annotations(pat:*[2], $patterns)" />
</xsl:template>

<xsl:template match="pat:oneOrMore | pat:concurOneOrMore | pat:partition"
              mode="pat:only-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:only-annotations(pat:*[1], $patterns)" />
</xsl:template>  
  
<xsl:template match="pat:define | pat:start" mode="pat:only-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="@only-annotations">
      <xsl:sequence select="@only-annotations = 'true'" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="pat:only-annotations(pat:*[1], $patterns)" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  

<xsl:template match="pat:ref" mode="pat:only-annotations" as="xs:boolean">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:only-annotations(pat:pattern(@name, $patterns), $patterns)" />
</xsl:template>  
  
<xsl:template match="pat:*" mode="pat:only-annotations" as="xs:boolean">
  <xsl:sequence select="false()" />
</xsl:template>  
    
</xsl:stylesheet>
