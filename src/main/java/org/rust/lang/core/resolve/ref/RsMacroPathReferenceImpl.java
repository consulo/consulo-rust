/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * A reference for path of macro call.
 */
public class RsMacroPathReferenceImpl extends RsReferenceBase<RsPath> implements RsPathReference {

    public RsMacroPathReferenceImpl(@NotNull RsPath element) {
        super(element);
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return (element instanceof RsMacroDefinitionBase || element instanceof RsFunction) && super.isReferenceTo(element);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        RsNamedElement resolved = resolve();
        if (resolved != null) {
            return new ResolveResult[]{new PsiElementResolveResult(resolved)};
        }
        return ResolveResult.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public List<RsElement> multiResolve() {
        RsNamedElement resolved = resolve();
        if (resolved != null) {
            return Collections.singletonList(resolved);
        }
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public RsNamedElement resolve() {
        return RsResolveCache.getInstance(getElement().getProject())
            .resolveWithCaching(getElement(), ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE, RESOLVER);
    }

    @Nullable
    public RsNamedElement resolveIfCached() {
        Object cached = RsResolveCache.getInstance(getElement().getProject())
            .getCached(getElement(), ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE);
        return cached instanceof RsNamedElement ? (RsNamedElement) cached : null;
    }

    private static final Function<RsPath, RsNamedElement> RESOLVER = element -> {
        org.rust.lang.core.psi.ext.RsElement resolved = NameResolution.pickFirstResolveVariant(element.getReferenceName(), processor ->
            NameResolution.processMacroCallPathResolveVariants(element, false, processor)
        );
        return resolved instanceof RsNamedElement ? (RsNamedElement) resolved : null;
    };
}
