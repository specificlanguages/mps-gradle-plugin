<?xml version="1.0" encoding="UTF-8"?>
<model ref="r:4984d1ec-a1c9-4ad1-8af7-b206011783d5(com.specificlanguages.json.editor)">
  <persistence version="9" />
  <languages>
    <use id="18bc6592-03a6-4e29-a83a-7ff23bde13ba" name="jetbrains.mps.lang.editor" version="14" />
    <use id="aee9cad2-acd4-4608-aef2-0004f6a1cdbd" name="jetbrains.mps.lang.actions" version="4" />
    <devkit ref="fbc25dd2-5da4-483a-8b19-70928e1b62d7(jetbrains.mps.devkit.general-purpose)" />
  </languages>
  <imports>
    <import index="k9h7" ref="r:fd752404-89d3-4ffe-bc3a-7fb7a27c63b6(com.specificlanguages.json.structure)" implicit="true" />
    <import index="tpck" ref="r:00000000-0000-4000-0000-011c89590288(jetbrains.mps.lang.core.structure)" implicit="true" />
  </imports>
  <registry>
    <language id="18bc6592-03a6-4e29-a83a-7ff23bde13ba" name="jetbrains.mps.lang.editor">
      <concept id="1071666914219" name="jetbrains.mps.lang.editor.structure.ConceptEditorDeclaration" flags="ig" index="24kQdi" />
      <concept id="1140524381322" name="jetbrains.mps.lang.editor.structure.CellModel_ListWithRole" flags="ng" index="2czfm3">
        <child id="1140524464360" name="cellLayout" index="2czzBx" />
      </concept>
      <concept id="1106270549637" name="jetbrains.mps.lang.editor.structure.CellLayout_Horizontal" flags="nn" index="2iRfu4" />
      <concept id="1237303669825" name="jetbrains.mps.lang.editor.structure.CellLayout_Indent" flags="nn" index="l2Vlx" />
      <concept id="1237307900041" name="jetbrains.mps.lang.editor.structure.IndentLayoutIndentStyleClassItem" flags="ln" index="lj46D" />
      <concept id="1237308012275" name="jetbrains.mps.lang.editor.structure.IndentLayoutNewLineStyleClassItem" flags="ln" index="ljvvj" />
      <concept id="1237375020029" name="jetbrains.mps.lang.editor.structure.IndentLayoutNewLineChildrenStyleClassItem" flags="ln" index="pj6Ft" />
      <concept id="1080736578640" name="jetbrains.mps.lang.editor.structure.BaseEditorComponent" flags="ig" index="2wURMF">
        <child id="1080736633877" name="cellModel" index="2wV5jI" />
      </concept>
      <concept id="1186414536763" name="jetbrains.mps.lang.editor.structure.BooleanStyleSheetItem" flags="ln" index="VOi$J">
        <property id="1186414551515" name="flag" index="VOm3f" />
      </concept>
      <concept id="1233758997495" name="jetbrains.mps.lang.editor.structure.PunctuationLeftStyleClassItem" flags="ln" index="11L4FC" />
      <concept id="1233759184865" name="jetbrains.mps.lang.editor.structure.PunctuationRightStyleClassItem" flags="ln" index="11LMrY" />
      <concept id="1139848536355" name="jetbrains.mps.lang.editor.structure.CellModel_WithRole" flags="ng" index="1$h60E">
        <reference id="1140103550593" name="relationDeclaration" index="1NtTu8" />
      </concept>
      <concept id="1073389446423" name="jetbrains.mps.lang.editor.structure.CellModel_Collection" flags="sn" stub="3013115976261988961" index="3EZMnI">
        <child id="1106270802874" name="cellLayout" index="2iSdaV" />
        <child id="1073389446424" name="childCellModel" index="3EZMnx" />
      </concept>
      <concept id="1073389577006" name="jetbrains.mps.lang.editor.structure.CellModel_Constant" flags="sn" stub="3610246225209162225" index="3F0ifn">
        <property id="1073389577007" name="text" index="3F0ifm" />
      </concept>
      <concept id="1073389658414" name="jetbrains.mps.lang.editor.structure.CellModel_Property" flags="sg" stub="730538219796134133" index="3F0A7n" />
      <concept id="1219418625346" name="jetbrains.mps.lang.editor.structure.IStyleContainer" flags="ngI" index="3F0Thp">
        <child id="1219418656006" name="styleItem" index="3F10Kt" />
      </concept>
      <concept id="1073389882823" name="jetbrains.mps.lang.editor.structure.CellModel_RefNode" flags="sg" stub="730538219795960754" index="3F1sOY" />
      <concept id="1073390211982" name="jetbrains.mps.lang.editor.structure.CellModel_RefNodeList" flags="sg" stub="2794558372793454595" index="3F2HdR" />
      <concept id="1166049232041" name="jetbrains.mps.lang.editor.structure.AbstractComponent" flags="ng" index="1XWOmA">
        <reference id="1166049300910" name="conceptDeclaration" index="1XX52x" />
      </concept>
    </language>
  </registry>
  <node concept="24kQdi" id="1P8oQ4NaXEq">
    <ref role="1XX52x" to="k9h7:1P8oQ4NaXDS" resolve="JsonFile" />
    <node concept="3EZMnI" id="1P8oQ4NaXEs" role="2wV5jI">
      <node concept="3F0ifn" id="1P8oQ4NaXEz" role="3EZMnx">
        <property role="3F0ifm" value="JSON File" />
      </node>
      <node concept="3F0A7n" id="1P8oQ4NaXED" role="3EZMnx">
        <ref role="1NtTu8" to="tpck:h0TrG11" resolve="name" />
      </node>
      <node concept="3F0ifn" id="1P8oQ4NaXEL" role="3EZMnx">
        <property role="3F0ifm" value=".json" />
        <node concept="11L4FC" id="1P8oQ4NaXEQ" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
        <node concept="ljvvj" id="1P8oQ4NaXEY" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="3F0ifn" id="1P8oQ4NaXFf" role="3EZMnx">
        <property role="3F0ifm" value="" />
        <node concept="ljvvj" id="1P8oQ4NaXFC" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="3F1sOY" id="1P8oQ4NaXFv" role="3EZMnx">
        <ref role="1NtTu8" to="k9h7:1P8oQ4NaXDY" resolve="content" />
        <node concept="ljvvj" id="1P8oQ4NaXFE" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="l2Vlx" id="1P8oQ4NaXEv" role="2iSdaV" />
    </node>
  </node>
  <node concept="24kQdi" id="1P8oQ4NaYfF">
    <ref role="1XX52x" to="k9h7:1P8oQ4NaYfe" resolve="JsonString" />
    <node concept="3EZMnI" id="1P8oQ4NaYfH" role="2wV5jI">
      <node concept="3F0ifn" id="1P8oQ4NaYfO" role="3EZMnx">
        <property role="3F0ifm" value="&quot;" />
        <node concept="11LMrY" id="1P8oQ4NaYg9" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="3F0A7n" id="1P8oQ4NaYfW" role="3EZMnx">
        <ref role="1NtTu8" to="k9h7:1P8oQ4NaYfU" resolve="value" />
      </node>
      <node concept="3F0ifn" id="1P8oQ4NaYg4" role="3EZMnx">
        <property role="3F0ifm" value="&quot;" />
        <node concept="11L4FC" id="1P8oQ4NaYgb" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="2iRfu4" id="1P8oQ4NaYfK" role="2iSdaV" />
    </node>
  </node>
  <node concept="24kQdi" id="1P8oQ4NaYgG">
    <ref role="1XX52x" to="k9h7:1P8oQ4NaYgd" resolve="JsonArray" />
    <node concept="3EZMnI" id="1P8oQ4NaYgL" role="2wV5jI">
      <node concept="l2Vlx" id="1P8oQ4NaYgO" role="2iSdaV" />
      <node concept="3F0ifn" id="1P8oQ4NaYgS" role="3EZMnx">
        <property role="3F0ifm" value="[" />
        <node concept="ljvvj" id="1P8oQ4NaYh1" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="3F2HdR" id="1P8oQ4NaYhb" role="3EZMnx">
        <ref role="1NtTu8" to="k9h7:1P8oQ4NaYgg" resolve="items" />
        <node concept="l2Vlx" id="1P8oQ4NaYhd" role="2czzBx" />
        <node concept="pj6Ft" id="1P8oQ4NaYhk" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
        <node concept="ljvvj" id="1P8oQ4NaYhm" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
        <node concept="lj46D" id="1P8oQ4NaYhp" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="3F0ifn" id="1P8oQ4NaYgX" role="3EZMnx">
        <property role="3F0ifm" value="]" />
        <node concept="ljvvj" id="1P8oQ4NaYh3" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
    </node>
  </node>
  <node concept="24kQdi" id="1P8oQ4NaYi2">
    <ref role="1XX52x" to="k9h7:1P8oQ4NaYht" resolve="JsonNumber" />
    <node concept="3F0A7n" id="1P8oQ4NaYi4" role="2wV5jI">
      <ref role="1NtTu8" to="k9h7:1P8oQ4NaYhw" resolve="value" />
    </node>
  </node>
  <node concept="24kQdi" id="1P8oQ4NaZcJ">
    <ref role="1XX52x" to="k9h7:1P8oQ4NaZcg" resolve="JsonBoolean" />
    <node concept="3F0A7n" id="1P8oQ4NaZcL" role="2wV5jI">
      <ref role="1NtTu8" to="k9h7:1P8oQ4NaZch" resolve="value" />
    </node>
  </node>
  <node concept="24kQdi" id="1P8oQ4NbA0S">
    <ref role="1XX52x" to="k9h7:1P8oQ4NbA0r" resolve="JsonNull" />
    <node concept="3F0ifn" id="1P8oQ4NbA0U" role="2wV5jI">
      <property role="3F0ifm" value="null" />
    </node>
  </node>
  <node concept="24kQdi" id="1P8oQ4NbAbH">
    <ref role="1XX52x" to="k9h7:1P8oQ4NaXFJ" resolve="KeyValuePair" />
    <node concept="3EZMnI" id="1P8oQ4NbAbJ" role="2wV5jI">
      <node concept="3F0ifn" id="6UYVSPrHXMU" role="3EZMnx">
        <property role="3F0ifm" value="&quot;" />
        <node concept="11LMrY" id="6UYVSPrHXN0" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="3F0A7n" id="1P8oQ4NbAbQ" role="3EZMnx">
        <ref role="1NtTu8" to="tpck:h0TrG11" resolve="name" />
      </node>
      <node concept="3F0ifn" id="6UYVSPrHXN9" role="3EZMnx">
        <property role="3F0ifm" value="&quot;" />
        <node concept="11L4FC" id="6UYVSPrHXNh" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
        <node concept="11LMrY" id="6UYVSPrHXNm" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="3F0ifn" id="1P8oQ4NbAbW" role="3EZMnx">
        <property role="3F0ifm" value=":" />
      </node>
      <node concept="3F1sOY" id="1P8oQ4NbAc4" role="3EZMnx">
        <ref role="1NtTu8" to="k9h7:1P8oQ4NaXFM" resolve="value" />
      </node>
      <node concept="l2Vlx" id="1P8oQ4NbAbM" role="2iSdaV" />
    </node>
  </node>
  <node concept="24kQdi" id="6UYVSPrHTZz">
    <ref role="1XX52x" to="k9h7:1P8oQ4NaXFG" resolve="JsonObject" />
    <node concept="3EZMnI" id="6UYVSPrHTZ_" role="2wV5jI">
      <node concept="3F0ifn" id="6UYVSPrHTZG" role="3EZMnx">
        <property role="3F0ifm" value="{" />
        <node concept="ljvvj" id="6UYVSPrHU01" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="3F2HdR" id="6UYVSPrHTZU" role="3EZMnx">
        <ref role="1NtTu8" to="k9h7:1P8oQ4NaXFO" resolve="contents" />
        <node concept="l2Vlx" id="6UYVSPrHTZW" role="2czzBx" />
        <node concept="lj46D" id="6UYVSPrHU03" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
        <node concept="pj6Ft" id="6UYVSPrHU05" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
        <node concept="ljvvj" id="6UYVSPrHU08" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="3F0ifn" id="6UYVSPrHTZM" role="3EZMnx">
        <property role="3F0ifm" value="}" />
        <node concept="ljvvj" id="6UYVSPrHU0c" role="3F10Kt">
          <property role="VOm3f" value="true" />
        </node>
      </node>
      <node concept="l2Vlx" id="6UYVSPrHTZC" role="2iSdaV" />
    </node>
  </node>
</model>

