/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.stubs.RsEnumVariantStub;

import javax.swing.*;
import consulo.ui.image.Image;

public abstract class RsEnumVariantImplMixin extends RsStubbedNamedElementImpl<RsEnumVariantStub>
    implements RsEnumVariant {

    public RsEnumVariantImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsEnumVariantImplMixin(@Nonnull RsEnumVariantStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public consulo.ui.image.Image getIcon(int flags) {
        return RsIcons.ENUM_VARIANT;
    }

    @Nonnull
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

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
