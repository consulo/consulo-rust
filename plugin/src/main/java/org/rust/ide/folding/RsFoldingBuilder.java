/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.folding;

import consulo.language.codeStyle.CodeStyle;
import consulo.language.editor.folding.CodeFoldingSettings;
import consulo.language.ast.ASTNode;
import consulo.language.editor.folding.CustomFoldingBuilder;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.document.Document;
import consulo.codeEditor.FoldingGroup;
import consulo.application.dumb.DumbAware;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.ast.TokenSet;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.ide.injected.RsDoctestLanguageInjector;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.List;

import static org.rust.lang.core.psi.RsElementTypes.*;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RsFoldingBuilder extends CustomFoldingBuilder implements DumbAware {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    private static final TokenSet COLLAPSED_BY_DEFAULT = TokenSet.create(LBRACE, RBRACE);
    private static final int ONE_LINER_PLACEHOLDERS_EXTRA_LENGTH = 4;

    @Override
    protected String getLanguagePlaceholderText(@Nonnull ASTNode node, @Nonnull TextRange range) {
        if (node.getElementType() == LBRACE) return " { ";
        if (node.getElementType() == RBRACE) return " }";
        if (node.getElementType() == USE_ITEM) return "...";

        PsiElement psi = node.getPsi();
        if (psi instanceof RsModDeclItem || psi instanceof RsExternCrateItem || psi instanceof RsWhereClause) {
            return "...";
        }
        if (psi instanceof PsiComment) return "/* ... */";
        if (psi instanceof RsValueParameterList) return "(...)";
        return "{...}";
    }

    @Override
    protected void buildLanguageFoldRegions(
        @Nonnull List<FoldingDescriptor> descriptors,
        @Nonnull PsiElement root,
        @Nonnull Document document,
        boolean quick
    ) {
        if (!(root instanceof RsFile)) return;

        List<TextRange> usingRanges = new ArrayList<>();
        List<TextRange> modsRanges = new ArrayList<>();
        List<TextRange> cratesRanges = new ArrayList<>();

        int rightMargin = CodeStyle.getSettings((consulo.language.psi.PsiFile) root).getRightMargin(RsLanguage.INSTANCE);
        FoldingVisitor visitor = new FoldingVisitor(descriptors, usingRanges, modsRanges, cratesRanges, rightMargin);
        PsiTreeUtil.processElements(root, it -> { it.accept(visitor); return true; });
    }

    @Override
    protected boolean isCustomFoldingRoot(@Nonnull ASTNode node) {
        return node.getElementType() == BLOCK;
    }

    @Override
    protected boolean isRegionCollapsedByDefault(@Nonnull ASTNode node) {
        return (RsCodeFoldingSettings.getInstance().getCollapsibleOneLineMethods() && COLLAPSED_BY_DEFAULT.contains(node.getElementType()))
            || isDefaultCollapsedNode(CodeFoldingSettings.getInstance(), node);
    }

    private static boolean isDefaultCollapsedNode(CodeFoldingSettings settings, ASTNode node) {
        return (settings.COLLAPSE_DOC_COMMENTS && RS_DOC_COMMENTS_TOKEN_SET.contains(node.getElementType()))
            || (settings.COLLAPSE_IMPORTS && node.getElementType() == USE_ITEM)
            || (settings.COLLAPSE_METHODS && node.getElementType() == BLOCK && node.getPsi().getParent() instanceof RsFunction);
    }

    private static final TokenSet RS_DOC_COMMENTS_TOKEN_SET = TokenSet.create(
        RustParserDefinition.INNER_BLOCK_DOC_COMMENT,
        RustParserDefinition.INNER_EOL_DOC_COMMENT,
        RustParserDefinition.OUTER_BLOCK_DOC_COMMENT,
        RustParserDefinition.OUTER_EOL_DOC_COMMENT
    );

    private static class FoldingVisitor extends RsVisitor {
        private final List<FoldingDescriptor> descriptors;
        private final List<TextRange> usesRanges;
        private final List<TextRange> modsRanges;
        private final List<TextRange> cratesRanges;
        private final int rightMargin;

        FoldingVisitor(
            List<FoldingDescriptor> descriptors,
            List<TextRange> usesRanges,
            List<TextRange> modsRanges,
            List<TextRange> cratesRanges,
            int rightMargin
        ) {
            this.descriptors = descriptors;
            this.usesRanges = usesRanges;
            this.modsRanges = modsRanges;
            this.cratesRanges = cratesRanges;
            this.rightMargin = rightMargin;
        }

        @Override
        public void visitMacroBody(@Nonnull RsMacroBody o) { fold(o); }

        @Override
        public void visitMacroExpansion(@Nonnull RsMacroExpansion o) { fold(o); }

        @Override
        public void visitMacro2(@Nonnull RsMacro2 o) {
            foldBetween(o, o.getLparen(), o.getRparen());
            foldBetween(o, o.getLbrace(), o.getRbrace());
        }

        @Override
        public void visitStructLiteralBody(@Nonnull RsStructLiteralBody o) { fold(o); }

        @Override
        public void visitEnumBody(@Nonnull RsEnumBody o) { fold(o); }

        @Override
        public void visitBlockFields(@Nonnull RsBlockFields o) { fold(o); }

        @Override
        public void visitBlock(@Nonnull RsBlock o) {
            if (tryFoldBlockWhitespaces(o)) return;
            RsFunction parentFn = o.getParent() instanceof RsFunction ? (RsFunction) o.getParent() : null;
            if (parentFn == null || !RsDoctestLanguageInjector.INJECTED_MAIN_NAME.equals(parentFn.getName())) {
                fold(o);
            }
        }

        @Override
        public void visitMatchBody(@Nonnull RsMatchBody o) { fold(o); }

        @Override
        public void visitUseGroup(@Nonnull RsUseGroup o) { fold(o); }

        @Override
        public void visitWhereClause(@Nonnull RsWhereClause clause) {
            int start = foldRegionStart(clause.getWhere());
            int end = clause.getLastChild().getTextRange().getEndOffset();
            if (end > start) {
                descriptors.add(new FoldingDescriptor(clause.getNode(), new TextRange(start, end)));
            }
        }

        @Override
        public void visitMembers(@Nonnull RsMembers o) { foldBetween(o, o.getLbrace(), o.getRbrace()); }

        @Override
        public void visitModItem(@Nonnull RsModItem o) { foldBetween(o, o.getLbrace(), o.getRbrace()); }

        @Override
        public void visitForeignModItem(@Nonnull RsForeignModItem o) { foldBetween(o, o.getLbrace(), o.getRbrace()); }

        @Override
        public void visitMacroArgument(@Nonnull RsMacroArgument o) {
            RsMacroCall macroCall = o.getParent() instanceof RsMacroCall ? (RsMacroCall) o.getParent() : null;
            if (macroCall != null && RsMacroCallUtil.getBracesKind(macroCall) == MacroBraces.BRACES) {
                foldBetween(o, o.getLbrace(), o.getRbrace());
            }
        }

        @Override
        public void visitValueParameterList(@Nonnull RsValueParameterList o) {
            if (o.getValueParameterList().isEmpty()) return;
            foldBetween(o, o.getFirstChild(), o.getLastChild());
        }

        @Override
        public void visitComment(@Nonnull PsiComment comment) {
            var tokenType = comment.getTokenType();
            if (tokenType == RustParserDefinition.BLOCK_COMMENT ||
                tokenType == RustParserDefinition.INNER_BLOCK_DOC_COMMENT ||
                tokenType == RustParserDefinition.OUTER_BLOCK_DOC_COMMENT ||
                tokenType == RustParserDefinition.INNER_EOL_DOC_COMMENT ||
                tokenType == RustParserDefinition.OUTER_EOL_DOC_COMMENT) {
                fold(comment);
            }
        }

        @Override
        public void visitStructItem(@Nonnull RsStructItem o) {
            RsBlockFields blockFields = o.getBlockFields();
            if (blockFields != null) {
                fold(blockFields);
            }
        }

        private void fold(PsiElement element) {
            descriptors.add(new FoldingDescriptor(element.getNode(), element.getTextRange()));
        }

        private void foldBetween(PsiElement element, PsiElement left, PsiElement right) {
            if (left != null && right != null && right.getTextLength() > 0) {
                TextRange range = new TextRange(left.getTextOffset(), right.getTextOffset() + 1);
                descriptors.add(new FoldingDescriptor(element.getNode(), range));
            }
        }

        private boolean tryFoldBlockWhitespaces(RsBlock block) {
            if (!(block.getParent() instanceof RsFunction)) return false;

            Document doc = block.getContainingFile().getViewProvider().getDocument();
            if (doc == null) return false;
            int maxLength = rightMargin - getOffsetInLine(block, doc) - ONE_LINER_PLACEHOLDERS_EXTRA_LENGTH;
            if (!isSingleLine(block, doc, maxLength)) return false;

            PsiElement lbrace = block.getLbrace();
            PsiElement rbrace = block.getRbrace();
            if (rbrace == null) return false;

            PsiElement blockElement = PsiElementUtil.getNextNonCommentSibling(lbrace);
            if (blockElement == null || blockElement != PsiElementUtil.getPrevNonCommentSibling(rbrace)) return false;
            if (blockElement.textContains('\n')) return false;
            if (!(areOnAdjacentLines(doc, lbrace, blockElement) && areOnAdjacentLines(doc, blockElement, rbrace))) return false;

            PsiElement leadingSpace = lbrace.getNextSibling() instanceof PsiWhiteSpace ? lbrace.getNextSibling() : null;
            PsiElement trailingSpace = rbrace.getPrevSibling() instanceof PsiWhiteSpace ? rbrace.getPrevSibling() : null;
            if (leadingSpace == null || trailingSpace == null) return false;

            PsiElement leftEl = block.getPrevSibling() instanceof PsiWhiteSpace ? block.getPrevSibling() : lbrace;
            TextRange range1 = new TextRange(leftEl.getTextOffset(), leadingSpace.getTextRange().getEndOffset());
            TextRange range2 = new TextRange(trailingSpace.getTextOffset(), rbrace.getTextRange().getEndOffset());
            FoldingGroup group = FoldingGroup.newGroup("one-liner");
            descriptors.add(new FoldingDescriptor(lbrace.getNode(), range1, group));
            descriptors.add(new FoldingDescriptor(rbrace.getNode(), range2, group));

            return true;
        }

        @Override
        public void visitUseItem(@Nonnull RsUseItem o) {
            // Simplified: just fold the element itself
            fold(o);
        }

        @Override
        public void visitModDeclItem(@Nonnull RsModDeclItem o) {
            fold(o);
        }

        @Override
        public void visitExternCrateItem(@Nonnull RsExternCrateItem o) {
            fold(o);
        }
    }

    private static boolean areOnAdjacentLines(Document doc, PsiElement first, PsiElement second) {
        return doc.getLineNumber(first.getTextRange().getEndOffset()) + 1 == doc.getLineNumber(second.getTextOffset());
    }

    private static boolean isSingleLine(RsBlock block, Document doc, int maxLength) {
        PsiElement startContents = null;
        PsiElement sibling = block.getLbrace().getNextSibling();
        while (sibling != null) {
            if (!(sibling instanceof PsiWhiteSpace)) {
                startContents = sibling;
                break;
            }
            sibling = sibling.getNextSibling();
        }
        if (startContents == null) return false;
        if (startContents.getNode().getElementType() == RBRACE) return false;

        PsiElement endContents = null;
        PsiElement rbrace = block.getRbrace();
        if (rbrace == null) return false;
        sibling = rbrace.getPrevSibling();
        while (sibling != null) {
            if (!(sibling instanceof PsiWhiteSpace)) {
                endContents = sibling;
                break;
            }
            sibling = sibling.getPrevSibling();
        }
        if (endContents == null) return false;
        if (endContents.getTextRange().getEndOffset() - startContents.getTextOffset() > maxLength) return false;

        return doc.getLineNumber(startContents.getTextOffset()) == doc.getLineNumber(endContents.getTextRange().getEndOffset());
    }

    private static int getOffsetInLine(PsiElement element, Document doc) {
        int blockLine = doc.getLineNumber(element.getTextOffset());
        int offset = 0;
        PsiElement leaf = element.getPrevSibling();
        while (leaf != null) {
            if (doc.getLineNumber(leaf.getTextRange().getEndOffset()) != blockLine) break;
            String text = leaf.getText();
            int lastNewline = text.lastIndexOf('\n');
            offset += text.length() - Math.max(lastNewline + 1, 0);
            leaf = leaf.getPrevSibling();
        }
        return offset;
    }

    private static int foldRegionStart(PsiElement element) {
        PsiElement nextLeaf = PsiTreeUtil.nextLeaf(element, true);
        if (nextLeaf == null) return element.getTextRange().getEndOffset();
        if (nextLeaf.getText().startsWith(" ")) {
            return element.getTextRange().getEndOffset() + 1;
        }
        return element.getTextRange().getEndOffset();
    }
}
