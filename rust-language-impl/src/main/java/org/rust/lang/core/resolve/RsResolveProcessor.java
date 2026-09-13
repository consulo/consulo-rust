/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.types.SubstitutionUtil;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Type alias equivalent for RsResolveProcessorBase&lt;ScopeEntry&gt;.
 */
public interface RsResolveProcessor extends RsResolveProcessorBase<ScopeEntry> {

    /**
     * Convenience method: process an element with a given name and namespaces.
     */
    default boolean process(@Nonnull String name, @Nonnull Set<Namespace> namespaces, @Nonnull RsElement element) {
        return process(new SimpleScopeEntry(name, element, namespaces, SubstitutionUtil.EMPTY));
    }

    /**
     * Convenience method: process a named element with given namespaces (uses element's name).
     */
    default boolean process(@Nonnull RsNamedElement element, @Nonnull Set<Namespace> namespaces) {
        String name = element.getName();
        if (name == null) return false;
        return process(new SimpleScopeEntry(name, element, namespaces, SubstitutionUtil.EMPTY));
    }

    /**
     * Lazy processing: the element is only resolved if the processor is interested in the given name.
     */
    default boolean lazy(@Nonnull String name, @Nonnull Set<Namespace> namespaces, @Nonnull Supplier<RsElement> elementSupplier) {
        if (!acceptsName(name)) return false;
        RsElement element = elementSupplier.get();
        if (element == null) return false;
        return process(new SimpleScopeEntry(name, element, namespaces, SubstitutionUtil.EMPTY));
    }

    /**
     * Process all named elements with the given namespaces.
     */
    default boolean processAll(@Nonnull Collection<? extends RsNamedElement> elements, @Nonnull Set<Namespace> namespaces) {
        for (RsNamedElement element : elements) {
            if (process(element, namespaces)) return true;
        }
        return false;
    }

    /**
     * Wrap this processor with a filter predicate.
     */
    @Nonnull
    default RsResolveProcessor wrapWithFilter(@Nonnull java.util.function.Predicate<ScopeEntry> filter) {
        RsResolveProcessor delegate = this;
        return new RsResolveProcessor() {
            @Override
            public boolean process(@Nonnull ScopeEntry entry) {
                return filter.test(entry) && delegate.process(entry);
            }

            @Nullable
            @Override
            public Set<String> getNames() {
                return delegate.getNames();
            }
        };
    }
}
