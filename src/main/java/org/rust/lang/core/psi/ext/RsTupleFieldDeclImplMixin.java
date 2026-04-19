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
import org.rust.lang.core.psi.RsTupleFieldDecl;
import org.rust.lang.core.stubs.RsPlaceholderStub;

import javax.swing.*;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

public abstract class RsTupleFieldDeclImplMixin extends RsStubbedElementImpl<RsPlaceholderStub<?>> implements RsTupleFieldDecl {

    public RsTupleFieldDeclImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsTupleFieldDeclImplMixin(@NotNull RsPlaceholderStub<?> stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Override
    public Icon getIcon(int flags) {
        RsFieldsOwner owner = RsFieldDeclUtil.getOwner(this);
        if (owner instanceof RsEnumVariant) {
            return RsIcons.FIELD;
        }
        return RsVisibilityUtil.iconWithVisibility(this, flags, RsIcons.FIELD);
    }

    @Nullable
    @Override
    public String getName() {
        Integer pos = RsTupleFieldDeclUtil.getPosition(this);
        return pos != null ? pos.toString() : null;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
