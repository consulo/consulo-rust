/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

public class RsHighlightingPassFactoryRegistrar implements TextEditorHighlightingPassFactoryRegistrar {
    @Override
    public void registerHighlightingPassFactory(@Nonnull TextEditorHighlightingPassRegistrar registrar, @Nonnull Project project) {
        new RsExternalLinterPassFactory(project, registrar);
        new RsMacroExpansionHighlightingPassFactory(project, registrar);
    }
}
