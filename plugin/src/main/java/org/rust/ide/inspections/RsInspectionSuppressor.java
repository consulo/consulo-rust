/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.editor.inspection.AbstractBatchSuppressByNoInspectionCommentFix;
import consulo.language.editor.inspection.InspectionSuppressor;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.editor.inspection.SuppressionUtil;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class RsInspectionSuppressor implements InspectionSuppressor {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Nonnull
    @Override
    public SuppressQuickFix[] getSuppressActions(@Nullable PsiElement element, @Nonnull String toolId) {
        return new SuppressQuickFix[]{
            new SuppressInspectionFix(toolId),
            new SuppressInspectionFix(SuppressionUtil.ALL)
        };
    }

    @Override
    public boolean isSuppressedFor(@Nonnull PsiElement element, @Nonnull String toolId) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof RsItemElement) {
                if (isSuppressedByComment((RsItemElement) current, toolId)) {
                    return true;
                }
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean isSuppressedByComment(@Nonnull RsItemElement element, @Nonnull String toolId) {
        for (PsiComment comment : leadingComments(element)) {
            Matcher matcher = SuppressionUtil.SUPPRESS_IN_LINE_COMMENT_PATTERN.matcher(comment.getText());
            if (matcher.matches() && SuppressionUtil.isInspectionToolIdMentioned(matcher.group(1), toolId)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private static List<PsiComment> leadingComments(@Nonnull RsItemElement element) {
        List<PsiComment> comments = new ArrayList<>();
        PsiElement psi = element.getFirstChild();
        while (psi != null) {
            if (psi instanceof PsiComment) {
                comments.add((PsiComment) psi);
            }
            PsiElement next = psi.getNextSibling();
            if (next == null) break;
            if (RsTokenType.RS_COMMENTS.contains(next.getNode().getElementType()) || next instanceof PsiWhiteSpace) {
                psi = next;
            } else {
                break;
            }
        }
        return comments;
    }

    private static class SuppressInspectionFix extends AbstractBatchSuppressByNoInspectionCommentFix {

        SuppressInspectionFix(@Nonnull String id) {
            super(id, id.equals(SuppressionUtil.ALL));
            if (id.equals(SuppressionUtil.ALL)) {
                setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.suppress.all.inspections.for.item")));
            } else {
                setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.suppress.for.item.with.comment")));
            }
        }

        @Nullable
        @Override
        public PsiElement getContainer(@Nullable PsiElement context) {
            if (context == null) return null;
            return RsElementUtil.ancestorOrSelf(context, RsItemElement.class);
        }
    }
}
