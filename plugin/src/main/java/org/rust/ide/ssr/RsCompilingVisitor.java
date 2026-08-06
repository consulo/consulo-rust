/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.ssr;

import consulo.language.psi.PsiElement;
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor;
import com.intellij.structuralsearch.impl.matcher.handlers.TopLevelMatchingHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsRecursiveVisitor;

// TODO: implement WordOptimizer and filters
public class RsCompilingVisitor extends RsRecursiveVisitor {
    @Nonnull
    private final GlobalCompilingVisitor myCompilingVisitor;

    public RsCompilingVisitor(@Nonnull GlobalCompilingVisitor compilingVisitor) {
        this.myCompilingVisitor = compilingVisitor;
    }

    public void compile(@Nullable PsiElement[] topLevelElements) {
        var pattern = myCompilingVisitor.getContext().getPattern();
        if (topLevelElements == null) return;

        for (PsiElement element : topLevelElements) {
            element.accept(this);
            pattern.setHandler(element, new TopLevelMatchingHandler(pattern.getHandler(element)));
        }
    }

    @Override
    public void visitElement(@Nonnull PsiElement element) {
        myCompilingVisitor.handle(element);
        super.visitElement(element);
    }
}
