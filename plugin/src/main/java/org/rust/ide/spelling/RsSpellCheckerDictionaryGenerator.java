/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import consulo.project.Project;
import consulo.language.psi.PsiFile;
import com.intellij.spellchecker.generator.SpellCheckerDictionaryGenerator;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;

import java.util.HashSet;

public class RsSpellCheckerDictionaryGenerator extends SpellCheckerDictionaryGenerator {

    public RsSpellCheckerDictionaryGenerator(@Nonnull Project project, @Nonnull String outputFolder) {
        super(project, outputFolder, "rust");
    }

    @Override
    protected void processFile(@Nonnull PsiFile file, @Nonnull HashSet<String> seenNames) {
        file.accept(new RsRecursiveVisitor() {
            @Override
            public void visitElement(@Nonnull RsElement element) {
                if (element instanceof RsConstParameter ||
                    element instanceof RsLabelDecl ||
                    element instanceof RsLifetime ||
                    element instanceof RsLifetimeParameter ||
                    element instanceof RsMacroBinding ||
                    element instanceof RsPatBinding ||
                    element instanceof RsTypeParameter) {
                    return;
                }
                if (element instanceof RsNameIdentifierOwner) {
                    processLeafsNames(element, seenNames);
                }
                super.visitElement(element);
            }
        });
    }
}
