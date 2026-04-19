/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsStructItemUtil;
import org.rust.lang.core.psi.ext.RsTraitOrImplUtil;
import org.rust.lang.core.psi.ext.RsVisibility;
import org.rust.lang.core.psi.ext.RsVisibilityOwnerUtil;
import org.rust.lang.core.types.Substitution;

import java.util.List;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

public abstract class GenerateAccessorHandler extends BaseGenerateHandler {

    @Override
    protected boolean isStructValid(@NotNull RsStructItem struct) {
        if (RsStructItemUtil.isTupleStruct(struct)) return false;
        if (struct.getBlockFields() == null) return false;
        if (struct.getBlockFields().getNamedFieldDeclList().isEmpty()) return false;
        return true;
    }

    @NotNull
    public abstract String methodName(@NotNull StructMember member);

    @Nullable
    protected abstract List<RsFunction> generateAccessors(
        @NotNull RsStructItem struct,
        @Nullable RsImplItem implBlock,
        @NotNull List<StructMember> chosenFields,
        @NotNull Substitution substitution,
        @NotNull Editor editor
    );

    @Override
    protected void performRefactoring(
        @NotNull RsStructItem struct,
        @Nullable RsImplItem implBlock,
        @NotNull List<StructMember> chosenFields,
        @NotNull Substitution substitution,
        @NotNull Editor editor
    ) {
        List<RsFunction> methods = generateAccessors(struct, implBlock, chosenFields, substitution, editor);
        if (methods != null && !methods.isEmpty()) {
            methods.get(0).navigate(true);
        }
    }

    @Override
    protected boolean isFieldValid(@NotNull StructMember member, @Nullable RsImplItem impl) {
        if (RsVisibilityUtil.getVisibility(member.getField()) == RsVisibility.Public.INSTANCE) return false;
        if (impl != null) {
            String methodName = methodName(member);
            return RsTraitOrImplUtil.getExpandedMembers(impl).stream()
                .noneMatch(it -> methodName.equals(it.getName()));
        }
        return true;
    }
}
