/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.colors;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.options.OptionsBundle;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.util.NlsContexts.AttributeDescriptor;
import org.rust.RsBundle;

import java.util.function.Supplier;

/**
 * See {@link RsColorSettingsPage} and {@link org.rust.ide.highlight.RsHighlighter}
 */
public enum RsColor {
    VARIABLE(RsBundle.messagePointer("settings.rust.color.variables.default"), DefaultLanguageHighlighterColors.IDENTIFIER),
    MUT_BINDING(RsBundle.messagePointer("settings.rust.color.mutable.binding"), DefaultLanguageHighlighterColors.IDENTIFIER),
    FIELD(RsBundle.messagePointer("settings.rust.color.field"), DefaultLanguageHighlighterColors.INSTANCE_FIELD),
    CONSTANT(RsBundle.messagePointer("settings.rust.color.constant"), DefaultLanguageHighlighterColors.CONSTANT),
    STATIC(RsBundle.messagePointer("settings.rust.color.static"), VARIABLE),
    MUT_STATIC(RsBundle.messagePointer("settings.rust.color.static.mutable"), MUT_BINDING),

    FUNCTION(RsBundle.messagePointer("settings.rust.color.function.declaration"), DefaultLanguageHighlighterColors.FUNCTION_DECLARATION),
    METHOD(RsBundle.messagePointer("settings.rust.color.method.declaration"), DefaultLanguageHighlighterColors.INSTANCE_METHOD),
    ASSOC_FUNCTION(RsBundle.messagePointer("settings.rust.color.associated.function.declaration"), DefaultLanguageHighlighterColors.STATIC_METHOD),
    FUNCTION_CALL(RsBundle.messagePointer("settings.rust.color.function.call"), DefaultLanguageHighlighterColors.FUNCTION_CALL),
    METHOD_CALL(RsBundle.messagePointer("settings.rust.color.method.call"), DefaultLanguageHighlighterColors.FUNCTION_CALL),
    ASSOC_FUNCTION_CALL(RsBundle.messagePointer("settings.rust.color.associated.function.call"), DefaultLanguageHighlighterColors.STATIC_METHOD),
    MACRO(RsBundle.messagePointer("settings.rust.color.macro"), DefaultLanguageHighlighterColors.IDENTIFIER),

    PARAMETER(RsBundle.messagePointer("settings.rust.color.parameter"), DefaultLanguageHighlighterColors.PARAMETER),
    MUT_PARAMETER(RsBundle.messagePointer("settings.rust.color.mutable.parameter"), DefaultLanguageHighlighterColors.PARAMETER),
    SELF_PARAMETER(RsBundle.messagePointer("settings.rust.color.self.parameter"), DefaultLanguageHighlighterColors.KEYWORD),
    LIFETIME(RsBundle.messagePointer("settings.rust.color.lifetime"), DefaultLanguageHighlighterColors.IDENTIFIER),
    TYPE_PARAMETER(RsBundle.messagePointer("settings.rust.color.type.parameter"), DefaultLanguageHighlighterColors.IDENTIFIER),
    CONST_PARAMETER(RsBundle.messagePointer("settings.rust.color.const.parameter"), DefaultLanguageHighlighterColors.CONSTANT),

    PRIMITIVE_TYPE(RsBundle.messagePointer("settings.rust.color.primitive"), DefaultLanguageHighlighterColors.KEYWORD),
    STRUCT(RsBundle.messagePointer("settings.rust.color.struct"), DefaultLanguageHighlighterColors.CLASS_NAME),
    UNION(RsBundle.messagePointer("settings.rust.color.union"), DefaultLanguageHighlighterColors.CLASS_NAME),
    TRAIT(RsBundle.messagePointer("settings.rust.color.trait"), DefaultLanguageHighlighterColors.INTERFACE_NAME),
    ENUM(RsBundle.messagePointer("settings.rust.color.enum"), DefaultLanguageHighlighterColors.CLASS_NAME),
    ENUM_VARIANT(RsBundle.messagePointer("settings.rust.color.enum.variant"), DefaultLanguageHighlighterColors.STATIC_FIELD),
    TYPE_ALIAS(RsBundle.messagePointer("settings.rust.color.type.alias"), DefaultLanguageHighlighterColors.CLASS_NAME),
    CRATE(RsBundle.messagePointer("settings.rust.color.crate"), DefaultLanguageHighlighterColors.IDENTIFIER),
    MODULE(RsBundle.messagePointer("settings.rust.color.module"), DefaultLanguageHighlighterColors.IDENTIFIER),

    KEYWORD(RsBundle.messagePointer("settings.rust.color.keyword"), DefaultLanguageHighlighterColors.KEYWORD),
    KEYWORD_UNSAFE(RsBundle.messagePointer("settings.rust.color.keyword.unsafe"), DefaultLanguageHighlighterColors.KEYWORD),

    CHAR(RsBundle.messagePointer("settings.rust.color.char"), DefaultLanguageHighlighterColors.STRING),
    NUMBER(RsBundle.messagePointer("settings.rust.color.number"), DefaultLanguageHighlighterColors.NUMBER),
    STRING(RsBundle.messagePointer("settings.rust.color.string"), DefaultLanguageHighlighterColors.STRING),
    VALID_STRING_ESCAPE(RsBundle.messagePointer("settings.rust.color.valid.escape.sequence"), DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE),
    INVALID_STRING_ESCAPE(RsBundle.messagePointer("settings.rust.color.invalid.escape.sequence"), DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE),
    FORMAT_PARAMETER(RsBundle.messagePointer("settings.rust.color.format.parameter"), DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE),
    FORMAT_SPECIFIER(RsBundle.messagePointer("settings.rust.color.format.specifier"), HighlighterColors.TEXT),

    BLOCK_COMMENT(OptionsBundle.messagePointer("options.language.defaults.block.comment"), DefaultLanguageHighlighterColors.BLOCK_COMMENT),
    EOL_COMMENT(OptionsBundle.messagePointer("options.language.defaults.line.comment"), DefaultLanguageHighlighterColors.LINE_COMMENT),

    DOC_COMMENT(RsBundle.messagePointer("settings.rust.color.rustdoc.comment"), DefaultLanguageHighlighterColors.DOC_COMMENT),
    DOC_HEADING(RsBundle.messagePointer("settings.rust.color.rustdoc.heading"), DefaultLanguageHighlighterColors.DOC_COMMENT_TAG),
    DOC_LINK(RsBundle.messagePointer("settings.rust.color.rustdoc.link"), DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE),
    DOC_EMPHASIS(RsBundle.messagePointer("settings.rust.color.rustdoc.italic")),
    DOC_STRONG(RsBundle.messagePointer("settings.rust.color.rustdoc.bold")),
    DOC_CODE(RsBundle.messagePointer("settings.rust.color.rustdoc.code"), DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP),

    BRACES(OptionsBundle.messagePointer("options.language.defaults.braces"), DefaultLanguageHighlighterColors.BRACES),
    BRACKETS(OptionsBundle.messagePointer("options.language.defaults.brackets"), DefaultLanguageHighlighterColors.BRACKETS),
    OPERATORS(RsBundle.messagePointer("settings.rust.color.operation.sign"), DefaultLanguageHighlighterColors.OPERATION_SIGN),
    Q_OPERATOR(RsBundle.messagePointer("settings.rust.color.question.mark"), DefaultLanguageHighlighterColors.KEYWORD),
    SEMICOLON(OptionsBundle.messagePointer("options.language.defaults.semicolon"), DefaultLanguageHighlighterColors.SEMICOLON),
    DOT(OptionsBundle.messagePointer("options.language.defaults.dot"), DefaultLanguageHighlighterColors.DOT),
    COMMA(OptionsBundle.messagePointer("options.language.defaults.comma"), DefaultLanguageHighlighterColors.COMMA),
    PARENTHESES(OptionsBundle.messagePointer("options.language.defaults.parentheses"), DefaultLanguageHighlighterColors.PARENTHESES),

    ATTRIBUTE(RsBundle.messagePointer("settings.rust.color.attribute"), DefaultLanguageHighlighterColors.METADATA),
    UNSAFE_CODE(RsBundle.messagePointer("settings.rust.color.unsafe.code")),
    CFG_DISABLED_CODE(RsBundle.messagePointer("settings.rust.color.conditionally.disabled.code")),
    GENERATED_ITEM(RsBundle.messagePointer("settings.rust.color.generated.items"));

    private final TextAttributesKey myTextAttributesKey;
    private final AttributesDescriptor myAttributesDescriptor;
    private final HighlightSeverity myTestSeverity;

    RsColor(Supplier<@AttributeDescriptor String> humanName, TextAttributesKey defaultKey) {
        this.myTextAttributesKey = TextAttributesKey.createTextAttributesKey("org.rust." + name(), defaultKey);
        this.myAttributesDescriptor = new AttributesDescriptor(humanName, myTextAttributesKey);
        this.myTestSeverity = new HighlightSeverity(name(), HighlightSeverity.INFORMATION.myVal);
    }

    RsColor(Supplier<@AttributeDescriptor String> humanName) {
        this(humanName, (TextAttributesKey) null);
    }

    RsColor(Supplier<@AttributeDescriptor String> humanName, RsColor fallbackColor) {
        this.myTextAttributesKey = TextAttributesKey.createTextAttributesKey("org.rust." + name(), fallbackColor.myTextAttributesKey);
        this.myAttributesDescriptor = new AttributesDescriptor(humanName, myTextAttributesKey);
        this.myTestSeverity = new HighlightSeverity(name(), HighlightSeverity.INFORMATION.myVal);
    }

    static {
        // Fix cross-references: STATIC defaults to VARIABLE's key, MUT_STATIC defaults to MUT_BINDING's key
        // These are handled by the TextAttributesKey fallback chain in the IDE
    }

    public TextAttributesKey getTextAttributesKey() {
        return myTextAttributesKey;
    }

    public AttributesDescriptor getAttributesDescriptor() {
        return myAttributesDescriptor;
    }

    public HighlightSeverity getTestSeverity() {
        return myTestSeverity;
    }
}
