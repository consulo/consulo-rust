/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.codeHighlighting.DirtyScopeTrackingHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.codeInsight.daemon.impl.FileStatusMap;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.MacroExpansionManagerUtil;
import org.rust.lang.core.macros.MacroExpansionMode;

public class RsMacroExpansionHighlightingPassFactory implements DirtyScopeTrackingHighlightingPassFactory {
    private static final RegistryValue MACRO_HIGHLIGHTING_ENABLED_KEY = Registry.get("org.rust.lang.highlight.macro.body");

    @NotNull
    private final Project myProject;
    private final int myPassId;

    public RsMacroExpansionHighlightingPassFactory(@NotNull Project project, @NotNull TextEditorHighlightingPassRegistrar registrar) {
        this.myProject = project;
        this.myPassId = registrar.registerTextEditorHighlightingPass(this, null, null, false, -1);
    }

    @Nullable
    @Override
    public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull Editor editor) {
        if (!(MacroExpansionManagerUtil.getMacroExpansionManager(myProject).getMacroExpansionMode() instanceof MacroExpansionMode.New)) return null;
        if (!MACRO_HIGHLIGHTING_ENABLED_KEY.asBoolean()) return null;

        TextRange restrictedRange = FileStatusMap.getDirtyTextRange(editor, getPassId());
        if (restrictedRange == null) return null;
        return new RsMacroExpansionHighlightingPass(file, restrictedRange, editor.getDocument());
    }

    @Override
    public int getPassId() {
        return myPassId;
    }
}
