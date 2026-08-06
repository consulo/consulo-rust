/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.index;

import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.StringStubIndexExtension;
import consulo.language.psi.stub.StubIndexKey;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.openapiext.OpenApiUtil;

import java.util.Collection;

public class RsNamedElementIndex extends StringStubIndexExtension<RsNamedElement> {
    @Nonnull
    public static final StubIndexKey<String, RsNamedElement> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RustNamedElementIndex");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @Nonnull
    @Override
    public StubIndexKey<String, RsNamedElement> getKey() {
        return KEY;
    }

    @Nonnull
    public static Collection<RsNamedElement> findElementsByName(
        @Nonnull Project project,
        @Nonnull String target,
        @Nonnull GlobalSearchScope scope
    ) {
        OpenApiUtil.checkCommitIsNotInProgress(project);
        return OpenApiUtil.getElements(KEY, target, project, scope);
    }

    @Nonnull
    public static Collection<RsNamedElement> findElementsByName(@Nonnull Project project, @Nonnull String target) {
        return findElementsByName(project, target, GlobalSearchScope.allScope(project));
    }
}
