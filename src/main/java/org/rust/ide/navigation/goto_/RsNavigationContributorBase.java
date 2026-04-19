/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.GotoClassContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.search.RsWithMacrosScope;
import org.rust.lang.core.macros.MacroExpansionUtil;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.openapiext.OpenApiUtil;

public abstract class RsNavigationContributorBase<T extends NavigationItem & RsNamedElement>
    implements ChooseByNameContributorEx, GotoClassContributor {

    private static final Logger LOG = Logger.getInstance(RsNavigationContributorBase.class);

    @NotNull
    private final StubIndexKey<String, T> indexKey;
    @NotNull
    private final Class<T> clazz;

    protected RsNavigationContributorBase(@NotNull StubIndexKey<String, T> indexKey, @NotNull Class<T> clazz) {
        this.indexKey = indexKey;
        this.clazz = clazz;
    }

    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        checkFilter(filter);
        StubIndex.getInstance().processAllKeys(
            indexKey,
            processor,
            withMacrosScope(scope),
            null // see `checkFilter`
        );
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        checkFilter(parameters.getIdFilter());
        GlobalSearchScope originScope = parameters.getSearchScope();
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
    public String getQualifiedName(@NotNull NavigationItem item) {
        if (item instanceof RsQualifiedNamedElement) {
            return ((RsQualifiedNamedElement) item).getQualifiedName();
        }
        return null;
    }

    @Override
    @NotNull
    public String getQualifiedNameSeparator() {
        return "::";
    }

    @NotNull
    private static GlobalSearchScope withMacrosScope(@NotNull GlobalSearchScope scope) {
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
