<?xml version="1.0" encoding="UTF-8"?>
<model ref="r:9f886d96-0ce3-485a-9a55-2e2be971036b(com.specificlanguages.json.textGen)">
  <persistence version="9" />
  <languages>
    <use id="b83431fe-5c8f-40bc-8a36-65e25f4dd253" name="jetbrains.mps.lang.textGen" version="1" />
    <devkit ref="fa73d85a-ac7f-447b-846c-fcdc41caa600(jetbrains.mps.devkit.aspect.textgen)" />
  </languages>
  <imports>
    <import index="k9h7" ref="r:fd752404-89d3-4ffe-bc3a-7fb7a27c63b6(com.specificlanguages.json.structure)" implicit="true" />
    <import index="wyt6" ref="6354ebe7-c22a-4a0f-ac54-50b52ab9b065/java:java.lang(JDK/)" implicit="true" />
    <import index="tpck" ref="r:00000000-0000-4000-0000-011c89590288(jetbrains.mps.lang.core.structure)" implicit="true" />
  </imports>
  <registry>
    <language id="f3061a53-9226-4cc5-a443-f952ceaf5816" name="jetbrains.mps.baseLanguage">
      <concept id="1202948039474" name="jetbrains.mps.baseLanguage.structure.InstanceMethodCallOperation" flags="nn" index="liA8E" />
      <concept id="1154032098014" name="jetbrains.mps.baseLanguage.structure.AbstractLoopStatement" flags="nn" index="2LF5Ji">
        <child id="1154032183016" name="body" index="2LFqv$" />
      </concept>
      <concept id="1197027756228" name="jetbrains.mps.baseLanguage.structure.DotExpression" flags="nn" index="2OqwBi">
        <child id="1197027771414" name="operand" index="2Oq$k0" />
        <child id="1197027833540" name="operation" index="2OqNvi" />
      </concept>
      <concept id="1137021947720" name="jetbrains.mps.baseLanguage.structure.ConceptFunction" flags="in" index="2VMwT0">
        <child id="1137022507850" name="body" index="2VODD2" />
      </concept>
      <concept id="1070475926800" name="jetbrains.mps.baseLanguage.structure.StringLiteral" flags="nn" index="Xl_RD">
        <property id="1070475926801" name="value" index="Xl_RC" />
      </concept>
      <concept id="1081236700938" name="jetbrains.mps.baseLanguage.structure.StaticMethodDeclaration" flags="ig" index="2YIFZL" />
      <concept id="1081236700937" name="jetbrains.mps.baseLanguage.structure.StaticMethodCall" flags="nn" index="2YIFZM">
        <reference id="1144433194310" name="classConcept" index="1Pybhc" />
      </concept>
      <concept id="1068390468198" name="jetbrains.mps.baseLanguage.structure.ClassConcept" flags="ig" index="312cEu" />
      <concept id="1068498886296" name="jetbrains.mps.baseLanguage.structure.VariableReference" flags="nn" index="37vLTw">
        <reference id="1068581517664" name="variableDeclaration" index="3cqZAo" />
      </concept>
      <concept id="1068498886292" name="jetbrains.mps.baseLanguage.structure.ParameterDeclaration" flags="ir" index="37vLTG" />
      <concept id="1225271177708" name="jetbrains.mps.baseLanguage.structure.StringType" flags="in" index="17QB3L" />
      <concept id="4972933694980447171" name="jetbrains.mps.baseLanguage.structure.BaseVariableDeclaration" flags="ng" index="19Szcq">
        <child id="5680397130376446158" name="type" index="1tU5fm" />
      </concept>
      <concept id="1068580123132" name="jetbrains.mps.baseLanguage.structure.BaseMethodDeclaration" flags="ng" index="3clF44">
        <child id="1068580123133" name="returnType" index="3clF45" />
        <child id="1068580123134" name="parameter" index="3clF46" />
        <child id="1068580123135" name="body" index="3clF47" />
      </concept>
      <concept id="1068580123155" name="jetbrains.mps.baseLanguage.structure.ExpressionStatement" flags="nn" index="3clFbF">
        <child id="1068580123156" name="expression" index="3clFbG" />
      </concept>
      <concept id="1068580123157" name="jetbrains.mps.baseLanguage.structure.Statement" flags="nn" index="3clFbH" />
      <concept id="1068580123159" name="jetbrains.mps.baseLanguage.structure.IfStatement" flags="nn" index="3clFbJ">
        <child id="1068580123160" name="condition" index="3clFbw" />
        <child id="1068580123161" name="ifTrue" index="3clFbx" />
      </concept>
      <concept id="1068580123136" name="jetbrains.mps.baseLanguage.structure.StatementList" flags="sn" stub="5293379017992965193" index="3clFbS">
        <child id="1068581517665" name="statement" index="3cqZAp" />
      </concept>
      <concept id="1068581242878" name="jetbrains.mps.baseLanguage.structure.ReturnStatement" flags="nn" index="3cpWs6">
        <child id="1068581517676" name="expression" index="3cqZAk" />
      </concept>
      <concept id="1204053956946" name="jetbrains.mps.baseLanguage.structure.IMethodCall" flags="ngI" index="1ndlxa">
        <reference id="1068499141037" name="baseMethodDeclaration" index="37wK5l" />
        <child id="1068499141038" name="actualArgument" index="37wK5m" />
      </concept>
      <concept id="1107461130800" name="jetbrains.mps.baseLanguage.structure.Classifier" flags="ng" index="3pOWGL">
        <child id="5375687026011219971" name="member" index="jymVt" unordered="true" />
      </concept>
      <concept id="1081773326031" name="jetbrains.mps.baseLanguage.structure.BinaryOperation" flags="nn" index="3uHJSO">
        <child id="1081773367579" name="rightExpression" index="3uHU7w" />
        <child id="1081773367580" name="leftExpression" index="3uHU7B" />
      </concept>
      <concept id="1073239437375" name="jetbrains.mps.baseLanguage.structure.NotEqualsExpression" flags="nn" index="3y3z36" />
      <concept id="1178549954367" name="jetbrains.mps.baseLanguage.structure.IVisible" flags="ngI" index="1B3ioH">
        <child id="1178549979242" name="visibility" index="1B3o_S" />
      </concept>
      <concept id="1146644602865" name="jetbrains.mps.baseLanguage.structure.PublicVisibility" flags="nn" index="3Tm1VV" />
    </language>
    <language id="b83431fe-5c8f-40bc-8a36-65e25f4dd253" name="jetbrains.mps.lang.textGen">
      <concept id="8931911391946696733" name="jetbrains.mps.lang.textGen.structure.ExtensionDeclaration" flags="in" index="9MYSb" />
      <concept id="1237305208784" name="jetbrains.mps.lang.textGen.structure.NewLineAppendPart" flags="ng" index="l8MVK" />
      <concept id="1237305334312" name="jetbrains.mps.lang.textGen.structure.NodeAppendPart" flags="ng" index="l9hG8">
        <child id="1237305790512" name="value" index="lb14g" />
      </concept>
      <concept id="1237305557638" name="jetbrains.mps.lang.textGen.structure.ConstantStringAppendPart" flags="ng" index="la8eA">
        <property id="1237305576108" name="value" index="lacIc" />
      </concept>
      <concept id="1237306079178" name="jetbrains.mps.lang.textGen.structure.AppendOperation" flags="nn" index="lc7rE">
        <child id="1237306115446" name="part" index="lcghm" />
      </concept>
      <concept id="4357423944233036906" name="jetbrains.mps.lang.textGen.structure.IndentPart" flags="ng" index="2BGw6n" />
      <concept id="1233670071145" name="jetbrains.mps.lang.textGen.structure.ConceptTextGenDeclaration" flags="ig" index="WtQ9Q">
        <reference id="1233670257997" name="conceptDeclaration" index="WuzLi" />
        <child id="1233749296504" name="textGenBlock" index="11c4hB" />
        <child id="7991274449437422201" name="extension" index="33IsuW" />
      </concept>
      <concept id="1233748055915" name="jetbrains.mps.lang.textGen.structure.NodeParameter" flags="nn" index="117lpO" />
      <concept id="1233749247888" name="jetbrains.mps.lang.textGen.structure.GenerateTextDeclaration" flags="in" index="11bSqf" />
      <concept id="1236188139846" name="jetbrains.mps.lang.textGen.structure.WithIndentOperation" flags="nn" index="3izx1p">
        <child id="1236188238861" name="list" index="3izTki" />
      </concept>
    </language>
    <language id="7866978e-a0f0-4cc7-81bc-4d213d9375e1" name="jetbrains.mps.lang.smodel">
      <concept id="1138056022639" name="jetbrains.mps.lang.smodel.structure.SPropertyAccess" flags="nn" index="3TrcHB">
        <reference id="1138056395725" name="property" index="3TsBF5" />
      </concept>
      <concept id="1138056143562" name="jetbrains.mps.lang.smodel.structure.SLinkAccess" flags="nn" index="3TrEf2">
        <reference id="1138056516764" name="link" index="3Tt5mk" />
      </concept>
      <concept id="1138056282393" name="jetbrains.mps.lang.smodel.structure.SLinkListAccess" flags="nn" index="3Tsc0h">
        <reference id="1138056546658" name="link" index="3TtcxE" />
      </concept>
    </language>
    <language id="ceab5195-25ea-4f22-9b92-103b95ca8c0c" name="jetbrains.mps.lang.core">
      <concept id="1169194658468" name="jetbrains.mps.lang.core.structure.INamedConcept" flags="ngI" index="TrEIO">
        <property id="1169194664001" name="name" index="TrG5h" />
      </concept>
    </language>
    <language id="83888646-71ce-4f1c-9c53-c54016f6ad4f" name="jetbrains.mps.baseLanguage.collections">
      <concept id="1153943597977" name="jetbrains.mps.baseLanguage.collections.structure.ForEachStatement" flags="nn" index="2Gpval">
        <child id="1153944400369" name="variable" index="2Gsz3X" />
        <child id="1153944424730" name="inputSequence" index="2GsD0m" />
      </concept>
      <concept id="1153944193378" name="jetbrains.mps.baseLanguage.collections.structure.ForEachVariable" flags="nr" index="2GrKxI" />
      <concept id="1153944233411" name="jetbrains.mps.baseLanguage.collections.structure.ForEachVariableReference" flags="nn" index="2GrUjf">
        <reference id="1153944258490" name="variable" index="2Gs0qQ" />
      </concept>
      <concept id="1165595910856" name="jetbrains.mps.baseLanguage.collections.structure.GetLastOperation" flags="nn" index="1yVyf7" />
      <concept id="1176501494711" name="jetbrains.mps.baseLanguage.collections.structure.IsNotEmptyOperation" flags="nn" index="3GX2aA" />
    </language>
  </registry>
  <node concept="WtQ9Q" id="1P8oQ4NaXGl">
    <ref role="WuzLi" to="k9h7:1P8oQ4NaXDS" resolve="JsonFile" />
    <node concept="9MYSb" id="1P8oQ4NaXKU" role="33IsuW">
      <node concept="3clFbS" id="1P8oQ4NaXKV" role="2VODD2">
        <node concept="3clFbF" id="1P8oQ4NaXLl" role="3cqZAp">
          <node concept="Xl_RD" id="1P8oQ4NaXLk" role="3clFbG">
            <property role="Xl_RC" value="json" />
          </node>
        </node>
      </node>
    </node>
    <node concept="11bSqf" id="1P8oQ4NaXMo" role="11c4hB">
      <node concept="3clFbS" id="1P8oQ4NaXMp" role="2VODD2">
        <node concept="lc7rE" id="1P8oQ4NaXRo" role="3cqZAp">
          <node concept="l9hG8" id="1P8oQ4NaXRG" role="lcghm">
            <node concept="2OqwBi" id="1P8oQ4NaY1t" role="lb14g">
              <node concept="117lpO" id="1P8oQ4NaXSy" role="2Oq$k0" />
              <node concept="3TrEf2" id="1P8oQ4NaYah" role="2OqNvi">
                <ref role="3Tt5mk" to="k9h7:1P8oQ4NaXDY" resolve="content" />
              </node>
            </node>
          </node>
        </node>
      </node>
    </node>
  </node>
  <node concept="WtQ9Q" id="1P8oQ4Nb3YX">
    <ref role="WuzLi" to="k9h7:1P8oQ4NaXFG" resolve="JsonObject" />
    <node concept="11bSqf" id="1P8oQ4Nb3YY" role="11c4hB">
      <node concept="3clFbS" id="1P8oQ4Nb3YZ" role="2VODD2">
        <node concept="lc7rE" id="1P8oQ4Nb3Zg" role="3cqZAp">
          <node concept="la8eA" id="1P8oQ4Nb3Z$" role="lcghm">
            <property role="lacIc" value="{" />
          </node>
        </node>
        <node concept="3izx1p" id="1P8oQ4Nb416" role="3cqZAp">
          <node concept="3clFbS" id="1P8oQ4Nb418" role="3izTki">
            <node concept="2Gpval" id="1P8oQ4Nb41t" role="3cqZAp">
              <node concept="2GrKxI" id="1P8oQ4Nb41u" role="2Gsz3X">
                <property role="TrG5h" value="pair" />
              </node>
              <node concept="2OqwBi" id="1P8oQ4Nb4b0" role="2GsD0m">
                <node concept="117lpO" id="1P8oQ4Nb42a" role="2Oq$k0" />
                <node concept="3Tsc0h" id="1P8oQ4Nb4lG" role="2OqNvi">
                  <ref role="3TtcxE" to="k9h7:1P8oQ4NaXFO" resolve="contents" />
                </node>
              </node>
              <node concept="3clFbS" id="1P8oQ4Nb41w" role="2LFqv$">
                <node concept="lc7rE" id="1P8oQ4Nb4ou" role="3cqZAp">
                  <node concept="l8MVK" id="1P8oQ4Nb4oM" role="lcghm" />
                  <node concept="2BGw6n" id="1P8oQ4Nb4po" role="lcghm" />
                  <node concept="l9hG8" id="1P8oQ4Nb4q0" role="lcghm">
                    <node concept="2GrUjf" id="1P8oQ4Nb4qS" role="lb14g">
                      <ref role="2Gs0qQ" node="1P8oQ4Nb41u" resolve="pair" />
                    </node>
                  </node>
                </node>
                <node concept="3clFbJ" id="1P8oQ4Nb4uv" role="3cqZAp">
                  <node concept="3clFbS" id="1P8oQ4Nb4ux" role="3clFbx">
                    <node concept="lc7rE" id="1P8oQ4Nb8SS" role="3cqZAp">
                      <node concept="la8eA" id="1P8oQ4Nb8Te" role="lcghm">
                        <property role="lacIc" value="," />
                      </node>
                    </node>
                  </node>
                  <node concept="3y3z36" id="1P8oQ4Nb4BS" role="3clFbw">
                    <node concept="2OqwBi" id="1P8oQ4Nb6M$" role="3uHU7w">
                      <node concept="2OqwBi" id="1P8oQ4Nb4ZT" role="2Oq$k0">
                        <node concept="117lpO" id="1P8oQ4Nb4NF" role="2Oq$k0" />
                        <node concept="3Tsc0h" id="1P8oQ4Nb5b3" role="2OqNvi">
                          <ref role="3TtcxE" to="k9h7:1P8oQ4NaXFO" resolve="contents" />
                        </node>
                      </node>
                      <node concept="1yVyf7" id="1P8oQ4Nb8Gc" role="2OqNvi" />
                    </node>
                    <node concept="2GrUjf" id="1P8oQ4Nb4v0" role="3uHU7B">
                      <ref role="2Gs0qQ" node="1P8oQ4Nb41u" resolve="pair" />
                    </node>
                  </node>
                </node>
              </node>
            </node>
          </node>
        </node>
        <node concept="3clFbJ" id="1P8oQ4Nb96M" role="3cqZAp">
          <node concept="3clFbS" id="1P8oQ4Nb96O" role="3clFbx">
            <node concept="lc7rE" id="1P8oQ4Nbd7x" role="3cqZAp">
              <node concept="l8MVK" id="1P8oQ4Nbd7R" role="lcghm" />
              <node concept="2BGw6n" id="1P8oQ4Nbdsz" role="lcghm" />
            </node>
          </node>
          <node concept="2OqwBi" id="1P8oQ4NbbdJ" role="3clFbw">
            <node concept="2OqwBi" id="1P8oQ4Nb9ia" role="2Oq$k0">
              <node concept="117lpO" id="1P8oQ4Nb99w" role="2Oq$k0" />
              <node concept="3Tsc0h" id="1P8oQ4Nb9sf" role="2OqNvi">
                <ref role="3TtcxE" to="k9h7:1P8oQ4NaXFO" resolve="contents" />
              </node>
            </node>
            <node concept="3GX2aA" id="1P8oQ4Nbd6u" role="2OqNvi" />
          </node>
        </node>
        <node concept="3clFbH" id="1P8oQ4Nbdl9" role="3cqZAp" />
        <node concept="lc7rE" id="1P8oQ4Nb408" role="3cqZAp">
          <node concept="la8eA" id="1P8oQ4Nb40w" role="lcghm">
            <property role="lacIc" value="}" />
          </node>
        </node>
      </node>
    </node>
  </node>
  <node concept="WtQ9Q" id="1P8oQ4NbkYw">
    <ref role="WuzLi" to="k9h7:1P8oQ4NaYgd" resolve="JsonArray" />
    <node concept="11bSqf" id="1P8oQ4NbkYx" role="11c4hB">
      <node concept="3clFbS" id="1P8oQ4NbkYy" role="2VODD2">
        <node concept="lc7rE" id="1P8oQ4NbkYN" role="3cqZAp">
          <node concept="la8eA" id="1P8oQ4NbkYO" role="lcghm">
            <property role="lacIc" value="[" />
          </node>
        </node>
        <node concept="3izx1p" id="1P8oQ4NbkYP" role="3cqZAp">
          <node concept="3clFbS" id="1P8oQ4NbkYQ" role="3izTki">
            <node concept="2Gpval" id="1P8oQ4NbkYR" role="3cqZAp">
              <node concept="2GrKxI" id="1P8oQ4NbkYS" role="2Gsz3X">
                <property role="TrG5h" value="item" />
              </node>
              <node concept="2OqwBi" id="1P8oQ4NbkYT" role="2GsD0m">
                <node concept="117lpO" id="1P8oQ4NbkYU" role="2Oq$k0" />
                <node concept="3Tsc0h" id="1P8oQ4Nbltu" role="2OqNvi">
                  <ref role="3TtcxE" to="k9h7:1P8oQ4NaYgg" resolve="items" />
                </node>
              </node>
              <node concept="3clFbS" id="1P8oQ4NbkYW" role="2LFqv$">
                <node concept="lc7rE" id="1P8oQ4NbkYX" role="3cqZAp">
                  <node concept="l8MVK" id="1P8oQ4NbkYY" role="lcghm" />
                  <node concept="2BGw6n" id="1P8oQ4NbkYZ" role="lcghm" />
                  <node concept="l9hG8" id="1P8oQ4NbkZ0" role="lcghm">
                    <node concept="2GrUjf" id="1P8oQ4NbkZ1" role="lb14g">
                      <ref role="2Gs0qQ" node="1P8oQ4NbkYS" resolve="item" />
                    </node>
                  </node>
                </node>
                <node concept="3clFbJ" id="1P8oQ4NbkZ2" role="3cqZAp">
                  <node concept="3clFbS" id="1P8oQ4NbkZ3" role="3clFbx">
                    <node concept="lc7rE" id="1P8oQ4NbkZ4" role="3cqZAp">
                      <node concept="la8eA" id="1P8oQ4NbkZ5" role="lcghm">
                        <property role="lacIc" value="," />
                      </node>
                    </node>
                  </node>
                  <node concept="3y3z36" id="1P8oQ4NbkZ6" role="3clFbw">
                    <node concept="2OqwBi" id="1P8oQ4NbkZ7" role="3uHU7w">
                      <node concept="2OqwBi" id="1P8oQ4NbkZ8" role="2Oq$k0">
                        <node concept="117lpO" id="1P8oQ4NbkZ9" role="2Oq$k0" />
                        <node concept="3Tsc0h" id="1P8oQ4Nbm3m" role="2OqNvi">
                          <ref role="3TtcxE" to="k9h7:1P8oQ4NaYgg" resolve="items" />
                        </node>
                      </node>
                      <node concept="1yVyf7" id="1P8oQ4NbkZb" role="2OqNvi" />
                    </node>
                    <node concept="2GrUjf" id="1P8oQ4NbkZc" role="3uHU7B">
                      <ref role="2Gs0qQ" node="1P8oQ4NbkYS" resolve="item" />
                    </node>
                  </node>
                </node>
              </node>
            </node>
          </node>
        </node>
        <node concept="3clFbH" id="1P8oQ4Nbmfb" role="3cqZAp" />
        <node concept="3clFbJ" id="1P8oQ4NbkZd" role="3cqZAp">
          <node concept="3clFbS" id="1P8oQ4NbkZe" role="3clFbx">
            <node concept="lc7rE" id="1P8oQ4NbkZf" role="3cqZAp">
              <node concept="l8MVK" id="1P8oQ4NbkZg" role="lcghm" />
              <node concept="2BGw6n" id="1P8oQ4NbkZh" role="lcghm" />
            </node>
          </node>
          <node concept="2OqwBi" id="1P8oQ4NbkZi" role="3clFbw">
            <node concept="2OqwBi" id="1P8oQ4NbkZj" role="2Oq$k0">
              <node concept="117lpO" id="1P8oQ4NbkZk" role="2Oq$k0" />
              <node concept="3Tsc0h" id="1P8oQ4NbkZl" role="2OqNvi">
                <ref role="3TtcxE" to="k9h7:1P8oQ4NaYgg" resolve="items" />
              </node>
            </node>
            <node concept="3GX2aA" id="1P8oQ4NbkZm" role="2OqNvi" />
          </node>
        </node>
        <node concept="lc7rE" id="1P8oQ4NbkZo" role="3cqZAp">
          <node concept="la8eA" id="1P8oQ4NbkZp" role="lcghm">
            <property role="lacIc" value="]" />
          </node>
        </node>
      </node>
    </node>
  </node>
  <node concept="WtQ9Q" id="1P8oQ4Nbmmc">
    <ref role="WuzLi" to="k9h7:1P8oQ4NaZcg" resolve="JsonBoolean" />
    <node concept="11bSqf" id="1P8oQ4Nbmmd" role="11c4hB">
      <node concept="3clFbS" id="1P8oQ4Nbmme" role="2VODD2">
        <node concept="lc7rE" id="1P8oQ4Nbmmv" role="3cqZAp">
          <node concept="l9hG8" id="1P8oQ4NbmmN" role="lcghm">
            <node concept="2YIFZM" id="1P8oQ4Nbmof" role="lb14g">
              <ref role="37wK5l" to="wyt6:~Boolean.toString(boolean)" resolve="toString" />
              <ref role="1Pybhc" to="wyt6:~Boolean" resolve="Boolean" />
              <node concept="2OqwBi" id="1P8oQ4NbmEd" role="37wK5m">
                <node concept="117lpO" id="1P8oQ4Nbmt1" role="2Oq$k0" />
                <node concept="3TrcHB" id="1P8oQ4NbmO$" role="2OqNvi">
                  <ref role="3TsBF5" to="k9h7:1P8oQ4NaZch" resolve="value" />
                </node>
              </node>
            </node>
          </node>
        </node>
      </node>
    </node>
  </node>
  <node concept="WtQ9Q" id="1P8oQ4NbmRN">
    <ref role="WuzLi" to="k9h7:1P8oQ4NaYht" resolve="JsonNumber" />
    <node concept="11bSqf" id="1P8oQ4NbmRO" role="11c4hB">
      <node concept="3clFbS" id="1P8oQ4NbmRP" role="2VODD2">
        <node concept="lc7rE" id="1P8oQ4NbmS6" role="3cqZAp">
          <node concept="l9hG8" id="1P8oQ4NbmSq" role="lcghm">
            <node concept="2OqwBi" id="1P8oQ4Nbn2X" role="lb14g">
              <node concept="117lpO" id="1P8oQ4NbmTg" role="2Oq$k0" />
              <node concept="3TrcHB" id="1P8oQ4NbndL" role="2OqNvi">
                <ref role="3TsBF5" to="k9h7:1P8oQ4NaYhw" resolve="value" />
              </node>
            </node>
          </node>
        </node>
      </node>
    </node>
  </node>
  <node concept="WtQ9Q" id="1P8oQ4Nbnhh">
    <ref role="WuzLi" to="k9h7:1P8oQ4NaYfe" resolve="JsonString" />
    <node concept="11bSqf" id="1P8oQ4Nbnhi" role="11c4hB">
      <node concept="3clFbS" id="1P8oQ4Nbnhj" role="2VODD2">
        <node concept="lc7rE" id="1P8oQ4Nbnh$" role="3cqZAp">
          <node concept="la8eA" id="1P8oQ4NbnhS" role="lcghm">
            <property role="lacIc" value="&quot;" />
          </node>
          <node concept="l9hG8" id="1P8oQ4Nb_DH" role="lcghm">
            <node concept="2YIFZM" id="1P8oQ4Nb_Fr" role="lb14g">
              <ref role="37wK5l" node="1P8oQ4Nb$s0" resolve="escapeJson" />
              <ref role="1Pybhc" node="1P8oQ4Nb$q_" resolve="Escaping" />
              <node concept="2OqwBi" id="1P8oQ4Nb_Nc" role="37wK5m">
                <node concept="117lpO" id="1P8oQ4Nb_G5" role="2Oq$k0" />
                <node concept="3TrcHB" id="1P8oQ4Nb_Xy" role="2OqNvi">
                  <ref role="3TsBF5" to="k9h7:1P8oQ4NaYfU" resolve="value" />
                </node>
              </node>
            </node>
          </node>
          <node concept="la8eA" id="1P8oQ4NbA5_" role="lcghm">
            <property role="lacIc" value="&quot;" />
          </node>
        </node>
      </node>
    </node>
  </node>
  <node concept="312cEu" id="1P8oQ4Nb$q_">
    <property role="TrG5h" value="Escaping" />
    <node concept="2YIFZL" id="1P8oQ4Nb$s0" role="jymVt">
      <property role="TrG5h" value="escapeJson" />
      <node concept="3clFbS" id="1P8oQ4Nb$s3" role="3clF47">
        <node concept="3cpWs6" id="1P8oQ4Nb_b8" role="3cqZAp">
          <node concept="2OqwBi" id="1P8oQ4Nb_b9" role="3cqZAk">
            <node concept="2OqwBi" id="1P8oQ4Nb_ba" role="2Oq$k0">
              <node concept="2OqwBi" id="1P8oQ4Nb_bb" role="2Oq$k0">
                <node concept="2OqwBi" id="1P8oQ4Nb_bc" role="2Oq$k0">
                  <node concept="2OqwBi" id="1P8oQ4Nb_bd" role="2Oq$k0">
                    <node concept="2OqwBi" id="1P8oQ4Nb_be" role="2Oq$k0">
                      <node concept="2OqwBi" id="1P8oQ4Nb_bf" role="2Oq$k0">
                        <node concept="37vLTw" id="1P8oQ4Nb_bg" role="2Oq$k0">
                          <ref role="3cqZAo" node="1P8oQ4Nb$sr" resolve="raw" />
                        </node>
                        <node concept="liA8E" id="1P8oQ4Nb_bh" role="2OqNvi">
                          <ref role="37wK5l" to="wyt6:~String.replace(java.lang.CharSequence,java.lang.CharSequence)" resolve="replace" />
                          <node concept="Xl_RD" id="1P8oQ4Nb_bi" role="37wK5m">
                            <property role="Xl_RC" value="\\" />
                          </node>
                          <node concept="Xl_RD" id="1P8oQ4Nb_bj" role="37wK5m">
                            <property role="Xl_RC" value="\\\\" />
                          </node>
                        </node>
                      </node>
                      <node concept="liA8E" id="1P8oQ4Nb_bk" role="2OqNvi">
                        <ref role="37wK5l" to="wyt6:~String.replace(java.lang.CharSequence,java.lang.CharSequence)" resolve="replace" />
                        <node concept="Xl_RD" id="1P8oQ4Nb_bl" role="37wK5m">
                          <property role="Xl_RC" value="\&quot;" />
                        </node>
                        <node concept="Xl_RD" id="1P8oQ4Nb_bm" role="37wK5m">
                          <property role="Xl_RC" value="\\\&quot;" />
                        </node>
                      </node>
                    </node>
                    <node concept="liA8E" id="1P8oQ4Nb_bn" role="2OqNvi">
                      <ref role="37wK5l" to="wyt6:~String.replace(java.lang.CharSequence,java.lang.CharSequence)" resolve="replace" />
                      <node concept="Xl_RD" id="1P8oQ4Nb_bo" role="37wK5m">
                        <property role="Xl_RC" value="\b" />
                      </node>
                      <node concept="Xl_RD" id="1P8oQ4Nb_bp" role="37wK5m">
                        <property role="Xl_RC" value="\\b" />
                      </node>
                    </node>
                  </node>
                  <node concept="liA8E" id="1P8oQ4Nb_bq" role="2OqNvi">
                    <ref role="37wK5l" to="wyt6:~String.replace(java.lang.CharSequence,java.lang.CharSequence)" resolve="replace" />
                    <node concept="Xl_RD" id="1P8oQ4Nb_br" role="37wK5m">
                      <property role="Xl_RC" value="\f" />
                    </node>
                    <node concept="Xl_RD" id="1P8oQ4Nb_bs" role="37wK5m">
                      <property role="Xl_RC" value="\\f" />
                    </node>
                  </node>
                </node>
                <node concept="liA8E" id="1P8oQ4Nb_bt" role="2OqNvi">
                  <ref role="37wK5l" to="wyt6:~String.replace(java.lang.CharSequence,java.lang.CharSequence)" resolve="replace" />
                  <node concept="Xl_RD" id="1P8oQ4Nb_bu" role="37wK5m">
                    <property role="Xl_RC" value="\n" />
                  </node>
                  <node concept="Xl_RD" id="1P8oQ4Nb_bv" role="37wK5m">
                    <property role="Xl_RC" value="\\n" />
                  </node>
                </node>
              </node>
              <node concept="liA8E" id="1P8oQ4Nb_bw" role="2OqNvi">
                <ref role="37wK5l" to="wyt6:~String.replace(java.lang.CharSequence,java.lang.CharSequence)" resolve="replace" />
                <node concept="Xl_RD" id="1P8oQ4Nb_bx" role="37wK5m">
                  <property role="Xl_RC" value="\r" />
                </node>
                <node concept="Xl_RD" id="1P8oQ4Nb_by" role="37wK5m">
                  <property role="Xl_RC" value="\\r" />
                </node>
              </node>
            </node>
            <node concept="liA8E" id="1P8oQ4Nb_bz" role="2OqNvi">
              <ref role="37wK5l" to="wyt6:~String.replace(java.lang.CharSequence,java.lang.CharSequence)" resolve="replace" />
              <node concept="Xl_RD" id="1P8oQ4Nb_b$" role="37wK5m">
                <property role="Xl_RC" value="\t" />
              </node>
              <node concept="Xl_RD" id="1P8oQ4Nb_b_" role="37wK5m">
                <property role="Xl_RC" value="\\t" />
              </node>
            </node>
          </node>
        </node>
      </node>
      <node concept="3Tm1VV" id="1P8oQ4Nb$rh" role="1B3o_S" />
      <node concept="17QB3L" id="1P8oQ4Nb$rO" role="3clF45" />
      <node concept="37vLTG" id="1P8oQ4Nb$sr" role="3clF46">
        <property role="TrG5h" value="raw" />
        <node concept="17QB3L" id="1P8oQ4Nb$sq" role="1tU5fm" />
      </node>
    </node>
    <node concept="3Tm1VV" id="1P8oQ4Nb$qA" role="1B3o_S" />
  </node>
  <node concept="WtQ9Q" id="1P8oQ4NbAcX">
    <ref role="WuzLi" to="k9h7:1P8oQ4NaXFJ" resolve="KeyValuePair" />
    <node concept="11bSqf" id="1P8oQ4NbAcY" role="11c4hB">
      <node concept="3clFbS" id="1P8oQ4NbAcZ" role="2VODD2">
        <node concept="lc7rE" id="1P8oQ4NbAdg" role="3cqZAp">
          <node concept="la8eA" id="1P8oQ4NbAd$" role="lcghm">
            <property role="lacIc" value="&quot;" />
          </node>
          <node concept="l9hG8" id="1P8oQ4NbAep" role="lcghm">
            <node concept="2YIFZM" id="1P8oQ4NbAg8" role="lb14g">
              <ref role="37wK5l" node="1P8oQ4Nb$s0" resolve="escapeJson" />
              <ref role="1Pybhc" node="1P8oQ4Nb$q_" resolve="Escaping" />
              <node concept="2OqwBi" id="1P8oQ4NbAnQ" role="37wK5m">
                <node concept="117lpO" id="1P8oQ4NbAgJ" role="2Oq$k0" />
                <node concept="3TrcHB" id="1P8oQ4NbAyc" role="2OqNvi">
                  <ref role="3TsBF5" to="tpck:h0TrG11" resolve="name" />
                </node>
              </node>
            </node>
          </node>
          <node concept="la8eA" id="1P8oQ4NbA_V" role="lcghm">
            <property role="lacIc" value="&quot;: " />
          </node>
          <node concept="l9hG8" id="1P8oQ4NbAEf" role="lcghm">
            <node concept="2OqwBi" id="1P8oQ4NbAOL" role="lb14g">
              <node concept="117lpO" id="1P8oQ4NbAFH" role="2Oq$k0" />
              <node concept="3TrEf2" id="1P8oQ4NbAZp" role="2OqNvi">
                <ref role="3Tt5mk" to="k9h7:1P8oQ4NaXFM" resolve="value" />
              </node>
            </node>
          </node>
        </node>
      </node>
    </node>
  </node>
  <node concept="WtQ9Q" id="1P8oQ4NbB30">
    <ref role="WuzLi" to="k9h7:1P8oQ4NbA0r" resolve="JsonNull" />
    <node concept="11bSqf" id="1P8oQ4NbB31" role="11c4hB">
      <node concept="3clFbS" id="1P8oQ4NbB32" role="2VODD2">
        <node concept="lc7rE" id="1P8oQ4NbB3j" role="3cqZAp">
          <node concept="la8eA" id="1P8oQ4NbB3B" role="lcghm">
            <property role="lacIc" value="null" />
          </node>
        </node>
      </node>
    </node>
  </node>
</model>

