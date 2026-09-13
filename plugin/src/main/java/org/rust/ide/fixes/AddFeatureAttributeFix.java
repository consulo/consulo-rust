/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.CompilerFeature;

import consulo.localize.LocalizeValue;

public class AddFeatureAttributeFix extends RsQuickFixBase<PsiElement> {
    private final String featureName;

    public AddFeatureAttributeFix(@Nonnull String featureName, @Nonnull PsiElement element) {
        super(element);
        this.featureName = featureName;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.add.feature.attribute"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.feature", featureName));
    }

    @Nullable
    @Override
    public FileModifier getFileModifierForPreview(@Nonnull PsiFile target) {
        return null;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        addFeatureAttribute(project, element, featureName);
    }

    public static void addFeatureAttribute(@Nonnull Project project, @Nonnull PsiElement context, @Nonnull String featureName) {
        CompilerFeature.addFeatureAttribute(project, context, featureName);
    }
}
