/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.utils.imports.ImportBridge;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsFile.Attributes;
import org.rust.lang.core.types.ty.TyFloat;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;
import org.rust.lang.core.psi.RsLiteralKindUtil;

public class RsApproxConstantInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitLitExpr(@NotNull RsLitExpr o) {
                RsLiteralKind literal = RsLiteralKindUtil.getKind(o);
                if (literal instanceof RsLiteralKind.FloatLiteral) {
                    Double value = ((RsLiteralKind.FloatLiteral) literal).getValue();
                    if (value == null) return;
                    PredefinedConstant constant = null;
                    for (PredefinedConstant c : KNOWN_CONSTS) {
                        if (c.matches(value)) {
                            constant = c;
                            break;
                        }
                    }
                    if (constant == null) return;
                    String lib;
                    Attributes attrs = ImportBridge.getStdlibAttributes(o);
                    if (attrs == Attributes.NONE) {
                        lib = AutoInjectedCrates.STD;
                    } else if (attrs == Attributes.NO_STD) {
                        lib = AutoInjectedCrates.CORE;
                    } else {
                        return;
                    }
                    String typeName;
                    Ty type = RsTypesUtil.getType(o);
                    if (type instanceof TyFloat) {
                        typeName = ((TyFloat) type).getName();
                    } else {
                        typeName = "f64";
                    }
                    String path = RsBundle.message("inspection.message.consts", lib, typeName, constant.name);
                    ReplaceWithPredefinedQuickFix fix = new ReplaceWithPredefinedQuickFix(o, path);
                    holder.registerProblem(o, RsBundle.message("inspection.message.approximate.value.found.consider.using.it.directly", path), fix);
                }
            }
        };
    }

    private static class ReplaceWithPredefinedQuickFix extends RsQuickFixBase<RsLitExpr> {
        private final String path;

        ReplaceWithPredefinedQuickFix(@NotNull RsLitExpr element, @NotNull String path) {
            super(element);
            this.path = path;
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return RsBundle.message("intention.family.name.replace.with.predefined.constant");
        }

        @NotNull
        @Override
        public String getText() {
            return RsBundle.message("intention.name.replace.with2", path);
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, @NotNull RsLitExpr element) {
            RsExpr pathExpr = new RsPsiFactory(project).createExpression(path);
            element.replace(pathExpr);
        }
    }

    private static final List<PredefinedConstant> KNOWN_CONSTS = List.of(
        new PredefinedConstant("E", Math.E, 4),
        new PredefinedConstant("FRAC_1_PI", 1.0 / Math.PI, 4),
        new PredefinedConstant("FRAC_1_SQRT_2", 1.0 / Math.sqrt(2.0), 5),
        new PredefinedConstant("FRAC_2_PI", 2.0 / Math.PI, 5),
        new PredefinedConstant("FRAC_2_SQRT_PI", 2.0 / Math.sqrt(Math.PI), 5),
        new PredefinedConstant("FRAC_PI_2", Math.PI / 2.0, 5),
        new PredefinedConstant("FRAC_PI_3", Math.PI / 3.0, 5),
        new PredefinedConstant("FRAC_PI_4", Math.PI / 4.0, 5),
        new PredefinedConstant("FRAC_PI_6", Math.PI / 6.0, 5),
        new PredefinedConstant("FRAC_PI_8", Math.PI / 8.0, 5),
        new PredefinedConstant("LN_10", Math.log(10.0), 5),
        new PredefinedConstant("LN_2", Math.log(2.0), 5),
        new PredefinedConstant("LOG10_E", Math.log10(Math.E), 5),
        new PredefinedConstant("LOG2_E", Math.log(Math.E) / Math.log(2.0), 5),
        new PredefinedConstant("PI", Math.PI, 3),
        new PredefinedConstant("SQRT_2", Math.sqrt(2.0), 5)
    );
}

class PredefinedConstant {
    public final String name;
    public final double value;
    public final int minDigits;
    private final double accuracy;

    public PredefinedConstant(String name, double value, int minDigits) {
        this.name = name;
        this.value = value;
        this.minDigits = minDigits;
        this.accuracy = Math.pow(0.1, minDigits);
    }

    public boolean matches(double value) {
        return Math.abs(value - this.value) < accuracy;
    }
}
