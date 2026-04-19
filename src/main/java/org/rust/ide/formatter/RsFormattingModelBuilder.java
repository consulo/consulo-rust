/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.formatter.blocks.RsFmtBlock;
import org.rust.ide.formatter.blocks.RsMacroArgFmtBlock;
import org.rust.ide.formatter.blocks.RsMultilineStringLiteralBlock;
import org.rust.lang.core.psi.RsTokenType;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.tokenSetOf;

public class RsFormattingModelBuilder implements FormattingModelBuilder {

    private static final TokenSet MACRO_COMPOSITE_NODES = tokenSetOf(
        MACRO_BODY, MACRO_CASE, MACRO_PATTERN, MACRO_PATTERN_CONTENTS, MACRO_BINDING, MACRO_BINDING_GROUP,
        MACRO_EXPANSION, MACRO_EXPANSION_CONTENTS, MACRO_REFERENCE, MACRO_EXPANSION_REFERENCE_GROUP,
        MACRO_BINDING_GROUP_SEPARATOR, META_VAR_IDENTIFIER,
        MACRO_ARGUMENT, MACRO_ARGUMENT_TT
    );

    @Nullable
    @Override
    public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
        return null;
    }

    @NotNull
    @Override
    public FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        com.intellij.psi.codeStyle.CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
        com.intellij.psi.PsiElement element = formattingContext.getPsiElement();
        RsFmtContext ctx = RsFmtContext.create(settings);
        ASTBlock block = createBlock(element.getNode(), null, Indent.getNoneIndent(), null, ctx);
        return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), block, settings);
    }

    @NotNull
    public static ASTBlock createBlock(@NotNull ASTNode node,
                                        @Nullable Alignment alignment,
                                        @Nullable Indent indent,
                                        @Nullable Wrap wrap,
                                        @NotNull RsFmtContext ctx) {
        IElementType type = node.getElementType();

        if (MACRO_COMPOSITE_NODES.contains(type)) {
            return new RsMacroArgFmtBlock(node, alignment, indent, wrap, ctx);
        }

        if ((RsTokenType.RS_STRING_LITERALS.contains(type) || RsTokenType.RS_RAW_LITERALS.contains(type))
            && node.textContains('\n')) {
            return new RsMultilineStringLiteralBlock(node, alignment, indent, wrap);
        }

        return new RsFmtBlock(node, alignment, indent, wrap, ctx);
    }
}
