/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.language.psi.stub.IndexSink;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.impl.RsAbstractableUtil;
import org.rust.lang.core.resolve.indexes.RsImplIndex;
import org.rust.lang.core.resolve.indexes.RsLangItemIndex;
import org.rust.lang.core.resolve.indexes.RsMacroIndex;
import org.rust.lang.core.stubs.index.*;

public final class StubIndexing {
    private StubIndexing() {
    }

    public static void indexExternCrate(@Nonnull IndexSink sink, @Nonnull RsExternCrateItemStub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexStructItem(@Nonnull IndexSink sink, @Nonnull RsStructItemStub stub) {
        indexNamedStub(sink, stub);
        indexGotoClass(sink, stub);
        RsLangItemIndex.index(stub.getPsi(), sink);
    }

    public static void indexEnumItem(@Nonnull IndexSink sink, @Nonnull RsEnumItemStub stub) {
        indexNamedStub(sink, stub);
        indexGotoClass(sink, stub);
    }

    public static void indexEnumVariant(@Nonnull IndexSink sink, @Nonnull RsEnumVariantStub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexModDeclItem(@Nonnull IndexSink sink, @Nonnull RsModDeclItemStub stub) {
        indexNamedStub(sink, stub);
        RsModulesIndex.index(stub, sink);
    }

    public static void indexModItem(@Nonnull IndexSink sink, @Nonnull RsModItemStub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexTraitItem(@Nonnull IndexSink sink, @Nonnull RsTraitItemStub stub) {
        indexNamedStub(sink, stub);
        indexGotoClass(sink, stub);
        RsLangItemIndex.index(stub.getPsi(), sink);
    }

    public static void indexImplItem(@Nonnull IndexSink sink, @Nonnull RsImplItemStub stub) {
        RsImplIndex.index(stub, sink);
    }

    public static void indexTraitAlias(@Nonnull IndexSink sink, @Nonnull RsTraitAliasStub stub) {
        indexNamedStub(sink, stub);
        indexGotoClass(sink, stub);
        RsLangItemIndex.index(stub.getPsi(), sink);
    }

    public static void indexFunction(@Nonnull IndexSink sink, @Nonnull RsFunctionStub stub) {
        indexNamedStub(sink, stub);
        RsLangItemIndex.index(stub.getPsi(), sink);
    }

    public static void indexConstant(@Nonnull IndexSink sink, @Nonnull RsConstantStub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexTypeAlias(@Nonnull IndexSink sink, @Nonnull RsTypeAliasStub stub) {
        indexNamedStub(sink, stub);
        if (!(RsAbstractableUtil.getOwnerBySyntaxOnly(stub.getPsi()) instanceof RsAbstractableOwner.Impl)) {
            indexGotoClass(sink, stub);
        }
    }

    public static void indexNamedFieldDecl(@Nonnull IndexSink sink, @Nonnull RsNamedFieldDeclStub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexMacro(@Nonnull IndexSink sink, @Nonnull RsMacroStub stub) {
        indexNamedStub(sink, stub);
        RsMacroIndex.index(stub, sink);
    }

    public static void indexMacroDef(@Nonnull IndexSink sink, @Nonnull RsMacro2Stub stub) {
        indexNamedStub(sink, stub);
    }

    public static void indexInnerAttr(@Nonnull IndexSink sink, @Nonnull RsInnerAttrStub stub) {
        RsFeatureIndex.index(stub, sink);
    }

    public static void indexMetaItem(@Nonnull IndexSink sink, @Nonnull RsMetaItemStub stub) {
        RsCfgNotTestIndex.index(stub, sink);
    }

    private static void indexNamedStub(@Nonnull IndexSink sink, @Nonnull RsNamedStub stub) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(RsNamedElementIndex.KEY, name);
        }
    }

    private static void indexGotoClass(@Nonnull IndexSink sink, @Nonnull RsNamedStub stub) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(RsGotoClassIndex.KEY, name);
        }
    }
}
