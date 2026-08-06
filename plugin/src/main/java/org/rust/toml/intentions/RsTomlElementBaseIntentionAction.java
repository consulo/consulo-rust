/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.intentions.RsElementBaseIntentionAction;
import org.rust.toml.Util;

public abstract class RsTomlElementBaseIntentionAction<Ctx> extends RsElementBaseIntentionAction<Ctx> {
    @Nullable
    @Override
    public final Ctx findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        if (!Util.tomlPluginIsAbiCompatible()) return null;
        return findApplicableContextInternal(project, editor, element);
    }

    @Nullable
    protected abstract Ctx findApplicableContextInternal(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element);
}
