/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import com.intellij.openapi.project.Project;
import com.intellij.psi.stubs.AbstractStubIndex;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.search.RsWithMacrosProjectScope;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.resolve.RsCachedImplItem;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsImplItemStub;
import org.rust.lang.core.types.TyFingerprint;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RsImplIndex extends AbstractStubIndex<TyFingerprint, RsImplItem> {

    private static final StubIndexKey<TyFingerprint, RsImplItem> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RustImplIndex.TraitImpls");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @NotNull
    @Override
    public StubIndexKey<TyFingerprint, RsImplItem> getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public KeyDescriptor<TyFingerprint> getKeyDescriptor() {
        return TyFingerprint.KEY_DESCRIPTOR;
    }

    /**
     * Note this method may return false positives
     * @see TyFingerprint
     */
    @NotNull
    public static List<RsCachedImplItem> findPotentialImpls(@NotNull Project project, @NotNull TyFingerprint tyf) {
        OpenApiUtil.checkCommitIsNotInProgress(project);
        Collection<RsImplItem> impls = OpenApiUtil.getElements(KEY, tyf, project, new RsWithMacrosProjectScope(project));
        List<RsCachedImplItem> result = new ArrayList<>(impls.size());
        for (RsImplItem impl : impls) {
            result.add(RsCachedImplItem.forImpl(impl));
        }
        return result;
    }

    public static void index(@NotNull RsImplItemStub stub, @NotNull IndexSink sink) {
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
