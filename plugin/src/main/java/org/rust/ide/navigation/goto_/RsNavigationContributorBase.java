/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import consulo.ide.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.GotoClassContributor;
import consulo.navigation.NavigationItem;
import consulo.logging.Logger;
import consulo.language.psi.scope.EverythingGlobalScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.StubIndex;
import consulo.language.psi.stub.StubIndexKey;
import consulo.application.util.function.Processor;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.IdFilter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.search.RsWithMacrosScope;
import org.rust.lang.core.macros.MacroExpansionUtil;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.openapiext.OpenApiUtil;

public abstract class RsNavigationContributorBase<T extends NavigationItem & RsNamedElement>
    implements ChooseByNameContributorEx, GotoClassContributor {

    private static final Logger LOG = Logger.getInstance(RsNavigationContributorBase.class);

    @Nonnull
    private final StubIndexKey<String, T> indexKey;
    @Nonnull
    private final Class<T> clazz;

    protected RsNavigationContributorBase(@Nonnull StubIndexKey<String, T> indexKey, @Nonnull Class<T> clazz) {
        this.indexKey = indexKey;
        this.clazz = clazz;
    }

    @Override
    public void processNames(@Nonnull Processor<String> processor, @Nonnull consulo.content.scope.SearchScope scope, @Nullable IdFilter filter) {
        checkFilter(filter);
        StubIndex.getInstance().processAllKeys(
            indexKey,
            processor,
            withMacrosScope((GlobalSearchScope) scope),
            null // see `checkFilter`
        );
    }

    @Override
    public void processElementsWithName(@Nonnull String name, @Nonnull Processor<NavigationItem> processor, @Nonnull FindSymbolParameters parameters) {
        checkFilter(parameters.getIdFilter());
        GlobalSearchScope originScope = (GlobalSearchScope) parameters.getSearchScope();
        StubIndex.getInstance().processElements(
            indexKey,
            name,
            parameters.getProject(),
            withMacrosScope(originScope),
            null, // see `checkFilter`
            clazz,
            element -> {
                // Filter out elements expanded from macros that are not in the scope
                var macroCall = MacroExpansionUtil.findMacroCallExpandedFrom(element);
                var macroVFile = macroCall != null
                    ? RsElementUtil.contextualFile(macroCall).getOriginalFile().getVirtualFile()
                    : null;
                if (macroVFile == null || originScope.contains(macroVFile)) {
                    return processor.process(element);
                } else {
                    return true;
                }
            }
        );
    }

    @Override
    @Nullable
    public String getQualifiedName(@Nonnull NavigationItem item) {
        if (item instanceof RsQualifiedNamedElement) {
            return ((RsQualifiedNamedElement) item).getQualifiedName();
        }
        return null;
    }

    @Override
    @Nonnull
    public String getQualifiedNameSeparator() {
        return "::";
    }

    @Nonnull
    private static GlobalSearchScope withMacrosScope(@Nonnull GlobalSearchScope scope) {
        var project = scope.getProject();
        if (project != null && !(scope instanceof EverythingGlobalScope)) {
            return new RsWithMacrosScope(project, scope);
        }
        return scope;
    }

    /**
     * {@link IdFilter} exists only for optimization purposes and can safely be null. If it is not null, we should
     * refine it in the same way as a scope in {@link #withMacrosScope}. But looks like in 2019.2 it's always null,
     * so I can't even test the solution. I decided to always use {@code null} as a filter and enable this check
     * (in the internal mode only) to catch the situation when it will become non null.
     */
    private static void checkFilter(@Nullable IdFilter filter) {
        if (OpenApiUtil.isInternal() && filter != null) {
            LOG.error("IdFilter is supposed to be null", new Throwable());
        }
    }
}
