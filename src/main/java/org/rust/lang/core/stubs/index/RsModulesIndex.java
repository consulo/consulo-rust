/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.index;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.util.PathUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.search.RsWithMacrosProjectScope;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsModDeclItemStub;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.CollectionsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RsModulesIndex extends StringStubIndexExtension<RsModDeclItem> {
    @NotNull
    private static final StubIndexKey<String, RsModDeclItem> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RustModulesIndex");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @NotNull
    @Override
    public StubIndexKey<String, RsModDeclItem> getKey() {
        return KEY;
    }

    @NotNull
    public static List<RsModDeclItem> getDeclarationsFor(@NotNull RsFile mod) {
        String key = key(mod);
        if (key == null) return Collections.emptyList();
        OpenApiUtil.checkCommitIsNotInProgress(mod.getProject());

        SmartList<RsModDeclItem> result = new SmartList<>();
        RsWithMacrosProjectScope scope = new RsWithMacrosProjectScope(mod.getProject());

        StubIndex.getInstance().processElements(
            KEY, key, mod.getProject(), scope, RsModDeclItem.class,
            modDecl -> {
                if (modDecl.getReference().resolve() == mod) {
                    result.add(modDecl);
                }
                return true;
            }
        );

        if (RsConstants.MOD_RS_FILE.equals(mod.getName()) && result.isEmpty()) {
            StubIndex.getInstance().processElements(
                KEY, "", mod.getProject(), scope, RsModDeclItem.class,
                modDecl -> {
                    if (modDecl.getReference().resolve() == mod) {
                        result.add(modDecl);
                    }
                    return true;
                }
            );
        }

        List<RsModDeclItem> filtered = CollectionsUtil.singleOrFilter(result, modDecl -> org.rust.lang.core.psi.ext.RsElementUtil.existsAfterExpansion(modDecl));
        return filtered.isEmpty() ? result : filtered;
    }

    public static void index(@NotNull RsModDeclItemStub stub, @NotNull IndexSink indexSink) {
        String name = stub.getName();
        if (name != null) {
            indexSink.occurrence(KEY, name.toLowerCase(Locale.ROOT));
        }
    }

    @Nullable
    private static String key(@NotNull RsFile mod) {
        String name;
        if (!RsConstants.MOD_RS_FILE.equals(mod.getName())) {
            name = FileUtil.getNameWithoutExtension(mod.getName());
        } else {
            name = mod.getParent() != null ? mod.getParent().getName() : null;
        }
        return name != null ? name.toLowerCase(Locale.ROOT) : null;
    }
}
