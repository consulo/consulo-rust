/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.AbstractStubIndex;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StubIndexKey;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;

@ExtensionImpl
public class RsLangItemIndex extends AbstractStubIndex<String, RsItemElement> {

    private static final StubIndexKey<String, RsItemElement> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.resolve.indexes.RsLangItemIndex");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @Nonnull
    @Override
    public StubIndexKey<String, RsItemElement> getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Nullable
    public static RsNamedElement findLangItem(@Nonnull Project project, @Nonnull String langAttribute) {
        return findLangItem(project, langAttribute, AutoInjectedCrates.CORE);
    }

    @Nullable
    public static RsNamedElement findLangItem(@Nonnull Project project, @Nonnull String langAttribute, @Nonnull String crateName) {
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

    public static void index(@Nonnull RsItemElement psi, @Nonnull IndexSink sink) {
        for (String key : org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil.getTraversedRawAttributes(psi, false).getLangAttributes()) {
            sink.occurrence(KEY, key);
        }
    }
}
