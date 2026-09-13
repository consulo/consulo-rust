/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.Alignment;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.Wrap;
import consulo.language.codeStyle.Spacing;
import consulo.language.codeStyle.ASTBlock;
import consulo.language.codeStyle.WrapType;
import consulo.language.codeStyle.ChildAttributes;
import consulo.language.codeStyle.FormattingModel;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.codeStyle.FormattingModelProvider;
import consulo.language.codeStyle.FormattingContext;
import consulo.language.codeStyle.SpacingBuilder;
import consulo.language.codeStyle.AbstractBlock;
import consulo.language.ast.ASTNode;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.formatter.blocks.RsFmtBlock;
import org.rust.ide.formatter.blocks.RsMacroArgFmtBlock;
import org.rust.ide.formatter.blocks.RsMultilineStringLiteralBlock;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.tokenSetOf;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.impl.RsTokenSets;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.psi.PsiElement;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsTokenType;

@ExtensionImpl
public class RsFormattingModelBuilder implements FormattingModelBuilder {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


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

    @Nonnull
    @Override
    public FormattingModel createModel(@Nonnull FormattingContext formattingContext) {
        consulo.language.codeStyle.CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
        consulo.language.psi.PsiElement element = formattingContext.getPsiElement();
        RsFmtContext ctx = RsFmtContext.create(settings);
        ASTBlock block = createBlock(element.getNode(), null, Indent.getNoneIndent(), null, ctx);
        return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), block, settings);
    }

    @Nonnull
    public static ASTBlock createBlock(@Nonnull ASTNode node,
                                        @Nullable Alignment alignment,
                                        @Nullable Indent indent,
                                        @Nullable Wrap wrap,
                                        @Nonnull RsFmtContext ctx) {
        IElementType type = node.getElementType();

        if (MACRO_COMPOSITE_NODES.contains(type)) {
            return new RsMacroArgFmtBlock(node, alignment, indent, wrap, ctx);
        }

        if ((RsTokenSets.RS_STRING_LITERALS.contains(type) || RsTokenSets.RS_RAW_LITERALS.contains(type))
            && node.textContains('\n')) {
            return new RsMultilineStringLiteralBlock(node, alignment, indent, wrap);
        }

        return new RsFmtBlock(node, alignment, indent, wrap, ctx);
    }
}
