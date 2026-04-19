/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.injected.DoctestUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAbstractableUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class AddUnsafeFix extends RsQuickFixBase<PsiElement> {
    @Nls
    private final String myText;

    private AddUnsafeFix(@NotNull PsiElement element) {
        super(element);
        @NlsSafe String item;
        if (element instanceof RsBlockExpr) {
            item = "block";
        } else if (element instanceof RsImplItem) {
            item = "impl";
        } else {
            item = "function";
        }
        myText = RsBundle.message("intention.name.add.unsafe.to", item);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public String getText() {
        return myText;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
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
    public static AddUnsafeFix create(@NotNull PsiElement element) {
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

    private static boolean isUnsafeApplicable(@NotNull RsFunction function) {
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
