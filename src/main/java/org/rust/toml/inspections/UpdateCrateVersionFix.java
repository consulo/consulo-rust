/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.toml.lang.psi.TomlPsiFactory;
import org.toml.lang.psi.TomlValue;

public class UpdateCrateVersionFix extends LocalQuickFixOnPsiElement {
    private final String myVersion;

    public UpdateCrateVersionFix(@NotNull TomlValue versionElement, @NotNull String version) {
        super(versionElement);
        myVersion = version;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.update.dependency.version");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.update.version.to", myVersion);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file,
                        @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        TomlPsiFactory factory = new TomlPsiFactory(project, false);
        PsiElement newValue = factory.createLiteral("\"" + myVersion + "\"");
        startElement.replace(newValue);
    }
}
