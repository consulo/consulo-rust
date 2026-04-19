/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.daemon.impl.actions.AbstractBatchSuppressByNoInspectionCommentFix;
import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.codeInspection.SuppressionUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class RsInspectionSuppressor implements InspectionSuppressor {

    @NotNull
    @Override
    public SuppressQuickFix[] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return new SuppressQuickFix[]{
            new SuppressInspectionFix(toolId),
            new SuppressInspectionFix(SuppressionUtil.ALL)
        };
    }

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
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

    private boolean isSuppressedByComment(@NotNull RsItemElement element, @NotNull String toolId) {
        for (PsiComment comment : leadingComments(element)) {
            Matcher matcher = SuppressionUtil.SUPPRESS_IN_LINE_COMMENT_PATTERN.matcher(comment.getText());
            if (matcher.matches() && SuppressionUtil.isInspectionToolIdMentioned(matcher.group(1), toolId)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static List<PsiComment> leadingComments(@NotNull RsItemElement element) {
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

        SuppressInspectionFix(@NotNull String id) {
            super(id, id.equals(SuppressionUtil.ALL));
            if (id.equals(SuppressionUtil.ALL)) {
                setText(RsBundle.message("intention.name.suppress.all.inspections.for.item"));
            } else {
                setText(RsBundle.message("intention.name.suppress.for.item.with.comment"));
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
