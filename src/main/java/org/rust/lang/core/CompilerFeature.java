/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.util.ThreeState;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.util.ToolchainUtil;
import org.rust.ide.annotator.RsAnnotationHolder;
import org.rust.ide.fixes.AddFeatureAttributeFix;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.stubs.index.RsFeatureIndex;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.lang.utils.evaluation.CfgEvaluator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class CompilerFeature {
    private final String name;
    private final FeatureState state;
    @JsonSerialize(using = ToStringSerializer.class)
    private final SemVer since;

    public CompilerFeature(String name, FeatureState state, @Nullable SemVer since) {
        this.name = name;
        this.state = state;
        this.since = since;
    }

    @JsonCreator
    public CompilerFeature(String name, FeatureState state, String since) {
        this(name, state, ToolchainUtil.parseSemVer(since));
    }

    public String getName() {
        return name;
    }

    public FeatureState getState() {
        return state;
    }

    @Nullable
    public SemVer getSince() {
        return since;
    }

    public FeatureAvailability availability(PsiElement element) {
        RsElement rsElement = RsPsiJavaUtil.ancestorOrSelf(element, RsElement.class);
        if (rsElement == null) return FeatureAvailability.UNKNOWN;
        var cargoProject = org.rust.lang.core.psi.ext.RsElementExtUtil.getCargoProject(rsElement);
        if (cargoProject == null) return FeatureAvailability.UNKNOWN;
        var rustcInfo = cargoProject.getRustcInfo();
        if (rustcInfo == null) return FeatureAvailability.UNKNOWN;
        var version = rustcInfo.getVersion();
        if (version == null) return FeatureAvailability.UNKNOWN;

        if (since == null || version.getSemver().isGreaterOrEqualThan(since.getMajor(), since.getMinor(), since.getPatch())) {
            if (state == FeatureState.ACCEPTED) return FeatureAvailability.AVAILABLE;
            if (state == FeatureState.REMOVED) return FeatureAvailability.REMOVED;
        }

        ThreeState unstableAvailable = RsDiagnostic.areUnstableFeaturesAvailable(rsElement, version);
        if (unstableAvailable == ThreeState.NO) return FeatureAvailability.NOT_AVAILABLE;
        if (unstableAvailable == ThreeState.UNSURE) return FeatureAvailability.UNKNOWN;

        var crate = Crate.asNotFake(rsElement.getContainingCrate());
        if (crate == null) return FeatureAvailability.UNKNOWN;
        var attrs = RsFeatureIndex.getFeatureAttributes(element.getProject(), name);
        for (var attr : attrs) {
            if (attr.getContainingCrate() != crate) continue;
            var featureAttr = attr.getMetaItem();
            if (!"feature".equals(featureAttr.getName())) continue;
            var metaItemArgs = featureAttr.getMetaItemArgs();
            if (metaItemArgs == null) continue;
            var metaItems = metaItemArgs.getMetaItemList();
            for (var feature : metaItems) {
                if (name.equals(feature.getName())) return FeatureAvailability.AVAILABLE;
            }
        }
        return FeatureAvailability.CAN_BE_ADDED;
    }

    public void check(
        RsAnnotationHolder holder,
        PsiElement element,
        @Nls String presentableFeatureName,
        List<LocalQuickFix> experimentalFixes,
        List<LocalQuickFix> removedFixes
    ) {
        check(
            holder,
            element,
            null,
            RsBundle.message("inspection.message.experimental", presentableFeatureName),
            RsBundle.message("inspection.message.has.been.removed2", presentableFeatureName),
            experimentalFixes,
            removedFixes
        );
    }

    public void check(
        RsAnnotationHolder holder,
        PsiElement element,
        @Nls String presentableFeatureName
    ) {
        check(holder, element, presentableFeatureName, Collections.emptyList(), Collections.emptyList());
    }

    public void check(
        AnnotationHolder holder,
        PsiElement element,
        String presentableFeatureName,
        List<LocalQuickFix> experimentalFixes,
        List<LocalQuickFix> removedFixes
    ) {
        check(
            holder,
            element,
            null,
            RsBundle.message("inspection.message.experimental", presentableFeatureName),
            RsBundle.message("inspection.message.has.been.removed2", presentableFeatureName),
            experimentalFixes,
            removedFixes
        );
    }

    public void check(
        AnnotationHolder holder,
        PsiElement element,
        String presentableFeatureName
    ) {
        check(holder, element, presentableFeatureName, Collections.emptyList(), Collections.emptyList());
    }

    public void check(
        AnnotationHolder holder,
        PsiElement startElement,
        @Nullable PsiElement endElement,
        @InspectionMessage String experimentalMessage,
        @InspectionMessage String removedMessage,
        List<LocalQuickFix> experimentalFixes,
        List<LocalQuickFix> removedFixes
    ) {
        RsDiagnostic diagnostic = getDiagnostic(startElement, endElement, experimentalMessage, removedMessage, experimentalFixes, removedFixes);
        if (diagnostic != null) {
            RsDiagnostic.addToHolder(diagnostic, holder);
        }
    }

    public void check(
        RsAnnotationHolder holder,
        PsiElement startElement,
        @Nullable PsiElement endElement,
        @InspectionMessage String experimentalMessage,
        @InspectionMessage String removedMessage,
        List<LocalQuickFix> experimentalFixes,
        List<LocalQuickFix> removedFixes
    ) {
        RsDiagnostic diagnostic = getDiagnostic(startElement, endElement, experimentalMessage, removedMessage, experimentalFixes, removedFixes);
        if (diagnostic != null) {
            RsDiagnostic.addToHolder(diagnostic, holder);
        }
    }

    public AddFeatureAttributeFix addFeatureFix(PsiElement element) {
        return new AddFeatureAttributeFix(name, element);
    }

    @Nullable
    private RsDiagnostic getDiagnostic(
        PsiElement startElement,
        @Nullable PsiElement endElement,
        @InspectionMessage String experimentalMessage,
        @InspectionMessage String removedMessage,
        List<LocalQuickFix> experimentalFixes,
        List<LocalQuickFix> removedFixes
    ) {
        FeatureAvailability availability = availability(startElement);
        switch (availability) {
            case NOT_AVAILABLE:
                return new RsDiagnostic.ExperimentalFeature(startElement, endElement, experimentalMessage, experimentalFixes);
            case CAN_BE_ADDED: {
                AddFeatureAttributeFix fix = addFeatureFix(startElement);
                List<LocalQuickFix> fixes = new ArrayList<>(experimentalFixes);
                fixes.add(fix);
                return new RsDiagnostic.ExperimentalFeature(startElement, endElement, experimentalMessage, fixes);
            }
            case REMOVED:
                return new RsDiagnostic.RemovedFeature(startElement, endElement, removedMessage, removedFixes);
            default:
                return null;
        }
    }

    // Companion object fields and methods
    private static final String COMPILER_FEATURES_PATH = "compiler-info/compiler-features.json";
    private static final Logger LOG = Logger.getInstance(CompilerFeature.class);

    private static volatile ObjectMapper MAPPER;
    private static ObjectMapper getMapper() {
        if (MAPPER == null) {
            synchronized (CompilerFeature.class) {
                if (MAPPER == null) {
                    MAPPER = new ObjectMapper().registerModule(new KotlinModule.Builder().build());
                }
            }
        }
        return MAPPER;
    }

    private static volatile Map<String, CompilerFeature> knownFeaturesField;
    private static Map<String, CompilerFeature> getKnownFeatures() {
        if (knownFeaturesField == null) {
            synchronized (CompilerFeature.class) {
                if (knownFeaturesField == null) {
                    knownFeaturesField = readFeaturesFromResources();
                }
            }
        }
        return knownFeaturesField;
    }

    private static Map<String, CompilerFeature> readFeaturesFromResources() {
        List<CompilerFeature> features;
        try {
            InputStream stream = CompilerFeature.class.getClassLoader()
                .getResourceAsStream(COMPILER_FEATURES_PATH);
            if (stream == null) {
                LOG.error("Can't find `" + COMPILER_FEATURES_PATH + "` file in resources");
                return Collections.emptyMap();
            }
            try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                features = Arrays.asList(getMapper().readValue(bis, CompilerFeature[].class));
            }
        } catch (IOException e) {
            LOG.error(e);
            features = Collections.emptyList();
        }

        Map<String, CompilerFeature> result = new HashMap<>();
        for (CompilerFeature feature : features) {
            result.put(feature.name, feature);
        }
        return result;
    }

    @Nullable
    public static CompilerFeature find(String featureName) {
        return getKnownFeatures().get(featureName);
    }

    @NotNull
    private static CompilerFeature get(String name) {
        CompilerFeature feature = getKnownFeatures().get(name);
        if (feature == null) {
            throw new NoSuchElementException("No compiler feature with name: " + name);
        }
        return feature;
    }

    public static CompilerFeature getABI_AMDGPU_KERNEL() { return get("abi_amdgpu_kernel"); }
    public static CompilerFeature getABI_AVR_INTERRUPT() { return get("abi_avr_interrupt"); }
    public static CompilerFeature getABI_C_CMSE_NONSECURE_CALL() { return get("abi_c_cmse_nonsecure_call"); }
    public static CompilerFeature getABI_EFIAPI() { return get("abi_efiapi"); }
    public static CompilerFeature getABI_MSP430_INTERRUPT() { return get("abi_msp430_interrupt"); }
    public static CompilerFeature getABI_PTX() { return get("abi_ptx"); }
    public static CompilerFeature getABI_THISCALL() { return get("abi_thiscall"); }
    public static CompilerFeature getABI_UNADJUSTED() { return get("abi_unadjusted"); }
    public static CompilerFeature getABI_VECTORCALL() { return get("abi_vectorcall"); }
    public static CompilerFeature getABI_X86_INTERRUPT() { return get("abi_x86_interrupt"); }
    public static CompilerFeature getADT_CONST_PARAMS() { return get("adt_const_params"); }
    public static CompilerFeature getARBITRARY_ENUM_DISCRIMINANT() { return get("arbitrary_enum_discriminant"); }
    public static CompilerFeature getASSOCIATED_TYPE_DEFAULTS() { return get("associated_type_defaults"); }
    public static CompilerFeature getBOX_PATTERNS() { return get("box_patterns"); }
    public static CompilerFeature getBOX_SYNTAX() { return get("box_syntax"); }
    public static CompilerFeature getCONST_FN_TRAIT_BOUND() { return get("const_fn_trait_bound"); }
    public static CompilerFeature getCONST_GENERICS_DEFAULTS() { return get("const_generics_defaults"); }
    public static CompilerFeature getCONST_TRAIT_IMPL() { return get("const_trait_impl"); }
    public static CompilerFeature getCRATE_IN_PATHS() { return get("crate_in_paths"); }
    public static CompilerFeature getC_UNWIND() { return get("c_unwind"); }
    public static CompilerFeature getC_VARIADIC() { return get("c_variadic"); }
    public static CompilerFeature getDECL_MACRO() { return get("decl_macro"); }
    public static CompilerFeature getEXCLUSIVE_RANGE_PATTERN() { return get("exclusive_range_pattern"); }
    public static CompilerFeature getEXTERN_CRATE_SELF() { return get("extern_crate_self"); }
    public static CompilerFeature getEXTERN_TYPES() { return get("extern_types"); }
    public static CompilerFeature getFORMAT_ARGS_CAPTURE() { return get("format_args_capture"); }
    public static CompilerFeature getGENERATORS() { return get("generators"); }
    public static CompilerFeature getGENERIC_ASSOCIATED_TYPES() { return get("generic_associated_types"); }
    public static CompilerFeature getIF_LET_GUARD() { return get("if_let_guard"); }
    public static CompilerFeature getIF_WHILE_OR_PATTERNS() { return get("if_while_or_patterns"); }
    public static CompilerFeature getINHERENT_ASSOCIATED_TYPES() { return get("inherent_associated_types"); }
    public static CompilerFeature getINLINE_CONST() { return get("inline_const"); }
    public static CompilerFeature getINLINE_CONST_PAT() { return get("inline_const_pat"); }
    public static CompilerFeature getINTRINSICS() { return get("intrinsics"); }
    public static CompilerFeature getIRREFUTABLE_LET_PATTERNS() { return get("irrefutable_let_patterns"); }
    public static CompilerFeature getLABEL_BREAK_VALUE() { return get("label_break_value"); }
    public static CompilerFeature getLET_CHAINS() { return get("let_chains"); }
    public static CompilerFeature getLET_ELSE() { return get("let_else"); }
    public static CompilerFeature getMIN_CONST_GENERICS() { return get("min_const_generics"); }
    public static CompilerFeature getNON_MODRS_MODS() { return get("non_modrs_mods"); }
    public static CompilerFeature getOR_PATTERNS() { return get("or_patterns"); }
    public static CompilerFeature getPARAM_ATTRS() { return get("param_attrs"); }
    public static CompilerFeature getPLATFORM_INTRINSICS() { return get("platform_intrinsics"); }
    public static CompilerFeature getRAW_REF_OP() { return get("raw_ref_op"); }
    public static CompilerFeature getRETURN_POSITION_IMPL_TRAIT_IN_TRAIT() { return get("return_position_impl_trait_in_trait"); }
    public static CompilerFeature getSLICE_PATTERNS() { return get("slice_patterns"); }
    public static CompilerFeature getSTART() { return get("start"); }
    public static CompilerFeature getUNBOXED_CLOSURES() { return get("unboxed_closures"); }
    public static CompilerFeature getWASM_ABI() { return get("wasm_abi"); }
    public static CompilerFeature getHALF_OPEN_RANGE_PATTERNS() { return get("half_open_range_patterns"); }
    public static CompilerFeature getCONST_CLOSURES() { return get("const_closures"); }
    public static CompilerFeature getC_STR_LITERAL() { return get("c_str_literals"); }
}
