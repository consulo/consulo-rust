/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.icons;

import consulo.application.AllIcons;
import consulo.rust.icon.RustIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

/**
 * @deprecated use {@link RustIconGroup} directly.
 */
@Deprecated
public final class RsIcons {
    private RsIcons() {
    }

    public static final Image RUST = RustIconGroup.rust();

    public static final Image RUST_FILE = RustIconGroup.rustfile();

    public static final Image FINAL_MARK = AllIcons.Nodes.FinalMark;
    public static final Image STATIC_MARK = AllIcons.Nodes.StaticMark;
    public static final Image TEST_MARK = AllIcons.Nodes.JunitTestMark;
    public static final Image DOCS_MARK = RustIconGroup.rustdocs();
    public static final Image FEATURE_CHECKED_MARK = AllIcons.General.InspectionsOK;
    public static final Image FEATURE_UNCHECKED_MARK = AllIcons.General.InspectionsTrafficOff;
    public static final Image FEATURE_CHECKED_MARK_GRAYED = FEATURE_CHECKED_MARK;
    public static final Image FEATURE_UNCHECKED_MARK_GRAYED = FEATURE_UNCHECKED_MARK;
    public static final Image FEATURES_SETTINGS = AllIcons.General.Settings;

    public static final Image CRATE = AllIcons.Nodes.PpLib;
    public static final Image MODULE = RustIconGroup.nodesModule();

    public static final Image TRAIT = RustIconGroup.nodesTrait();
    public static final Image STRUCT = RustIconGroup.nodesStruct();
    public static final Image UNION = RustIconGroup.nodesUnion();
    public static final Image ENUM = RustIconGroup.nodesEnum();
    public static final Image TYPE_ALIAS = RustIconGroup.nodesTypealias();
    public static final Image IMPL = RustIconGroup.nodesImpl();
    public static final Image FUNCTION = RustIconGroup.nodesFunction();
    public static final Image MACRO = RustIconGroup.nodesMacro();
    public static final Image MACRO2 = RustIconGroup.nodesMacro2();
    public static final Image PROC_MACRO = RustIconGroup.nodesMacrop();

    public static final Image CONSTANT = RustIconGroup.nodesConstant();
    public static final Image MUT_STATIC = RustIconGroup.nodesStatic();
    public static final Image STATIC = addFinalMark(MUT_STATIC);

    public static final Image METHOD = RustIconGroup.nodesMethod();
    public static final Image ASSOC_FUNCTION = addStaticMark(FUNCTION);
    public static final Image ASSOC_CONSTANT = addStaticMark(CONSTANT);
    public static final Image ASSOC_TYPE_ALIAS = addStaticMark(TYPE_ALIAS);

    public static final Image ABSTRACT_METHOD = RustIconGroup.nodesAbstractmethod();
    public static final Image ABSTRACT_ASSOC_FUNCTION = addStaticMark(RustIconGroup.nodesAbstractfunction());
    public static final Image ABSTRACT_ASSOC_CONSTANT = addStaticMark(RustIconGroup.nodesAbstractconstant());
    public static final Image ABSTRACT_ASSOC_TYPE_ALIAS = addStaticMark(RustIconGroup.nodesAbstracttypealias());

    public static final Image ATTRIBUTE = AllIcons.Nodes.Annotationtype;
    public static final Image MUT_ARGUMENT = AllIcons.Nodes.Parameter;
    public static final Image ARGUMENT = addFinalMark(MUT_ARGUMENT);
    public static final Image MUT_BINDING = AllIcons.Nodes.Variable;
    public static final Image BINDING = addFinalMark(MUT_BINDING);

    public static final Image FIELD = RustIconGroup.nodesField();
    public static final Image ENUM_VARIANT = RustIconGroup.nodesEnumvariant();

    public static final Image MACRO_EXPANSION = AllIcons.Nodes.ErrorMark;
    public static final Image VISIBILITY_SORT = AllIcons.ObjectBrowser.VisibilitySort;

    public static final Image IMPLEMENTED = AllIcons.Gutter.ImplementedMethod;
    public static final Image IMPLEMENTING_METHOD = AllIcons.Gutter.ImplementingMethod;
    public static final Image OVERRIDING_METHOD = AllIcons.Gutter.OverridingMethod;
    public static final Image RECURSIVE_CALL = AllIcons.Gutter.RecursiveMethod;

    public static final Image REPL = RustIconGroup.rustrepl();

    public static final Image CARGO_GENERATE = RustIconGroup.cargogenerate();
    public static final Image WASM_PACK = RustIconGroup.wasmpack();

    public static final Image GEAR = RustIconGroup.gear();
    public static final Image GEAR_OFF = RustIconGroup.gearoff();
    public static final Image GEAR_ANIMATED = GEAR;

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
