/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.AddGenericArguments;
import org.rust.ide.fixes.RemoveGenericArguments;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.openapiext.SmartPointerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.intellij.openapi.util.Pair;

/**
 * Inspection that detects the E0107 error.
 */
public class RsWrongGenericArgumentsNumberInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMethodCall(@NotNull RsMethodCall methodCall) {
                checkTypeArguments(holder, methodCall);
            }

            @Override
            public void visitPath(@NotNull RsPath path) {
                if (!isPathValid(path)) return;
                checkTypeArguments(holder, path);
            }
        };
    }

    private static boolean isPathValid(@Nullable RsPath path) {
        return path != null && path.getValueParameterList() == null && path.getCself() == null;
    }

    private static void checkTypeArguments(@NotNull RsProblemsHolder holder, @NotNull RsMethodOrPath element) {
        Pair<RsTypeArgumentList, RsGenericDeclaration> result = getTypeArgumentsAndDeclaration(element);
        if (result == null) return;
        RsTypeArgumentList actualArguments = result.getFirst();
        RsGenericDeclaration declaration = result.getSecond();

        int actualTypeArgs = actualArguments != null ? RsTypeArgumentListUtil.getTypeArguments(actualArguments).size() : 0;
        int actualConstArgs = actualArguments != null ? RsTypeArgumentListUtil.getConstArguments(actualArguments).size() : 0;
        int actualArgs = actualTypeArgs + actualConstArgs;

        int expectedTotalTypeParams = RsGenericDeclarationUtil.getTypeParameters(declaration).size();
        int expectedTotalConstParams = RsGenericDeclarationUtil.getConstParameters(declaration).size();
        int expectedTotalParams = expectedTotalTypeParams + expectedTotalConstParams;

        if (actualArgs == expectedTotalParams) return;

        int expectedRequiredParams = RsGenericDeclarationUtil.getRequiredGenericParameters(declaration).size();
        int minRequiredParams;
        PsiElement parent = ((PsiElement) element).getParent();
        if (parent instanceof RsPathType || parent instanceof RsTraitRef) {
            minRequiredParams = 0;
        } else {
            minRequiredParams = 1;
        }

        String errorText;
        if (actualArgs > expectedTotalParams) {
            errorText = expectedRequiredParams != expectedTotalParams ? "at most " + expectedTotalParams : String.valueOf(expectedTotalParams);
        } else if (actualArgs >= minRequiredParams && actualArgs < expectedRequiredParams) {
            errorText = expectedRequiredParams != expectedTotalParams ? "at least " + expectedRequiredParams : String.valueOf(expectedTotalParams);
        } else {
            return;
        }

        boolean haveTypeParams = expectedTotalTypeParams > 0 || actualTypeArgs > 0;
        boolean haveConstParams = expectedTotalConstParams > 0 || actualConstArgs > 0;
        String argumentName;
        if (haveTypeParams && !haveConstParams) {
            argumentName = "type";
        } else if (!haveTypeParams && haveConstParams) {
            argumentName = "const";
        } else {
            argumentName = "generic";
        }

        String problemText = RsBundle.message("inspection.message.wrong.number.arguments.expected.found", argumentName, errorText, actualArgs);
        List<LocalQuickFix> fixes = getFixes(declaration, element, actualArgs, expectedTotalParams);

        new RsDiagnostic.WrongNumberOfGenericArguments(element, problemText, fixes).addToHolder(holder);
    }

    @NotNull
    private static List<LocalQuickFix> getFixes(
        @NotNull RsGenericDeclaration declaration,
        @NotNull RsMethodOrPath element,
        int actualArgs,
        int expectedTotalParams
    ) {
        if (actualArgs > expectedTotalParams) {
            List<LocalQuickFix> fixes = new ArrayList<>();
            fixes.add(new RemoveGenericArguments(element, expectedTotalParams, actualArgs));
            return fixes;
        } else if (actualArgs < expectedTotalParams) {
            List<LocalQuickFix> fixes = new ArrayList<>();
            fixes.add(new AddGenericArguments(SmartPointerUtil.createSmartPointer(declaration), element));
            return fixes;
        }
        return Collections.emptyList();
    }

    @Nullable
    public static Pair<RsTypeArgumentList, RsGenericDeclaration> getTypeArgumentsAndDeclaration(@NotNull RsMethodOrPath pathOrMethodCall) {
        RsTypeArgumentList arguments = pathOrMethodCall.getTypeArgumentList();
        PsiElement resolved = pathOrMethodCall.getReference() != null ? pathOrMethodCall.getReference().resolve() : null;
        if (!(resolved instanceof RsGenericDeclaration)) return null;
        return new Pair<>(arguments, (RsGenericDeclaration) resolved);
    }
}
