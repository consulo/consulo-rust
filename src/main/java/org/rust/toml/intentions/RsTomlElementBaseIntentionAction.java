/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.intentions.RsElementBaseIntentionAction;
import org.rust.toml.Util;

public abstract class RsTomlElementBaseIntentionAction<Ctx> extends RsElementBaseIntentionAction<Ctx> {
    @Nullable
    @Override
    public final Ctx findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (!Util.tomlPluginIsAbiCompatible()) return null;
        return findApplicableContextInternal(project, editor, element);
    }

    @Nullable
    protected abstract Ctx findApplicableContextInternal(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element);
}
