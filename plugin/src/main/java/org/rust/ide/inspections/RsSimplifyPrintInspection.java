/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.lang.core.psi.RsFormatMacroArgument;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;

import java.util.List;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;

/**
 * Replace {@code println!("")} with {@code println!()} available since Rust 1.14.0
 */
@ExtensionImpl
public class RsSimplifyPrintInspection extends RsLocalInspectionTool {

    @SuppressWarnings("DialogTitleCapitalization")
@Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMacroCall2(@Nonnull RsMacroCall o) {
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

        RemoveUnnecessaryPrintlnArgument(@Nonnull RsMacroCall element) {
            super(element);
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.unnecessary.argument"));
            }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return getName();
            }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsMacroCall element) {
            RsFormatMacroArgument formatMacroArgument = element.getFormatMacroArgument();
            if (formatMacroArgument == null) return;
            PsiElement arg = emptyStringArg(formatMacroArgument);
            if (arg == null) return;
            arg.delete();
        }
    }

    @Nullable
    private static PsiElement emptyStringArg(@Nonnull RsFormatMacroArgument arg) {
        List<? extends PsiElement> argList = arg.getFormatMacroArgList();
        if (argList.size() != 1) return null;
        PsiElement singleArg = argList.get(0);
        if (!"\"\"".equals(singleArg.getText())) return null;
        return singleArg;
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.simplify.print.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WEAK_WARNING;
    }
}
