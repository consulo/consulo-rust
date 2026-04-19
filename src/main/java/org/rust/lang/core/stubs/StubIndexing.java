/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.IndexSink;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.RsAbstractableUtil;
import org.rust.lang.core.resolve.indexes.RsImplIndex;
import org.rust.lang.core.resolve.indexes.RsLangItemIndex;
import org.rust.lang.core.resolve.indexes.RsMacroIndex;
import org.rust.lang.core.stubs.index.*;

public final class StubIndexing {
    private StubIndexing() {
    }

    public static void indexExternCrate(@NotNull IndexSink sink, @NotNull RsExternCrateItemStub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexStructItem(@NotNull IndexSink sink, @NotNull RsStructItemStub stub) {
        indexNamedStub(sink, stub);
        indexGotoClass(sink, stub);
        RsLangItemIndex.index(stub.getPsi(), sink);
    }

    public static void indexEnumItem(@NotNull IndexSink sink, @NotNull RsEnumItemStub stub) {
        indexNamedStub(sink, stub);
        indexGotoClass(sink, stub);
    }

    public static void indexEnumVariant(@NotNull IndexSink sink, @NotNull RsEnumVariantStub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexModDeclItem(@NotNull IndexSink sink, @NotNull RsModDeclItemStub stub) {
        indexNamedStub(sink, stub);
        RsModulesIndex.index(stub, sink);
    }

    public static void indexModItem(@NotNull IndexSink sink, @NotNull RsModItemStub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexTraitItem(@NotNull IndexSink sink, @NotNull RsTraitItemStub stub) {
        indexNamedStub(sink, stub);
        indexGotoClass(sink, stub);
        RsLangItemIndex.index(stub.getPsi(), sink);
    }

    public static void indexImplItem(@NotNull IndexSink sink, @NotNull RsImplItemStub stub) {
        RsImplIndex.index(stub, sink);
    }

    public static void indexTraitAlias(@NotNull IndexSink sink, @NotNull RsTraitAliasStub stub) {
        indexNamedStub(sink, stub);
        indexGotoClass(sink, stub);
        RsLangItemIndex.index(stub.getPsi(), sink);
    }

    public static void indexFunction(@NotNull IndexSink sink, @NotNull RsFunctionStub stub) {
        indexNamedStub(sink, stub);
        RsLangItemIndex.index(stub.getPsi(), sink);
    }

    public static void indexConstant(@NotNull IndexSink sink, @NotNull RsConstantStub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexTypeAlias(@NotNull IndexSink sink, @NotNull RsTypeAliasStub stub) {
        indexNamedStub(sink, stub);
        if (!(RsAbstractableUtil.getOwnerBySyntaxOnly(stub.getPsi()) instanceof RsAbstractableOwner.Impl)) {
            indexGotoClass(sink, stub);
        }
    }

    public static void indexNamedFieldDecl(@NotNull IndexSink sink, @NotNull RsNamedFieldDeclStub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexMacro(@NotNull IndexSink sink, @NotNull RsMacroStub stub) {
        indexNamedStub(sink, stub);
        RsMacroIndex.index(stub, sink);
    }

    public static void indexMacroDef(@NotNull IndexSink sink, @NotNull RsMacro2Stub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexInnerAttr(@NotNull IndexSink sink, @NotNull RsInnerAttrStub stub) {
        RsFeatureIndex.index(stub, sink);
    }

    public static void indexMetaItem(@NotNull IndexSink sink, @NotNull RsMetaItemStub stub) {
        RsCfgNotTestIndex.index(stub, sink);
    }

    private static void indexNamedStub(@NotNull IndexSink sink, @NotNull RsNamedStub stub) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(RsNamedElementIndex.KEY, name);
        }
    }

    private static void indexGotoClass(@NotNull IndexSink sink, @NotNull RsNamedStub stub) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(RsGotoClassIndex.KEY, name);
        }
    }
}
