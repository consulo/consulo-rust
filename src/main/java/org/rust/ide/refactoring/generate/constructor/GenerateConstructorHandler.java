/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate.constructor;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    @Override
    protected String getDialogTitle() {
        return "Select constructor parameters";
    }

    @Override
    public boolean getAllowEmptySelection() {
        return true;
    }

    @Override
    protected boolean isImplBlockValid(@NotNull RsImplItem impl) {
        return super.isImplBlockValid(impl) && isSuitableForConstructor(impl);
    }

    @Override
    protected boolean allowEmptyFields() {
        return true;
    }

    @Override
    protected void performRefactoring(
        @NotNull RsStructItem struct,
        @Nullable RsImplItem implBlock,
        @NotNull List<StructMember> chosenFields,
        @NotNull Substitution substitution,
        @NotNull Editor editor
    ) {
        org.rust.openapiext.OpenApiUtil.checkWriteAccessAllowed();
        Project project = editor.getProject();
        if (project == null) return;
        String structName = struct.getName();
        if (structName == null) return;
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        RsImplItem impl = getOrCreateImplBlock(implBlock, psiFactory, structName, struct);

        com.intellij.psi.PsiElement anchor = impl.getLastChild().getLastChild();
        RsFunction constructor = createConstructor(struct, chosenFields, psiFactory, substitution);
        com.intellij.psi.PsiElement inserted = impl.getLastChild().addBefore(constructor, anchor);
        editor.getCaretModel().moveToOffset(inserted.getTextRange().getEndOffset());
    }

    @NotNull
    private RsFunction createConstructor(
        @NotNull RsStructItem structItem,
        @NotNull List<StructMember> selectedFields,
        @NotNull RsPsiFactory psiFactory,
        @NotNull Substitution substitution
    ) {
        String arguments = selectedFields.stream()
            .map(f -> f.getArgumentIdentifier() + ": " + f.getTypeReferenceText())
            .collect(Collectors.joining(",", "(", ")"));

        String body = generateBody(structItem, selectedFields, substitution);
        return psiFactory.createTraitMethodMember("pub fn new" + arguments + "->Self{\n" + body + "}\n");
    }

    @NotNull
    private String generateBody(
        @NotNull RsStructItem structItem,
        @NotNull List<StructMember> selectedFields,
        @NotNull Substitution substitution
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

    private static boolean isSuitableForConstructor(@NotNull RsImplItem impl) {
        if (impl.getTraitRef() != null) return false;
        if (impl.getMembers() == null) return false;
        List<RsFunction> functions = PsiElementUtil.childrenOfType(impl.getMembers(), RsFunction.class);
        for (RsFunction fn : functions) {
            if ("new".equals(fn.getName())) return false;
        }
        return true;
    }
}
