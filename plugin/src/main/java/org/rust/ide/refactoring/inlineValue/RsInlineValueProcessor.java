/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineValue;

import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewDescriptor;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsInlineUsageViewDescriptor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsStructLiteralFieldUtil;
import org.rust.lang.core.resolve.ref.RsReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import consulo.localize.LocalizeValue;

public class RsInlineValueProcessor extends BaseRefactoringProcessor {
    @Nonnull
    private final InlineValueContext myContext;
    @Nonnull
    private final InlineValueMode myMode;

    public RsInlineValueProcessor(
        @Nonnull Project project,
        @Nonnull InlineValueContext context,
        @Nonnull InlineValueMode mode
    ) {
        super(project);
        myContext = context;
        myMode = mode;
    }

    @Nonnull
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
    protected void performRefactoring(@Nonnull UsageInfo[] usages) {
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

    @Nonnull
    @Override
    protected consulo.localize.LocalizeValue getCommandName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("command.name.inline", myContext.getType(), myContext.getName()));
    }

    @Nonnull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
        String type = myContext.getType();
        String capitalizedType = type.substring(0, 1).toUpperCase() + type.substring(1);
        return new RsInlineUsageViewDescriptor(myContext.getElement(), RsBundle.message("list.item.to.inline", capitalizedType));
    }
}
