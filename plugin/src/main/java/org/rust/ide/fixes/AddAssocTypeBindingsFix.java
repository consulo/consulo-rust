/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.editor.inspection.FileModifier.SafeFieldForPreview;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.template.EditorExt;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;

public class AddAssocTypeBindingsFix extends RsQuickFixBase<RsElement> {
    @SafeFieldForPreview
    private final List<String> missingTypes;

    public AddAssocTypeBindingsFix(@Nonnull RsElement element, @Nonnull List<String> missingTypes) {
        super(element);
        this.missingTypes = missingTypes;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.missing.associated.types"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsElement element) {
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

        List<consulo.language.psi.PsiElement> allArgs = new ArrayList<>();
        allArgs.addAll(arguments.getAssocTypeBindingList());
        allArgs.addAll(arguments.getTypeReferenceList());
        allArgs.addAll(arguments.getLifetimeList());

        consulo.language.psi.PsiElement lastArgument = allArgs.stream()
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
