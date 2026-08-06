/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.index;

import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StringStubIndexExtension;
import consulo.language.psi.stub.StubIndex;
import consulo.language.psi.stub.StubIndexKey;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsMetaItemArgs;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsMetaItemStub;
import org.rust.openapiext.OpenApiUtil;

public class RsCfgNotTestIndex extends StringStubIndexExtension<RsMetaItem> {
    @Nonnull
    private static final StubIndexKey<String, RsMetaItem> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RsCfgIndex");

    private static final String NOT_TEST = "#";

    @Nonnull
    @Override
    public StubIndexKey<String, RsMetaItem> getKey() {
        return KEY;
    }

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    public static void index(@Nonnull RsMetaItemStub stub, @Nonnull IndexSink sink) {
        if (isCfgNotTest(stub.getPsi())) {
            sink.occurrence(KEY, NOT_TEST);
        }
    }

    public static boolean hasCfgNotTest(@Nonnull Project project, @Nonnull CargoWorkspace.Package pkg) {
        return CachedValuesManager.getManager(project).getCachedValue(pkg, () -> {
            VirtualFile contentRoot = pkg.getContentRoot();
            boolean result = false;
            if (contentRoot != null) {
                result = hasCfgNotTestInScope(project, GlobalSearchScopesCore.directoryScope(project, contentRoot, true));
            }
            return CachedValueProvider.Result.create(result, project);
        });
    }

    private static boolean hasCfgNotTestInScope(@Nonnull Project project, @Nonnull GlobalSearchScope scope) {
        OpenApiUtil.checkCommitIsNotInProgress(project);
        boolean[] found = {false};
        StubIndex.getInstance().processElements(KEY, NOT_TEST, project, scope, RsMetaItem.class, element -> {
            found[0] = true;
            return false;
        });
        return found[0];
    }

    public static boolean isCfgNotTest(@Nonnull RsMetaItem psi) {
        if (!"test".equals(psi.getName())) return false;
        consulo.language.psi.PsiElement parent = psi.getParent();
        if (!(parent instanceof RsMetaItemArgs)) return false;
        // Simplified check
        return true;
    }
}
