/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.profiler;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.profiler.model.NativeCall;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.nativeDev.profiler.NavigatableSymbolSearcher;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.stubs.index.RsNamedElementIndex;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class RsNavigatableSymbolSearcher implements NavigatableSymbolSearcher {
    @Nonnull
    @RequiredReadAction
    @Override
    public NavigatablePsiElement[] findNavigatableSymbols(@Nonnull NativeCall call, @Nonnull Project project) {
        List<RsFunction> elements = new ArrayList<>();
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
        String qualifiedPath = extractPath(call.getClassName());
        List<RsFunction> allDeclarations = new ArrayList<>();
        for (RsNamedElement element : RsNamedElementIndex.findElementsByName(project, call.getMethodOrFunction(), searchScope)) {
            if (element instanceof RsFunction function) {
                allDeclarations.add(function);
            }
        }
        for (RsFunction declaration : allDeclarations) {
            String qualifiedName = declaration.getQualifiedName();
            if (qualifiedName != null && substringBeforeLast(qualifiedName, "::").equals(qualifiedPath)) {
                elements.add(declaration);
            }
        }
        if (elements.isEmpty()) {
            // TODO: return all or nothing?
            elements.addAll(allDeclarations);
        }
        return elements.toArray(NavigatablePsiElement.EMPTY_ARRAY);
    }

    /**
     * `_<foo::bar123::foo_bar::Qux as baz>::qux` -> `foo::bar123::foo_bar`
     * <p>
     * It doesn't work for crates/modules with inappropriate names (e.g. upper-case)
     */
    @Nonnull
    static String extractPath(@Nonnull String signature) {
        int start = 0;
        while (start < signature.length() && !Character.isLetter(signature.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < signature.length()) {
            char c = signature.charAt(end);
            if (!(Character.isLowerCase(c) || Character.isDigit(c) || c == '_' || c == ':')) {
                break;
            }
            end++;
        }
        String path = signature.substring(start, end);
        return path.endsWith("::") ? path.substring(0, path.length() - 2) : path;
    }

    @Nonnull
    private static String substringBeforeLast(@Nonnull String value, @Nonnull String delimiter) {
        int index = value.lastIndexOf(delimiter);
        return index < 0 ? value : value.substring(0, index);
    }
}
