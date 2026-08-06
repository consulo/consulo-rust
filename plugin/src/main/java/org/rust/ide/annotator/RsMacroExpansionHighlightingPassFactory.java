/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;
import consulo.language.editor.FileStatusMap;

import consulo.language.editor.impl.highlight.DirtyScopeTrackingHighlightingPassFactory;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.application.util.registry.Registry;
import consulo.application.util.registry.RegistryValue;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.MacroExpansionManagerUtil;
import org.rust.lang.core.macros.MacroExpansionMode;

public class RsMacroExpansionHighlightingPassFactory implements DirtyScopeTrackingHighlightingPassFactory {
    private static final RegistryValue MACRO_HIGHLIGHTING_ENABLED_KEY = Registry.get("org.rust.lang.highlight.macro.body");

    @Nonnull
    private final Project myProject;
    private final int myPassId;

    public RsMacroExpansionHighlightingPassFactory(@Nonnull Project project, @Nonnull TextEditorHighlightingPassRegistrar registrar) {
        this.myProject = project;
        this.myPassId = registrar.registerTextEditorHighlightingPass(this, null, null, false, -1);
    }

    @Nullable
    @Override
    public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull Editor editor) {
        if (!(MacroExpansionManagerUtil.getMacroExpansionManager(myProject).getMacroExpansionMode() instanceof MacroExpansionMode.New)) return null;
        if (!MACRO_HIGHLIGHTING_ENABLED_KEY.asBoolean()) return null;

        // FileStatusMap.getDirtyTextRange is not exposed in Consulo; use the full document range
        TextRange restrictedRange = new TextRange(0, editor.getDocument().getTextLength());
        return new RsMacroExpansionHighlightingPass(file, restrictedRange, editor.getDocument());
    }

    @Override
    public int getPassId() {
        return myPassId;
    }

    @Override
    public void register(@Nonnull consulo.language.editor.highlight.TextEditorHighlightingPassFactory.Registrar registrar) {
        // TODO: wire Registrar-based registration
    }
}
