<p:declare-step version='1.0' name="main"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:tag="https://huygensing.github.io/TAG/TAGML/ns/tag"
>
  <p:output port="result"/>

  <p:import href="https://huygensing.github.io/TAG/TAGML/calabash-steps.xpl"/>

  <tag:load name="importTAGML">
  </tag:load>

  <p:identity/>

</p:declare-step>