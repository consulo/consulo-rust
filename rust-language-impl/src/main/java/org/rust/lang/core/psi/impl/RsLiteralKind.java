/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.language.ast.ASTNode;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.lexer.RsEscapesLexer;
import org.rust.lang.utils.RsEscapesUtils;
import consulo.language.ast.IElementType;
import org.rust.lang.core.psi.*;

/**
 * Represents a Rust literal kind, parsed from AST.
 */
public abstract class RsLiteralKind {
    @Nonnull
    protected final ASTNode node;

    protected RsLiteralKind(@Nonnull ASTNode node) {
        this.node = node;
    }

    @Nonnull
    public ASTNode getNode() {
        return node;
    }

    // --- Boolean literal ---
    public static final class BooleanLiteral extends RsLiteralKind {
        private final boolean value;

        public BooleanLiteral(@Nonnull ASTNode node) {
            super(node);
            this.value = "true".contentEquals(node.getChars());
        }

        public boolean getValue() {
            return value;
        }
    }

    // --- Integer literal ---
    public static final class IntegerLiteral extends RsLiteralKind implements RsLiteralWithSuffix {
        private LiteralOffsets offsets;

        public IntegerLiteral(@Nonnull ASTNode node) {
            super(node);
        }

        @Nonnull
        @Override
        public java.util.List<String> getValidSuffixes() {
            return java.util.Arrays.asList("u8", "i8", "u16", "i16", "u32", "i32", "u64", "i64", "u128", "i128", "isize", "usize");
        }

        @Nonnull
        @Override
        public LiteralOffsets getOffsets() {
            if (offsets == null) {
                offsets = RsLiteralKindUtil.offsetsForNumber(node);
            }
            return offsets;
        }

        @Nullable
        public Long getValue() {
            TextRange valueRange = getOffsets().getValue();
            if (valueRange == null) return null;
            String textValue = valueRange.substring(node.getText());
            int start;
            int radix;
            if (textValue.length() >= 2) {
                String prefix = textValue.substring(0, 2);
                switch (prefix) {
                    case "0x":
                        start = 2;
                        radix = 16;
                        break;
                    case "0o":
                        start = 2;
                        radix = 8;
                        break;
                    case "0b":
                        start = 2;
                        radix = 2;
                        break;
                    default:
                        start = 0;
                        radix = 10;
                        break;
                }
            } else {
                start = 0;
                radix = 10;
            }
            String cleanTextValue = textValue.substring(start).replace("_", "");
            try {
                return Long.parseLong(cleanTextValue, radix);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    // --- Float literal ---
    public static final class FloatLiteral extends RsLiteralKind implements RsLiteralWithSuffix {
        private LiteralOffsets offsets;

        public FloatLiteral(@Nonnull ASTNode node) {
            super(node);
        }

        @Nonnull
        @Override
        public java.util.List<String> getValidSuffixes() {
            return java.util.Arrays.asList("f32", "f64");
        }

        @Nonnull
        @Override
        public LiteralOffsets getOffsets() {
            if (offsets == null) {
                offsets = RsLiteralKindUtil.offsetsForNumber(node);
            }
            return offsets;
        }

        @Nullable
        public Double getValue() {
            TextRange valueRange = getOffsets().getValue();
            if (valueRange == null) return null;
            String text = valueRange.substring(node.getText()).replace("_", "");
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    // --- String literal ---
    public static final class StringLiteral extends RsLiteralKind implements RsLiteralWithSuffix, RsTextLiteral {
        private final boolean isByte;
        private final boolean isCStr;
        private LiteralOffsets offsets;

        public StringLiteral(@Nonnull ASTNode node, boolean isByte, boolean isCStr) {
            super(node);
            this.isByte = isByte;
            this.isCStr = isCStr;
        }

        public boolean isByte() {
            return isByte;
        }

        public boolean isCStr() {
            return isCStr;
        }

        @Nonnull
        @Override
        public LiteralOffsets getOffsets() {
            if (offsets == null) {
                offsets = RsLiteralKindUtil.offsetsForText(node);
            }
            return offsets;
        }

        @Nonnull
        @Override
        public java.util.List<String> getValidSuffixes() {
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean getHasUnpairedQuotes() {
            LiteralOffsets o = getOffsets();
            return o.getOpenDelim() == null || o.getCloseDelim() == null;
        }

        @Nullable
        @Override
        public String getValue() {
            if (RsTokenSets.RS_RAW_LITERALS.contains(node.getElementType())) {
                return getRawValue();
            } else {
                String raw = getRawValue();
                return raw != null ? RsEscapesUtils.unescapeRust(raw, RsEscapesLexer.of(node.getElementType())) : null;
            }
        }

        @Nullable
        public String getRawValue() {
            TextRange valueRange = getOffsets().getValue();
            return valueRange != null ? valueRange.substring(node.getText()) : null;
        }
    }

    // --- Char literal ---
    public static final class CharLiteral extends RsLiteralKind implements RsLiteralWithSuffix, RsTextLiteral {
        private final boolean isByte;
        private LiteralOffsets offsets;

        public CharLiteral(@Nonnull ASTNode node, boolean isByte) {
            super(node);
            this.isByte = isByte;
        }

        public boolean isByte() {
            return isByte;
        }

        @Nonnull
        @Override
        public LiteralOffsets getOffsets() {
            if (offsets == null) {
                offsets = RsLiteralKindUtil.offsetsForText(node);
            }
            return offsets;
        }

        @Nonnull
        @Override
        public java.util.List<String> getValidSuffixes() {
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean getHasUnpairedQuotes() {
            LiteralOffsets o = getOffsets();
            return o.getOpenDelim() == null || o.getCloseDelim() == null;
        }

        @Nullable
        @Override
        public String getValue() {
            TextRange valueRange = getOffsets().getValue();
            if (valueRange == null) return null;
            String raw = valueRange.substring(node.getText());
            return RsEscapesUtils.unescapeRust(raw, RsEscapesLexer.of(node.getElementType()));
        }
    }

    // --- Factory method ---
    @Nullable
    public static RsLiteralKind fromAstNode(@Nonnull ASTNode node) {
        consulo.language.ast.IElementType type = node.getElementType();
        if (type == RsElementTypes.BOOL_LITERAL) return new BooleanLiteral(node);
        if (type == RsElementTypes.INTEGER_LITERAL) return new IntegerLiteral(node);
        if (type == RsElementTypes.FLOAT_LITERAL) return new FloatLiteral(node);
        if (type == RsElementTypes.STRING_LITERAL || type == RsElementTypes.RAW_STRING_LITERAL) {
            return new StringLiteral(node, false, false);
        }
        if (type == RsElementTypes.BYTE_STRING_LITERAL || type == RsElementTypes.RAW_BYTE_STRING_LITERAL) {
            return new StringLiteral(node, true, false);
        }
        if (type == RsElementTypes.CSTRING_LITERAL || type == RsElementTypes.RAW_CSTRING_LITERAL) {
            return new StringLiteral(node, false, true);
        }
        if (type == RsElementTypes.CHAR_LITERAL) return new CharLiteral(node, false);
        if (type == RsElementTypes.BYTE_LITERAL) return new CharLiteral(node, true);
        return null;
    }

    // --- Interfaces ---
    public interface RsComplexLiteral {
        @Nonnull
        ASTNode getNode();

        @Nonnull
        LiteralOffsets getOffsets();
    }

    public interface RsLiteralWithSuffix extends RsComplexLiteral {
        @Nullable
        default String getSuffix() {
            TextRange suffixRange = getOffsets().getSuffix();
            return suffixRange != null ? suffixRange.substring(getNode().getText()) : null;
        }

        @Nonnull
        java.util.List<String> getValidSuffixes();
    }

    public interface RsTextLiteral {
        @Nullable
        String getValue();

        boolean getHasUnpairedQuotes();
    }
}
