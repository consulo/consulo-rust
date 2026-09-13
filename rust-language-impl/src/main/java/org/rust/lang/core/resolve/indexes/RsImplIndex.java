/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;


import consulo.project.Project;
import consulo.language.psi.stub.AbstractStubIndex;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StubIndexKey;
import consulo.index.io.KeyDescriptor;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.search.RsWithMacrosProjectScope;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.impl.RsGenericDeclarationUtil;
import org.rust.lang.core.resolve.RsCachedImplItem;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsImplItemStub;
import org.rust.lang.core.types.TyFingerprint;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.psi.RsTypeReference;

@ExtensionImpl
public class RsImplIndex extends AbstractStubIndex<TyFingerprint, RsImplItem> {

    private static final StubIndexKey<TyFingerprint, RsImplItem> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RustImplIndex.TraitImpls");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @Nonnull
    @Override
    public StubIndexKey<TyFingerprint, RsImplItem> getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public KeyDescriptor<TyFingerprint> getKeyDescriptor() {
        return TyFingerprint.KEY_DESCRIPTOR;
    }

    /**
     * Note this method may return false positives
     * @see TyFingerprint
     */
    @Nonnull
    public static List<RsCachedImplItem> findPotentialImpls(@Nonnull Project project, @Nonnull TyFingerprint tyf) {
        OpenApiUtil.checkCommitIsNotInProgress(project);
        Collection<RsImplItem> impls = OpenApiUtil.getElements(KEY, tyf, project, new RsWithMacrosProjectScope(project));
        List<RsCachedImplItem> result = new ArrayList<>(impls.size());
        for (RsImplItem impl : impls) {
            result.add(RsCachedImplItem.forImpl(impl));
        }
        return result;
    }

    public static void index(@Nonnull RsImplItemStub stub, @Nonnull IndexSink sink) {
        RsImplItem impl = stub.getPsi();
        org.rust.lang.core.psi.RsTypeReference typeRef = impl.getTypeReference();
        if (typeRef == null) return;
        List<String> typeParamNames = new ArrayList<>();
        for (org.rust.lang.core.psi.RsTypeParameter tp : RsGenericDeclarationUtil.getTypeParameters(impl)) {
            String name = tp.getName();
            if (name != null) {
                typeParamNames.add(name);
            }
        }
        for (TyFingerprint tyf : TyFingerprint.create(typeRef, typeParamNames)) {
            sink.occurrence(KEY, tyf);
        }
    }
}
