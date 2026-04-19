/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.template.EditorExt;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AddAssocTypeBindingsFix extends RsQuickFixBase<RsElement> {
    @SafeFieldForPreview
    private final List<String> missingTypes;

    public AddAssocTypeBindingsFix(@NotNull RsElement element, @NotNull List<String> missingTypes) {
        super(element);
        this.missingTypes = missingTypes;
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.missing.associated.types");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsElement element) {
        RsPath path;
        if (element instanceof RsTraitRef) {
            path = ((RsTraitRef) element).getPath();
        } else if (element instanceof RsPathType) {
            path = ((RsPathType) element).getPath();
        } else {
            return;
        }

        RsPsiFactory factory = new RsPsiFactory(project);
        String defaultType = "()";

        RsTypeArgumentList arguments = path.getTypeArgumentList();
        if (arguments == null) {
            arguments = AddGenericArguments.addEmptyTypeArguments(path, factory);
        }

        List<com.intellij.psi.PsiElement> allArgs = new ArrayList<>();
        allArgs.addAll(arguments.getAssocTypeBindingList());
        allArgs.addAll(arguments.getTypeReferenceList());
        allArgs.addAll(arguments.getLifetimeList());

        com.intellij.psi.PsiElement lastArgument = allArgs.stream()
            .max(java.util.Comparator.comparingInt(e -> RsElementUtil.getStartOffset(e)))
            .orElse(arguments.getLt());

        List<RsAssocTypeBinding> missingTypeBindings = missingTypes.stream()
            .map(t -> factory.createAssocTypeBinding(t, defaultType))
            .collect(Collectors.toList());

        List<RsAssocTypeBinding> addedArguments = AddGenericArguments.addElements(arguments, missingTypeBindings, lastArgument, factory);

        if (editor != null) {
            List<RsTypeReference> typeRefs = addedArguments.stream()
                .map(RsAssocTypeBinding::getTypeReference)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
            EditorExt.buildAndRunTemplate(editor, element, new ArrayList<>(typeRefs));
        }
    }
}
