/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceConstant;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsModItem;

import java.util.Objects;

public class InsertionCandidate {
    @NotNull
    private final PsiElement context;
    @NotNull
    private final PsiElement parent;
    @NotNull
    private final PsiElement anchor;

    public InsertionCandidate(@NotNull PsiElement context, @NotNull PsiElement parent, @NotNull PsiElement anchor) {
        this.context = context;
        this.parent = parent;
        this.anchor = anchor;
    }

    @NotNull
    public PsiElement getContext() {
        return context;
    }

    @NotNull
    public PsiElement getParent() {
        return parent;
    }

    @NotNull
    public PsiElement getAnchor() {
        return anchor;
    }

    @Nls
    @NotNull
    public String description() {
        if (context instanceof RsFunction) {
            String name = ((RsFunction) context).getName();
            return RsBundle.message("fn.0", name != null ? name : "");
        } else if (context instanceof RsModItem) {
            String name = ((RsModItem) context).getName();
            return RsBundle.message("mod.0", name != null ? name : "");
        } else if (context instanceof RsFile) {
            return RsBundle.message("file");
        } else {
            throw new IllegalStateException("unreachable");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InsertionCandidate that = (InsertionCandidate) o;
        return Objects.equals(context, that.context) &&
            Objects.equals(parent, that.parent) &&
            Objects.equals(anchor, that.anchor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, parent, anchor);
    }

    @Override
    public String toString() {
        return "InsertionCandidate{context=" + context + ", parent=" + parent + ", anchor=" + anchor + "}";
    }
}
