<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:test="http://www.jenitennison.com/xslt/unit-test"
                exclude-result-prefixes="xs test"
                xmlns:ev="http://www.lmnl.org/event"
                xmlns:pat="http://www.lmnl.org/schema/pattern">
  
<xsl:import href="parse-lmnl.xsl"/>
<xsl:import href="creole-impl.xsl"/>
  
<xsl:param name="lmnl-doc" as="xs:anyURI" select="xs:anyURI('')" />
<xsl:param name="lmnl" as="xs:string" select="unparsed-text(resolve-uri($lmnl-doc, base-uri()))" />

<xsl:param name="schema-doc" as="xs:anyURI" select="xs:anyURI('')"/>
<xsl:param name="schema" as="document-node()" select="doc(resolve-uri($schema-doc, base-uri()))" />
  
<xsl:template match="/" name="validate-lmnl">
  <xsl:choose>
    <xsl:when test="pat:*">
      <xsl:sequence select="pat:validate-lmnl($lmnl, .)" />
    </xsl:when>
    <xsl:when test="string($lmnl)">
      <xsl:sequence select="pat:validate-lmnl($lmnl, $schema)" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="pat:validate-lmnl(string(.), $schema)" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>  
  
<test:tests>
  <test:title>LMNL Validation</test:title>
  <test:test>
    <test:title>Plain text</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      ...
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <text />
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Plain text against element pattern (invalid)</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      ...
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <text />
      </element>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>Single range against element pattern</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <text />
      </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Single range against element pattern (invalid name)</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="bar">
        <text />
      </element>
    </test:param>
    <test:expect select="false()" />
  </test:test>

  <test:test>
    <test:title>Single range against element pattern (invalid content)</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <empty />
      </element>
    </test:param>
    <test:expect select="false()" />
  </test:test>

  <test:test>
    <test:title>Single range against range pattern</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range name="foo">
        <text />
      </range>
    </test:param>
    <test:expect select="true()" />
  </test:test>

  <test:test>
    <test:title>Single range against range pattern (invalid name)</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range name="bar">
        <text />
      </range>
    </test:param>
    <test:expect select="false()" />
  </test:test>

  <test:test>
    <test:title>Single range against range pattern (invalid content)</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range name="foo">
        <empty />
      </range>
    </test:param>
    <test:expect select="false()" />
  </test:test>

  <test:test>
    <test:title>Single range against choice pattern</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <choice>
        <element name="foo">
          <text />
        </element>
        <range name="foo">
          <text />
        </range>
      </choice>
    </test:param>
    <test:expect select="true()" />
  </test:test>

  <test:test>
    <test:title>Single range against choice pattern (invalid choices)</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <choice>
        <element name="bar">
          <text />
        </element>
        <range name="bar">
          <text />
        </range>
      </choice>
    </test:param>
    <test:expect select="false()" />
  </test:test>

  <test:test>
    <test:title>Range with range content against range pattern</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}[bar}...{bar]{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range name="foo">
        <range name="bar">
          <text />
        </range>
      </range>
    </test:param>
    <test:expect select="true()" />
  </test:test>

  <test:test>
    <test:title>Element with element content against range pattern</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}[bar}...{bar]{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <element name="bar">
          <text />
        </element>
      </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>

  <test:test>
    <test:title>Michael's favourite without annotations</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [play}
        [act}
          [scene}
            [sp}[l} Peer, you're lying!{sp]
            [sp}[stage}without stopping{stage] No, I'm not! {l]{sp]
            [sp}[l} Well then, swear to me it's true! {l]{sp]
            [sp}[l} Swear? Why should I? {sp]
            [sp} See, you dare not! {l]
                [l} Every word of it's a lie! {l] {sp]
          {scene]
        {act]
      {play] 
    </test:param>
    <test:param name="schema" select="/" href="play-no-annotations.crl" />
    <test:expect select="true()" />
  </test:test>

  <test:test>
    <test:title>Elements appearing in concur</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [chapter}
      [section}[heading}The creation of the world{heading]
       [para}[v}...{v][v}...{para]
       [para}...{v][v}...{v]{para]
      {section]
      {chapter]    
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
       <concur>
         <oneOrMore>
           <range name="chapter">
             <interleave>
               <zeroOrMore>
                 <element name="heading"><text /></element>
               </zeroOrMore>
               <oneOrMore>
                 <range name="v"><text /></range>
               </oneOrMore>
             </interleave>
           </range>
         </oneOrMore>
         <oneOrMore>
           <range name="section">
             <element name="heading"><text /></element>
             <oneOrMore>
               <range name="para"><text /></range>
             </oneOrMore>
           </range>
         </oneOrMore>
       </concur>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Elements appearing in concur</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [chapter}
      [section}[heading}The creation of the world{heading]
       [para}[v}...{v][v}...{para]
       [para}...{v][v}...{v]{para]
      {section]
      {chapter]    
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
       <concur>
         <oneOrMore>
           <range name="chapter">
             <oneOrMore>
               <range name="v"><text /></range>
             </oneOrMore>
           </range>
         </oneOrMore>
         <oneOrMore>
           <range name="section">
             <element name="heading"><text /></element>
             <oneOrMore>
               <range name="para"><text /></range>
             </oneOrMore>
           </range>
         </oneOrMore>
       </concur>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Interleaved ranges</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [v}[s}In the beginning of creation, when God made heaven and earth,{v] 
      [v}the earth was without form and void, with darkness over the face of 
       the abyss, and a mighty wind that swept over the surface of the waters.{s]{v] 
      [v}[s}God said, "Let there be a light," and there was light;{v] 
      [v}and God saw that the light was good, and he separated the light from darkness.{s]{v] 
      [v}[s}He called the light day, and the darkness night. So evening came, and 
       morning came, the first day.{s]{v]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <interleave>
        <oneOrMore>
          <range name="s"><text /></range>
        </oneOrMore>
        <oneOrMore>
          <range name="v"><text /></range>
        </oneOrMore>
      </interleave>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>concurring ranges</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [v}[s}In the beginning of creation, when God made heaven and earth,{v] 
      [v}the earth was without form and void, with darkness over the face of 
       the abyss, and a mighty wind that swept over the surface of the waters.{s]{v] 
      [v}[s}God said, "Let there be a light," and there was light;{v] 
      [v}and God saw that the light was good, and he separated the light from darkness.{s]{v] 
      [v}[s}He called the light day, and the darkness night. So evening came, and 
       morning came, the first day.{s]{v]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <concur>
        <oneOrMore>
          <range name="s"><text /></range>
        </oneOrMore>
        <oneOrMore>
          <range name="v"><text /></range>
        </oneOrMore>
      </concur>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Interleaved ranges</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [s}In the beginning of creation, when God made heaven and earth, 
       the earth was without form and void, with darkness over the face of 
       the abyss, and a mighty wind that swept over the surface of the waters.{s] 
      [v}God said, "Let there be a light," and there was light;{v] 
      [v}and God saw that the light was good, and he separated the light from darkness.{v] 
      [v}[s}He called the light day, and the darkness night. So evening came, and 
       morning came, the first day.{s]{v]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <interleave>
        <oneOrMore>
          <range name="s"><text /></range>
        </oneOrMore>
        <oneOrMore>
          <range name="v"><text /></range>
        </oneOrMore>
      </interleave>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>concurped ranges (invalid)</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [s}In the beginning of creation, when God made heaven and earth, 
       the earth was without form and void, with darkness over the face of 
       the abyss, and a mighty wind that swept over the surface of the waters.{s] 
      [v}God said, "Let there be a light," and there was light;{v] 
      [v}and God saw that the light was good, and he separated the light from darkness.{v] 
      [v}[s}He called the light day, and the darkness night. So evening came, and 
       morning came, the first day.{s]{v]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <concur>
        <oneOrMore>
          <range name="s"><text /></range>
        </oneOrMore>
        <oneOrMore>
          <range name="v"><text /></range>
        </oneOrMore>
      </concur>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>Self-concurring ranges</test:title>
    <test:param name="lmnl" select="string(.)">
      [chapter}
        [page=ed1n1}
        [page=ed2n1}
          [para}...{para]
          [para}...{page=ed2n1][page=ed2n2}...{para]
          [para}...{page=ed1n1][page=ed1n2}...{para]
          [para}...{para]
        {page=ed2n2]
        {page=ed1n2]
      {chapter]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
       <element name="chapter">
         <concur>
           <concurOneOrMore>
             <oneOrMore>
               <range name="page">
                 <text />
               </range>
             </oneOrMore>
           </concurOneOrMore>
           <oneOrMore>
             <range name="para"><text /></range>
           </oneOrMore>
         </concur>
       </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Self-concurring ranges (invalid)</test:title>
    <test:param name="lmnl" select="string(.)">
      [chapter}
        [page=ed1n1}
          [para}...{para]
        [page=ed2n1}
          [para}...{page=ed2n1][page=ed2n2}...{para]
          [para}...{page=ed1n1][page=ed1n2}...{para]
          [para}...{para]
        {page=ed2n2]
        {page=ed1n2]
      {chapter]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
       <element name="chapter">
         <concur>
           <concurOneOrMore>
             <oneOrMore>
               <range name="page">
                 <text />
               </range>
             </oneOrMore>
           </concurOneOrMore>
           <oneOrMore>
             <range name="para"><text /></range>
           </oneOrMore>
         </concur>
       </element>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>Self-concurring ranges in mixed content</test:title>
    <test:param name="lmnl" select="string(.)">
      [para}
        ...[phrase=a}...[phrase=b}...{phrase=a]...{phrase=b]...
      {para]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
       <element name="para">
         <concurZeroOrMore>
           <mixed>
             <range name="phrase"><text /></range>
           </mixed>
         </concurZeroOrMore>
       </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Self-concurring choice of ranges in mixed content</test:title>
    <test:param name="lmnl" select="string(.)">
      [para}
        ...[b}...[i}...{b]...{i]...
      {para]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
       <element name="para">
         <concurZeroOrMore>
           <mixed>
             <choice>
               <range name="b"><text /></range>
               <range name="i"><text /></range>
             </choice>
           </mixed>
         </concurZeroOrMore>
       </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Two concurring range sequences with same name</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [chapter}
        [page=ed1n1}
        [page=ed2n1}
          [para}...{para]
          [para}...{page=ed2n1][page=ed2n2}...{para]
          [para}...{page=ed1n1][page=ed1n2}...{para]
          [para}...{para]
        {page=ed2n2]
        {page=ed1n2]
      {chapter]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
       <element name="chapter">
         <concur>
           <oneOrMore>
             <range name="page">
               <text />
             </range>
           </oneOrMore>
           <oneOrMore>
             <range name="page">
               <text />
             </range>
           </oneOrMore>
           <oneOrMore>
             <range name="para"><text /></range>
           </oneOrMore>
         </concur>
       </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Michael's favourite</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [play [title}Peer Gynt{title]}
        [act [n}1{]}
          [scene [n}i{]}
            [sp [who}Aase{]}[l} Peer, you're lying!{sp]
            [sp [who}Peer{]}[stage}without stopping{stage] No, I'm not! {l]{sp]
            [sp [who}Aase{]}[l} Well then, swear to me it's true! {l]{sp]
            [sp [who}Peer{]}[l} Swear? Why should I? {sp]
            [sp [who}Aase{]} See, you dare not! {l]
                            [l} Every word of it's a lie! {l] {sp]
          {scene]
        {act]
      {play] 
    </test:param>
    <test:param name="schema" select="/" href="play.crl" />
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Interleaving partitions (valid)</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}...{foo][bar}...{bar][baz}...{baz]
    </test:param>
    <test:param name="schema" select="/"
      xmlns="http://www.lmnl.org/schema/pattern">
      <interleave>
        <partition>
          <range name="foo"><text /></range>
          <range name="bar"><text /></range>
        </partition>
        <range name="baz"><text /></range>
      </interleave>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Interleaving partitions (valid)</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [baz}...{baz][foo}...{foo][bar}...{bar]
    </test:param>
    <test:param name="schema" select="/"
      xmlns="http://www.lmnl.org/schema/pattern">
      <interleave>
        <partition>
          <range name="foo"><text /></range>
          <range name="bar"><text /></range>
        </partition>
        <range name="baz"><text /></range>
      </interleave>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Interleaving partitions (invalid)</test:title>
    <test:param name="lmnl" select="normalize-space(.)">
      [foo}...{foo][baz}...{baz][bar}...{bar]
    </test:param>
    <test:param name="schema" select="/"
      xmlns="http://www.lmnl.org/schema/pattern">
      <interleave>
        <partition>
          <range name="foo"><text /></range>
          <range name="bar"><text /></range>
        </partition>
        <range name="baz"><text /></range>
      </interleave>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>Mixed up annotations: valid</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo [select1}...{] 
           [choice2}...{]}
        [bar}...{bar]
        [select2}...{select2]
        [choice1}...{choice1]
      {foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <element name="bar"><text /></element>
        <choice>
          <group>
            <annotation name="select1"><text /></annotation>
            <element name="select2"><text /></element>
          </group>
          <group>
            <element name="select1"><text /></element>
            <annotation name="select2"><text /></annotation>
          </group>
        </choice>
        <choice>
          <group>
            <annotation name="choice1"><text /></annotation>
            <element name="choice2"><text /></element>
          </group>
          <group>
            <element name="choice1"><text /></element>
            <annotation name="choice2"><text /></annotation>
          </group>
        </choice>
      </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Mixed up annotations: invalid annotation order</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo [choice2}...{] 
           [select1}...{]}
        [bar}...{bar]
        [select2}...{select2]
        [choice1}...{choice1]
      {foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <element name="bar"><text /></element>
        <choice>
          <group>
            <annotation name="select1"><text /></annotation>
            <element name="select2"><text /></element>
          </group>
          <group>
            <element name="select1"><text /></element>
            <annotation name="select2"><text /></annotation>
          </group>
        </choice>
        <choice>
          <group>
            <annotation name="choice1"><text /></annotation>
            <element name="choice2"><text /></element>
          </group>
          <group>
            <element name="choice1"><text /></element>
            <annotation name="choice2"><text /></annotation>
          </group>
        </choice>
      </element>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>Mixed up annotations: invalid associated elements</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo [select1}...{] 
           [choice2}...{]}
        [bar}...{bar]
        [select1}...{select1]
        [choice1}...{choice1]
      {foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <element name="bar"><text /></element>
        <choice>
          <group>
            <annotation name="select1"><text /></annotation>
            <element name="select2"><text /></element>
          </group>
          <group>
            <element name="select1"><text /></element>
            <annotation name="select2"><text /></annotation>
          </group>
        </choice>
        <choice>
          <group>
            <annotation name="choice1"><text /></annotation>
            <element name="choice2"><text /></element>
          </group>
          <group>
            <element name="choice1"><text /></element>
            <annotation name="choice2"><text /></annotation>
          </group>
        </choice>
      </element>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>Mixed up annotations: invalid association</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo [bar}...{]
           [select1}...{] 
           [choice2}...{]}
        [bar}...{bar]
        [select1}...{select1]
        [choice1}...{choice1]
      {foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <element name="bar"><text /></element>
        <choice>
          <group>
            <annotation name="select1"><text /></annotation>
            <element name="select2"><text /></element>
          </group>
          <group>
            <element name="select1"><text /></element>
            <annotation name="select2"><text /></annotation>
          </group>
        </choice>
        <choice>
          <group>
            <annotation name="choice1"><text /></annotation>
            <element name="choice2"><text /></element>
          </group>
          <group>
            <element name="choice1"><text /></element>
            <annotation name="choice2"><text /></annotation>
          </group>
        </choice>
      </element>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>Interleaving annotation and element</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo [bar}...{]}[baz}...{baz]{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <interleave>
          <element name="baz"><text /></element>
          <attribute name="bar" />
        </interleave>
      </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>concurring annotation and element</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo [bar}...{]}[baz}...{baz]{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <concur>
          <element name="baz"><text /></element>
          <attribute name="bar" />
        </concur>
      </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Grouping annotation and element</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo [bar}...{]}[baz}...{baz]{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <element name="baz"><text /></element>
        <attribute name="bar" />
      </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Annotation in end tag</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}[baz}...{baz]{foo [bar}...{]]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <element name="baz"><text /></element>
        <attribute name="bar" />
      </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Annotation in end tag declared before element</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}[baz}...{baz]{foo [bar}...{]]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <element name="foo">
        <attribute name="bar" />
        <element name="baz"><text /></element>
      </element>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>anyName</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range>
        <anyName />
        <text />
      </range>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>anyName except (valid)</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range>
        <anyName>
          <except>
            <name>bar</name>
          </except>
        </anyName>
        <text />
      </range>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>anyName except (invalid)</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range>
        <anyName>
          <except>
            <name>foo</name>
          </except>
        </anyName>
        <text />
      </range>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>choice of names (valid)</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}...{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range>
        <choice>
          <name>foo</name>
          <name>bar</name>
        </choice>
        <text />
      </range>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>choice of names (invalid)</test:title>
    <test:param name="lmnl" select="string(.)">
      [baz}...{baz]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range>
        <choice>
          <name>foo</name>
          <name>bar</name>
        </choice>
        <text />
      </range>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>Matching namespace</test:title>
    <test:param name="lmnl" select="string(.)">
      [!ns x="http://www.example.com"]
      [x:bar}...{x:bar]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range ns="http://www.example.com" name="bar">
        <text />
      </range>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Non-matching namespace (invalid)</test:title>
    <test:param name="lmnl" select="string(.)">
      [!ns x="http://www.example.com"]
      [x:bar}...{x:bar]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <range ns="http://www.example.com/foo" name="bar">
        <text />
      </range>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>Built-in entities</test:title>
    <test:param name="lmnl" select="string(.)">
      &amp;lsqb;, &amp;rsqb;, &amp;lcub;, &amp;rcub;, &amp;quot;, &amp;amp; &amp;apos; should all work
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <text />
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Single atom, no annotations</test:title>
    <test:param name="lmnl" select="string(.)">
      {{foo}}
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <atom name="foo" />
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Single atom with annotation</test:title>
    <test:param name="lmnl" select="string(.)">
      {{foo [bar}...{]}}
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <atom name="foo">
        <annotation name="bar"><text /></annotation>
      </atom>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Single atom with annotation containing atom</test:title>
    <test:param name="lmnl" select="string(.)">
      {{foo [bar}...{{baz}}...{]}}
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <atom name="foo">
        <annotation name="bar">
          <mixed>
            <atom name="baz" />
          </mixed>
        </annotation>
      </atom>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Concur and atoms: valid example</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}[bar}...{{baz}}...{bar]{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <concur>
        <range name="foo">
          <mixed>
            <atom name="baz" />
          </mixed>
        </range>
        <range name="bar">
          <mixed>
            <atom><anyName /></atom>
          </mixed>
        </range>
      </concur>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Concur and atoms: valid example with optional atom</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}[bar}...{{baz}}...{bar]{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <concur>
        <range name="foo">
          <mixed>
            <atom name="baz" />
            <optional>
              <atom name="fred" />
            </optional>
          </mixed>
        </range>
        <range name="bar">
          <mixed>
            <atom><anyName /></atom>
          </mixed>
        </range>
      </concur>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Concur and atoms: valid example with annotated atom</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}[bar}...{{baz [a}...{]}}...{bar]{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <concur>
        <range name="foo">
          <mixed>
            <atom name="baz">
              <attribute name="a" />
            </atom>
          </mixed>
        </range>
        <range name="bar">
          <mixed>
            <atom>
              <anyName />
              <zeroOrMore>
                <annotation>
                  <anyName />
                  <text />
                </annotation>
              </zeroOrMore>
            </atom>
          </mixed>
        </range>
      </concur>
    </test:param>
    <test:expect select="true()" />
  </test:test>
  <test:test>
    <test:title>Concur and atoms: invalid example with annotated atom</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}[bar}...{{baz [a}...{]}}...{bar]{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <concur>
        <range name="foo">
          <mixed>
            <atom name="baz">
              <attribute name="a" />
            </atom>
          </mixed>
        </range>
        <range name="bar">
          <mixed>
            <atom>
              <anyName />
            </atom>
          </mixed>
        </range>
      </concur>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>Concur and atoms: invalid due to atom only being in one branch</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo}[bar}...{{baz [a}...{]}}...{bar]{foo]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <concur>
        <range name="foo">
          <mixed>
            <atom name="baz">
              <attribute name="a" />
            </atom>
          </mixed>
        </range>
        <range name="bar">
          <text />
        </range>
      </concur>
    </test:param>
    <test:expect select="false()" />
  </test:test>
  <test:test>
    <test:title>ConcurOneOrMore and atoms: valid</test:title>
    <test:param name="lmnl" select="string(.)">
      [foo=f1}...[foo=f2}...{{bar}}...{foo=f1]...{foo=f2]
    </test:param>
    <test:param name="schema" select="/" xmlns="http://www.lmnl.org/schema/pattern">
      <concurOneOrMore>
        <mixed>
          <range name="foo">
            <mixed>
              <atom name="bar" />
            </mixed>
          </range>
        </mixed>
      </concurOneOrMore>
    </test:param>
    <test:expect select="true()" />
  </test:test>
</test:tests>
<xsl:function name="pat:validate-lmnl" as="xs:boolean">
  <xsl:param name="lmnl" as="xs:string" />
  <xsl:param name="schema" as="document-node()" />
  <xsl:variable name="events" as="element(ev:events)">
    <xsl:call-template name="ev:parse-lmnl">
      <xsl:with-param name="lmnl" select="$lmnl" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="$events/ev:error">
      <xsl:message>
        ERROR parsing LMNL:
        <xsl:copy-of select="$events/ev:error" copy-namespaces="no" />
      </xsl:message>
      <xsl:sequence select="false()" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="compiled" as="element()+">
        <xsl:call-template name="pat:compile-creole">
          <xsl:with-param name="schema" select="$schema" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="result" as="document-node()">
        <xsl:document>
          <xsl:sequence select="pat:validate($events/ev:*, $compiled)" />
        </xsl:document>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="$result instance of document-node(element(pat:error))">
          <xsl:message>
            <xsl:text>Error</xsl:text>
            <xsl:apply-templates select="$result/pat:error/ev:*" 
              mode="ev:report-error" />
            <xsl:text>: </xsl:text>
            <xsl:apply-templates select="$result/pat:error/pat:grammar"
              mode="pat:report-error" />
          </xsl:message>
          <xsl:sequence select="false()" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="true()" />
        </xsl:otherwise>
      </xsl:choose>      
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>  

<xsl:template match="ev:*" mode="ev:report-error">
  <xsl:text>: found </xsl:text>
  <xsl:value-of select="translate(local-name(.), '-', ' ')" />
  <xsl:choose>
    <xsl:when test="@name">
      <xsl:text> [{</xsl:text>
      <xsl:value-of select="@ns" />
      <xsl:text>}</xsl:text>
      <xsl:value-of select="@name" />
      <xsl:if test="string(@id)">
        <xsl:text>=</xsl:text>
        <xsl:value-of select="@id" />
      </xsl:if>
      <xsl:text>]</xsl:text>
    </xsl:when>
    <xsl:when test="@chars">
      <xsl:text> "</xsl:text>
      <xsl:value-of select="substring(@chars, 1, 10)" />
      <xsl:if test="string-length(@chars) > 10">...</xsl:if>
      <xsl:text>"</xsl:text>
    </xsl:when>
  </xsl:choose>
  <xsl:text> at </xsl:text>
  <xsl:value-of select="@line" />
  <xsl:text>:</xsl:text>
  <xsl:value-of select="@col" />
</xsl:template>  
  
<xsl:template match="pat:grammar" mode="pat:report-error">
  <xsl:text> expected </xsl:text>
  <xsl:variable name="start" as="element()" select="key('patterns', @start, .)" />
  <xsl:apply-templates select="$start" mode="pat:report-error">
    <xsl:with-param name="grammar" select="." tunnel="yes" />
  </xsl:apply-templates>
</xsl:template>  
  
<xsl:key name="patterns" match="pat:define" use="@name" />
  
<xsl:template match="pat:define" mode="pat:report-error">
  <xsl:apply-templates select="pat:*" mode="pat:report-error" />
</xsl:template>
  
<xsl:template match="pat:ref" mode="pat:report-error">
  <xsl:param name="grammar" as="element()" tunnel="yes" />
  <xsl:apply-templates select="key('patterns', @name, $grammar)"
    mode="pat:report-error" />
</xsl:template>
  
<xsl:template match="pat:choice | pat:interleave"
  mode="pat:report-error" as="xs:string">
  <xsl:variable name="choices" as="xs:string+">
    <xsl:apply-templates select="pat:*" mode="pat:report-error" />
  </xsl:variable>
  <xsl:value-of>
    <xsl:for-each select="$choices">
      <xsl:value-of select="." />
      <xsl:choose>
        <xsl:when test="position() = last()" />
        <xsl:when test="position() = last() - 1">
          <xsl:if test="last() > 2">,</xsl:if>
          <xsl:text> or </xsl:text>
        </xsl:when>
        <xsl:otherwise>, </xsl:otherwise>
      </xsl:choose>    
    </xsl:for-each>
  </xsl:value-of>
</xsl:template>

<xsl:template match="pat:concur"
  mode="pat:report-error" as="xs:string">
  <xsl:variable name="choices" as="xs:string+">
    <xsl:apply-templates select="pat:*" mode="pat:report-error" />
  </xsl:variable>
  <xsl:value-of>
    <xsl:text>concurrent </xsl:text>
    <xsl:for-each select="$choices">
      <xsl:value-of select="." />
      <xsl:choose>
        <xsl:when test="position() = last()" />
        <xsl:when test="position() = last() - 1">
          <xsl:if test="last() > 2">,</xsl:if>
          <xsl:text> and </xsl:text>
        </xsl:when>
        <xsl:otherwise>, </xsl:otherwise>
      </xsl:choose>    
    </xsl:for-each>
  </xsl:value-of>
</xsl:template>
  
<xsl:template match="pat:all" mode="pat:report-error" as="xs:string">
  <xsl:variable name="choices" as="xs:string+">
    <xsl:apply-templates select="pat:*" mode="pat:report-error" />
  </xsl:variable>
  <xsl:value-of>
    <xsl:for-each select="$choices">
      <xsl:value-of select="." />
      <xsl:choose>
        <xsl:when test="position() = last()" />
        <xsl:when test="position() = last() - 1">
          <xsl:if test="last() > 2">,</xsl:if>
          <xsl:text> and </xsl:text>
        </xsl:when>
        <xsl:otherwise>, </xsl:otherwise>
      </xsl:choose>    
    </xsl:for-each>
  </xsl:value-of>
</xsl:template>  
  
<xsl:template match="pat:group | pat:after" mode="pat:report-error"
  as="xs:string">
  <xsl:apply-templates select="pat:*[1]" mode="pat:report-error" />
</xsl:template>
  
<xsl:template match="pat:range | pat:annotation | pat:atom |
                     pat:end-tag | pat:end-annotation | pat:end-atom" 
              mode="pat:report-error" as="xs:string">
  <xsl:value-of>
    <xsl:if test="self::pat:end-tag or 
                  self::pat:end-annotation or
                  self::pat:end-atom">
      <xsl:text>end of </xsl:text>
    </xsl:if>
    <xsl:text>[</xsl:text>
    <xsl:apply-templates select="pat:*[1]" mode="pat:report-error" />
    <xsl:text>] </xsl:text>
    <xsl:choose>
      <xsl:when test="self::pat:end-tag">range</xsl:when>
      <xsl:when test="starts-with(local-name(.), 'end-')">
        <xsl:value-of select="substring-after(local-name(.), 'end-')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="local-name(.)" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:value-of>
</xsl:template>
  
<xsl:template match="pat:name" mode="pat:report-error" as="xs:string">
  <xsl:sequence select="concat('{', @ns, '}', .)" />
</xsl:template>
  
<xsl:template match="pat:nsName" mode="pat:report-error" as="xs:string">
  <xsl:sequence select="concat('{', @ns, '}*')" />
</xsl:template>    

<xsl:template match="pat:nsNameExcept" mode="pat:report-error" as="xs:string">
  <xsl:value-of>
    <xsl:text>{</xsl:text>
    <xsl:value-of select="@ns" />
    <xsl:text>}* - </xsl:text>
    <xsl:apply-templates select="pat:*" mode="pat:report-error" />
  </xsl:value-of>
</xsl:template>    
    
<xsl:template match="pat:anyName" mode="pat:report-error" as="xs:string">
  <xsl:text>*</xsl:text>
</xsl:template>

<xsl:template match="pat:anyNameExcept" mode="pat:report-error" as="xs:string">
  <xsl:value-of>
    <xsl:text>* - </xsl:text>
    <xsl:apply-templates select="pat:*" mode="pat:report-error" />
  </xsl:value-of>
</xsl:template>
    
<xsl:template match="pat:text" mode="pat:report-error">
  <xsl:text>text</xsl:text>
</xsl:template>
  
<xsl:template match="pat:empty" mode="pat:report-error">
  <xsl:text>nothing</xsl:text>
</xsl:template>
  
</xsl:stylesheet>
