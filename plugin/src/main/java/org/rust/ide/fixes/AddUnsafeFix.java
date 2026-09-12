/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;

import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.injected.DoctestUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAbstractableUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import consulo.localize.LocalizeValue;

public class AddUnsafeFix extends RsQuickFixBase<PsiElement> {
    
    private final String myText;

    private AddUnsafeFix(@Nonnull PsiElement element) {
        super(element);
         String item;
        if (element instanceof RsBlockExpr) {
            item = "block";
        } else if (element instanceof RsImplItem) {
            item = "impl";
        } else {
            item = "function";
        }
        myText = RsBundle.message("intention.name.add.unsafe.to", item);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(myText);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        PsiElement unsafe = new RsPsiFactory(project).createUnsafeKeyword();

        if (element instanceof RsBlockExpr) {
            element.addBefore(unsafe, ((RsBlockExpr) element).getBlock());
        } else if (element instanceof RsFunction) {
            element.addBefore(unsafe, ((RsFunction) element).getFn());
        } else if (element instanceof RsImplItem) {
            element.addBefore(unsafe, ((RsImplItem) element).getImpl());
        } else {
            throw new IllegalStateException("unreachable");
        }
    }

    @Nullable
    public static AddUnsafeFix create(@Nonnull PsiElement element) {
        PsiElement parent = PsiTreeUtil.getParentOfType(
            element,
            RsBlockExpr.class,
            RsFunction.class,
            RsImplItem.class
        );
        if (parent == null) return null;

        if (parent instanceof RsFunction && !isUnsafeApplicable((RsFunction) parent)) {
            return null;
        }
        return new AddUnsafeFix(parent);
    }

    private static boolean isUnsafeApplicable(@Nonnull RsFunction function) {
        // Unsafe modifier cannot be added to main function or tests
        if (RsFunctionUtil.isActuallyUnsafe(function) || RsFunctionUtil.isMain(function) || RsFunctionUtil.isTest(function) || DoctestUtil.isDoctestInjectedMain(function)) {
            return false;
        }

        PsiElement superItem = RsAbstractableUtil.getSuperItem(function);
        if (!(superItem instanceof RsFunction)) return true;
        RsFunction superFn = (RsFunction) superItem;
        // An implementing function cannot be unsafe unless the trait function is unsafe as well
        return superFn.isUnsafe();
    }
}
