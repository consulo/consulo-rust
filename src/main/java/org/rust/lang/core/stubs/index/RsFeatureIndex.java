/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.index;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsInnerAttrStub;
import org.rust.openapiext.OpenApiUtil;

import java.util.Collection;
import java.util.List;

public class RsFeatureIndex extends StringStubIndexExtension<RsInnerAttr> {
    @NotNull
    private static final StubIndexKey<String, RsInnerAttr> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RsFeatureIndex");

    @NotNull
    @Override
    public StubIndexKey<String, RsInnerAttr> getKey() {
        return KEY;
    }

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    public static void index(@NotNull RsInnerAttrStub stub, @NotNull IndexSink sink) {
        RsMetaItem metaItem = stub.getPsi().getMetaItem();
        indexMetaItem(metaItem, sink);
    }

    private static void indexMetaItem(@NotNull RsMetaItem metaItem, @NotNull IndexSink sink) {
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

    @NotNull
    public static Collection<RsInnerAttr> getFeatureAttributes(@NotNull Project project, @NotNull String featureName) {
        OpenApiUtil.checkCommitIsNotInProgress(project);
        return OpenApiUtil.getElements(KEY, featureName, project, GlobalSearchScope.allScope(project));
    }
}
