/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

/**
 * Represents a qualified name used for documentation link resolution.
 */
public class RsQualifiedName {
    private final String qualifiedName;

    private RsQualifiedName(@NotNull String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    @Nullable
    public static RsQualifiedName from(@NotNull String link) {
        if (link.isEmpty()) return null;
        return new RsQualifiedName(link);
    }

    @Nullable
    public static RsQualifiedName from(@NotNull RsQualifiedNamedElement element) {
        String name = element.getQualifiedName();
        if (name == null) return null;
        return new RsQualifiedName(name);
    }

    @Nullable
    public static RsQualifiedName from(@NotNull PsiElement element) {
        if (element instanceof RsQualifiedNamedElement) {
            return from((RsQualifiedNamedElement) element);
        }
        return null;
    }

    @Nullable
    public PsiElement findPsiElement(@NotNull PsiManager psiManager, @NotNull RsElement context) {
        // Stub implementation
        return null;
    }

    @NotNull
    public String toUrlPath() {
        return qualifiedName.replace("::", "/");
    }

    @Override
    public String toString() {
        return qualifiedName;
    }
}
