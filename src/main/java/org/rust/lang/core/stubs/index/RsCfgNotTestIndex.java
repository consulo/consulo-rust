/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.index;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsMetaItemArgs;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsMetaItemStub;
import org.rust.openapiext.OpenApiUtil;

public class RsCfgNotTestIndex extends StringStubIndexExtension<RsMetaItem> {
    @NotNull
    private static final StubIndexKey<String, RsMetaItem> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RsCfgIndex");

    private static final String NOT_TEST = "#";

    @NotNull
    @Override
    public StubIndexKey<String, RsMetaItem> getKey() {
        return KEY;
    }

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    public static void index(@NotNull RsMetaItemStub stub, @NotNull IndexSink sink) {
        if (isCfgNotTest(stub.getPsi())) {
            sink.occurrence(KEY, NOT_TEST);
        }
    }

    public static boolean hasCfgNotTest(@NotNull Project project, @NotNull CargoWorkspace.Package pkg) {
        return CachedValuesManager.getManager(project).getCachedValue(pkg, () -> {
            VirtualFile contentRoot = pkg.getContentRoot();
            boolean result = false;
            if (contentRoot != null) {
                result = hasCfgNotTestInScope(project, new GlobalSearchScopesCore.DirectoryScope(project, contentRoot, true));
            }
            return CachedValueProvider.Result.create(result, project);
        });
    }

    private static boolean hasCfgNotTestInScope(@NotNull Project project, @NotNull GlobalSearchScope scope) {
        OpenApiUtil.checkCommitIsNotInProgress(project);
        boolean[] found = {false};
        StubIndex.getInstance().processElements(KEY, NOT_TEST, project, scope, RsMetaItem.class, element -> {
            found[0] = true;
            return false;
        });
        return found[0];
    }

    public static boolean isCfgNotTest(@NotNull RsMetaItem psi) {
        if (!"test".equals(psi.getName())) return false;
        com.intellij.psi.PsiElement parent = psi.getParent();
        if (!(parent instanceof RsMetaItemArgs)) return false;
        // Simplified check
        return true;
    }
}
