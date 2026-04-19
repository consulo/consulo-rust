/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.Ty;

/**
 * Utility class providing static entry points for the PSI rendering methods.
 */
public final class RsPsiRendererUtil {

    private RsPsiRendererUtil() {
    }

    /** Return text of the element without switching to AST (loses non-stubbed parts of PSI) */
    @NlsSafe
    @NotNull
    public static String getStubOnlyText(@NotNull RsTypeReference ref) {
        return getStubOnlyText(ref, Substitution.EMPTY, true, true);
    }

    @NlsSafe
    @NotNull
    public static String getStubOnlyText(@NotNull RsTypeReference ref, @NotNull Substitution subst) {
        return getStubOnlyText(ref, subst, true, true);
    }

    @NlsSafe
    @NotNull
    public static String getStubOnlyText(@NotNull RsTypeReference ref, @NotNull Substitution subst, boolean renderLifetimes) {
        return getStubOnlyText(ref, subst, renderLifetimes, true);
    }

    @NlsSafe
    @NotNull
    public static String getStubOnlyText(
        @NotNull RsTypeReference ref,
        @NotNull Substitution subst,
        boolean renderLifetimes,
        boolean shortPaths
    ) {
        PsiRenderingOptions options = new PsiRenderingOptions(renderLifetimes, true, shortPaths);
        TypeSubstitutingPsiRenderer renderer = new TypeSubstitutingPsiRenderer(options, subst);
        return renderTypeReference(renderer, ref);
    }

    /** Return text of the element without switching to AST (loses non-stubbed parts of PSI) */
    @NotNull
    public static String getStubOnlyText(@NotNull RsValueParameterList list) {
        return getStubOnlyText(list, Substitution.EMPTY, true);
    }

    @NotNull
    public static String getStubOnlyText(@NotNull RsValueParameterList list, @NotNull Substitution subst, boolean renderLifetimes) {
        TypeSubstitutingPsiRenderer renderer = new TypeSubstitutingPsiRenderer(new PsiRenderingOptions(renderLifetimes), subst);
        return renderValueParameterList(renderer, list);
    }

    @NotNull
    public static String getStubOnlyText(@NotNull RsExpr expr, @NotNull Substitution subst, @NotNull Ty expectedTy) {
        TypeSubstitutingPsiRenderer renderer = new TypeSubstitutingPsiRenderer(new PsiRenderingOptions(), subst);
        return renderConstExpr(renderer, expr, expectedTy);
    }

    /** Return text of the element without switching to AST (loses non-stubbed parts of PSI) */
    @NotNull
    public static String getStubOnlyText(@NotNull RsTraitRef ref) {
        return getStubOnlyText(ref, Substitution.EMPTY, true);
    }

    @NotNull
    public static String getStubOnlyText(@NotNull RsTraitRef ref, @NotNull Substitution subst, boolean renderLifetimes) {
        TypeSubstitutingPsiRenderer renderer = new TypeSubstitutingPsiRenderer(new PsiRenderingOptions(renderLifetimes), subst);
        StringBuilder sb = new StringBuilder();
        renderer.appendPath(sb, ref.getPath());
        return sb.toString();
    }

    @NotNull
    public static String renderTypeReference(@NotNull RsPsiRenderer renderer, @NotNull RsTypeReference ref) {
        StringBuilder sb = new StringBuilder();
        renderer.appendTypeReference(sb, ref);
        return sb.toString();
    }

    @NotNull
    public static String renderTraitRef(@NotNull RsPsiRenderer renderer, @NotNull RsTraitRef ref) {
        StringBuilder sb = new StringBuilder();
        renderer.appendPath(sb, ref.getPath());
        return sb.toString();
    }

    @NotNull
    public static String renderConstExpr(@NotNull RsPsiRenderer renderer, @NotNull RsExpr expr, @NotNull Ty expectedTy) {
        StringBuilder sb = new StringBuilder();
        renderer.appendConstExpr(sb, expr, expectedTy);
        return sb.toString();
    }

    @NotNull
    public static String renderValueParameterList(@NotNull RsPsiRenderer renderer, @NotNull RsValueParameterList list) {
        StringBuilder sb = new StringBuilder();
        renderer.appendValueParameterList(sb, list);
        return sb.toString();
    }

    @NotNull
    public static String renderFunctionSignature(@NotNull RsPsiRenderer renderer, @NotNull RsFunction fn) {
        StringBuilder sb = new StringBuilder();
        renderer.appendFunctionSignature(sb, fn);
        return sb.toString();
    }

    @NotNull
    public static String renderTypeAliasSignature(@NotNull RsPsiRenderer renderer, @NotNull RsTypeAlias ta, boolean renderBounds) {
        StringBuilder sb = new StringBuilder();
        renderer.appendTypeAliasSignature(sb, ta, renderBounds);
        return sb.toString();
    }
}
