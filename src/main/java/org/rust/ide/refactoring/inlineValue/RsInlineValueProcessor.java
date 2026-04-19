/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineValue;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsInlineUsageViewDescriptor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsStructLiteralFieldUtil;
import org.rust.lang.core.resolve.ref.RsReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RsInlineValueProcessor extends BaseRefactoringProcessor {
    @NotNull
    private final InlineValueContext myContext;
    @NotNull
    private final InlineValueMode myMode;

    public RsInlineValueProcessor(
        @NotNull Project project,
        @NotNull InlineValueContext context,
        @NotNull InlineValueMode mode
    ) {
        super(project);
        myContext = context;
        myMode = mode;
    }

    @NotNull
    @Override
    protected UsageInfo[] findUsages() {
        if (myMode == InlineValueMode.INLINE_THIS_ONLY && myContext.getReference() != null) {
            return new UsageInfo[]{new UsageInfo(myContext.getReference())};
        }

        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(myProject);
        List<PsiReference> usages = new ArrayList<>(ReferencesSearch.search(myContext.getElement(), projectScope).findAll());
        return usages.stream().map(UsageInfo::new).toArray(UsageInfo[]::new);
    }

    @Override
    protected void performRefactoring(@NotNull UsageInfo[] usages) {
        RsPsiFactory factory = new RsPsiFactory(myProject);
        for (UsageInfo usage : usages) {
            PsiReference reference = usage.getReference();
            if (!(reference instanceof RsReference)) continue;
            PsiElement element = reference.getElement();

            if (element instanceof RsStructLiteralField) {
                RsStructLiteralField field = (RsStructLiteralField) element;
                if (RsStructLiteralFieldUtil.isShorthand(field)) {
                    field.addAfter(factory.createColon(), field.getReferenceNameElement());
                }
                if (field.getExpr() == null) {
                    field.addAfter(myContext.getExpr(), field.getColon());
                }
            } else if (element instanceof RsPath) {
                PsiElement parent = element.getParent();
                if (parent instanceof RsPathExpr) {
                    InlineValueUtils.replaceWithAddingParentheses((RsElement) parent, myContext.getExpr(), factory);
                }
            } else if (element instanceof RsElement) {
                InlineValueUtils.replaceWithAddingParentheses((RsElement) element, myContext.getExpr(), factory);
            }
        }
        if (myMode == InlineValueMode.INLINE_ALL_AND_REMOVE_ORIGINAL) {
            myContext.delete();
        }
    }

    @NotNull
    @Override
    protected String getCommandName() {
        return RsBundle.message("command.name.inline", myContext.getType(), myContext.getName());
    }

    @NotNull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
        String type = myContext.getType();
        String capitalizedType = type.substring(0, 1).toUpperCase() + type.substring(1);
        return new RsInlineUsageViewDescriptor(myContext.getElement(), RsBundle.message("list.item.to.inline", capitalizedType));
    }
}
