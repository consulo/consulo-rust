/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.fixes.RemoveParameterFix;
import org.rust.ide.fixes.RemoveVariableFix;
import org.rust.ide.fixes.RenameFix;
import org.rust.ide.injected.DoctestUtils;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.dfa.liveness.Liveness;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;

import java.util.ArrayList;
import java.util.List;
import org.rust.ide.injected.RsDoctestLanguageInjector;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;

public class RsLivenessInspection extends RsLintInspection {

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.UnusedVariables;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitFunction2(@NotNull RsFunction func) {
                // Disable inside doc tests
                if (RsDoctestLanguageInjector.isDoctestInjection(func)) return;

                // Don't analyze functions with unresolved macro calls
                boolean hasUnresolvedMacroCall = false;
                for (RsMacroCall macroCall : RsPsiJavaUtil.descendantsWithMacrosOfType(func, RsMacroCall.class)) {
                    RsMacroDefinitionBase macroDef = RsMacroCallUtil.resolveToMacro(macroCall);
                    if (macroDef == null) {
                        hasUnresolvedMacroCall = true;
                        break;
                    }
                    boolean hasRustcBuiltinMacro = macroDef.getHasRustcBuiltinMacro();
                    if (hasRustcBuiltinMacro) continue;
                    if (macroCall.getMacroArgument() == null && macroDef.getContainingCrate().getOrigin() == PackageOrigin.STDLIB) {
                        continue;
                    }
                    if (RsMacroCallUtil.getExpansion(macroCall) == null) {
                        hasUnresolvedMacroCall = true;
                        break;
                    }
                }
                if (hasUnresolvedMacroCall) return;

                // Don't analyze functions with unresolved struct literals
                for (RsStructLiteral structLiteral : RsPsiJavaUtil.descendantsWithMacrosOfType(func, RsStructLiteral.class)) {
                    if (structLiteral.getPath().getReference() != null && structLiteral.getPath().getReference().resolve() == null) {
                        return;
                    }
                }

                // TODO: Remove this check when type inference is implemented for `asm!` macro calls
                if (!RsPsiJavaUtil.descendantsWithMacrosOfType(func, RsAsmMacroArgument.class).isEmpty()) return;

                Liveness.LivenessResult liveness = ExtensionsUtil.getLiveness(func);
                if (liveness == null) return;

                for (Liveness.DeadDeclaration deadDeclaration : liveness.deadDeclarations) {
                    String name = deadDeclaration.binding.getName();
                    if (name == null) continue;
                    if (name.startsWith("_")) continue;
                    registerUnusedProblem(holder, deadDeclaration.binding, name, deadDeclaration.kind, func);
                }
            }
        };
    }

    private void registerUnusedProblem(
        @NotNull RsProblemsHolder holder,
        @NotNull RsPatBinding binding,
        @NotNull String name,
        @NotNull Liveness.DeclarationKind kind,
        @NotNull RsFunction function
    ) {
        if (!binding.isPhysical()) return;

        if (CfgUtils.isCfgUnknown(binding)) return;

        // TODO: remove this check when multi-resolve for `RsOrPat` is implemented
        if (RsElementUtil.ancestorStrict(binding, RsOrPat.class) != null) return;

        boolean isSimplePat = RsPatBindingUtil.getTopLevelPattern(binding) instanceof RsPatIdent;
        String message;
        if (isSimplePat) {
            switch (kind) {
                case Parameter:
                    message = RsBundle.message("inspection.message.parameter.never.used", name);
                    break;
                case Variable:
                    message = RsBundle.message("inspection.message.variable.never.used", name);
                    break;
                default:
                    message = RsBundle.message("inspection.message.binding.never.used", name);
            }
        } else {
            message = RsBundle.message("inspection.message.binding.never.used", name);
        }

        List<LocalQuickFix> fixes = new ArrayList<>();
        fixes.add(new RenameFix(binding, "_" + name));
        if (isSimplePat) {
            switch (kind) {
                case Parameter: {
                    RsAbstractableOwner owner = function.getOwner();
                    boolean isTraitOrTraitImpl = owner.isTraitImpl() || owner instanceof RsAbstractableOwner.Trait;
                    if (!isTraitOrTraitImpl && !RsFunctionUtil.isProcMacroDef(function)) {
                        fixes.add(new RemoveParameterFix(binding, name));
                    }
                    break;
                }
                case Variable: {
                    if (RsPatBindingUtil.getTopLevelPattern(binding).getParent() instanceof RsLetDecl) {
                        fixes.add(new RemoveVariableFix(binding, name));
                    }
                    break;
                }
            }
        }

        registerLintProblem(holder, binding, message, RsLintHighlightingType.UNUSED_SYMBOL, fixes);
    }
}
