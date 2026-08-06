/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.index;

import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StringStubIndexExtension;
import consulo.language.psi.stub.StubIndexKey;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsInnerAttrStub;
import org.rust.openapiext.OpenApiUtil;

import java.util.Collection;
import java.util.List;

public class RsFeatureIndex extends StringStubIndexExtension<RsInnerAttr> {
    @Nonnull
    private static final StubIndexKey<String, RsInnerAttr> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RsFeatureIndex");

    @Nonnull
    @Override
    public StubIndexKey<String, RsInnerAttr> getKey() {
        return KEY;
    }

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    public static void index(@Nonnull RsInnerAttrStub stub, @Nonnull IndexSink sink) {
        RsMetaItem metaItem = stub.getPsi().getMetaItem();
        indexMetaItem(metaItem, sink);
    }

    private static void indexMetaItem(@Nonnull RsMetaItem metaItem, @Nonnull IndexSink sink) {
        String name = metaItem.getName();
        if ("feature".equals(name)) {
            if (metaItem.getMetaItemArgs() != null) {
                List<RsMetaItem> features = metaItem.getMetaItemArgs().getMetaItemList();
                for (RsMetaItem feature : features) {
                    String featureName = feature.getName();
                    if (featureName != null) {
                        sink.occurrence(KEY, featureName);
                    }
                }
            }
        } else if ("cfg_attr".equals(name)) {
            if (metaItem.getMetaItemArgs() != null) {
                List<RsMetaItem> children = metaItem.getMetaItemArgs().getMetaItemList();
                for (int i = 1; i < children.size(); i++) {
                    indexMetaItem(children.get(i), sink);
                }
            }
        }
    }

    @Nonnull
    public static Collection<RsInnerAttr> getFeatureAttributes(@Nonnull Project project, @Nonnull String featureName) {
        OpenApiUtil.checkCommitIsNotInProgress(project);
        return OpenApiUtil.getElements(KEY, featureName, project, GlobalSearchScope.allScope(project));
    }
}
