<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:test="http://www.jenitennison.com/xslt/unit-test"
                exclude-result-prefixes="xs test"
                xmlns:ev="http://lmnl.net/ns/event"
                xmlns:syn="http://lmnl.net/ns/event/syntactic">
  
<xsl:template match="/">
  <xsl:apply-templates select="." mode="ev:parse-lmnl" />
</xsl:template>
  
<xsl:template match="node() | /" mode="ev:parse-lmnl">
  <xsl:call-template name="ev:parse-lmnl">
    <xsl:with-param name="lmnl" select="string(.)" />
  </xsl:call-template>
</xsl:template>  
  
<test:tests>
  <test:title>Parsing LMNL</test:title>
  <!--
  <test:test>
    <test:title>Text</test:title>
    <test:param name="lmnl" select="string(/text())">test</test:param>
    <test:expect>
      <ev:events>
        <ev:text chars="test" ws="false" hash="text('false')" line="1" col="0" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Atom</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      {{test}}
    </test:param>
    <test:expect>
      <ev:events>
        <ev:atom-open name="test" ns="" hash="atom-open('','test')" line="1" col="0" />
        <ev:atom-close name="test" ns="" hash="atom-close('','test')" line="1" col="6" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Text and atom</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      ...{{test}}...
    </test:param>
    <test:expect>
      <ev:events>
        <ev:text chars="..." ws="false" hash="text('false')" line="1" col="0" />
        <ev:atom-open name="test" ns="" hash="atom-open('','test')" line="1" col="3" />
        <ev:atom-close name="test" ns="" hash="atom-close('','test')" line="1" col="9" />
        <ev:text chars="..." ws="false" hash="text('false')" line="1" col="11" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Empty range</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [test]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="test" ns="" id="" line="1" col="0" />
        <ev:start-tag-close name="test" ns="" id="" line="1" col="5" />
        <ev:end-tag-open name="test" ns="" id="" line="1" col="5" />
        <ev:end-tag-close name="test" ns="" id="" line="1" col="5" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Simple range</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [test}...{test]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="test" ns="" id="" line="1" col="0" />
        <ev:start-tag-close name="test" ns="" id="" line="1" col="5" />
        <ev:text chars="..." ws="false" line="1" col="6" />
        <ev:end-tag-open name="test" ns="" id="" line="1" col="9" />
        <ev:end-tag-close name="test" ns="" id="" line="1" col="14" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Simple range in text</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      ...[test}...{test]...
    </test:param>
    <test:expect>
      <ev:events>
        <ev:text chars="..." ws="false" line="1" col="0" />
        <ev:start-tag-open name="test" ns="" id="" line="1" col="3" />
        <ev:start-tag-close name="test" ns="" id="" line="1" col="8" />
        <ev:text chars="..." ws="false" line="1" col="9" />
        <ev:end-tag-open name="test" ns="" id="" line="1" col="12" />
        <ev:end-tag-close name="test" ns="" id="" line="1" col="17" />
        <ev:text chars="..." ws="false" line="1" col="18" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Non-overlapping ranges</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [foo}...{foo]...[bar}...{bar]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="foo" ns="" id="" />
        <ev:start-tag-close name="foo" ns="" id="" />
        <ev:text chars="..." ws="false" />
        <ev:end-tag-open name="foo" ns="" id="" />
        <ev:end-tag-close name="foo" ns="" id="" />
        <ev:text chars="..." ws="false" />
        <ev:start-tag-open name="bar" ns="" id="" />
        <ev:start-tag-close name="bar" ns="" id="" />
        <ev:text chars="..." ws="false" />
        <ev:end-tag-open name="bar" ns="" id="" />
        <ev:end-tag-close name="bar" ns="" id="" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Overlapping ranges</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [foo}...[bar}...{foo]...{bar]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="foo" ns="" id="" />
        <ev:start-tag-close name="foo" ns="" id="" />
        <ev:text chars="..." ws="false" />
        <ev:start-tag-open name="bar" ns="" id="" />
        <ev:start-tag-close name="bar" ns="" id="" />
        <ev:text chars="..." ws="false" />
        <ev:end-tag-open name="foo" ns="" id="" />
        <ev:end-tag-close name="foo" ns="" id="" />
        <ev:text chars="..." ws="false" />
        <ev:end-tag-open name="bar" ns="" id="" />
        <ev:end-tag-close name="bar" ns="" id="" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Identical ranges</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [foo}[bar}...{bar]{foo]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="foo" ns="" id="" />
        <ev:start-tag-close name="foo" ns="" id="" />
        <ev:start-tag-open name="bar" ns="" id="" />
        <ev:start-tag-close name="bar" ns="" id="" />
        <ev:text chars="..." ws="false" />
        <ev:end-tag-open name="bar" ns="" id="" />
        <ev:end-tag-close name="bar" ns="" id="" />
        <ev:end-tag-open name="foo" ns="" id="" />
        <ev:end-tag-close name="foo" ns="" id="" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Simple annotation</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [foo [bar]]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="foo" ns="" id="" />
        <ev:start-annotation-open name="bar" ns="" />
        <ev:start-annotation-close name="bar" ns="" />
        <ev:end-annotation-open name="bar" ns="" />
        <ev:end-annotation-close name="bar" ns="" />
        <ev:start-tag-close name="foo" ns="" id="" />
        <ev:end-tag-open name="foo" ns="" id="" />
        <ev:end-tag-close name="foo" ns="" id="" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Text annotation</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [foo [bar}...{bar]]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="foo" ns="" id="" />
        <ev:start-annotation-open name="bar" ns="" />
        <ev:start-annotation-close name="bar" ns="" />
        <ev:text chars="..." ws="false" />
        <ev:end-annotation-open name="bar" ns="" />
        <ev:end-annotation-close name="bar" ns="" />
        <ev:start-tag-close name="foo" ns="" id="" />
        <ev:end-tag-open name="foo" ns="" id="" />
        <ev:end-tag-close name="foo" ns="" id="" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Annotation with internal range</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [foo [bar}...[baz]...{bar]]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="foo" ns="" id="" />
        <ev:start-annotation-open name="bar" ns="" />
        <ev:start-annotation-close name="bar" ns="" />
        <ev:text chars="..." ws="false" />
        <ev:start-tag-open name="baz" ns="" id="" />
        <ev:start-tag-close name="baz" ns="" id="" />
        <ev:end-tag-open name="baz" ns="" id="" />
        <ev:end-tag-close name="baz" ns="" id="" />
        <ev:text chars="..." ws="false" />
        <ev:end-annotation-open name="bar" ns="" />
        <ev:end-annotation-close name="bar" ns="" />
        <ev:start-tag-close name="foo" ns="" id="" />
        <ev:end-tag-open name="foo" ns="" id="" />
        <ev:end-tag-close name="foo" ns="" id="" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Two annotations</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [foo [bar}...{bar] [baz}...{baz]]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="foo" ns="" id="" />
        <ev:start-annotation-open name="bar" ns="" />
        <ev:start-annotation-close name="bar" ns="" />
        <ev:text chars="..." ws="false" />
        <ev:end-annotation-open name="bar" ns="" />
        <ev:end-annotation-close name="bar" ns="" />
        <ev:start-annotation-open name="baz" ns="" />
        <ev:start-annotation-close name="baz" ns="" />
        <ev:text chars="..." ws="false" />
        <ev:end-annotation-open name="baz" ns="" />
        <ev:end-annotation-close name="baz" ns="" />
        <ev:start-tag-close name="foo" ns="" id="" />
        <ev:end-tag-open name="foo" ns="" id="" />
        <ev:end-tag-close name="foo" ns="" id="" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Overlapping annotations (error)</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [foo [bar}...[baz}...{bar]...{baz]]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="foo" ns="" id="" />
        <ev:start-annotation-open name="bar" ns="" />
        <ev:start-annotation-close name="bar" ns="" />
        <ev:text chars="..." ws="false" />
        <ev:start-tag-open name="baz" ns="" id="" />
        <ev:start-tag-close name="baz" ns="" id="" />
        <ev:text chars="..." ws="false" />
        <ev:error parsing="end tag"
                  found="{bar]...{baz]]"
                  message="end tag doesn't match any open start tag"/>
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Nested annotations</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [foo [bar [baz}...{baz]}...{bar]]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="foo" ns="" id="" />
        <ev:start-annotation-open name="bar" ns="" />
        <ev:start-annotation-open name="baz" ns="" />
        <ev:start-annotation-close name="baz" ns="" />
        <ev:text chars="..." ws="false" />
        <ev:end-annotation-open name="baz" ns="" />
        <ev:end-annotation-close name="baz" ns="" />
        <ev:start-annotation-close name="bar" ns="" />
        <ev:text chars="..." ws="false" />
        <ev:end-annotation-open name="bar" ns="" />
        <ev:end-annotation-close name="bar" ns="" />
        <ev:start-tag-close name="foo" ns="" id="" />
        <ev:end-tag-open name="foo" ns="" id="" />
        <ev:end-tag-close name="foo" ns="" id="" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>l range</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [l}...{l]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="l" ns="" id="" />
        <ev:start-tag-close name="l" ns="" id="" />
        <ev:text chars="..." ws="false" />
        <ev:end-tag-open name="l" ns="" id="" />
        <ev:end-tag-close name="l" ns="" id="" />
      </ev:events>
    </test:expect>
  </test:test>
  -->
  <test:test>
    <test:title>Simple annotation with shortened end-tag</test:title>
    <test:param name="lmnl" select="normalize-space(/text())">
      [foo [bar}...{]]
    </test:param>
    <test:expect>
      <ev:events>
        <ev:start-tag-open name="foo" prefix="" ns="" id="" hash="start-tag('','foo','')" line="1" col="0" depth="0" />
        <ev:start-annotation-open name="bar" prefix="" ns="" hash="start-annotation('','bar')" line="1" col="5" depth="1" />
        <ev:start-annotation-close name="bar" prefix="" ns="" hash="start-annotation-close('','bar')" line="1" col="9" depth="1" />
        <ev:text chars="..." ws="false" hash="text('false')" line="1" col="10" />
        <ev:end-annotation-open name="bar" prefix="" ns="" hash="end-annotation('','bar')" line="1" col="13" depth="1" />
        <ev:end-annotation-close name="bar" prefix="" ns="" hash="end-annotation-close('','bar')" line="1" col="13" depth="1" />
        <ev:start-tag-close name="foo" prefix="" ns="" id="" hash="start-tag-close('','foo','')" line="1" col="15" depth="0" />
        <ev:end-tag-open name="foo" prefix="" ns="" id="" hash="end-tag('','foo','')" line="1" col="15" depth="0" />
        <ev:end-tag-close name="foo" prefix="" ns="" id="" hash="end-tag-close('','foo','')" line="1" col="15" depth="0" />
      </ev:events>
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Namespaced range</test:title>
    <test:param name="lmnl" select="string(.)">[!ns x="http://www.example.com/"]
[x:foo}...{x:foo]</test:param>
    <test:expect>
      <ev:events>
        <syn:ns-decl prefix="x" ns="http://www.example.com/" hash="ns-decl('x')" line="1" col="0"  />
        <ev:text chars="&#xA;" ws="true" hash="text('true')" line="1" col="33" />
        <ev:start-tag-open name="foo" prefix="x" ns="http://www.example.com/" id="" hash="start-tag('http://www.example.com/','foo','')" line="2" col="0" depth="0" />
        <ev:start-tag-close name="foo" prefix="x" ns="http://www.example.com/" id="" hash="start-tag-close('http://www.example.com/','foo','')" line="2" col="6" depth="0" />
        <ev:text chars="..." ws="false" hash="text('false')" line="2" col="7" />
        <ev:end-tag-open name="foo" prefix="x" ns="http://www.example.com/" id="" hash="end-tag('http://www.example.com/','foo','')" line="2" col="10" depth="0" />
        <ev:end-tag-close name="foo" prefix="x" ns="http://www.example.com/" id="" hash="end-tag-close('http://www.example.com/','foo','')" line="2" col="16" depth="0" />
      </ev:events>
    </test:expect>
  </test:test>
</test:tests>  
<xsl:template name="ev:parse-lmnl">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <ev:events>
    <xsl:call-template name="ev:parse-document">
      <xsl:with-param name="lmnl" select="$lmnl" />
      <xsl:with-param name="namespaces" tunnel="yes" as="document-node()">
        <xsl:document>
          <syn:namespaces />
        </xsl:document>
      </xsl:with-param>
      <xsl:with-param name="entities" tunnel="yes" as="document-node()">
        <xsl:document>
          <syn:entities>
            <syn:entity-decl prefix="" ns="" name="lsqb" content="&amp;#91;" />
            <syn:entity-decl prefix="" ns="" name="rsqb" content="]" />
            <syn:entity-decl prefix="" ns="" name="lcub" content="&amp;#123;" />
            <!-- Note: doubling up of }} due to XSLT interpretation of attribute
                 value templates -->
            <syn:entity-decl prefix="" ns="" name="rcub" content="}}" />
            <syn:entity-decl prefix="" ns="" name="amp" content="&amp;#38;" />
            <syn:entity-decl prefix="" ns="" name="apos" content="'" />
            <syn:entity-decl prefix="" ns="" name="quot" content='"' />
          </syn:entities>
        </xsl:document>
      </xsl:with-param>
      <!-- $open-ranges is a sequence of the ev:X-open elements that are
           currently open. This is set to an empty sequence when metadata
           is being parsed. -->
      <xsl:with-param name="open-ranges" tunnel="yes" select="()" />
      <!-- $open-documents is a sequence of ev:document elements which
           hold information about the documents that are currently being
           processed, namely the currently open ranges within them. A
           new document is added to the stack each time you enter a tag,
           and the first document removed each time you leave a tag. The
           first ev:X-open element within a ev:document is the currently
           open tag. -->
      <xsl:with-param name="open-documents" tunnel="yes" select="()" />
      <!-- $open-entities is a sequence of ev:entity-open elements that 
           are currently open. -->
      <xsl:with-param name="open-entities" tunnel="yes" select="()" />
      <xsl:with-param name="line" tunnel="yes" select="1" />
      <xsl:with-param name="col" tunnel="yes" select="0" />
      <xsl:with-param name="depth" tunnel="yes" select="0" />
    </xsl:call-template>
  </ev:events>
</xsl:template>
  
<xsl:template name="ev:parse-document">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="open-ranges" as="element()*" tunnel="yes" required="yes" />
  <xsl:param name="open-documents" as="element(ev:document)*" tunnel="yes" required="yes" />
  <xsl:choose>
    <xsl:when test="$lmnl = ''">
      <xsl:choose>
        <xsl:when test="exists($open-ranges)">
          <xsl:call-template name="ev:error">
            <xsl:with-param name="message" 
              select="concat('end of document, but not all ranges are closed: ',
                             string-join($open-ranges/concat(@name, '=', @id), ', '))" />
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="exists($open-documents)">
          <xsl:call-template name="ev:error">
            <xsl:with-param name="message" 
              select="concat('end of document, but not all documents are closed: ',
                             string-join($open-documents/@in, ', '))" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>Parse completed successfully</xsl:message>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:when test="starts-with($lmnl, '[!--')">
      <xsl:call-template name="ev:parse-comment">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="starts-with($lmnl, '[!')">
      <xsl:call-template name="ev:parse-declaration">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="starts-with($lmnl, '[')">
      <xsl:call-template name="ev:parse-start-tag">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="starts-with($lmnl, '{{')">
      <xsl:call-template name="ev:parse-atom">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="starts-with($lmnl, '{')">
      <xsl:call-template name="ev:parse-end-tag">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>      
    </xsl:when>
    <xsl:when test="starts-with($lmnl, '&amp;#')">
      <xsl:call-template name="ev:parse-character-reference">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="starts-with($lmnl, '&amp;')">
      <xsl:call-template name="ev:parse-entity">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="ev:parse-text">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  

<xsl:template name="ev:parse-escaped-content">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:choose>
    <xsl:when test="contains($lmnl, '[') or 
                    contains($lmnl, '{')">
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'escaped content'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="starts-with($lmnl, '&amp;#')">
      <xsl:call-template name="ev:parse-character-reference">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="starts-with($lmnl, '&amp;')">
      <xsl:call-template name="ev:parse-entity">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="text-regex">^([^\[\{&amp;]+)(.*)$</xsl:variable>
      <xsl:analyze-string select="$lmnl" flags="s" regex="{$text-regex}">
        <xsl:matching-substring>
          <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
          <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
          <xsl:variable name="text" as="xs:string" select="regex-group(1)" />
          <xsl:variable name="rest" as="xs:string" select="regex-group(2)" />
          <xsl:variable name="ws" as="xs:boolean" select="normalize-space($text) = ''" />
          <ev:text chars="{$text}" ws="{$ws}" hash="text('{$ws}')" line="{$line}" col="{$col}" />
          <xsl:call-template name="ev:parse-escaped-content">
            <xsl:with-param name="lmnl" select="$rest" />
            <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
            <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
          </xsl:call-template>
        </xsl:matching-substring>
        <xsl:non-matching-substring>
          <xsl:call-template name="ev:error">
            <xsl:with-param name="parsing" select="'escaped content'" />
            <xsl:with-param name="found" select="$lmnl" />
          </xsl:call-template>
        </xsl:non-matching-substring>
      </xsl:analyze-string>
    </xsl:otherwise>
  </xsl:choose>  
</xsl:template>  
  
<test:tests>
  <test:title>Character References</test:title>
  <test:test>
    <test:title>Numeric non-breaking space</test:title>
    <test:param name="lmnl" select="'&amp;#160;'" />
    <test:param name="line" select="1" tunnel="yes" />
    <test:param name="col" select="0" tunnel="yes" />
    <test:param name="open-ranges" select="()" tunnel="yes" />
    <test:param name="open-documents" select="()" tunnel="yes" />
    <test:expect>
      <ev:text chars="&#160;" ws="false" hash="text('false')" line="1" col="0" />
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Hex non-breaking space</test:title>
    <test:param name="lmnl" select="'&amp;#xA0;'" />
    <test:param name="line" select="1" tunnel="yes" />
    <test:param name="col" select="0" tunnel="yes" />
    <test:param name="open-ranges" select="()" tunnel="yes" />
    <test:param name="open-documents" select="()" tunnel="yes" />
    <test:expect>
      <ev:text chars="&#160;" ws="false" hash="text('false')" line="1" col="0" />
    </test:expect>
  </test:test>
</test:tests>
<xsl:template name="ev:parse-character-reference">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:variable name="text-regex">^(&amp;#(([0-9]+)|(x[0-9a-fA-F]+));)(.*)$</xsl:variable>
  <xsl:analyze-string select="$lmnl" flags="s" regex="{$text-regex}">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="rest" as="xs:string" select="regex-group(5)" />
      <xsl:variable name="text" as="xs:string">
        <xsl:choose>
          <xsl:when test="regex-group(3)">
            <xsl:sequence select="codepoints-to-string(xs:integer(regex-group(3)))" />
          </xsl:when>
          <xsl:when test="regex-group(4)">
            <xsl:sequence
              select="codepoints-to-string(ev:hex-to-int(substring(regex-group(4),
              2)))" />
          </xsl:when>
        </xsl:choose>        
      </xsl:variable>      
      <xsl:variable name="ws" as="xs:boolean" select="normalize-space($text) = ''" />
      <ev:text chars="{$text}" ws="{$ws}" hash="text('{$ws}')" line="{$line}" col="{$col}" />
      <xsl:call-template name="ev:parse-document">
        <xsl:with-param name="lmnl" select="$rest" />
        <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
        <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
      </xsl:call-template>
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'character reference'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>  

<xsl:key name="entities" match="syn:entity-decl" use="QName(@ns, @name)" />  

<test:tests>
  <test:title>Entities</test:title>
  <test:test>
    <test:title>Simple text entity</test:title>
    <test:param name="lmnl" select="'&amp;foo;...'" />
    <test:param name="entities" select="/" tunnel="yes">
      <syn:entities>
        <syn:entity-decl prefix="" ns="" name="foo" content="foo" />
      </syn:entities>
    </test:param>
    <test:param name="namespaces" select="/" tunnel="yes">
      <ev:namespaces />
    </test:param>
    <test:param name="line" select="10" tunnel="yes" />
    <test:param name="col" select="5" tunnel="yes" />
    <test:param name="open-ranges" select="()" tunnel="yes" />
    <test:param name="open-documents" select="()" tunnel="yes" />
    <test:param name="open-entities" select="()" tunnel="yes" />
    <test:param name="depth" select="0" tunnel="yes" />
    <test:expect>
      <syn:entity-open prefix="" ns="" name="foo" hash="entity-open('','foo')" depth="0" line="10" col="5" />
      <ev:text chars="foo" ws="false" hash="text('false')" line="1" col="0" />
      <syn:entity-close prefix="" ns="" name="foo" hash="entity-close('','foo')" depth="0" line="10" col="5" />
      <ev:text chars="..." ws="false" hash="text('false')" line="10" col="10" />
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Entity with another entity inside</test:title>
    <test:param name="lmnl" select="'&amp;foo;'" />
    <test:param name="entities" select="/" tunnel="yes">
      <syn:entities>
        <syn:entity-decl prefix="" ns="" name="foo" content="&amp;bar;" />
        <syn:entity-decl prefix="" ns="" name="bar" content="bar" />
      </syn:entities>
    </test:param>
    <test:param name="namespaces" select="/" tunnel="yes">
      <ev:namespaces />
    </test:param>
    <test:param name="line" select="10" tunnel="yes" />
    <test:param name="col" select="5" tunnel="yes" />
    <test:param name="open-ranges" select="()" tunnel="yes" />
    <test:param name="open-documents" select="()" tunnel="yes" />
    <test:param name="open-entities" select="()" tunnel="yes" />
    <test:param name="depth" select="0" tunnel="yes" />
    <test:expect>
      <syn:entity-open prefix="" ns="" name="foo" hash="entity-open('','foo')" depth="0" line="10" col="5" />
      <syn:entity-open prefix="" ns="" name="bar" hash="entity-open('','bar')" depth="1" line="1" col="0" />
      <ev:text chars="bar" ws="false" hash="text('false')" line="1" col="0" />
      <syn:entity-close prefix="" ns="" name="bar" hash="entity-close('','bar')" depth="1" line="1" col="0" />
      <syn:entity-close prefix="" ns="" name="foo" hash="entity-close('','foo')" depth="0" line="10" col="5" />
    </test:expect>
  </test:test>
  <test:test>
    <test:title>Recursive entity declaration</test:title>
    <test:param name="lmnl" select="'&amp;foo;'" />
    <test:param name="entities" select="/" tunnel="yes">
      <syn:entities>
        <syn:entity-decl prefix="" ns="" name="foo" content="&amp;bar;" />
        <syn:entity-decl prefix="" ns="" name="bar" content="&amp;foo;" />
      </syn:entities>
    </test:param>
    <test:param name="namespaces" select="/" tunnel="yes">
      <ev:namespaces />
    </test:param>
    <test:param name="line" select="10" tunnel="yes" />
    <test:param name="col" select="5" tunnel="yes" />
    <test:param name="open-ranges" select="()" tunnel="yes" />
    <test:param name="open-documents" select="()" tunnel="yes" />
    <test:param name="open-entities" select="()" tunnel="yes" />
    <test:param name="depth" select="0" tunnel="yes" />
    <test:expect>
      <syn:entity-open prefix="" ns="" name="foo" hash="entity-open('','foo')" depth="0" line="10" col="5" />
      <syn:entity-open prefix="" ns="" name="bar" hash="entity-open('','bar')" depth="1" line="1" col="0" />
      <ev:error parsing="entity" message="detected recursive entity declaration for {}foo" found="" line="1" col="0" />
    </test:expect>
  </test:test>
</test:tests>
<xsl:template name="ev:parse-entity">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="entities" as="document-node()" tunnel="yes" required="yes" />
  <xsl:param name="open-entities" as="element(syn:entity-open)*" tunnel="yes" required="yes" />
  <xsl:param name="namespaces" as="document-node()" tunnel="yes" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="depth" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:variable name="text-regex">^(&amp;((\i\c*):)?(\i\c*);)(.*)$</xsl:variable>
  <xsl:analyze-string select="$lmnl" flags="s" regex="{$text-regex}">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="prefix" as="xs:string" select="regex-group(3)" />
      <xsl:variable name="ns" as="xs:string" select="ev:ns($prefix, $namespaces, $line, $col)" />
      <xsl:variable name="name" as="xs:string" select="regex-group(4)" />
      <xsl:choose>
        <xsl:when test="$open-entities[@name = $name and @ns = $ns]">
          <xsl:call-template name="ev:error">
            <xsl:with-param name="parsing" select="'entity'" />
            <xsl:with-param name="message" select="concat('detected recursive entity declaration for {', $ns, '}', $name)" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="rest" as="xs:string" select="regex-group(5)" />
          <xsl:variable name="text" as="xs:string" select="key('entities', QName($ns, $name), $entities)/@content" />
          <xsl:variable name="ws" as="xs:boolean" select="normalize-space($text) = ''" />
          <xsl:variable name="entity-open" as="element(syn:entity-open)">
            <syn:entity-open prefix="{$prefix}" ns="{$ns}" name="{$name}" depth="{$depth}" hash="entity-open('{$ns}','{$name}')" line="{$line}" col="{$col}" />
          </xsl:variable>
          <xsl:sequence select="$entity-open" />
          <xsl:variable name="events" as="element()*">
            <xsl:call-template name="ev:parse-escaped-content">
              <xsl:with-param name="lmnl" select="$text" />
              <xsl:with-param name="line" select="1" tunnel="yes" />
              <xsl:with-param name="col" select="0" tunnel="yes" />
              <xsl:with-param name="depth" select="$depth + 1" tunnel="yes" />
              <xsl:with-param name="open-entities" select="$entity-open, $open-entities" tunnel="yes" />
            </xsl:call-template>
          </xsl:variable>
          <xsl:sequence select="$events" />
          <xsl:if test="not($events[last()] instance of element(ev:error))">
            <syn:entity-close prefix="{$prefix}" ns="{$ns}" name="{$name}" depth="{$depth}" hash="entity-close('{$ns}','{$name}')" line="{$line}" col="{$col}" />
            <xsl:call-template name="ev:parse-document">
              <xsl:with-param name="lmnl" select="$rest" />
              <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
              <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
            </xsl:call-template>
          </xsl:if>
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'entity'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>  
  
<xsl:template name="ev:parse-text">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:variable name="text-regex">^([^\[\{&amp;]+)(.*)$</xsl:variable>
  <xsl:analyze-string select="$lmnl" flags="s" regex="{$text-regex}">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="text" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="rest" as="xs:string" select="regex-group(2)" />
      <xsl:variable name="ws" as="xs:boolean" select="normalize-space($text) = ''" />
      <ev:text chars="{$text}" ws="{$ws}" hash="text('{$ws}')" line="{$line}" col="{$col}" />
      <xsl:call-template name="ev:parse-document">
        <xsl:with-param name="lmnl" select="$rest" />
        <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
        <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
      </xsl:call-template>
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'text'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>
  
<xsl:template name="ev:parse-atom">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="open-documents" as="element(ev:document)*" tunnel="yes" required="yes" />
  <xsl:param name="open-ranges" as="element()*" tunnel="yes" required="yes" />
  <xsl:param name="namespaces" as="document-node()" tunnel="yes" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="depth" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:variable name="start-tag-regex">^(\{\{(((\i\c*):)?(\i\c*)))(.+)$</xsl:variable>
  <xsl:analyze-string select="$lmnl" flags="s" regex="{$start-tag-regex}">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="prefix" as="xs:string" select="regex-group(4)" />
      <xsl:variable name="ns" as="xs:string" select="ev:ns($prefix, $namespaces, $line, $col)" />
      <xsl:variable name="name" as="xs:string" select="regex-group(5)" />
      <xsl:variable name="rest" as="xs:string" select="regex-group(6)" />
      <xsl:variable name="atom-open" as="element()">
        <ev:atom-open name="{$name}" prefix="{$prefix}" ns="{$ns}" hash="atom-open('{$ns}','{$name}')" line="{$line}" col="{$col}" depth="{$depth}" />
      </xsl:variable>
      <xsl:sequence select="$atom-open" />
      <xsl:call-template name="ev:parse-initial-metadata">
        <xsl:with-param name="lmnl" select="$rest" />
        <xsl:with-param name="open-ranges" select="()" tunnel="yes" />
        <xsl:with-param name="open-documents" as="element(ev:document)+" tunnel="yes">
          <ev:document in="atom">
            <xsl:sequence select="$atom-open, $open-ranges" />
          </ev:document>
          <xsl:sequence select="$open-documents" />
        </xsl:with-param>
        <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
        <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
        <xsl:with-param name="depth" select="$depth + 1" tunnel="yes" />
      </xsl:call-template>
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'atom'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>  
  
<xsl:template name="ev:parse-start-tag">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="namespaces" as="document-node()" tunnel="yes" required="yes" />
  <xsl:param name="open-ranges" as="element()*" tunnel="yes" required="yes" />
  <xsl:param name="open-documents" as="element(ev:document)*" tunnel="yes" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="depth" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:variable name="start-tag-regex">^(\[(((\i\c*):)?(\i\c*))?(\s?=(\i\c*))?)(.+)$</xsl:variable>
  <xsl:analyze-string select="$lmnl" flags="s" regex="{$start-tag-regex}">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="prefix" as="xs:string" select="regex-group(4)" />
      <xsl:variable name="ns" as="xs:string" select="ev:ns($prefix, $namespaces, $line, $col)" />
      <xsl:variable name="name" as="xs:string" select="regex-group(5)" />
      <xsl:variable name="id" as="xs:string" select="regex-group(7)" />
      <xsl:variable name="rest" as="xs:string" select="regex-group(8)" />
      <xsl:variable name="tag" as="element(ev:start-tag-open)">
        <ev:start-tag-open name="{$name}" prefix="{$prefix}" ns="{$ns}" id="{$id}" 
          hash="start-tag('{$ns}','{$name}','{$id}')"
          line="{$line}" col="{$col}" depth="{$depth}" />
      </xsl:variable>
      <xsl:sequence select="$tag" />
      <xsl:call-template name="ev:parse-initial-metadata">
        <xsl:with-param name="lmnl" select="$rest" />
        <xsl:with-param name="open-ranges" select="()" tunnel="yes" />
        <xsl:with-param name="open-documents" as="element(ev:document)+" tunnel="yes">
          <ev:document in="start-tag">
            <xsl:sequence select="($tag, $open-ranges)" />
          </ev:document>
          <xsl:sequence select="$open-documents" />
        </xsl:with-param>
        <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
        <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
        <xsl:with-param name="depth" select="$depth + 1" tunnel="yes" />
      </xsl:call-template>
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'start tag'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>
  
<xsl:template name="ev:parse-end-tag">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="namespaces" as="document-node()" tunnel="yes" required="yes" />
  <xsl:param name="open-ranges" as="element()*" tunnel="yes" required="yes" />
  <xsl:param name="open-documents" as="element(ev:document)*" tunnel="yes" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="depth" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:variable name="end-tag-regex">^(\{(((\i\c*):)?(\i\c*))?(\s?=(\i\c*)\s?)?)(.+)$</xsl:variable>
  <xsl:analyze-string select="$lmnl" flags="s" regex="{$end-tag-regex}">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="prefix" as="xs:string" select="regex-group(4)" />
      <xsl:variable name="ns" as="xs:string" select="ev:ns($prefix, $namespaces, $line, $col)" />
      <xsl:variable name="name" as="xs:string" select="regex-group(5)" />
      <xsl:variable name="id" as="xs:string" select="regex-group(7)" />
      <xsl:variable name="rest" as="xs:string" select="regex-group(8)" />
      <xsl:choose>
        <xsl:when test="$open-ranges instance of element(ev:start-annotation-open)">
          <xsl:variable name="open-annotation" as="element(ev:start-annotation-open)" 
            select="$open-ranges" />
          <xsl:choose>
            <xsl:when test="starts-with($lmnl, '{]')">
              <ev:end-annotation-open name="{$open-annotation/@name}"
                                      prefix="{$open-annotation/@prefix}"
                                      ns="{$open-annotation/@ns}"
                                      hash="end-annotation('{$open-annotation/@ns}','{$open-annotation/@name}')"
                                      line="{$line}" col="{$col}" depth="{$depth}" />
              <ev:end-annotation-close name="{$open-annotation/@name}"
                                       prefix="{$open-annotation/@prefix}"
                                       ns="{$open-annotation/@ns}"
                                       hash="end-annotation-close('{$open-annotation/@ns}','{$open-annotation/@name}')"
                                       line="{$line}" col="{$col}" depth="{$depth}" />
              <xsl:call-template name="ev:parse-metadata">
                <xsl:with-param name="lmnl" select="substring($lmnl, 3)" />
                <xsl:with-param name="open-ranges" select="()" tunnel="yes" />
                <xsl:with-param name="col" select="$col + 2" tunnel="yes" />
              </xsl:call-template>
            </xsl:when>
            <xsl:when test="$open-annotation/@name eq $name and
                            $open-annotation/@ns eq $ns">
              <xsl:variable name="end-annotation" as="element()">
                <ev:end-annotation-open name="{$name}"
                                        prefix="{$prefix}" ns="{$ns}"
                                        hash="end-annotation('{$ns}','{$name}')"
                                        line="{$line}" col="{$col}" depth="{$depth}" />
              </xsl:variable>
              <xsl:sequence select="$end-annotation" />
              <xsl:call-template name="ev:parse-initial-metadata">
                <xsl:with-param name="lmnl" select="$rest" />
                <xsl:with-param name="open-ranges" select="()" tunnel="yes" />
                <xsl:with-param name="open-documents" as="element(ev:document)+" tunnel="yes">
                  <ev:document in="end-annotation">
                    <xsl:sequence select="$end-annotation" />
                  </ev:document>
                  <xsl:sequence select="$open-documents" />
                </xsl:with-param>
                <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
                <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
                <xsl:with-param name="depth" select="$depth + 1" tunnel="yes" />
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="ev:error">
                <xsl:with-param name="parsing" select="'end tag'" />
                <xsl:with-param name="found" select="$lmnl" />
                <xsl:with-param name="message" select="'end annotation doesn''t match open annotation'" />
              </xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="exists($open-ranges)">
          <xsl:variable name="matching-start-tag" as="element(ev:start-tag-open)*" 
            select="if ($id ne '')
                    then $open-ranges[@id eq $id]
                    else $open-ranges[self::ev:start-tag-open and
                                      @name eq $name and @ns eq $ns and @id eq ''][1]" />
          <xsl:choose>
            <xsl:when test="count($matching-start-tag) > 1">
              <xsl:call-template name="ev:error">
                <xsl:with-param name="parsing" select="'end tag'" />
                <xsl:with-param name="found" select="$lmnl" />
                <xsl:with-param name="message" select="'duplicate start tag id'" />
              </xsl:call-template>
            </xsl:when>
            <xsl:when test="exists($matching-start-tag)">
              <xsl:variable name="end-tag" as="element()">
                <ev:end-tag-open name="{$name}" prefix="{$prefix}" ns="{$ns}" id="{$id}" hash="end-tag('{$ns}','{$name}','{$id}')" 
                                 line="{$line}" col="{$col}" depth="{$depth}" />
              </xsl:variable>
              <xsl:sequence select="$end-tag" />
              <xsl:call-template name="ev:parse-initial-metadata">
                <xsl:with-param name="lmnl" select="$rest" />
                <xsl:with-param name="open-ranges" select="()" tunnel="yes" />
                <xsl:with-param name="open-documents" as="element(ev:document)+" tunnel="yes">
                  <ev:document in="end-tag">
                    <xsl:sequence select="$end-tag, ($open-ranges except $matching-start-tag)" />
                  </ev:document>
                  <xsl:sequence select="$open-documents" />
                </xsl:with-param>
                <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
                <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
                <xsl:with-param name="depth" select="$depth + 1" tunnel="yes" />
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="ev:error">
                <xsl:with-param name="parsing" select="'end tag'" />
                <xsl:with-param name="found" select="$lmnl" />
                <xsl:with-param name="message" select="'end tag doesn''t match any open start tag'" />
              </xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>      
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="ev:error">
            <xsl:with-param name="parsing" select="'end tag'" />
            <xsl:with-param name="found" select="$lmnl" />
            <xsl:with-param name="message" select="'no open range or annotation to match this end tag'" />
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'end tag'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>

<xsl:template name="ev:parse-initial-metadata">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:analyze-string select="$lmnl" flags="s" regex="^(\s+)(.+)$">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="rest" as="xs:string" select="regex-group(2)" />
      <xsl:call-template name="ev:parse-metadata">
        <xsl:with-param name="lmnl" select="$rest" />
        <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
        <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
      </xsl:call-template>
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:parse-metadata-end">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>

<xsl:template name="ev:parse-metadata">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:analyze-string select="$lmnl" flags="s" regex="^(\s*)(\[.+)$">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string*" select="tokenize($parsed, '\n')" />
      <xsl:call-template name="ev:parse-annotation">
        <xsl:with-param name="lmnl" select="regex-group(2)" />
        <xsl:with-param name="line" select="$line + (if (empty($lines)) then 0 else (count($lines) - 1))" tunnel="yes" />
        <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
      </xsl:call-template>
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:parse-metadata-end">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>  
  
<xsl:template name="ev:parse-metadata-end">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="open-documents" as="element(ev:document)*" tunnel="yes" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="depth" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:variable name="context" as="xs:string" select="$open-documents[1]/@in" />
  <xsl:variable name="ranges" as="element()*" select="$open-documents[1]/ev:*" />
  <xsl:variable name="name" as="xs:string" select="$ranges[1]/@name" />
  <xsl:variable name="ns" as="xs:string" select="$ranges[1]/@ns" />
  <xsl:variable name="prefix" as="xs:string" select="$ranges[1]/@prefix" />
  <xsl:variable name="id" as="xs:string" select="string($ranges[1]/@id)" />
  <xsl:choose>
    <xsl:when test="$context = 'atom' and 
                    starts-with($lmnl, '}}')">
      <ev:atom-close name="{$name}" prefix="{$prefix}" ns="{$ns}" hash="atom-close('{$ns}','{$name}')" 
                     line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <xsl:call-template name="ev:parse-document">
        <xsl:with-param name="lmnl" select="substring($lmnl, 3)" />
        <xsl:with-param name="open-ranges" select="$ranges[position() > 1]" tunnel="yes" />
        <xsl:with-param name="open-documents" select="$open-documents[position() > 1]" tunnel="yes" />
        <xsl:with-param name="col" select="$col + 2" tunnel="yes" />
        <xsl:with-param name="depth" select="$depth - 1" tunnel="yes" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$context = 'start-tag' and
                    starts-with($lmnl, '}')">
      <ev:start-tag-close name="{$name}" prefix="{$prefix}" ns="{$ns}" id="{$id}" hash="start-tag-close('{$ns}','{$name}','{$id}')" 
                          line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <xsl:call-template name="ev:parse-document">
        <xsl:with-param name="lmnl" select="substring($lmnl, 2)" />
        <xsl:with-param name="open-ranges" select="$ranges" tunnel="yes" />
        <xsl:with-param name="open-documents" select="$open-documents[position() > 1]" tunnel="yes" />
        <xsl:with-param name="col" select="$col + 1" tunnel="yes" />
        <xsl:with-param name="depth" select="$depth - 1" tunnel="yes" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$context = 'start-tag' and
                    starts-with($lmnl, ']')">
      <ev:start-tag-close name="{$name}" prefix="{$prefix}" ns="{$ns}" id="{$id}" hash="start-tag-close('{$ns}','{$name}','{$id}')" 
                          line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <ev:end-tag-open name="{$name}" prefix="{$prefix}" ns="{$ns}" id="{$id}" hash="end-tag('{$ns}','{$name}','{$id}')" 
                       line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <ev:end-tag-close name="{$name}" prefix="{$prefix}" ns="{$ns}" id="{$id}" hash="end-tag-close('{$ns}','{$name}','{$id}')" 
                        line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <xsl:call-template name="ev:parse-document">
        <xsl:with-param name="lmnl" select="substring($lmnl, 2)" />
        <xsl:with-param name="open-ranges" select="$ranges[position() > 1]" tunnel="yes" />
        <xsl:with-param name="open-documents" select="$open-documents[position() > 1]" tunnel="yes" />
        <xsl:with-param name="col" select="$col + 1" tunnel="yes" />
        <xsl:with-param name="depth" select="$depth - 1" tunnel="yes" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$context = 'end-tag' and
                    starts-with($lmnl, ']')">
      <ev:end-tag-close name="{$name}" prefix="{$prefix}" ns="{$ns}" id="{$id}" hash="end-tag-close('{$ns}','{$name}','{$id}')" 
                        line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <xsl:call-template name="ev:parse-document">
        <xsl:with-param name="lmnl" select="substring($lmnl, 2)" />
        <xsl:with-param name="open-ranges" select="$ranges[position() > 1]" tunnel="yes" />
        <xsl:with-param name="open-documents" select="$open-documents[position() > 1]" tunnel="yes" />
        <xsl:with-param name="col" select="$col + 1" tunnel="yes" />
        <xsl:with-param name="depth" select="$depth - 1" tunnel="yes" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$context = 'start-annotation' and
                    starts-with($lmnl, '}')">
      <ev:start-annotation-close name="{$name}" prefix="{$prefix}" ns="{$ns}" hash="start-annotation-close('{$ns}','{$name}')" 
                                 line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <xsl:call-template name="ev:parse-document">
        <xsl:with-param name="lmnl" select="substring($lmnl, 2)" />
        <xsl:with-param name="open-ranges" select="$ranges" tunnel="yes" />
        <xsl:with-param name="open-documents" select="$open-documents[position() > 1]" tunnel="yes" />
        <xsl:with-param name="col" select="$col + 1" tunnel="yes" />
        <xsl:with-param name="depth" select="$depth - 1" tunnel="yes" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$context = 'start-annotation' and
                    starts-with($lmnl, ']')">
      <ev:start-annotation-close name="{$name}" prefix="{$prefix}" ns="{$ns}" hash="start-annotation-close('{$ns}','{$name}')" 
                                 line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <ev:end-annotation-open name="{$name}" prefix="{$prefix}" ns="{$ns}" hash="end-annotation('{$ns}','{$name}')" 
                              line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <ev:end-annotation-close name="{$name}" prefix="{$prefix}" ns="{$ns}" hash="end-annotation-close('{$ns}','{$name}')" 
                               line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <xsl:call-template name="ev:parse-metadata">
        <xsl:with-param name="lmnl" select="substring($lmnl, 2)" />
        <xsl:with-param name="open-ranges" select="()" tunnel="yes" />
        <xsl:with-param name="open-documents" select="$open-documents[position() > 1]" tunnel="yes" />
        <xsl:with-param name="col" select="$col + 1" tunnel="yes" />
        <xsl:with-param name="depth" select="$depth - 1" tunnel="yes" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$context = 'end-annotation' and
                    starts-with($lmnl, ']')">
      <ev:end-annotation-close name="{$name}" prefix="{$prefix}" ns="{$ns}" hash="end-annotation-close('{$ns}','{$name}')" 
                               line="{$line}" col="{$col}" depth="{$depth - 1}" />
      <xsl:call-template name="ev:parse-metadata">
        <xsl:with-param name="lmnl" select="substring($lmnl, 2)" />
        <xsl:with-param name="open-ranges" select="()" tunnel="yes" />
        <xsl:with-param name="open-documents" select="$open-documents[position() > 1]" tunnel="yes" />
        <xsl:with-param name="col" select="$col + 1" tunnel="yes" />
        <xsl:with-param name="depth" select="$depth - 1" tunnel="yes" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'metadata'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>  
</xsl:template>  

<xsl:template name="ev:parse-annotation">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="namespaces" as="document-node()" tunnel="yes" select="/" />
  <xsl:param name="open-ranges" as="element()*" tunnel="yes" select="()" />
  <xsl:param name="open-documents" as="element(ev:document)*" tunnel="yes" select="()" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="depth" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:variable name="start-annotation-regex">^(\[(((\i\c*):)?(\i\c*)))(.+)$</xsl:variable>
  <xsl:analyze-string select="$lmnl" flags="s" regex="{$start-annotation-regex}">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="prefix" as="xs:string" select="regex-group(4)" />
      <xsl:variable name="ns" as="xs:string" select="ev:ns($prefix, $namespaces, $line, $col)" />
      <xsl:variable name="name" as="xs:string" select="regex-group(5)" />
      <xsl:variable name="rest" as="xs:string" select="regex-group(6)" />
      <xsl:variable name="annotation" as="element(ev:start-annotation-open)">
        <ev:start-annotation-open name="{$name}" prefix="{$prefix}" ns="{$ns}" hash="start-annotation('{$ns}','{$name}')" 
                                  line="{$line}" col="{$col}" depth="{$depth}" />
      </xsl:variable>
      <xsl:sequence select="$annotation" />
      <xsl:call-template name="ev:parse-initial-metadata">
        <xsl:with-param name="lmnl" select="$rest" />
        <xsl:with-param name="open-ranges" select="$annotation" tunnel="yes" />
        <xsl:with-param name="open-documents" as="element(ev:document)+" tunnel="yes">
          <ev:document in="start-annotation">
            <xsl:sequence select="$annotation" />
          </ev:document>
          <xsl:sequence select="$open-documents" />
        </xsl:with-param>
        <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
        <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
        <xsl:with-param name="depth" select="$depth + 1" tunnel="yes" />
      </xsl:call-template>
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'annotation'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>  

<xsl:template name="ev:parse-comment">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:variable name="comment-regex">^(\[!--(([^-]|(-[^-])|(--[^\]]))*)--\])(.*)$</xsl:variable>
  <xsl:analyze-string select="$lmnl" flags="s" regex="{$comment-regex}">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="content" as="xs:string" select="regex-group(2)" />
      <xsl:variable name="rest" as="xs:string" select="regex-group(6)" />
      <syn:comment content="{$content}" hash="comment()" line="{$line}" col="{$col}" />
      <!-- *** TODO: parsing comments in tags *** -->
      <xsl:call-template name="ev:parse-document">
        <xsl:with-param name="lmnl" select="$rest" />
        <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
        <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
      </xsl:call-template>
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'comment'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>  
  
<xsl:template name="ev:parse-declaration">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:choose>
    <xsl:when test="starts-with($lmnl, '[!entity')">
      <xsl:call-template name="ev:parse-entity-declaration">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="starts-with($lmnl, '[!ns')">
      <xsl:call-template name="ev:parse-ns-declaration">
        <xsl:with-param name="lmnl" select="$lmnl" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'declaration'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<xsl:template name="ev:parse-entity-declaration">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="namespaces" as="document-node()" tunnel="yes" required="yes" />
  <xsl:param name="entities" as="document-node()" tunnel="yes" required="yes" />
  <xsl:variable name="entity-decl-regex">^(\[!entity\s+(((\i\c*):)?(\i\c*))\s?=\s?(('[^']+')|("[^"]+"))\s*\])(.*)$</xsl:variable>
  <xsl:analyze-string select="$lmnl" flags="s" regex="{$entity-decl-regex}">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="prefix" as="xs:string" select="regex-group(4)" />
      <xsl:variable name="ns" as="xs:string" select="ev:ns($prefix, $namespaces, $line, $col)" />
      <xsl:variable name="name" as="xs:string" select="regex-group(5)" />
      <xsl:variable name="replacement" as="xs:string" select="substring(regex-group(6), 2, string-length(regex-group(6)) - 2)" />
      <xsl:variable name="rest" as="xs:string" select="regex-group(9)" />
      <xsl:variable name="existing" as="element(ev:entity)?" select="key('entities', QName($ns, $name), $entities)" />
      <xsl:choose>
        <xsl:when test="$existing != $replacement">
          <xsl:call-template name="ev:error">
            <xsl:with-param name="parsing" select="'entity declaration'" />
            <xsl:with-param name="found" select="$lmnl" />
            <xsl:with-param name="message" select="concat('the entity ', $prefix, ' has already been declared with different replacement text')" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="entity-decl" as="element(syn:entity-decl)">
            <syn:entity-decl prefix="{$prefix}" ns="{$ns}" name="{$name}" content="{$replacement}" 
                             line="{$line}" col="{$col}" hash="entity-decl('{$ns}','{$name}')" />
          </xsl:variable>
          <xsl:sequence select="$entity-decl" />
          <!-- *** TODO: parsing entity declarations in tags *** -->
          <xsl:call-template name="ev:parse-document">
            <xsl:with-param name="lmnl" select="$rest" />
            <xsl:with-param name="entities" as="document-node()" tunnel="yes">
              <xsl:document>
                <syn:entities>
                  <xsl:sequence select="$entities/syn:entities/syn:entity-decl, $entity-decl" />
                </syn:entities>
              </xsl:document>
            </xsl:with-param>
            <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
            <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'entity declaration'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>  

<xsl:template name="ev:parse-ns-declaration">
  <xsl:param name="lmnl" as="xs:string" required="yes" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="namespaces" as="document-node()" tunnel="yes" required="yes" />
  <xsl:variable name="ns-decl-regex">^(\[!ns\s+(\i\c*)\s?=\s?(('[^']+')|("[^"]+"))\s*\])(.*)$</xsl:variable>
  <xsl:analyze-string select="$lmnl" flags="s" regex="{$ns-decl-regex}">
    <xsl:matching-substring>
      <xsl:variable name="parsed" as="xs:string" select="regex-group(1)" />
      <xsl:variable name="lines" as="xs:string+" select="tokenize($parsed, '\n')" />
      <xsl:variable name="prefix" as="xs:string" select="regex-group(2)" />
      <xsl:variable name="existing" as="element(ev:ns)?" select="key('namespaces', $prefix, $namespaces)" />
      <xsl:variable name="uri" as="xs:string" select="substring(regex-group(3), 2, string-length(regex-group(3)) - 2)" />
      <xsl:variable name="rest" as="xs:string" select="regex-group(6)" />
      <xsl:choose>
        <xsl:when test="$existing != $uri">
          <xsl:call-template name="ev:error">
            <xsl:with-param name="parsing" select="'namespace declaration'" />
            <xsl:with-param name="found" select="$lmnl" />
            <xsl:with-param name="message" select="concat('the prefix ', $prefix, ' has already been declared with a different URI')" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="ns-decl" as="element(syn:ns-decl)">
            <syn:ns-decl prefix="{$prefix}" ns="{$uri}" line="{$line}" col="{$col}" hash="ns-decl('{$prefix}')" />
          </xsl:variable>
          <xsl:sequence select="$ns-decl" />
          <!-- *** TODO: parsing namespace declarations in tags *** -->
          <xsl:call-template name="ev:parse-document">
            <xsl:with-param name="lmnl" select="$rest" />
            <xsl:with-param name="namespaces" as="document-node()" tunnel="yes">
              <xsl:document>
                <syn:namespaces>
                  <xsl:sequence select="$namespaces/syn:namespaces/syn:ns-decl, $ns-decl" />
                </syn:namespaces>
              </xsl:document>
            </xsl:with-param>
            <xsl:with-param name="line" select="$line + count($lines) - 1" tunnel="yes" />
            <xsl:with-param name="col" select="string-length($lines[last()]) + (if (count($lines) > 1) then 0 else $col)" tunnel="yes" />
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:matching-substring>
    <xsl:non-matching-substring>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="parsing" select="'namespace declaration'" />
        <xsl:with-param name="found" select="$lmnl" />
      </xsl:call-template>
    </xsl:non-matching-substring>
  </xsl:analyze-string>
</xsl:template>  
  
<xsl:key name="namespaces" match="syn:ns-decl" use="@prefix" />
  
<xsl:function name="ev:ns" as="xs:string">
  <xsl:param name="prefix" as="xs:string" />
  <xsl:param name="namespaces" as="document-node()" />
  <xsl:param name="line" as="xs:integer" />
  <xsl:param name="col" as="xs:integer" />
  <xsl:variable name="ns" as="xs:string?" select="key('namespaces', $prefix, $namespaces)/@ns" />
  <xsl:choose>
    <xsl:when test="exists($ns)">
      <xsl:sequence select="$ns" />
    </xsl:when>
    <xsl:when test="$prefix = ''">
      <xsl:sequence select="''" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="ev:error">
        <xsl:with-param name="message" select="concat('no namespace with prefix ', $prefix)" />
        <xsl:with-param name="line" select="$line" tunnel="yes" />
        <xsl:with-param name="col" select="$col" tunnel="yes" />
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>  
  
<xsl:template name="ev:error">
  <xsl:param name="parsing" as="xs:string" select="''" />
  <xsl:param name="found" as="xs:string" select="''" />
  <xsl:param name="message" as="xs:string" select="''" />
  <xsl:param name="line" as="xs:integer" tunnel="yes" required="yes" />
  <xsl:param name="col" as="xs:integer" tunnel="yes" required="yes" />
  <ev:error parsing="{$parsing}" 
            found="{$found}"
            message="{$message}"
            line="{$line}"
            col="{$col}" />
</xsl:template>  
  
<!-- *** ev:hash() *** -->
  
<xsl:function name="ev:hash" as="xs:string">
  <xsl:param name="kind" as="xs:string" />
  <xsl:param name="arg" as="xs:string" />
  <xsl:sequence select="concat($kind, '(''', $arg, ''')')" />
</xsl:function>  
  
<xsl:function name="ev:hash" as="xs:string">
  <xsl:param name="kind" as="xs:string" />
  <xsl:param name="ns" as="xs:string" />
  <xsl:param name="name" as="xs:string" />
  <xsl:sequence select="concat($kind, '(''', $ns, ''',''', $name, ''')')" />
</xsl:function>  
  
<xsl:function name="ev:hash" as="xs:string">
  <xsl:param name="kind" as="xs:string" />
  <xsl:param name="ns" as="xs:string" />
  <xsl:param name="name" as="xs:string" />
  <xsl:param name="id" as="xs:string" />
  <xsl:sequence select="concat($kind, '(''', $ns, ''',''', $name, ''',''', $id, ''')')" />
</xsl:function>  
  
<xsl:function name="ev:hash" as="xs:string">
  <xsl:param name="event" as="element()" />
  <xsl:apply-templates select="$event" mode="ev:hash" />
</xsl:function>
  
<xsl:template match="ev:start-tag-open | ev:start-tag-close | 
                     ev:end-tag-open | ev:end-tag-close"
              mode="ev:hash" as="xs:string">
  <xsl:sequence select="concat(local-name(), '(''', @ns, ''',''', @name,
    ''',''', @id, ''')')" />
</xsl:template>
  
<xsl:template match="ev:start-annotation-open | ev:start-annotation-close | 
                     ev:end-annotation-open | ev:end-annotation-close |
                     ev:atom-open | ev:atom-close"
              mode="ev:hash" as="xs:string">
  <xsl:sequence select="concat(local-name(), '(''', @ns, ''',''', @name, ''')')" />
</xsl:template>
  
<xsl:template match="ev:text"
              mode="ev:hash" as="xs:string">
  <xsl:sequence select="concat(local-name(), '(''', @ws, ''')')" />
</xsl:template>

<!-- *** ev:hex-to-int() *** -->
  
<xsl:variable name="ev:hex" as="xs:string" select="'0123456789ABCDEF'" />  
  
<test:tests>
  <test:title>ev:hex-to-int()</test:title>
  <test:test>
    <test:title>1</test:title>
    <test:param name="hex" select="'1'" />
    <test:expect select="1" />
  </test:test>
  <test:test>
    <test:title>10</test:title>
    <test:param name="hex" select="'10'" />
    <test:expect select="16" />
  </test:test>
  <test:test>
    <test:title>100</test:title>
    <test:param name="hex" select="'100'" />
    <test:expect select="256" />
  </test:test>
  <test:test>
    <test:title>FF</test:title>
    <test:param name="hex" select="'FF'" />
    <test:expect select="255" />
  </test:test>
</test:tests>
<xsl:function name="ev:hex-to-int" as="xs:integer">
  <xsl:param name="hex" as="xs:string" />
  <xsl:sequence select="ev:hex-to-int(upper-case($hex), 0)" />
</xsl:function>
  
<xsl:function name="ev:hex-to-int" as="xs:integer">
  <xsl:param name="hex" as="xs:string" />
  <xsl:param name="int" as="xs:integer" />
  <xsl:choose>
    <xsl:when test="string($hex)">
      <xsl:variable name="digit" as="xs:string" select="substring($hex, 1, 1)" />
      <xsl:variable name="value" as="xs:integer"
        select="string-length(substring-before($ev:hex, $digit))" />
      <xsl:sequence select="ev:hex-to-int(substring($hex, 2), 
                                          $int * 16 + $value)" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$int" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>
  
</xsl:stylesheet>
