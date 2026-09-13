/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.presentation;


import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.Ty;

/**
 * Utility class providing static entry points for the PSI rendering methods.
 */
public final class RsPsiRendererUtil {

    private RsPsiRendererUtil() {
    }

    /** Return text of the element without switching to AST (loses non-stubbed parts of PSI) */
    
    @Nonnull
    public static String getStubOnlyText(@Nonnull RsTypeReference ref) {
        return getStubOnlyText(ref, Substitution.EMPTY, true, true);
    }

    
    @Nonnull
    public static String getStubOnlyText(@Nonnull RsTypeReference ref, @Nonnull Substitution subst) {
        return getStubOnlyText(ref, subst, true, true);
    }

    
    @Nonnull
    public static String getStubOnlyText(@Nonnull RsTypeReference ref, @Nonnull Substitution subst, boolean renderLifetimes) {
        return getStubOnlyText(ref, subst, renderLifetimes, true);
    }

    
    @Nonnull
    public static String getStubOnlyText(
        @Nonnull RsTypeReference ref,
        @Nonnull Substitution subst,
        boolean renderLifetimes,
        boolean shortPaths
    ) {
        PsiRenderingOptions options = new PsiRenderingOptions(renderLifetimes, true, shortPaths);
        TypeSubstitutingPsiRenderer renderer = new TypeSubstitutingPsiRenderer(options, subst);
        return renderTypeReference(renderer, ref);
    }

    /** Return text of the element without switching to AST (loses non-stubbed parts of PSI) */
    @Nonnull
    public static String getStubOnlyText(@Nonnull RsValueParameterList list) {
        return getStubOnlyText(list, Substitution.EMPTY, true);
    }

    @Nonnull
    public static String getStubOnlyText(@Nonnull RsValueParameterList list, @Nonnull Substitution subst, boolean renderLifetimes) {
        TypeSubstitutingPsiRenderer renderer = new TypeSubstitutingPsiRenderer(new PsiRenderingOptions(renderLifetimes), subst);
        return renderValueParameterList(renderer, list);
    }

    @Nonnull
    public static String getStubOnlyText(@Nonnull RsExpr expr, @Nonnull Substitution subst, @Nonnull Ty expectedTy) {
        TypeSubstitutingPsiRenderer renderer = new TypeSubstitutingPsiRenderer(new PsiRenderingOptions(), subst);
        return renderConstExpr(renderer, expr, expectedTy);
    }

    /** Return text of the element without switching to AST (loses non-stubbed parts of PSI) */
    @Nonnull
    public static String getStubOnlyText(@Nonnull RsTraitRef ref) {
        return getStubOnlyText(ref, Substitution.EMPTY, true);
    }

    @Nonnull
    public static String getStubOnlyText(@Nonnull RsTraitRef ref, @Nonnull Substitution subst, boolean renderLifetimes) {
        TypeSubstitutingPsiRenderer renderer = new TypeSubstitutingPsiRenderer(new PsiRenderingOptions(renderLifetimes), subst);
        StringBuilder sb = new StringBuilder();
        renderer.appendPath(sb, ref.getPath());
        return sb.toString();
    }

    @Nonnull
    public static String renderTypeReference(@Nonnull RsPsiRenderer renderer, @Nonnull RsTypeReference ref) {
        StringBuilder sb = new StringBuilder();
        renderer.appendTypeReference(sb, ref);
        return sb.toString();
    }

    @Nonnull
    public static String renderTraitRef(@Nonnull RsPsiRenderer renderer, @Nonnull RsTraitRef ref) {
        StringBuilder sb = new StringBuilder();
        renderer.appendPath(sb, ref.getPath());
        return sb.toString();
    }

    @Nonnull
    public static String renderConstExpr(@Nonnull RsPsiRenderer renderer, @Nonnull RsExpr expr, @Nonnull Ty expectedTy) {
        StringBuilder sb = new StringBuilder();
        renderer.appendConstExpr(sb, expr, expectedTy);
        return sb.toString();
    }

    @Nonnull
    public static String renderValueParameterList(@Nonnull RsPsiRenderer renderer, @Nonnull RsValueParameterList list) {
        StringBuilder sb = new StringBuilder();
        renderer.appendValueParameterList(sb, list);
        return sb.toString();
    }

    @Nonnull
    public static String renderFunctionSignature(@Nonnull RsPsiRenderer renderer, @Nonnull RsFunction fn) {
        StringBuilder sb = new StringBuilder();
        renderer.appendFunctionSignature(sb, fn);
        return sb.toString();
    }

    @Nonnull
    public static String renderTypeAliasSignature(@Nonnull RsPsiRenderer renderer, @Nonnull RsTypeAlias ta, boolean renderBounds) {
        StringBuilder sb = new StringBuilder();
        renderer.appendTypeAliasSignature(sb, ta, renderBounds);
        return sb.toString();
    }
}
