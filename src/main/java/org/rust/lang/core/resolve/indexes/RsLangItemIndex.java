/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.AbstractStubIndex;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.CollectionExtUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RsLangItemIndex extends AbstractStubIndex<String, RsItemElement> {

    private static final StubIndexKey<String, RsItemElement> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.resolve.indexes.RsLangItemIndex");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @NotNull
    @Override
    public StubIndexKey<String, RsItemElement> getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Nullable
    public static RsNamedElement findLangItem(@NotNull Project project, @NotNull String langAttribute) {
        return findLangItem(project, langAttribute, AutoInjectedCrates.CORE);
    }

    @Nullable
    public static RsNamedElement findLangItem(@NotNull Project project, @NotNull String langAttribute, @NotNull String crateName) {
        OpenApiUtil.checkCommitIsNotInProgress(project);
        Collection<RsItemElement> elements = OpenApiUtil.getElements(KEY, langAttribute, project, GlobalSearchScope.allScope(project));
        List<RsNamedElement> namedElements = new ArrayList<>();
        for (RsItemElement element : elements) {
            if (element instanceof RsNamedElement) {
                namedElements.add((RsNamedElement) element);
            }
        }
        List<RsNamedElement> filtered = CollectionExtUtil.singleOrFilter(namedElements, it -> it.getContainingCrate().getNormName().equals(crateName));
        List<RsNamedElement> filtered2 = CollectionExtUtil.singleOrFilter(filtered, it -> RsElementUtil.existsAfterExpansion(it));
        return filtered2.isEmpty() ? null : filtered2.get(0);
    }

    public static void index(@NotNull RsItemElement psi, @NotNull IndexSink sink) {
        for (String key : org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil.getTraversedRawAttributes(psi, false).getLangAttributes()) {
            sink.occurrence(KEY, key);
        }
    }
}
