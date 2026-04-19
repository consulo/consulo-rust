/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.RsCachedImplItem;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsImplItemUtil;
import org.rust.lang.core.psi.ext.RsTypeReferenceUtil;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public abstract class BaseGenerateHandler implements LanguageCodeInsightActionHandler {

    @Override
    public boolean isValidFor(@NotNull Editor editor, @NotNull PsiFile file) {
        return getContext(editor, file) != null;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    public boolean getAllowEmptySelection() {
        return false;
    }

    @Nullable
    private GenerateContext getContext(@NotNull Editor editor, @NotNull PsiFile file) {
        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        if (element == null) return null;

        RsStructItem struct = RsElementUtil.ancestorOrSelf(element, RsStructItem.class);
        RsStructItem structItem;
        RsImplItem impl;

        if (struct != null) {
            structItem = struct;
            impl = null;
        } else {
            RsItemElement item = RsElementUtil.ancestorStrict(element, RsItemElement.class);
            if (!(item instanceof RsImplItem)) return null;
            impl = (RsImplItem) item;

            if (!isImplBlockValid(impl)) return null;

            RsTypeReference typeRef = impl.getTypeReference();
            if (typeRef == null) return null;
            PsiElement skipped = RsTypeReferenceUtil.skipParens(typeRef);
            if (!(skipped instanceof RsPathType)) return null;
            PsiElement resolved = ((RsPathType) skipped).getPath().getReference().resolve();
            if (!(resolved instanceof RsStructItem)) return null;
            structItem = (RsStructItem) resolved;
        }

        if (!isStructValid(structItem)) return null;
        Substitution substitution = impl != null && impl.getTypeReference() != null
            ? RsTypesUtil.getRawType(impl.getTypeReference()).getTypeParameterValues()
            : SubstitutionUtil.getEmptySubstitution();
        RsImplItem finalImpl = impl;
        List<StructMember> fields = StructMember.fromStruct(structItem, substitution).stream()
            .filter(f -> isFieldValid(f, finalImpl))
            .collect(Collectors.toList());
        if (fields.isEmpty() && !allowEmptyFields()) return null;

        return new GenerateContext(structItem, fields, substitution, impl);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        GenerateContext context = getContext(editor, file);
        if (context == null) return;
        selectMembers(context, editor);
    }

    private void selectMembers(@NotNull GenerateContext context, @NotNull Editor editor) {
        org.rust.openapiext.OpenApiUtil.checkWriteAccessNotAllowed();

        List<StructMember> chosenFields = StructMemberChooserUi.showStructMemberChooserDialog(
            context.getStruct().getProject(),
            context.getStruct(),
            context.getFields(),
            getDialogTitle(),
            getAllowEmptySelection()
        );
        if (chosenFields == null) return;

        ApplicationManager.getApplication().runWriteAction(() -> {
            performRefactoring(context.getStruct(), context.getImplBlock(), chosenFields, context.getSubstitution(), editor);
        });
    }

    @NotNull
    protected RsImplItem getOrCreateImplBlock(
        @Nullable RsImplItem implBlock,
        @NotNull RsPsiFactory psiFactory,
        @NotNull String structName,
        @NotNull RsStructItem struct
    ) {
        if (implBlock == null) {
            RsImplItem sibling = findSiblingImplItem(struct);
            if (sibling != null) return sibling;

            RsImplItem impl = psiFactory.createInherentImplItem(structName, struct.getTypeParameterList(), struct.getWhereClause());
            return (RsImplItem) struct.getParent().addAfter(impl, struct);
        } else {
            return implBlock;
        }
    }

    protected boolean isImplBlockValid(@NotNull RsImplItem impl) {
        return impl.getTraitRef() == null;
    }

    protected boolean isStructValid(@NotNull RsStructItem struct) {
        return true;
    }

    protected boolean isFieldValid(@NotNull StructMember member, @Nullable RsImplItem impl) {
        return true;
    }

    protected boolean allowEmptyFields() {
        return false;
    }

    protected abstract void performRefactoring(
        @NotNull RsStructItem struct,
        @Nullable RsImplItem implBlock,
        @NotNull List<StructMember> chosenFields,
        @NotNull Substitution substitution,
        @NotNull Editor editor
    );

    @NotNull
    protected abstract String getDialogTitle();

    @Nullable
    private static RsImplItem findSiblingImplItem(@NotNull RsStructItem struct) {
        RsItemsOwner owner = RsElementUtil.contextStrict(struct, RsItemsOwner.class);
        if (owner == null) return null;
        List<RsImplItem> impls = PsiElementUtil.childrenOfType(owner, RsImplItem.class);
        for (RsImplItem impl : impls) {
            RsCachedImplItem cachedImpl = RsCachedImplItem.forImpl(impl);
            Ty cachedType = cachedImpl.getType();
            java.util.List<?> cachedGenerics = cachedImpl.getGenerics();
            java.util.List<?> cachedConstGenerics = cachedImpl.getConstGenerics();
            if (cachedType == null) continue;
            if (cachedImpl.isInherent() && cachedImpl.isValid() && !cachedImpl.isNegativeImpl()
                && (cachedGenerics == null || cachedGenerics.isEmpty()) && (cachedConstGenerics == null || cachedConstGenerics.isEmpty())
                && cachedType.isEquivalentTo(struct.getDeclaredType())) {
                return impl;
            }
        }
        return null;
    }

    public static class GenerateContext {
        @NotNull
        private final RsStructItem myStruct;
        @NotNull
        private final List<StructMember> myFields;
        @NotNull
        private final Substitution mySubstitution;
        @Nullable
        private final RsImplItem myImplBlock;

        public GenerateContext(
            @NotNull RsStructItem struct,
            @NotNull List<StructMember> fields,
            @NotNull Substitution substitution,
            @Nullable RsImplItem implBlock
        ) {
            myStruct = struct;
            myFields = fields;
            mySubstitution = substitution;
            myImplBlock = implBlock;
        }

        @NotNull
        public RsStructItem getStruct() {
            return myStruct;
        }

        @NotNull
        public List<StructMember> getFields() {
            return myFields;
        }

        @NotNull
        public Substitution getSubstitution() {
            return mySubstitution;
        }

        @Nullable
        public RsImplItem getImplBlock() {
            return myImplBlock;
        }
    }
}
