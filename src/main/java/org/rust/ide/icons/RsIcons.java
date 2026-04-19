/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.IconUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.RGBImageFilter;

/**
 * Icons that are used by various plugin components.
 *
 * The order of properties matters in this class. When conflating an icon from simple elements,
 * make sure that all those elements are declared above to the icon.
 */
public final class RsIcons {
    private RsIcons() {
    }

    // Logos
    public static final Icon RUST = load("/icons/rust.svg");

    // File types
    public static final Icon RUST_FILE = load("/icons/rustFile.svg");
    public static final Icon MAIN_RS = load("/icons/rustMain.svg");
    public static final Icon MOD_RS = load("/icons/rustMod.svg");

    // Marks
    public static final Icon FINAL_MARK = AllIcons.Nodes.FinalMark;
    public static final Icon STATIC_MARK = AllIcons.Nodes.StaticMark;
    public static final Icon TEST_MARK = AllIcons.Nodes.JunitTestMark;
    public static final Icon DOCS_MARK = load("/icons/rustDocs.svg");
    public static final Icon FEATURE_CHECKED_MARK = AllIcons.Diff.GutterCheckBoxSelected;
    public static final Icon FEATURE_UNCHECKED_MARK = AllIcons.Diff.GutterCheckBox;
    public static final Icon FEATURE_CHECKED_MARK_GRAYED = grayed(FEATURE_CHECKED_MARK);
    public static final Icon FEATURE_UNCHECKED_MARK_GRAYED = grayed(FEATURE_UNCHECKED_MARK);
    public static final Icon FEATURES_SETTINGS = AllIcons.General.Settings;

    // Source code elements
    public static final Icon CRATE = AllIcons.Nodes.PpLib;
    public static final Icon MODULE = load("/icons/nodes/module.svg");

    public static final Icon TRAIT = load("/icons/nodes/trait.svg");
    public static final Icon STRUCT = load("/icons/nodes/struct.svg");
    public static final Icon UNION = load("/icons/nodes/union.svg");
    public static final Icon ENUM = load("/icons/nodes/enum.svg");
    public static final Icon TYPE_ALIAS = load("/icons/nodes/typeAlias.svg");
    public static final Icon IMPL = load("/icons/nodes/impl.svg");
    public static final Icon FUNCTION = load("/icons/nodes/function.svg");
    public static final Icon MACRO = load("/icons/nodes/macro.svg");
    public static final Icon MACRO2 = load("/icons/nodes/macro2.svg");
    public static final Icon PROC_MACRO = load("/icons/nodes/macroP.svg");

    public static final Icon CONSTANT = load("/icons/nodes/constant.svg");
    public static final Icon MUT_STATIC = load("/icons/nodes/static.svg");
    public static final Icon STATIC = addFinalMark(MUT_STATIC);

    public static final Icon METHOD = load("/icons/nodes/method.svg");
    public static final Icon ASSOC_FUNCTION = addStaticMark(FUNCTION);
    public static final Icon ASSOC_CONSTANT = addStaticMark(CONSTANT);
    public static final Icon ASSOC_TYPE_ALIAS = addStaticMark(TYPE_ALIAS);

    public static final Icon ABSTRACT_METHOD = load("/icons/nodes/abstractMethod.svg");
    public static final Icon ABSTRACT_ASSOC_FUNCTION = addStaticMark(load("/icons/nodes/abstractFunction.svg"));
    public static final Icon ABSTRACT_ASSOC_CONSTANT = addStaticMark(load("/icons/nodes/abstractConstant.svg"));
    public static final Icon ABSTRACT_ASSOC_TYPE_ALIAS = addStaticMark(load("/icons/nodes/abstractTypeAlias.svg"));

    public static final Icon ATTRIBUTE = AllIcons.Nodes.Annotationtype;
    public static final Icon MUT_ARGUMENT = AllIcons.Nodes.Parameter;
    public static final Icon ARGUMENT = addFinalMark(MUT_ARGUMENT);
    public static final Icon MUT_BINDING = AllIcons.Nodes.Variable;
    public static final Icon BINDING = addFinalMark(MUT_BINDING);

    public static final Icon FIELD = load("/icons/nodes/field.svg");
    public static final Icon ENUM_VARIANT = load("/icons/nodes/enumVariant.svg");

    // Structure view
    public static final Icon MACRO_EXPANSION = AllIcons.Nodes.ErrorIntroduction;
    public static final Icon VISIBILITY_SORT = AllIcons.ObjectBrowser.VisibilitySort;

    // Gutter
    public static final Icon IMPLEMENTED = AllIcons.Gutter.ImplementedMethod;
    public static final Icon IMPLEMENTING_METHOD = AllIcons.Gutter.ImplementingMethod;
    public static final Icon OVERRIDING_METHOD = AllIcons.Gutter.OverridingMethod;
    public static final Icon RECURSIVE_CALL = AllIcons.Gutter.RecursiveMethod;

    // Repl
    public static final Icon REPL = load("/icons/rustRepl.svg");

    public static final Icon CARGO_GENERATE = load("/icons/cargoGenerate.svg");
    public static final Icon WASM_PACK = load("/icons/wasmPack.svg");

    // Progress
    public static final Icon GEAR = load("/icons/gear.svg");
    public static final Icon GEAR_OFF = load("/icons/gearOff.svg");
    public static final Icon GEAR_ANIMATED = new AnimatedIcon(AnimatedIcon.Default.DELAY, GEAR, rotated(GEAR, 15.0), rotated(GEAR, 30.0), rotated(GEAR, 45.0));

    private static Icon load(String path) {
        return IconLoader.getIcon(path, RsIcons.class);
    }

    public static Icon addFinalMark(Icon icon) {
        return new LayeredIcon(icon, FINAL_MARK);
    }

    public static Icon addStaticMark(Icon icon) {
        return new LayeredIcon(icon, STATIC_MARK);
    }

    public static Icon addTestMark(Icon icon) {
        return new LayeredIcon(icon, TEST_MARK);
    }

    public static Icon multiple(Icon icon) {
        LayeredIcon compoundIcon = new LayeredIcon(2);
        compoundIcon.setIcon(icon, 0, 2 * icon.getIconWidth() / 5, 0);
        compoundIcon.setIcon(icon, 1, 0, 0);
        return compoundIcon;
    }

    public static Icon grayed(Icon icon) {
        return IconUtil.filterIcon(icon, () -> new RGBImageFilter() {
            @Override
            public int filterRGB(int x, int y, int rgb) {
                Color color = new Color(rgb, true);
                return ColorUtil.toAlpha(color, (int) (color.getAlpha() / 2.2)).getRGB();
            }
        }, null);
    }

    /**
     * Rotates the icon by the given angle, in degrees.
     *
     * <b>Important</b>: Do <i>not</i> rotate the icon by +/-90 degrees (or any sufficiently close amount)!
     * The implementation of rotation by that amount in AWT is broken, and results in erratic shifts for composed
     * transformations. In other words, the (final) transformation matrix as a function of rotation angle
     * is discontinuous at those points.
     */
    public static Icon rotated(Icon original, double angle) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                try {
                    g2d.translate((double) x, (double) y);
                    g2d.rotate(Math.toRadians(angle), original.getIconWidth() / 2.0, original.getIconHeight() / 2.0);
                    original.paintIcon(c, g2d, 0, 0);
                } finally {
                    g2d.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return original.getIconWidth();
            }

            @Override
            public int getIconHeight() {
                return original.getIconHeight();
            }
        };
    }
}
