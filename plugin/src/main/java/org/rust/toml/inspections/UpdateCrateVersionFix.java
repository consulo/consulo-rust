/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.toml.lang.psi.TomlPsiFactory;
import org.toml.lang.psi.TomlValue;

public class UpdateCrateVersionFix extends LocalQuickFixOnPsiElement {
    private final String myVersion;

    public UpdateCrateVersionFix(@Nonnull TomlValue versionElement, @Nonnull String version) {
        super(versionElement);
        myVersion = version;
    }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.update.dependency.version"));
        }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.update.version.to", myVersion));
        }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiFile file,
                        @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
        TomlPsiFactory factory = new TomlPsiFactory(project, false);
        PsiElement newValue = factory.createLiteral("\"" + myVersion + "\"");
        startElement.replace(newValue);
    }
}
