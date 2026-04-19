/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.RsFile;

/**
 * {@code RsExpandedElement}s are those elements which exist in temporary,
 * in-memory PSI-files and are injected into real PSI. Their real
 * parent is this temp PSI-file, but they are seen by the rest of
 * the plugin as the children of {@code getContext()} element.
 */
public interface RsExpandedElement extends RsElement {
    @Override
    @Nullable
    PsiElement getContext();

    Key<RsElement> RS_EXPANSION_CONTEXT = Key.create("org.rust.lang.core.psi.RS_EXPANSION_CONTEXT");

    @Nullable
    static PsiElement getContextImpl(RsExpandedElement psi, boolean isIndexAccessForbidden) {
        RsElement data = psi.getUserData(RS_EXPANSION_CONTEXT);
        if (data != null) return data;
        PsiElement parent = psi.getParent();
        if (parent instanceof RsFile && !isIndexAccessForbidden) {
            RsFile rsFile = (RsFile) parent;
            Project project = rsFile.getProject();
            if (!DumbService.isDumb(project)) {
                MacroExpansionManager macroExpansionManager = MacroExpansionManagerUtil.getMacroExpansionManager(project);
                PsiElement context = macroExpansionManager.getContextOfMacroCallExpandedFrom(rsFile);
                if (context != null) return context;
                if (rsFile.isIncludedByIncludeMacro()) {
                    org.rust.lang.core.psi.RsMacroCall includedFrom = macroExpansionManager.getIncludedFrom(rsFile);
                    if (includedFrom != null) return includedFrom.getContainingMod();
                }
            }
        }
        return parent;
    }

    @Nullable
    static PsiElement getContextImpl(RsExpandedElement psi) {
        return getContextImpl(psi, false);
    }
}
