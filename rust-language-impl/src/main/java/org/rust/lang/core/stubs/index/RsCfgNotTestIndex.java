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
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.impl.RsPsiManagerUtil;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsMetaItemArgs;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import org.rust.lang.core.psi.ext.impl.RsMetaItemUtil;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsMetaItemStub;
import org.rust.openapiext.OpenApiUtil;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
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
            return CachedValueProvider.Result.create(
                result, RsPsiManagerUtil.getRustStructureModificationTracker(project));
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

    /**
     * {@code true} for the {@code test} meta item of {@code #[cfg(not(test))]} and of any deeper
     * negated cfg condition such as {@code #[cfg(not(and(or(test, ...), ...)))]}.
     */
    public static boolean isCfgNotTest(@Nonnull RsMetaItem psi) {
        if (!"test".equals(RsMetaItemUtil.getName(psi))) return false;
        PsiElement parent = PsiElementUtil.getStubParent(psi);
        if (!(parent instanceof RsMetaItemArgs)) return false;

        PsiElement not = null;
        for (PsiElement it = parent; it != null; it = stubAncestor(it)) {
            if (it instanceof RsMetaItem && "not".equals(RsMetaItemUtil.getName((RsMetaItem) it))) {
                not = it;
                break;
            }
        }
        if (not == null) return false;

        for (PsiElement it = not; it != null; it = stubAncestor(it)) {
            if (it instanceof RsMetaItem && RsPsiPattern.anyCfgCondition.accepts(it)) return true;
        }
        return false;
    }

    @Nullable
    private static PsiElement stubAncestor(@Nonnull PsiElement element) {
        return element instanceof PsiFile ? null : PsiElementUtil.getStubParent(element);
    }
}
