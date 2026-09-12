/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.index;

import consulo.util.io.FileUtil;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StringStubIndexExtension;
import consulo.language.psi.stub.StubIndex;
import consulo.language.psi.stub.StubIndexKey;
import consulo.util.io.PathUtil;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.ext.RsElementUtil;

@ExtensionImpl
public class RsModulesIndex extends StringStubIndexExtension<RsModDeclItem> {
    @Nonnull
    private static final StubIndexKey<String, RsModDeclItem> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RustModulesIndex");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @Nonnull
    @Override
    public StubIndexKey<String, RsModDeclItem> getKey() {
        return KEY;
    }

    @Nonnull
    public static List<RsModDeclItem> getDeclarationsFor(@Nonnull RsFile mod) {
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

    public static void index(@Nonnull RsModDeclItemStub stub, @Nonnull IndexSink indexSink) {
        String name = stub.getName();
        if (name != null) {
            indexSink.occurrence(KEY, name.toLowerCase(Locale.ROOT));
        }
    }

    @Nullable
    private static String key(@Nonnull RsFile mod) {
        String name;
        if (!RsConstants.MOD_RS_FILE.equals(mod.getName())) {
            name = FileUtil.getNameWithoutExtension(mod.getName());
        } else {
            name = mod.getParent() != null ? mod.getParent().getName() : null;
        }
        return name != null ? name.toLowerCase(Locale.ROOT) : null;
    }
}
