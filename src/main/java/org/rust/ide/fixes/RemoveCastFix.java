/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsCastExpr;

public class RemoveCastFix extends RsQuickFixBase<RsCastExpr> {

    @IntentionName
    private final String fixText;

    public RemoveCastFix(@NotNull RsCastExpr element) {
        super(element);
        this.fixText = RsBundle.message("intention.name.remove.as", element.getTypeReference().getText());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.remove.unnecessary.cast");
    }

    @NotNull
    @Override
    public String getText() {
        return fixText;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsCastExpr element) {
        element.replace(element.getExpr());
    }
}
