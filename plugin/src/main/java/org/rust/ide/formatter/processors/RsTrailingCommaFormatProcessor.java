/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.processors;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.PostFormatProcessor;
import consulo.language.codeStyle.PostFormatProcessorHelper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.formatter.RsFormatterUtil;
import org.rust.ide.formatter.impl.RsFmtImplUtil;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsBlockFields;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RsTrailingCommaFormatProcessor implements PostFormatProcessor {

    @Nonnull
    @Override
    public PsiElement processElement(@Nonnull PsiElement source, @Nonnull CodeStyleSettings settings) {
        doProcess(source, settings, null);
        return source;
    }

    @Nonnull
    @Override
    public TextRange processText(@Nonnull PsiFile source, @Nonnull TextRange rangeToReformat, @Nonnull CodeStyleSettings settings) {
        return doProcess(source, settings, rangeToReformat).getResultTextRange();
    }

    @Nonnull
    private static PostFormatProcessorHelper doProcess(@Nonnull PsiElement source,
                                                       @Nonnull CodeStyleSettings settings,
                                                       @Nullable TextRange range) {
        PostFormatProcessorHelper helper = new PostFormatProcessorHelper(settings.getCommonSettings(RsLanguage.INSTANCE));
        helper.setResultTextRange(range);
        if (RsFormatterUtil.getRustSettings(settings).PRESERVE_PUNCTUATION) return helper;

        source.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@Nonnull PsiElement element) {
                super.visitElement(element);
                if (!helper.isElementFullyInRange(element)) return;
                RsFmtImplUtil.CommaList commaList = RsFmtImplUtil.CommaList.forElement(element.getNode().getElementType());
                if (commaList == null) return;
                if (removeTrailingComma(commaList, element)) {
                    helper.updateResultRange(1, 0);
                } else if (addTrailingCommaForElement(commaList, element)) {
                    helper.updateResultRange(0, 1);
                }
            }
        });

        return helper;
    }

    /**
     * Delete trailing comma in one-line blocks
     */
    public static boolean removeTrailingComma(@Nonnull RsFmtImplUtil.CommaList commaList, @Nonnull PsiElement list) {
        assert list.getNode().getElementType() == commaList.getList();
        if (PostFormatProcessorHelper.isMultiline(list)) return false;
        PsiElement rbrace = list.getLastChild();
        if (rbrace == null || rbrace.getNode().getElementType() != commaList.getClosingBrace()) return false;
        PsiElement comma = PsiElementUtil.getPrevNonCommentSibling(rbrace);
        if (comma == null) return false;
        if (comma.getNode().getElementType() == RsElementTypes.COMMA) {
            comma.delete();
            return true;
        }
        return false;
    }

    public static boolean addTrailingCommaForElement(@Nonnull RsFmtImplUtil.CommaList commaList, @Nonnull PsiElement list) {
        assert list.getNode().getElementType() == commaList.getList();
        if (!PostFormatProcessorHelper.isMultiline(list)) return false;
        PsiElement rbrace = list.getLastChild();
        if (rbrace == null || rbrace.getNode().getElementType() != commaList.getClosingBrace()) return false;

        PsiElement lastElement = PsiElementUtil.getPrevNonCommentSibling(rbrace);
        if (lastElement == null) return false;
        if (!commaList.isElement(lastElement)) return false;

        CharSequence nodeChars = list.getNode().getChars();
        CharSequence trailingSpace = nodeChars.subSequence(
            lastElement.getStartOffsetInParent() + lastElement.getTextLength(),
            rbrace.getStartOffsetInParent()
        );
        boolean containsNewline = false;
        for (int i = 0; i < trailingSpace.length(); i++) {
            if (trailingSpace.charAt(i) == '\n') {
                containsNewline = true;
                break;
            }
        }
        if (!containsNewline) return false;

        PsiElement firstChild = list.getFirstChild();
        if (firstChild != null && PsiElementUtil.getNextNonCommentSibling(firstChild) == lastElement) {
            // allow trailing comma only for struct block fields with a single item
            if (!(list instanceof RsBlockFields) || !(list.getParent() instanceof RsStructItem)) {
                return false;
            }
        }

        PsiElement comma = new RsPsiFactory(list.getProject()).createComma();
        list.addAfter(comma, lastElement);
        return true;
    }

    public static boolean isOnSameLineAsLastElement(@Nonnull RsFmtImplUtil.CommaList commaList,
                                                    @Nonnull PsiElement list,
                                                    @Nonnull PsiElement element) {
        assert list.getNode().getElementType() == commaList.getList() && commaList.isElement(element);
        PsiElement rbrace = list.getLastChild();
        if (rbrace == null || rbrace.getNode().getElementType() != commaList.getClosingBrace()) return false;
        PsiElement lastElement = PsiElementUtil.getPrevNonCommentSibling(rbrace);
        if (lastElement == null || !commaList.isElement(lastElement)) return false;
        if (element == lastElement) return true;
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) return false;
        Document document = PsiDocumentManager.getInstance(containingFile.getProject()).getDocument(containingFile);
        if (document == null) return false;
        return document.getLineNumber(element.getTextRange().getEndOffset()) == document.getLineNumber(lastElement.getTextRange().getEndOffset());
    }
}
