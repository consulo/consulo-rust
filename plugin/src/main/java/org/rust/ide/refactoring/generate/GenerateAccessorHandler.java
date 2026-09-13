/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import consulo.codeEditor.Editor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.impl.RsStructItemUtil;
import org.rust.lang.core.psi.ext.impl.RsTraitOrImplUtil;
import org.rust.lang.core.psi.ext.RsVisibility;
import org.rust.lang.core.psi.ext.impl.RsVisibilityOwnerUtil;
import org.rust.lang.core.types.Substitution;

import java.util.List;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;

public abstract class GenerateAccessorHandler extends BaseGenerateHandler {

    @Override
    protected boolean isStructValid(@Nonnull RsStructItem struct) {
        if (RsStructItemUtil.isTupleStruct(struct)) return false;
        if (struct.getBlockFields() == null) return false;
        if (struct.getBlockFields().getNamedFieldDeclList().isEmpty()) return false;
        return true;
    }

    @Nonnull
    public abstract String methodName(@Nonnull StructMember member);

    @Nullable
    protected abstract List<RsFunction> generateAccessors(
        @Nonnull RsStructItem struct,
        @Nullable RsImplItem implBlock,
        @Nonnull List<StructMember> chosenFields,
        @Nonnull Substitution substitution,
        @Nonnull Editor editor
    );

    @Override
    protected void performRefactoring(
        @Nonnull RsStructItem struct,
        @Nullable RsImplItem implBlock,
        @Nonnull List<StructMember> chosenFields,
        @Nonnull Substitution substitution,
        @Nonnull Editor editor
    ) {
        List<RsFunction> methods = generateAccessors(struct, implBlock, chosenFields, substitution, editor);
        if (methods != null && !methods.isEmpty()) {
            methods.get(0).navigate(true);
        }
    }

    @Override
    protected boolean isFieldValid(@Nonnull StructMember member, @Nullable RsImplItem impl) {
        if (RsVisibilityUtil.getVisibility(member.getField()) == RsVisibility.Public.INSTANCE) return false;
        if (impl != null) {
            String methodName = methodName(member);
            return RsTraitOrImplUtil.getExpandedMembers(impl).stream()
                .noneMatch(it -> methodName.equals(it.getName()));
        }
        return true;
    }
}
