/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.ide.fixes.RemoveParameterFix;
import org.rust.ide.fixes.RemoveVariableFix;
import org.rust.ide.fixes.RenameFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.dfa.liveness.Liveness;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;

import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.injected.RsDoctestLanguageInjector;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.impl.RsPatBindingUtil;
import consulo.localize.LocalizeValue;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.ext.impl.*;

@ExtensionImpl
public class RsLivenessInspection extends RsLintInspection {

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.UnusedVariables;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitFunction2(@Nonnull RsFunction func) {
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
        @Nonnull RsProblemsHolder holder,
        @Nonnull RsPatBinding binding,
        @Nonnull String name,
        @Nonnull Liveness.DeclarationKind kind,
        @Nonnull RsFunction function
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

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.liveness.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("lints"));
    }

    @Nonnull
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.of(RsBundle.message("rust"))};
    }
}
