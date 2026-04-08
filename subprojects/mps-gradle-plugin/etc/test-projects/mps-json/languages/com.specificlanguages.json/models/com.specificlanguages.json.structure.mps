<?xml version="1.0" encoding="UTF-8"?>
<model ref="r:fd752404-89d3-4ffe-bc3a-7fb7a27c63b6(com.specificlanguages.json.structure)">
  <persistence version="9" />
  <languages>
    <use id="c72da2b9-7cce-4447-8389-f407dc1158b7" name="jetbrains.mps.lang.structure" version="9" />
    <devkit ref="78434eb8-b0e5-444b-850d-e7c4ad2da9ab(jetbrains.mps.devkit.aspect.structure)" />
  </languages>
  <imports>
    <import index="tpck" ref="r:00000000-0000-4000-0000-011c89590288(jetbrains.mps.lang.core.structure)" implicit="true" />
  </imports>
  <registry>
    <language id="c72da2b9-7cce-4447-8389-f407dc1158b7" name="jetbrains.mps.lang.structure">
      <concept id="1082978164218" name="jetbrains.mps.lang.structure.structure.DataTypeDeclaration" flags="ng" index="AxPO6">
        <property id="7791109065626895363" name="datatypeId" index="3F6X1D" />
      </concept>
      <concept id="1082978499127" name="jetbrains.mps.lang.structure.structure.ConstrainedDataTypeDeclaration" flags="ng" index="Az7Fb">
        <property id="1083066089218" name="constraint" index="FLfZY" />
      </concept>
      <concept id="1169125787135" name="jetbrains.mps.lang.structure.structure.AbstractConceptDeclaration" flags="ig" index="PkWjJ">
        <property id="6714410169261853888" name="conceptId" index="EcuMT" />
        <property id="4628067390765907488" name="conceptShortDescription" index="R4oN_" />
        <property id="5092175715804935370" name="conceptAlias" index="34LRSv" />
        <child id="1071489727083" name="linkDeclaration" index="1TKVEi" />
        <child id="1071489727084" name="propertyDeclaration" index="1TKVEl" />
      </concept>
      <concept id="1169125989551" name="jetbrains.mps.lang.structure.structure.InterfaceConceptDeclaration" flags="ig" index="PlHQZ" />
      <concept id="1169127622168" name="jetbrains.mps.lang.structure.structure.InterfaceConceptReference" flags="ig" index="PrWs8">
        <reference id="1169127628841" name="intfc" index="PrY4T" />
      </concept>
      <concept id="1071489090640" name="jetbrains.mps.lang.structure.structure.ConceptDeclaration" flags="ig" index="1TIwiD">
        <property id="1096454100552" name="rootable" index="19KtqR" />
        <reference id="1071489389519" name="extends" index="1TJDcQ" />
        <child id="1169129564478" name="implements" index="PzmwI" />
      </concept>
      <concept id="1071489288299" name="jetbrains.mps.lang.structure.structure.PropertyDeclaration" flags="ig" index="1TJgyi">
        <property id="241647608299431129" name="propertyId" index="IQ2nx" />
        <reference id="1082985295845" name="dataType" index="AX2Wp" />
      </concept>
      <concept id="1071489288298" name="jetbrains.mps.lang.structure.structure.LinkDeclaration" flags="ig" index="1TJgyj">
        <property id="1071599776563" name="role" index="20kJfa" />
        <property id="1071599893252" name="sourceCardinality" index="20lbJX" />
        <property id="1071599937831" name="metaClass" index="20lmBu" />
        <property id="241647608299431140" name="linkId" index="IQ2ns" />
        <reference id="1071599976176" name="target" index="20lvS9" />
      </concept>
    </language>
    <language id="ceab5195-25ea-4f22-9b92-103b95ca8c0c" name="jetbrains.mps.lang.core">
      <concept id="1169194658468" name="jetbrains.mps.lang.core.structure.INamedConcept" flags="ngI" index="TrEIO">
        <property id="1169194664001" name="name" index="TrG5h" />
      </concept>
    </language>
  </registry>
  <node concept="1TIwiD" id="1P8oQ4NaXDS">
    <property role="EcuMT" value="2110045694544566904" />
    <property role="TrG5h" value="JsonFile" />
    <property role="19KtqR" value="true" />
    <property role="34LRSv" value="JSON File" />
    <property role="R4oN_" value="Json File" />
    <ref role="1TJDcQ" to="tpck:gw2VY9q" resolve="BaseConcept" />
    <node concept="PrWs8" id="1P8oQ4NaXDT" role="PzmwI">
      <ref role="PrY4T" to="tpck:h0TrEE$" resolve="INamedConcept" />
    </node>
    <node concept="1TJgyj" id="1P8oQ4NaXDY" role="1TKVEi">
      <property role="IQ2ns" value="2110045694544566910" />
      <property role="20lmBu" value="fLJjDmT/aggregation" />
      <property role="20kJfa" value="content" />
      <property role="20lbJX" value="fLJekj4/_1" />
      <ref role="20lvS9" node="1P8oQ4NaXDX" resolve="IJsonValue" />
    </node>
  </node>
  <node concept="PlHQZ" id="1P8oQ4NaXDX">
    <property role="EcuMT" value="2110045694544566909" />
    <property role="TrG5h" value="IJsonValue" />
  </node>
  <node concept="1TIwiD" id="1P8oQ4NaXFG">
    <property role="EcuMT" value="2110045694544567020" />
    <property role="TrG5h" value="JsonObject" />
    <property role="34LRSv" value="{" />
    <property role="R4oN_" value="JSON Object" />
    <ref role="1TJDcQ" to="tpck:gw2VY9q" resolve="BaseConcept" />
    <node concept="PrWs8" id="1P8oQ4NaXFH" role="PzmwI">
      <ref role="PrY4T" node="1P8oQ4NaXDX" resolve="IJsonValue" />
    </node>
    <node concept="1TJgyj" id="1P8oQ4NaXFO" role="1TKVEi">
      <property role="IQ2ns" value="2110045694544567028" />
      <property role="20lmBu" value="fLJjDmT/aggregation" />
      <property role="20kJfa" value="contents" />
      <property role="20lbJX" value="fLJekj5/_0__n" />
      <ref role="20lvS9" node="1P8oQ4NaXFJ" resolve="KeyValuePair" />
    </node>
  </node>
  <node concept="1TIwiD" id="1P8oQ4NaXFJ">
    <property role="EcuMT" value="2110045694544567023" />
    <property role="TrG5h" value="KeyValuePair" />
    <property role="34LRSv" value="&quot;" />
    <property role="R4oN_" value="key/value pair" />
    <ref role="1TJDcQ" to="tpck:gw2VY9q" resolve="BaseConcept" />
    <node concept="1TJgyj" id="1P8oQ4NaXFM" role="1TKVEi">
      <property role="IQ2ns" value="2110045694544567026" />
      <property role="20lmBu" value="fLJjDmT/aggregation" />
      <property role="20kJfa" value="value" />
      <property role="20lbJX" value="fLJekj4/_1" />
      <ref role="20lvS9" node="1P8oQ4NaXDX" resolve="IJsonValue" />
    </node>
    <node concept="PrWs8" id="1P8oQ4NaXFK" role="PzmwI">
      <ref role="PrY4T" to="tpck:h0TrEE$" resolve="INamedConcept" />
    </node>
  </node>
  <node concept="1TIwiD" id="1P8oQ4NaYfe">
    <property role="EcuMT" value="2110045694544569294" />
    <property role="TrG5h" value="JsonString" />
    <property role="34LRSv" value="&quot;" />
    <property role="R4oN_" value="JSON String" />
    <ref role="1TJDcQ" to="tpck:gw2VY9q" resolve="BaseConcept" />
    <node concept="PrWs8" id="1P8oQ4NaYff" role="PzmwI">
      <ref role="PrY4T" node="1P8oQ4NaXDX" resolve="IJsonValue" />
    </node>
    <node concept="1TJgyi" id="1P8oQ4NaYfU" role="1TKVEl">
      <property role="IQ2nx" value="2110045694544569338" />
      <property role="TrG5h" value="value" />
      <ref role="AX2Wp" to="tpck:fKAOsGN" resolve="string" />
    </node>
  </node>
  <node concept="1TIwiD" id="1P8oQ4NaYgd">
    <property role="EcuMT" value="2110045694544569357" />
    <property role="TrG5h" value="JsonArray" />
    <property role="34LRSv" value="[" />
    <property role="R4oN_" value="JSON Array" />
    <ref role="1TJDcQ" to="tpck:gw2VY9q" resolve="BaseConcept" />
    <node concept="1TJgyj" id="1P8oQ4NaYgg" role="1TKVEi">
      <property role="IQ2ns" value="2110045694544569360" />
      <property role="20lmBu" value="fLJjDmT/aggregation" />
      <property role="20kJfa" value="items" />
      <property role="20lbJX" value="fLJekj5/_0__n" />
      <ref role="20lvS9" node="1P8oQ4NaXDX" resolve="IJsonValue" />
    </node>
    <node concept="PrWs8" id="1P8oQ4NaYge" role="PzmwI">
      <ref role="PrY4T" node="1P8oQ4NaXDX" resolve="IJsonValue" />
    </node>
  </node>
  <node concept="1TIwiD" id="1P8oQ4NaYht">
    <property role="EcuMT" value="2110045694544569437" />
    <property role="TrG5h" value="JsonNumber" />
    <property role="R4oN_" value="JSON Number" />
    <ref role="1TJDcQ" to="tpck:gw2VY9q" resolve="BaseConcept" />
    <node concept="1TJgyi" id="1P8oQ4NaYhw" role="1TKVEl">
      <property role="IQ2nx" value="2110045694544569440" />
      <property role="TrG5h" value="value" />
      <ref role="AX2Wp" node="1P8oQ4NaYhz" resolve="JsonNumberDatatype" />
    </node>
    <node concept="PrWs8" id="1P8oQ4NaYhu" role="PzmwI">
      <ref role="PrY4T" node="1P8oQ4NaXDX" resolve="IJsonValue" />
    </node>
  </node>
  <node concept="Az7Fb" id="1P8oQ4NaYhz">
    <property role="3F6X1D" value="2110045694544569443" />
    <property role="TrG5h" value="JsonNumberDatatype" />
    <property role="FLfZY" value="-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?" />
  </node>
  <node concept="1TIwiD" id="1P8oQ4NaZcg">
    <property role="EcuMT" value="2110045694544573200" />
    <property role="TrG5h" value="JsonBoolean" />
    <property role="R4oN_" value="JSON Boolean" />
    <ref role="1TJDcQ" to="tpck:gw2VY9q" resolve="BaseConcept" />
    <node concept="1TJgyi" id="1P8oQ4NaZch" role="1TKVEl">
      <property role="IQ2nx" value="2110045694544573201" />
      <property role="TrG5h" value="value" />
      <ref role="AX2Wp" to="tpck:fKAQMTB" resolve="boolean" />
    </node>
    <node concept="PrWs8" id="1P8oQ4NaZcj" role="PzmwI">
      <ref role="PrY4T" node="1P8oQ4NaXDX" resolve="IJsonValue" />
    </node>
  </node>
  <node concept="1TIwiD" id="1P8oQ4NbA0r">
    <property role="EcuMT" value="2110045694544732187" />
    <property role="TrG5h" value="JsonNull" />
    <property role="34LRSv" value="null" />
    <property role="R4oN_" value="JSON null" />
    <ref role="1TJDcQ" to="tpck:gw2VY9q" resolve="BaseConcept" />
    <node concept="PrWs8" id="1P8oQ4NbA0s" role="PzmwI">
      <ref role="PrY4T" node="1P8oQ4NaXDX" resolve="IJsonValue" />
    </node>
  </node>
</model>

