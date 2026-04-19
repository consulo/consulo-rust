/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsReferenceElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class RsReferenceCached<T extends RsReferenceElement> extends RsReferenceBase<T> {

    public RsReferenceCached(@NotNull T element) {
        super(element);
    }

    @NotNull
    protected abstract List<RsElement> resolveInner();

    @NotNull
    @Override
    public final ResolveResult[] multiResolve(boolean incompleteCode) {
        List<PsiElementResolveResult> results = cachedMultiResolve();
        return results.toArray(ResolveResult.EMPTY_ARRAY);
    }

    @NotNull
    @Override
    public final List<RsElement> multiResolve() {
        List<PsiElementResolveResult> results = cachedMultiResolve();
        List<RsElement> elements = new ArrayList<>(results.size());
        for (PsiElementResolveResult result : results) {
            if (result.getElement() instanceof RsElement) {
                elements.add((RsElement) result.getElement());
            }
        }
        return elements;
    }

    @NotNull
    private List<PsiElementResolveResult> cachedMultiResolve() {
        List<PsiElementResolveResult> result = RsResolveCache.getInstance(getElement().getProject())
            .resolveWithCaching(getElement(), getCacheDependency(), RESOLVER);
        return result != null ? result : Collections.emptyList();
    }

    @NotNull
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE;
    }

    private static final Function<RsReferenceElement, List<PsiElementResolveResult>> RESOLVER = ref -> {
        RsReference reference = ref.getReference();
        if (reference instanceof RsReferenceCached) {
            List<RsElement> inner = ((RsReferenceCached<?>) reference).resolveInner();
            List<PsiElementResolveResult> results = new ArrayList<>(inner.size());
            for (RsElement element : inner) {
                results.add(new PsiElementResolveResult(element));
            }
            return results;
        }
        return Collections.emptyList();
    };
}
