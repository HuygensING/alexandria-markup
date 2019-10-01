<p:declare-step version='1.0' name="main"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:tag="https://huygensing.github.io/TAG/TAGML/ns/tag"
>
  <p:output port="result"/>

  <!-- import the tag:load step declaration -->
  <p:import href="https://huygensing.github.io/TAG/TAGML/calabash-steps.xpl"/>

  <!-- load the tagmlfile, and export as XML (using trojan horse notation for overlapping hierarchies -->
  <tag:load name="importTAGML" tagmlfile="example.tagml"/>

  <!-- show the resulting XML -->
  <p:identity/>

</p:declare-step>