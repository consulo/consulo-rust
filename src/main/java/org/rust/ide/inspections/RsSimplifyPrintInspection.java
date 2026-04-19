/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.lang.core.psi.RsFormatMacroArgument;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;

import java.util.List;

/**
 * Replace {@code println!("")} with {@code println!()} available since Rust 1.14.0
 */
public class RsSimplifyPrintInspection extends RsLocalInspectionTool {

    @SuppressWarnings("DialogTitleCapitalization")
    @Override
    public String getDisplayName() {
        return RsBundle.message("println.usage");
    }

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMacroCall2(@NotNull RsMacroCall o) {
                String macroName = RsMacroCallUtil.getMacroName(o);
                RsFormatMacroArgument formatMacroArg = o.getFormatMacroArgument();
                if (formatMacroArg == null) return;
                if (!macroName.endsWith("println")) return;

                if (emptyStringArg(formatMacroArg) == null) return;
                holder.registerProblem(
                    o,
                    RsBundle.message("inspection.message.println.macro.invocation.can.be.simplified"),
                    new RemoveUnnecessaryPrintlnArgument(o)
                );
            }
        };
    }

    private static class RemoveUnnecessaryPrintlnArgument extends RsQuickFixBase<RsMacroCall> {

        RemoveUnnecessaryPrintlnArgument(@NotNull RsMacroCall element) {
            super(element);
        }

        @NotNull
        @Override
        public String getText() {
            return RsBundle.message("intention.name.remove.unnecessary.argument");
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getName();
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsMacroCall element) {
            RsFormatMacroArgument formatMacroArgument = element.getFormatMacroArgument();
            if (formatMacroArgument == null) return;
            PsiElement arg = emptyStringArg(formatMacroArgument);
            if (arg == null) return;
            arg.delete();
        }
    }

    @Nullable
    private static PsiElement emptyStringArg(@NotNull RsFormatMacroArgument arg) {
        List<? extends PsiElement> argList = arg.getFormatMacroArgList();
        if (argList.size() != 1) return null;
        PsiElement singleArg = argList.get(0);
        if (!"\"\"".equals(singleArg.getText())) return null;
        return singleArg;
    }
}
