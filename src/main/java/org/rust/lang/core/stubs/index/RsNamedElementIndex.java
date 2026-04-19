/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.index;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.openapiext.OpenApiUtil;

import java.util.Collection;

public class RsNamedElementIndex extends StringStubIndexExtension<RsNamedElement> {
    @NotNull
    public static final StubIndexKey<String, RsNamedElement> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RustNamedElementIndex");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @NotNull
    @Override
    public StubIndexKey<String, RsNamedElement> getKey() {
        return KEY;
    }

    @NotNull
    public static Collection<RsNamedElement> findElementsByName(
        @NotNull Project project,
        @NotNull String target,
        @NotNull GlobalSearchScope scope
    ) {
        OpenApiUtil.checkCommitIsNotInProgress(project);
        return OpenApiUtil.getElements(KEY, target, project, scope);
    }

    @NotNull
    public static Collection<RsNamedElement> findElementsByName(@NotNull Project project, @NotNull String target) {
        return findElementsByName(project, target, GlobalSearchScope.allScope(project));
    }
}
