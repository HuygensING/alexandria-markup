<?xml version="1.0" encoding="UTF-8"?>
<!-- TODO
  
* add datatype support
  - buffer text to test against specified datatype

-->
<xsl:stylesheet version="2.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:test="http://www.jenitennison.com/xslt/unit-test"
                exclude-result-prefixes="xs test"
                xmlns:pat="http://www.lmnl.org/schema/pattern"
                xmlns:mem="http://www.lmnl.org/schema/pattern/memo"
                xmlns:ev="http://www.lmnl.org/event">
  
<xsl:import href="compile-creole.xsl" />  
  
<xsl:param name="trace" as="xs:boolean" select="false()" />  
  
<xsl:function name="pat:valid" as="xs:boolean">
  <xsl:param name="events" as="element()*" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:variable name="validated" as="element()+" 
    select="pat:validate($events, $patterns)" />
  <xsl:if test="not(pat:nullable($validated))">
    <xsl:message>
      final pattern: <xsl:value-of select="$patterns[1]/@name" />
      grammar: <xsl:copy-of select="$patterns" copy-namespaces="no" />
    </xsl:message>
  </xsl:if>
  <xsl:sequence select="pat:nullable($validated)" />
</xsl:function>  
  
<xsl:function name="pat:validate" as="element()+">
  <xsl:param name="events" as="element()*" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:choose>
    <xsl:when test="empty($events)">
      <xsl:choose>
        <xsl:when test="pat:nullable($patterns)">
          <xsl:if test="$trace">
            <xsl:message>
              grammar: <xsl:copy-of select="$patterns" copy-namespaces="no" />
            </xsl:message>
          </xsl:if>
          <xsl:sequence select="$patterns" />
        </xsl:when>
        <xsl:otherwise>
          <pat:error>
            <pat:grammar start="{$patterns[1]/@name}">
              <xsl:copy-of select="$patterns" />
            </pat:grammar>
          </pat:error>          
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="deriv" as="element()+" 
        select="pat:derivative($events[1], $patterns)" />
      <xsl:if test="$trace">
        <xsl:message>
          <xsl:for-each select="$events[1]">
            <xsl:copy copy-namespaces="no">
              <xsl:copy-of select="@*" />
              <xsl:attribute name="deriv" select="$deriv[1]/@name" />
            </xsl:copy>
          </xsl:for-each>
        <!--
          event:   <xsl:copy-of select="$events[1]" copy-namespaces="no" />
          pattern: <xsl:copy-of select="$patterns[1]" copy-namespaces="no" />
          deriv:   <xsl:copy-of select="$deriv[1]" copy-namespaces="no" />
          grammar: <xsl:copy-of select="$deriv" copy-namespaces="no" />
        -->
        </xsl:message>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="$deriv[1]/@name = '#notAllowed'">
          <xsl:if test="$trace">
            <xsl:message>
              pattern: <xsl:value-of select="$patterns[1]/@name" />
              grammar: <xsl:copy-of select="$deriv" copy-namespaces="no" />
            </xsl:message>
          </xsl:if>
          <xsl:variable name="error">
            <pat:error>
              <xsl:copy-of select="$events[1]" />
              <pat:grammar start="{$patterns[1]/@name}">
                <xsl:copy-of select="$deriv" />
              </pat:grammar>
            </pat:error>
          </xsl:variable>
          <xsl:sequence select="$error/pat:error" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="pat:validate($events[position() > 1], $deriv)" />
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>  

<!-- *** pat:derivative() *** -->  
  
<xsl:function name="pat:derivative" as="element()+">
  <xsl:param name="event" as="element()" />
  <xsl:param name="name" as="xs:string" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:variable name="pattern" as="element()" select="pat:pattern($name, $patterns)" />
  <xsl:sequence select="pat:derivative($event, ($pattern, $patterns except $pattern))" />
</xsl:function>  
  
<xsl:function name="pat:derivative" as="element()+">
  <xsl:param name="event" as="element()" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:apply-templates select="$event" mode="pat:derivative">
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:function>  
  
<!-- ** Event-based derivatives ** -->  
  
<xsl:template match="ev:text" mode="pat:derivative" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="not(pat:allows-text($patterns))">
      <xsl:choose>
        <xsl:when test="@ws = 'true'">
          <xsl:sequence select="$patterns" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="notAllowed" as="element()" select="$patterns[@name = '#notAllowed']" />
          <xsl:sequence select="$notAllowed, $patterns except $notAllowed" />
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="$patterns[1]" mode="pat:text-derivative">
        <xsl:with-param name="event" select="." />
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
  
<xsl:template match="ev:start-tag-open" mode="pat:derivative" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:apply-templates select="$patterns[1]" mode="pat:start-tag-derivative">
    <xsl:with-param name="event" select="." />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:template>
  
<xsl:template match="ev:end-tag-close" mode="pat:derivative" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:apply-templates select="$patterns[1]" mode="pat:end-tag-derivative">
    <xsl:with-param name="event" select="." />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:template>
  
<xsl:template match="ev:start-annotation-open" mode="pat:derivative" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:apply-templates select="$patterns[1]" mode="pat:start-annotation-derivative">
    <xsl:with-param name="event" select="." />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:template>  
  
<xsl:template match="ev:end-annotation-close" mode="pat:derivative" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:apply-templates select="$patterns[1]" mode="pat:end-annotation-derivative">
    <xsl:with-param name="event" select="." />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:template>  

<xsl:template match="ev:atom-open" mode="pat:derivative" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:apply-templates select="$patterns[1]" mode="pat:start-atom-derivative">
    <xsl:with-param name="event" select="." />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:template>  
  
<xsl:template match="ev:atom-close" mode="pat:derivative" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:apply-templates select="$patterns[1]" mode="pat:end-atom-derivative">
    <xsl:with-param name="event" select="." />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:template>  
  
<!-- ignore other kinds of events -->
<xsl:template match="ev:*" mode="pat:derivative" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="$patterns" />
</xsl:template>
  
<!-- ** Pattern-based derivatives ** -->  

<xsl:template match="pat:define" as="element()+"
              mode="pat:text-derivative
                    pat:start-tag-derivative 
                    pat:end-tag-derivative
                    pat:start-annotation-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="mem-deriv" as="element(mem:deriv)?" 
    select="mem:deriv[@event = $event/@hash]" />
  <xsl:choose>
    <xsl:when test="exists($mem-deriv)">
      <xsl:variable name="deriv" as="element()" select="pat:pattern($mem-deriv/@pattern, $patterns)" />
      <xsl:sequence select="$deriv, $patterns except $deriv" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="deriv" as="element()+">
        <xsl:apply-templates select="pat:*[1]" mode="#current">
          <xsl:with-param name="event" select="$event" />
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:apply-templates>
      </xsl:variable>
      <xsl:variable name="new-define" as="element()">
        <pat:define>
          <xsl:copy-of select="@*" />
          <xsl:copy-of select="pat:*" />
          <xsl:copy-of select="mem:*" />
          <mem:deriv event="{$event/@hash}" pattern="{$deriv[1]/@name}" />
        </pat:define>
      </xsl:variable>

      <xsl:choose>
        <xsl:when test="$deriv[1]/@name = @name">
          <xsl:sequence select="$new-define, $deriv[position() > 1]" />          
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="$deriv[1], $deriv[position() > 1][@name != current()/@name], $new-define" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<xsl:template match="pat:ref" as="element()+"
              mode="pat:text-derivative
                    pat:start-tag-derivative 
                    pat:end-tag-derivative
                    pat:start-annotation-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:apply-templates select="pat:pattern(@name, $patterns)" mode="#current">
    <xsl:with-param name="event" select="$event" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:template>  
  
<!-- *** Generic behaviour for constructs *** -->  
  
<xsl:template match="pat:choice" as="element()+" 
              mode="pat:text-derivative
                    pat:start-tag-derivative 
                    pat:end-tag-derivative
                    pat:start-annotation-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="deriv1" as="element()+"
    select="pat:derivative($event, pat:ref[1]/@name, $patterns)" />
  <xsl:variable name="deriv2" as="element()+"
    select="pat:derivative($event, pat:ref[2]/@name, $deriv1)" />
  <xsl:call-template name="pat:choice">
    <xsl:with-param name="pattern1" select="$deriv1[1]/@name" />
    <xsl:with-param name="pattern2" select="$deriv2[1]/@name" />
    <xsl:with-param name="patterns" select="$deriv2" />
  </xsl:call-template>
</xsl:template>  
  
<xsl:template match="pat:group" as="element()+" 
              mode="pat:text-derivative
                    pat:start-tag-derivative
                    pat:end-tag-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="not(pat:only-annotations(pat:ref[1], $patterns))">
      <xsl:variable name="deriv1" as="element()+"
        select="pat:derivative($event, pat:ref[1]/@name, $patterns)" />  
      <xsl:variable name="group" as="element()+">
        <xsl:call-template name="pat:group">
          <xsl:with-param name="pattern1" select="$deriv1[1]/@name" />
          <xsl:with-param name="pattern2" select="pat:ref[2]/@name" /> 
          <xsl:with-param name="patterns" select="$deriv1" />
        </xsl:call-template>
      </xsl:variable>
      
      <xsl:choose>
        <xsl:when test="pat:nullable(pat:ref[1], $deriv1)">
          <xsl:variable name="deriv2" as="element()+"
            select="pat:derivative($event, pat:ref[2]/@name, $group)" />      
          <xsl:call-template name="pat:choice">
            <xsl:with-param name="pattern1" select="$group[1]/@name" />
            <xsl:with-param name="pattern2" select="$deriv2[1]/@name" />
            <xsl:with-param name="patterns" select="$deriv2" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="$group" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="deriv2" as="element()+"
        select="pat:derivative($event, pat:ref[2]/@name, $patterns)" />
      <xsl:call-template name="pat:group">
        <xsl:with-param name="pattern1" select="pat:ref[1]/@name" />
        <xsl:with-param name="pattern2" select="$deriv2[1]/@name" /> 
        <xsl:with-param name="patterns" select="$deriv2" />
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
  
<xsl:template match="pat:group" as="element()+"
              mode="pat:start-annotation-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="not(pat:allows-annotations(pat:ref[1], $patterns))">
      <xsl:variable name="deriv" as="element()+" 
        select="pat:derivative($event, pat:ref[2]/@name, $patterns)" />
      <xsl:call-template name="pat:group">
        <xsl:with-param name="pattern1" select="pat:ref[1]/@name" />
        <xsl:with-param name="pattern2" select="$deriv[1]/@name" />
        <xsl:with-param name="patterns" select="$deriv" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="deriv" as="element()+"
        select="pat:derivative($event, pat:ref[1]/@name, $patterns)" />  
      <xsl:variable name="after" as="element()+">
        <xsl:call-template name="pat:group">
          <xsl:with-param name="pattern1" select="$deriv[1]/@name" />
          <xsl:with-param name="pattern2" select="pat:ref[2]/@name" />
          <xsl:with-param name="patterns" select="$deriv" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="pat:nullable(pat:ref[1], $after)">
          <xsl:variable name="deriv2" as="element()+"
            select="pat:derivative($event, pat:ref[2]/@name, $after)" />      
          <xsl:call-template name="pat:choice">
            <xsl:with-param name="pattern1" select="$deriv[1]/@name" />
            <xsl:with-param name="pattern2" select="$after[1]/@name" />
            <xsl:with-param name="patterns" select="$after" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="$after" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
  
<xsl:template match="pat:after" as="element()+" 
              mode="pat:text-derivative
                    pat:start-tag-derivative
                    pat:end-tag-derivative
                    pat:start-annotation-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="deriv1" as="element()+"
    select="pat:derivative($event, pat:ref[1]/@name, $patterns)" />  
  <xsl:variable name="group" as="element()+">
    <xsl:call-template name="pat:after">
      <xsl:with-param name="pattern1" select="$deriv1[1]/@name" />
      <xsl:with-param name="pattern2" select="pat:ref[2]/@name" /> 
      <xsl:with-param name="patterns" select="$deriv1" />
    </xsl:call-template>
  </xsl:variable>      
  <xsl:choose>
    <xsl:when test="pat:nullable(pat:ref[1], $deriv1)">
      <xsl:variable name="deriv2" as="element()+"
        select="pat:derivative($event, pat:ref[2]/@name, $group)" />      
      <xsl:call-template name="pat:choice">
        <xsl:with-param name="pattern1" select="$group[1]/@name" />
        <xsl:with-param name="pattern2" select="$deriv2[1]/@name" />
        <xsl:with-param name="patterns" select="$deriv2" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$group" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="pat:interleave" as="element()+" 
              mode="pat:text-derivative
                    pat:start-tag-derivative
                    pat:end-tag-derivative
                    pat:start-annotation-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="deriv1" as="element()+"
    select="pat:derivative($event, pat:ref[1]/@name, $patterns)" />
  <xsl:variable name="interleave1" as="element()+">
    <xsl:call-template name="pat:interleave">
      <xsl:with-param name="pattern1" select="$deriv1[1]/@name" />
      <xsl:with-param name="pattern2" select="pat:ref[2]/@name" /> 
      <xsl:with-param name="patterns" select="$deriv1" />
    </xsl:call-template>
  </xsl:variable>
      
  <xsl:variable name="deriv2" as="element()+"
    select="pat:derivative($event, pat:ref[2]/@name, $interleave1)" />
  <xsl:variable name="interleave2" as="element()+">
    <xsl:call-template name="pat:interleave">
      <xsl:with-param name="pattern1" select="pat:ref[1]/@name" />
      <xsl:with-param name="pattern2" select="$deriv2[1]/@name" /> 
      <xsl:with-param name="patterns" select="$deriv2" />
    </xsl:call-template>
  </xsl:variable>
  
  <xsl:call-template name="pat:choice">
    <xsl:with-param name="pattern1" select="$interleave1[1]/@name" />
    <xsl:with-param name="pattern2" select="$interleave2[1]/@name" />
    <xsl:with-param name="patterns" select="$interleave2" />
  </xsl:call-template>
</xsl:template>  
  
<xsl:template match="pat:concur" as="element()+" 
              mode="pat:start-tag-derivative
                    pat:end-tag-derivative
                    pat:start-annotation-derivative
                    pat:end-annotation-derivative
                    pat:end-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="deriv1" as="element()+"
    select="pat:derivative($event, pat:ref[1]/@name, $patterns)" />
  <xsl:variable name="concur1" as="element()+">
    <xsl:call-template name="pat:concur">
      <xsl:with-param name="pattern1" select="$deriv1[1]/@name" />
      <xsl:with-param name="pattern2" select="pat:ref[2]/@name" /> 
      <xsl:with-param name="patterns" select="$deriv1" />
    </xsl:call-template>
  </xsl:variable>
      
  <xsl:variable name="deriv2" as="element()+"
    select="pat:derivative($event, pat:ref[2]/@name, $concur1)" />
  <xsl:variable name="concur2" as="element()+">
    <xsl:call-template name="pat:concur">
      <xsl:with-param name="pattern1" select="pat:ref[1]/@name" />
      <xsl:with-param name="pattern2" select="$deriv2[1]/@name" /> 
      <xsl:with-param name="patterns" select="$deriv2" />
    </xsl:call-template>
  </xsl:variable>
  
  <xsl:variable name="either" as="element()+">
    <xsl:call-template name="pat:choice">
      <xsl:with-param name="pattern1" select="$concur1[1]/@name" />
      <xsl:with-param name="pattern2" select="$concur2[1]/@name" />
      <xsl:with-param name="patterns" select="$concur2" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:sequence select="$either" />
</xsl:template>  

<xsl:template match="pat:concur" as="element()+" 
              mode="pat:text-derivative
                    pat:start-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="deriv1" as="element()+"
    select="pat:derivative($event, pat:ref[1]/@name, $patterns)" />      
  <xsl:variable name="deriv2" as="element()+"
    select="pat:derivative($event, pat:ref[2]/@name, $deriv1)" />
  <xsl:call-template name="pat:concur">
    <xsl:with-param name="pattern1" select="$deriv1[1]/@name" />
    <xsl:with-param name="pattern2" select="$deriv2[1]/@name" />
    <xsl:with-param name="patterns" select="$deriv2" />
  </xsl:call-template>
</xsl:template>

<xsl:template match="pat:all" as="element()+" 
              mode="pat:text-derivative
                    pat:start-tag-derivative
                    pat:end-tag-derivative
                    pat:start-annotation-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="deriv1" as="element()+"
    select="pat:derivative($event, pat:ref[1]/@name, $patterns)" />      
  <xsl:variable name="deriv2" as="element()+"
    select="pat:derivative($event, pat:ref[2]/@name, $deriv1)" />
  <xsl:call-template name="pat:all">
    <xsl:with-param name="pattern1" select="$deriv1[1]/@name" />
    <xsl:with-param name="pattern2" select="$deriv2[1]/@name" />
    <xsl:with-param name="patterns" select="$deriv2" />
  </xsl:call-template>
</xsl:template>  

<xsl:template match="pat:partition" as="element()+"
              mode="pat:start-tag-derivative
                    pat:text-derivative
                    pat:end-tag-derivative
                    pat:start-annotation-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="deriv" as="element()+"
    select="pat:derivative($event, pat:ref[1]/@name, $patterns)" />
  <xsl:call-template name="pat:after">
    <xsl:with-param name="pattern1" select="$deriv[1]/@name" />
    <xsl:with-param name="pattern2" select="'#empty'" />
    <xsl:with-param name="patterns" select="$deriv" />
  </xsl:call-template>
</xsl:template>  
  
<xsl:template match="pat:oneOrMore
                     | pat:concurOneOrMore" as="element()+"
              mode="pat:text-derivative
                    pat:start-tag-derivative
                    pat:end-tag-derivative
                    pat:start-annotation-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="deriv" as="element()+"
    select="pat:derivative($event, pat:ref[1]/@name, $patterns)" />
  <xsl:variable name="choice" as="element()+">
    <xsl:call-template name="pat:choice">
      <xsl:with-param name="pattern1" select="parent::pat:define/@name" />
      <xsl:with-param name="pattern2" select="if (self::pat:oneOrMore) then '#empty' else '#anyContent'" />
      <xsl:with-param name="patterns" select="$deriv" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="self::pat:oneOrMore">
      <xsl:call-template name="pat:group">
        <xsl:with-param name="pattern1" select="$deriv[1]/@name" />
        <xsl:with-param name="pattern2" select="$choice[1]/@name" />
        <xsl:with-param name="patterns" select="$choice" />    
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="pat:concur">
        <xsl:with-param name="pattern1" select="$deriv[1]/@name" />
        <xsl:with-param name="pattern2" select="$choice[1]/@name" />
        <xsl:with-param name="patterns" select="$choice" />    
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>  
</xsl:template>  
  
<xsl:template match="pat:empty | pat:notAllowed" as="element()+"
              mode="pat:text-derivative
                    pat:start-tag-derivative
                    pat:end-tag-derivative
                    pat:start-annotation-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="notAllowed" as="element()" select="$patterns[@name = '#notAllowed']" />
  <xsl:sequence select="$notAllowed, $patterns except $notAllowed" />
</xsl:template>  
  
<xsl:template match="pat:*" as="element()+"
              mode="pat:text-derivative
                    pat:start-tag-derivative
                    pat:end-tag-derivative
                    pat:end-annotation-derivative
                    pat:start-atom-derivative
                    pat:end-atom-derivative">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:variable name="notAllowed" as="element()" select="$patterns[@name = '#notAllowed']" />
  <xsl:sequence select="$notAllowed, $patterns except $notAllowed" />
</xsl:template>  

<!-- *** pat:start-tag-derivative mode *** -->
  
<xsl:template match="pat:range" as="element()+" 
              mode="pat:start-tag-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="pat:contains(pat:ref[1], QName($event/@ns, $event/@name), $patterns)">
      <xsl:variable name="end-tag" as="element()">
        <pat:end-tag id="{$event/@id}">
          <pat:name ns="{$event/@ns}">
            <xsl:value-of select="$event/@name" />
          </pat:name>
        </pat:end-tag>
      </xsl:variable>

      <xsl:variable name="compiled-end-tag" as="element()+">
        <xsl:apply-templates select="$end-tag" mode="pat:compile">
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:apply-templates>
      </xsl:variable>      
      <xsl:call-template name="pat:group">
        <xsl:with-param name="pattern1" select="pat:ref[2]/@name" />
        <xsl:with-param name="pattern2" select="$compiled-end-tag[1]/@name" />
        <xsl:with-param name="patterns" select="$compiled-end-tag" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:next-match>
        <xsl:with-param name="event" select="$event" />
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:next-match>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template> 
  
<!-- *** pat:end-tag-derivative mode *** -->
  
<xsl:template match="pat:end-tag" mode="pat:end-tag-derivative" as="element()+">
  <xsl:param name="event" as="element(ev:end-tag-close)" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:param name="nc" as="element(pat:name)"
    select="pat:pattern(pat:ref[1]/@name, $patterns)/pat:name" />
  <xsl:choose>
    <xsl:when test="string($nc) eq $event/@name and
                    $nc/@ns eq $event/@ns and
                    @id eq $event/@id">
      <xsl:variable name="empty" as="element()" select="$patterns[@name = '#empty']" />
      <xsl:sequence select="$empty, $patterns except $empty" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:next-match>
        <xsl:with-param name="event" select="$event" />
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:next-match>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<!-- *** pat:start-annotation-derivative mode *** -->
  
<xsl:template match="pat:annotation" as="element()+" 
              mode="pat:start-annotation-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="pat:contains(pat:ref[1], QName($event/@ns, $event/@name), $patterns)">
      <xsl:variable name="end-annotation" as="element()">
        <pat:end-annotation>
          <pat:name ns="{$event/@ns}">
            <xsl:value-of select="$event/@name" />
          </pat:name>
        </pat:end-annotation>
      </xsl:variable>

      <xsl:variable name="compiled-end-annotation" as="element()+">
        <xsl:apply-templates select="$end-annotation" mode="pat:compile">
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:apply-templates>
      </xsl:variable>      
      <xsl:variable name="group" as="element()+">
        <xsl:call-template name="pat:group">
          <xsl:with-param name="pattern1" select="pat:ref[2]/@name" />
          <xsl:with-param name="pattern2" select="$compiled-end-annotation[1]/@name" />
          <xsl:with-param name="patterns" select="$compiled-end-annotation" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="after" as="element()+">
        <xsl:call-template name="pat:after">
          <xsl:with-param name="pattern1" select="$group[1]/@name" />
          <xsl:with-param name="pattern2" select="'#empty'" />
          <xsl:with-param name="patterns" select="$group" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:sequence select="$after" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="notAllowed" as="element()" select="$patterns[@name = '#notAllowed']" />
      <xsl:sequence select="$notAllowed, $patterns except $notAllowed" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  

<xsl:template match="pat:*" as="element()+" mode="pat:start-annotation-derivative">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select=".., $patterns except .." />
</xsl:template>

<!-- *** pat:end-annotation-derivative mode *** -->
  
<xsl:template match="pat:end-annotation" mode="pat:end-annotation-derivative" as="element()+">
  <xsl:param name="event" as="element(ev:end-annotation-close)" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:param name="nc" as="element(pat:name)"
    select="pat:pattern(pat:ref[1]/@name, $patterns)/pat:name" />
  <xsl:choose>
    <xsl:when test="string($nc) eq $event/@name and
                    $nc/@ns eq $event/@ns">
      <xsl:variable name="empty" as="element()" select="$patterns[@name = '#empty']" />
      <xsl:sequence select="$empty, $patterns except $empty" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:next-match>
        <xsl:with-param name="event" select="$event" />
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:next-match>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<!-- *** pat:start-atom-derivative mode *** -->
  
<xsl:template match="pat:atom" as="element()+" 
              mode="pat:start-atom-derivative">
  <xsl:param name="event" as="element()" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:choose>
    <xsl:when test="pat:contains(pat:ref[1], QName($event/@ns, $event/@name), $patterns)">
      <xsl:variable name="end-atom" as="element()">
        <pat:end-atom>
          <pat:name ns="{$event/@ns}">
            <xsl:value-of select="$event/@name" />
          </pat:name>
        </pat:end-atom>
      </xsl:variable>

      <xsl:variable name="compiled-end-atom" as="element()+">
        <xsl:apply-templates select="$end-atom" mode="pat:compile">
          <xsl:with-param name="patterns" select="$patterns" />
        </xsl:apply-templates>
      </xsl:variable>      
      <xsl:variable name="group" as="element()+">
        <xsl:call-template name="pat:group">
          <xsl:with-param name="pattern1" select="pat:ref[2]/@name" />
          <xsl:with-param name="pattern2" select="$compiled-end-atom[1]/@name" />
          <xsl:with-param name="patterns" select="$compiled-end-atom" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="after" as="element()+">
        <xsl:call-template name="pat:after">
          <xsl:with-param name="pattern1" select="$group[1]/@name" />
          <xsl:with-param name="pattern2" select="'#empty'" />
          <xsl:with-param name="patterns" select="$group" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:sequence select="$after" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="notAllowed" as="element()" select="$patterns[@name = '#notAllowed']" />
      <xsl:sequence select="$notAllowed, $patterns except $notAllowed" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  

<!-- *** pat:end-atom-derivative mode *** -->
  
<xsl:template match="pat:end-atom" mode="pat:end-atom-derivative" as="element()+">
  <xsl:param name="event" as="element(ev:atom-close)" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:param name="nc" as="element(pat:name)"
    select="pat:pattern(pat:ref[1]/@name, $patterns)/pat:name" />
  <xsl:choose>
    <xsl:when test="string($nc) eq $event/@name and
                    $nc/@ns eq $event/@ns">
      <xsl:variable name="empty" as="element()" select="$patterns[@name = '#empty']" />
      <xsl:sequence select="$empty, $patterns except $empty" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:next-match>
        <xsl:with-param name="event" select="$event" />
        <xsl:with-param name="patterns" select="$patterns" />
      </xsl:next-match>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<!-- *** pat:text-derivative mode *** -->
  
<xsl:template match="pat:text" mode="pat:text-derivative" as="element()+">
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select=".., $patterns except .." />
</xsl:template>  
  
<!-- *** pat:contains() *** -->
  
<xsl:function name="pat:contains" as="xs:boolean">
  <xsl:param name="nc" as="element()" />
  <xsl:param name="qname" as="xs:QName" />
  <xsl:param name="patterns" as="element()+" />
  <xsl:apply-templates select="$nc" mode="pat:contains">
    <xsl:with-param name="qname" select="$qname" />
    <xsl:with-param name="patterns" select="$patterns" />
  </xsl:apply-templates>
</xsl:function>
  
<xsl:template match="pat:ref" mode="pat:contains" as="xs:boolean">
  <xsl:param name="qname" as="xs:QName" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence 
    select="pat:contains(pat:pattern(@name, $patterns), $qname, $patterns)" />
</xsl:template>  

<xsl:template match="pat:define" mode="pat:contains" as="xs:boolean">
  <xsl:param name="qname" as="xs:QName" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:contains(pat:*, $qname, $patterns)" />
</xsl:template>  
  
<xsl:template match="pat:name" mode="pat:contains" as="xs:boolean">
  <xsl:param name="qname" as="xs:QName" required="yes" />
  <xsl:sequence select="string(.) = local-name-from-QName($qname) and
                        @ns = namespace-uri-from-QName($qname)" />
</xsl:template>
  
<xsl:template match="pat:nsName" mode="pat:contains" as="xs:boolean">
  <xsl:param name="qname" as="xs:QName" required="yes" />
  <xsl:sequence select="@ns = namespace-uri-from-QName($qname)" />
</xsl:template>  

<xsl:template match="pat:nsNameExcept" mode="pat:contains" as="xs:boolean">
  <xsl:param name="qname" as="xs:QName" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="@ns = namespace-uri-from-QName($qname) and
                        not(pat:contains(pat:*, $qname, $patterns))" />
</xsl:template>
  
<xsl:template match="pat:anyName" mode="pat:contains" as="xs:boolean">
  <xsl:sequence select="true()" />
</xsl:template>
  
<xsl:template match="pat:anyNameExcept" mode="pat:contains" as="xs:boolean">
  <xsl:param name="qname" as="xs:QName" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="not(pat:contains(pat:*, $qname, $patterns))" />
</xsl:template>
  
<xsl:template match="pat:choice" mode="pat:contains" as="xs:boolean">
  <xsl:param name="qname" as="xs:QName" required="yes" />
  <xsl:param name="patterns" as="element()+" required="yes" />
  <xsl:sequence select="pat:contains(pat:*[1], $qname, $patterns) or
                        pat:contains(pat:*[2], $qname, $patterns)" />
</xsl:template>
    
</xsl:stylesheet>
