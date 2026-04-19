/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.settings;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.SmartIndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.*;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;

public class RsLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

    @NotNull
    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }

    @Override
    public CustomCodeStyleSettings createCustomSettings(@NotNull CodeStyleSettings settings) {
        return new RsCodeStyleSettings(settings);
    }

    @NotNull
    @Override
    public CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings baseSettings,
                                                    @NotNull CodeStyleSettings modelSettings) {
        return new CodeStyleAbstractConfigurable(baseSettings, modelSettings, getConfigurableDisplayName()) {
            @Override
            protected CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings settings) {
                return new RsCodeStyleMainPanel(getCurrentSettings(), settings);
            }
        };
    }

    @Override
    public String getConfigurableDisplayName() {
        return RsBundle.message("settings.rust.code.style.name");
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        switch (settingsType) {
            case INDENT_SETTINGS:
                return INDENT_SAMPLE;
            case SPACING_SETTINGS:
                return SPACING_SAMPLE;
            case WRAPPING_AND_BRACES_SETTINGS:
                return WRAPPING_AND_BRACES_SAMPLE;
            case BLANK_LINES_SETTINGS:
                return BLANK_LINES_SAMPLE;
            default:
                return "";
        }
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        switch (settingsType) {
            case BLANK_LINES_SETTINGS: {
                consumer.showStandardOptions(
                    CodeStyleSettingsCustomizable.BlankLinesOption.KEEP_BLANK_LINES_IN_DECLARATIONS.name(),
                    CodeStyleSettingsCustomizable.BlankLinesOption.KEEP_BLANK_LINES_IN_CODE.name());

                consumer.showCustomOption(RsCodeStyleSettings.class,
                    "MIN_NUMBER_OF_BLANKS_BETWEEN_ITEMS",
                    RsBundle.message("settings.rust.code.style.between.declarations"),
                    CodeStyleSettingsCustomizableOptions.getInstance().BLANK_LINES);
                break;
            }

            case SPACING_SETTINGS: {
                consumer.showCustomOption(RsCodeStyleSettings.class,
                    "SPACE_AROUND_ASSOC_TYPE_BINDING",
                    RsBundle.message("settings.rust.code.style.around.associated.type.bindings"),
                    CodeStyleSettingsCustomizableOptions.getInstance().SPACES_IN_TYPE_PARAMETERS);
                break;
            }

            case WRAPPING_AND_BRACES_SETTINGS: {
                consumer.showStandardOptions(
                    CodeStyleSettingsCustomizable.WrappingOrBraceOption.KEEP_LINE_BREAKS.name(),
                    CodeStyleSettingsCustomizable.WrappingOrBraceOption.RIGHT_MARGIN.name(),
                    CodeStyleSettingsCustomizable.WrappingOrBraceOption.ALIGN_MULTILINE_CHAINED_METHODS.name(),
                    CodeStyleSettingsCustomizable.WrappingOrBraceOption.ALIGN_MULTILINE_PARAMETERS.name(),
                    CodeStyleSettingsCustomizable.WrappingOrBraceOption.ALIGN_MULTILINE_PARAMETERS_IN_CALLS.name());

                consumer.showCustomOption(RsCodeStyleSettings.class,
                    "ALLOW_ONE_LINE_MATCH",
                    RsBundle.message("settings.rust.code.style.match.expressions.in.one.line"),
                    CodeStyleSettingsCustomizableOptions.getInstance().WRAPPING_KEEP);

                consumer.showCustomOption(RsCodeStyleSettings.class,
                    "PRESERVE_PUNCTUATION",
                    RsBundle.message("settings.rust.code.style.punctuation"),
                    CodeStyleSettingsCustomizableOptions.getInstance().WRAPPING_KEEP);

                consumer.showCustomOption(RsCodeStyleSettings.class,
                    "ALIGN_RET_TYPE",
                    RsBundle.message("settings.rust.code.style.align.return.type"),
                    CodeStyleSettingsCustomizableOptions.getInstance().WRAPPING_METHOD_PARAMETERS);

                consumer.showCustomOption(RsCodeStyleSettings.class,
                    "ALIGN_WHERE_CLAUSE",
                    RsBundle.message("settings.rust.code.style.align.where.clause"),
                    CodeStyleSettingsCustomizableOptions.getInstance().WRAPPING_METHOD_PARAMETERS);

                consumer.showCustomOption(RsCodeStyleSettings.class,
                    "ALIGN_TYPE_PARAMS",
                    ApplicationBundle.message("wrapping.align.when.multiline"),
                    CodeStyleSettingsCustomizableOptions.getInstance().SPACES_IN_TYPE_PARAMETERS);

                consumer.showCustomOption(RsCodeStyleSettings.class,
                    "INDENT_WHERE_CLAUSE",
                    RsBundle.message("settings.rust.code.style.indent.where.clause"),
                    CodeStyleSettingsCustomizableOptions.getInstance().SPACES_IN_TYPE_PARAMETERS);

                consumer.showCustomOption(RsCodeStyleSettings.class,
                    "ALIGN_WHERE_BOUNDS",
                    RsBundle.message("settings.rust.code.style.align.where.clause.bounds"),
                    CodeStyleSettingsCustomizableOptions.getInstance().SPACES_IN_TYPE_PARAMETERS);
                break;
            }

            case COMMENTER_SETTINGS: {
                consumer.showStandardOptions(
                    CodeStyleSettingsCustomizable.CommenterOption.LINE_COMMENT_AT_FIRST_COLUMN.name(),
                    CodeStyleSettingsCustomizable.CommenterOption.LINE_COMMENT_ADD_SPACE.name(),
                    CodeStyleSettingsCustomizable.CommenterOption.BLOCK_COMMENT_AT_FIRST_COLUMN.name());
                break;
            }

            default:
                break;
        }
    }

    @Override
    public IndentOptionsEditor getIndentOptionsEditor() {
        return new SmartIndentOptionsEditor();
    }

    @Override
    public void customizeDefaults(@NotNull CommonCodeStyleSettings commonSettings,
                                  @NotNull CommonCodeStyleSettings.IndentOptions indentOptions) {
        commonSettings.RIGHT_MARGIN = 100;
        commonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = true;

        // Make default behavior consistent with rustfmt
        commonSettings.LINE_COMMENT_AT_FIRST_COLUMN = false;
        commonSettings.LINE_COMMENT_ADD_SPACE = true;
        commonSettings.BLOCK_COMMENT_AT_FIRST_COLUMN = false;

        // FIXME(mkaput): It's a hack
        // Nobody else does this and still somehow achieve similar effect
        indentOptions.CONTINUATION_INDENT_SIZE = indentOptions.INDENT_SIZE;
    }

    private static String sample(String code) {
        return code.trim();
    }

    private static final String INDENT_SAMPLE = sample(
        "struct Vector {\n" +
        "    x: f64,\n" +
        "    y: f64,\n" +
        "    z: f64\n" +
        "}\n" +
        "\n" +
        "impl Vector {\n" +
        "    fn add(&self, other: &Vector) -> Vector {\n" +
        "        Vector {\n" +
        "            x: self.x + other.x,\n" +
        "            y: self.y + other.y,\n" +
        "            z: self.z + other.z,\n" +
        "        }\n" +
        "    }\n" +
        "}"
    );

    private static final String SPACING_SAMPLE = sample(
        "trait Trait0<A, B, T: Trait1<A>> {\n" +
        "    type Output;\n" +
        "}\n" +
        "\n" +
        "trait Trait1<T> {}\n" +
        "\n" +
        "fn method<A, B, T, C>(value: T) where T: Trait0<A, B, T, Output=C> {}"
    );

    private static final String WRAPPING_AND_BRACES_SAMPLE = sample(
        "fn concat<X, Y, I>(xs: X,\n" +
        "                   ys: Y)\n" +
        "                   -> Box<Iterator<Item=I>>\n" +
        "    where X: Iterator<Item=I>,\n" +
        "          Y: Iterator<Item=I>\n" +
        "{\n" +
        "    unimplemented!()\n" +
        "}\n" +
        "\n" +
        "\n" +
        "fn main() {\n" +
        "    let xs = vec![1, 2, 3].into_iter()\n" +
        "        .map(|x| x * 2)\n" +
        "        .filter(|x| x > 2);\n" +
        "\n" +
        "    let ys = vec![1,\n" +
        "                  2,\n" +
        "                  3].into_iter();\n" +
        "\n" +
        "    let zs = concat(xs,\n" +
        "                    ys);\n" +
        "\n" +
        "    let is_even = match zs.next { Some(x) => x % 2 == 0, None => false, };\n" +
        "\n" +
        "    match is_even {\n" +
        "        true => {\n" +
        "            // comment\n" +
        "        },\n" +
        "        _ => println(\"false\"),\n" +
        "    }\n" +
        "    return\n" +
        "}"
    );

    private static final String BLANK_LINES_SAMPLE = sample(
        "#![allow(dead_code)]\n" +
        "\n" +
        "\n" +
        "\n" +
        "use std::cmp::{max, min};\n" +
        "\n" +
        "\n" +
        "\n" +
        "struct Rectangle {\n" +
        "    p1: (i32, i32),\n" +
        "\n" +
        "\n" +
        "\n" +
        "    p2: (i32, i32),\n" +
        "}\n" +
        "\n" +
        "\n" +
        "\n" +
        "impl Rectangle {\n" +
        "    fn dimensions(&self) -> (i32, i32) {\n" +
        "        let (x1, y1) = self.p1;\n" +
        "        let (x2, y2) = self.p2;\n" +
        "\n" +
        "\n" +
        "\n" +
        "        ((x1 - x2).abs(), (y1 - y2).abs())\n" +
        "    }\n" +
        "\n" +
        "\n" +
        "\n" +
        "    fn area(&self) -> i32 {\n" +
        "        let (a, b) = self.dimensions();\n" +
        "        a * b\n" +
        "    }\n" +
        "}"
    );
}
