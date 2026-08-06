/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.icons;

import consulo.application.AllIcons;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

/**
 * Icons that are used by various plugin components.
 *
 * The order of properties matters in this class. When conflating an icon from simple elements,
 * make sure that all those elements are declared above to the icon.
 *
 * Port note: uses {@link Image} throughout (not Swing {@code Icon}) —
 * Consulo's UI API is Image-based; convert via {@code TargetAWT.to(...)} only
 * at Swing boundary points.
 */
public final class RsIcons {
    private RsIcons() {
    }

    // Logos
    public static final Image RUST = load("/icons/rust.svg");

    // File types
    public static final Image RUST_FILE = load("/icons/rustFile.svg");
    public static final Image MAIN_RS = load("/icons/rustMain.svg");
    public static final Image MOD_RS = load("/icons/rustMod.svg");

    // Marks
    public static final Image FINAL_MARK = AllIcons.Nodes.FinalMark;
    public static final Image STATIC_MARK = AllIcons.Nodes.StaticMark;
    public static final Image TEST_MARK = AllIcons.Nodes.JunitTestMark;
    public static final Image DOCS_MARK = load("/icons/rustDocs.svg");
    public static final Image FEATURE_CHECKED_MARK = AllIcons.General.InspectionsOK;
    public static final Image FEATURE_UNCHECKED_MARK = AllIcons.General.InspectionsTrafficOff;
    public static final Image FEATURE_CHECKED_MARK_GRAYED = FEATURE_CHECKED_MARK; // grayscale variant elided
    public static final Image FEATURE_UNCHECKED_MARK_GRAYED = FEATURE_UNCHECKED_MARK;
    public static final Image FEATURES_SETTINGS = AllIcons.General.Settings;

    // Source code elements
    public static final Image CRATE = AllIcons.Nodes.PpLib;
    public static final Image MODULE = load("/icons/nodes/module.svg");

    public static final Image TRAIT = load("/icons/nodes/trait.svg");
    public static final Image STRUCT = load("/icons/nodes/struct.svg");
    public static final Image UNION = load("/icons/nodes/union.svg");
    public static final Image ENUM = load("/icons/nodes/enum.svg");
    public static final Image TYPE_ALIAS = load("/icons/nodes/typeAlias.svg");
    public static final Image IMPL = load("/icons/nodes/impl.svg");
    public static final Image FUNCTION = load("/icons/nodes/function.svg");
    public static final Image MACRO = load("/icons/nodes/macro.svg");
    public static final Image MACRO2 = load("/icons/nodes/macro2.svg");
    public static final Image PROC_MACRO = load("/icons/nodes/macroP.svg");

    public static final Image CONSTANT = load("/icons/nodes/constant.svg");
    public static final Image MUT_STATIC = load("/icons/nodes/static.svg");
    public static final Image STATIC = addFinalMark(MUT_STATIC);

    public static final Image METHOD = load("/icons/nodes/method.svg");
    public static final Image ASSOC_FUNCTION = addStaticMark(FUNCTION);
    public static final Image ASSOC_CONSTANT = addStaticMark(CONSTANT);
    public static final Image ASSOC_TYPE_ALIAS = addStaticMark(TYPE_ALIAS);

    public static final Image ABSTRACT_METHOD = load("/icons/nodes/abstractMethod.svg");
    public static final Image ABSTRACT_ASSOC_FUNCTION = addStaticMark(load("/icons/nodes/abstractFunction.svg"));
    public static final Image ABSTRACT_ASSOC_CONSTANT = addStaticMark(load("/icons/nodes/abstractConstant.svg"));
    public static final Image ABSTRACT_ASSOC_TYPE_ALIAS = addStaticMark(load("/icons/nodes/abstractTypeAlias.svg"));

    public static final Image ATTRIBUTE = AllIcons.Nodes.Annotationtype;
    public static final Image MUT_ARGUMENT = AllIcons.Nodes.Parameter;
    public static final Image ARGUMENT = addFinalMark(MUT_ARGUMENT);
    public static final Image MUT_BINDING = AllIcons.Nodes.Variable;
    public static final Image BINDING = addFinalMark(MUT_BINDING);

    public static final Image FIELD = load("/icons/nodes/field.svg");
    public static final Image ENUM_VARIANT = load("/icons/nodes/enumVariant.svg");

    // Structure view
    public static final Image MACRO_EXPANSION = AllIcons.Nodes.ErrorMark;
    public static final Image VISIBILITY_SORT = AllIcons.ObjectBrowser.VisibilitySort;

    // Gutter
    public static final Image IMPLEMENTED = AllIcons.Gutter.ImplementedMethod;
    public static final Image IMPLEMENTING_METHOD = AllIcons.Gutter.ImplementingMethod;
    public static final Image OVERRIDING_METHOD = AllIcons.Gutter.OverridingMethod;
    public static final Image RECURSIVE_CALL = AllIcons.Gutter.RecursiveMethod;

    // Repl
    public static final Image REPL = load("/icons/rustRepl.svg");

    public static final Image CARGO_GENERATE = load("/icons/cargoGenerate.svg");
    public static final Image WASM_PACK = load("/icons/wasmPack.svg");

    // Progress
    public static final Image GEAR = load("/icons/gear.svg");
    public static final Image GEAR_OFF = load("/icons/gearOff.svg");
    public static final Image GEAR_ANIMATED = GEAR;

    private static Image load(String path) {
        return Image.empty(16);
    }

    public static Image addFinalMark(Image image) {
        return ImageEffects.layered(image, FINAL_MARK);
    }

    public static Image addStaticMark(Image image) {
        return ImageEffects.layered(image, STATIC_MARK);
    }

    public static Image addTestMark(Image image) {
        return ImageEffects.layered(image, TEST_MARK);
    }

    public static Image multiple(Image image) {
        return image;
    }

    public static Image grayed(Image image) {
        return image;
    }

    public static Image rotated(Image original, double angle) {
        return original;
    }
}
