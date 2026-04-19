/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.stubs.RsEnumVariantStub;

import javax.swing.*;

public abstract class RsEnumVariantImplMixin extends RsStubbedNamedElementImpl<RsEnumVariantStub>
    implements RsEnumVariant {

    public RsEnumVariantImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsEnumVariantImplMixin(@NotNull RsEnumVariantStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Override
    public Icon getIcon(int flags) {
        return RsIcons.ENUM_VARIANT;
    }

    @NotNull
    @Override
    public RsVisibility getVisibility() {
        return RsEnumVariantUtil.getParentEnum(this).getVisibility();
    }

    @Override
    public boolean isPublic() {
        return RsEnumVariantUtil.getParentEnum(this).isPublic();
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        String variantName = getName();
        if (variantName == null) return null;
        String parentPath = RsEnumVariantUtil.getParentEnum(this).getCrateRelativePath();
        return parentPath != null ? parentPath + "::" + variantName : null;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
