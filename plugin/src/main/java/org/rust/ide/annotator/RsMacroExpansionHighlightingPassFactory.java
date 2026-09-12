/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.registry.Registry;
import consulo.application.util.registry.RegistryValue;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.DirtyScopeTrackingHighlightingPassFactory;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.rust.lang.core.macros.MacroExpansionManagerUtil;
import org.rust.lang.core.macros.MacroExpansionMode;

/**
 * Creates the highlighting pass that highlights the bodies of expanded macros.
 */
@ExtensionImpl
public class RsMacroExpansionHighlightingPassFactory implements DirtyScopeTrackingHighlightingPassFactory {
    private static final RegistryValue MACRO_HIGHLIGHTING_ENABLED_KEY = Registry.get("org.rust.lang.highlight.macro.body");

    @Nonnull
    private final Project myProject;

    private int myPassId = -1;

    @Inject
    public RsMacroExpansionHighlightingPassFactory(@Nonnull Project project) {
        myProject = project;
    }

    @Override
    public void register(@Nonnull Registrar registrar) {
        myPassId = registrar.registerTextEditorHighlightingPass(this, null, null, false, -1);
    }

    @Nullable
    @Override
    public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull Editor editor) {
        if (!(MacroExpansionManagerUtil.getMacroExpansionManager(myProject).getMacroExpansionMode() instanceof MacroExpansionMode.New)) return null;
        if (!MACRO_HIGHLIGHTING_ENABLED_KEY.asBoolean(true)) return null;

        TextRange restrictedRange = new TextRange(0, editor.getDocument().getTextLength());
        return new RsMacroExpansionHighlightingPass(file, restrictedRange, editor.getDocument());
    }

    @Override
    public int getPassId() {
        return myPassId;
    }
}
