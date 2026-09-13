/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ThreeState;
import consulo.util.lang.SemVer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.toolchain.RustChannel;
import org.rust.cargo.api.toolchain.RustcVersion;
import org.rust.cargo.api.util.ToolchainUtil;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.stubs.index.RsFeatureIndex;
import org.rust.lang.utils.RsFeatureDiagnostic;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.rust.lang.core.psi.ext.impl.RsElementExtUtil;

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
        var cargoProject = org.rust.lang.core.psi.ext.impl.RsElementExtUtil.getCargoProject(rsElement);
        if (cargoProject == null) return FeatureAvailability.UNKNOWN;
        var rustcInfo = cargoProject.getRustcInfo();
        if (rustcInfo == null) return FeatureAvailability.UNKNOWN;
        var version = rustcInfo.getVersion();
        if (version == null) return FeatureAvailability.UNKNOWN;

        if (since == null || version.getSemver().isGreaterOrEqualThan(since.getMajor(), since.getMinor(), since.getPatch())) {
            if (state == FeatureState.ACCEPTED) return FeatureAvailability.AVAILABLE;
            if (state == FeatureState.REMOVED) return FeatureAvailability.REMOVED;
        }

        ThreeState unstableAvailable = areUnstableFeaturesAvailable(rsElement, version);
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

    /**
     * Returns the problem with using this feature at {@code startElement}, or {@code null} when the
     * feature is available there.
     *
     * @param endElement          last element of the reported range, or {@code null} to report {@code startElement} alone
     * @param experimentalMessage message used when the feature is not stable yet
     * @param removedMessage      message used when the feature no longer exists
     */
    @Nullable
    public RsFeatureDiagnostic checkAvailability(
        @Nonnull PsiElement startElement,
        @Nullable PsiElement endElement,
        @Nonnull String experimentalMessage,
        @Nonnull String removedMessage
    ) {
        FeatureAvailability availability = availability(startElement);
        switch (availability) {
            case NOT_AVAILABLE:
                return new RsFeatureDiagnostic(startElement, endElement, name, experimentalMessage,
                    RsFeatureDiagnostic.Kind.EXPERIMENTAL, false);
            case CAN_BE_ADDED:
                return new RsFeatureDiagnostic(startElement, endElement, name, experimentalMessage,
                    RsFeatureDiagnostic.Kind.EXPERIMENTAL, true);
            case REMOVED:
                return new RsFeatureDiagnostic(startElement, endElement, name, removedMessage,
                    RsFeatureDiagnostic.Kind.REMOVED, false);
            default:
                return null;
        }
    }

    /**
     * Declares this feature in the crate root of {@code context}.
     */
    public void addFeatureAttribute(@Nonnull Project project, @Nonnull PsiElement context) {
        addFeatureAttribute(project, context, name);
    }

    /**
     * Adds a {@code #![feature(featureName)]} inner attribute to the crate root of {@code context},
     * after the last feature attribute already declared there.
     */
    public static void addFeatureAttribute(@Nonnull Project project, @Nonnull PsiElement context, @Nonnull String featureName) {
        RsElement rsElement = RsPsiJavaUtil.ancestorOrSelf(context, RsElement.class);
        if (rsElement == null) {
            return;
        }
        RsMod mod = rsElement.getCrateRoot();
        if (mod == null) {
            return;
        }

        List<RsInnerAttr> attrs = RsPsiJavaUtil.childrenOfType(mod, RsInnerAttr.class);
        RsInnerAttr lastFeatureAttribute = null;
        for (RsInnerAttr attr : attrs) {
            if ("feature".equals(attr.getMetaItem().getName())) {
                lastFeatureAttribute = attr;
            }
        }

        RsPsiFactory psiFactory = new RsPsiFactory(project);
        RsInnerAttr attr = psiFactory.createInnerAttr("feature(" + featureName + ")");
        if (lastFeatureAttribute != null) {
            mod.addAfter(attr, lastFeatureAttribute);
        }
        else {
            PsiElement insertedElement = mod.addBefore(attr, mod.getFirstChild());
            mod.addAfter(psiFactory.createNewline(), insertedElement);
        }
    }

    /**
     * Whether unstable features may be used in the crate owning {@code element}: only a nightly
     * toolchain accepts them, the standard library and its dependencies aside.
     */
    @Nonnull
    public static ThreeState areUnstableFeaturesAvailable(@Nonnull RsElement element, @Nonnull RustcVersion version) {
        Crate crate = RsElementUtil.getContainingCrate(element);
        PackageOrigin origin = crate.getOrigin();
        boolean isStdlibPart = origin == PackageOrigin.STDLIB || origin == PackageOrigin.STDLIB_DEPENDENCY;
        return (version.getChannel() != RustChannel.NIGHTLY && !isStdlibPart) ? ThreeState.NO : ThreeState.YES;
    }

    /**
     * Calling conventions accepted in {@code extern "..."}, each mapped to the compiler feature that
     * has to be enabled to use it, or to {@code null} when the convention is stable.
     */
    @Nonnull
    public static Map<String, CompilerFeature> getSupportedCallingConventions() {
        return CallingConventions.MAP;
    }

    private static final class CallingConventions {
        private static final Map<String, CompilerFeature> MAP;

        static {
            Map<String, CompilerFeature> map = new LinkedHashMap<>();
            map.put("Rust", null);
            map.put("C", null);
            map.put("C-unwind", getC_UNWIND());
            map.put("cdecl", null);
            map.put("stdcall", null);
            map.put("stdcall-unwind", getC_UNWIND());
            map.put("fastcall", null);
            map.put("vectorcall", getABI_VECTORCALL());
            map.put("thiscall", getABI_THISCALL());
            map.put("thiscall-unwind", getC_UNWIND());
            map.put("aapcs", null);
            map.put("win64", null);
            map.put("sysv64", null);
            map.put("ptx-kernel", getABI_PTX());
            map.put("msp430-interrupt", getABI_MSP430_INTERRUPT());
            map.put("x86-interrupt", getABI_X86_INTERRUPT());
            map.put("amdgpu-kernel", getABI_AMDGPU_KERNEL());
            map.put("efiapi", getABI_EFIAPI());
            map.put("avr-interrupt", getABI_AVR_INTERRUPT());
            map.put("avr-non-blocking-interrupt", getABI_AVR_INTERRUPT());
            map.put("C-cmse-nonsecure-call", getABI_C_CMSE_NONSECURE_CALL());
            map.put("wasm", getWASM_ABI());
            map.put("system", null);
            map.put("system-unwind", getC_UNWIND());
            map.put("rust-intrinsic", getINTRINSICS());
            map.put("rust-call", getUNBOXED_CLOSURES());
            map.put("platform-intrinsic", getPLATFORM_INTRINSICS());
            map.put("unadjusted", getABI_UNADJUSTED());
            MAP = Collections.unmodifiableMap(map);
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
                    MAPPER = new ObjectMapper().registerModule(new ParameterNamesModule());
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

    @Nonnull
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
