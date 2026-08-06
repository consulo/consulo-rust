/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate.constructor;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.refactoring.generate.BaseGenerateHandler;
import org.rust.ide.refactoring.generate.StructMember;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsStructItemUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.openapiext.OpenApiUtil;

import java.util.List;
import java.util.stream.Collectors;

public class GenerateConstructorHandler extends BaseGenerateHandler {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Nonnull
    @Override
    protected String getDialogTitle() {
        return "Select constructor parameters";
    }

    @Override
    public boolean getAllowEmptySelection() {
        return true;
    }

    @Override
    protected boolean isImplBlockValid(@Nonnull RsImplItem impl) {
        return super.isImplBlockValid(impl) && isSuitableForConstructor(impl);
    }

    @Override
    protected boolean allowEmptyFields() {
        return true;
    }

    @Override
    protected void performRefactoring(
        @Nonnull RsStructItem struct,
        @Nullable RsImplItem implBlock,
        @Nonnull List<StructMember> chosenFields,
        @Nonnull Substitution substitution,
        @Nonnull Editor editor
    ) {
        org.rust.openapiext.OpenApiUtil.checkWriteAccessAllowed();
        Project project = editor.getProject();
        if (project == null) return;
        String structName = struct.getName();
        if (structName == null) return;
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        RsImplItem impl = getOrCreateImplBlock(implBlock, psiFactory, structName, struct);

        consulo.language.psi.PsiElement anchor = impl.getLastChild().getLastChild();
        RsFunction constructor = createConstructor(struct, chosenFields, psiFactory, substitution);
        consulo.language.psi.PsiElement inserted = impl.getLastChild().addBefore(constructor, anchor);
        editor.getCaretModel().moveToOffset(inserted.getTextRange().getEndOffset());
    }

    @Nonnull
    private RsFunction createConstructor(
        @Nonnull RsStructItem structItem,
        @Nonnull List<StructMember> selectedFields,
        @Nonnull RsPsiFactory psiFactory,
        @Nonnull Substitution substitution
    ) {
        String arguments = selectedFields.stream()
            .map(f -> f.getArgumentIdentifier() + ": " + f.getTypeReferenceText())
            .collect(Collectors.joining(",", "(", ")"));

        String body = generateBody(structItem, selectedFields, substitution);
        return psiFactory.createTraitMethodMember("pub fn new" + arguments + "->Self{\n" + body + "}\n");
    }

    @Nonnull
    private String generateBody(
        @Nonnull RsStructItem structItem,
        @Nonnull List<StructMember> selectedFields,
        @Nonnull Substitution substitution
    ) {
        boolean isTuple = RsStructItemUtil.isTupleStruct(structItem);
        String prefix = isTuple ? "(" : "{";
        String postfix = isTuple ? ")" : "}";
        List<StructMember> allFields = StructMember.fromStruct(structItem, substitution);
        String arguments = allFields.stream()
            .map(f -> selectedFields.contains(f) ? f.getArgumentIdentifier() : f.getFieldIdentifier())
            .collect(Collectors.joining(",", prefix, postfix));
        return "Self" + arguments;
    }

    private static boolean isSuitableForConstructor(@Nonnull RsImplItem impl) {
        if (impl.getTraitRef() != null) return false;
        if (impl.getMembers() == null) return false;
        List<RsFunction> functions = PsiElementUtil.childrenOfType(impl.getMembers(), RsFunction.class);
        for (RsFunction fn : functions) {
            if ("new".equals(fn.getName())) return false;
        }
        return true;
    }
}
