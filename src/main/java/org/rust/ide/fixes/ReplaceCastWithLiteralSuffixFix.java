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
import org.rust.lang.core.psi.RsPsiFactory;

public class ReplaceCastWithLiteralSuffixFix extends RsQuickFixBase<RsCastExpr> {

    @IntentionName
    private final String fixText;

    public ReplaceCastWithLiteralSuffixFix(@NotNull RsCastExpr element) {
        super(element);
        this.fixText = RsBundle.message("intention.name.replace.with.0.1",
            element.getExpr().getText(), element.getTypeReference().getText());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.replace.cast.with.literal.suffix");
    }

    @NotNull
    @Override
    public String getText() {
        return fixText;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsCastExpr element) {
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        element.replace(psiFactory.createExpression(element.getExpr().getText() + element.getTypeReference().getText()));
    }
}
