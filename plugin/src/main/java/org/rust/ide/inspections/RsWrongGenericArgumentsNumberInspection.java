/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

import consulo.util.lang.Pair;

/**
 * Inspection that detects the E0107 error.
 */
public class RsWrongGenericArgumentsNumberInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMethodCall(@Nonnull RsMethodCall methodCall) {
                checkTypeArguments(holder, methodCall);
            }

            @Override
            public void visitPath(@Nonnull RsPath path) {
                if (!isPathValid(path)) return;
                checkTypeArguments(holder, path);
            }
        };
    }

    private static boolean isPathValid(@Nullable RsPath path) {
        return path != null && path.getValueParameterList() == null && path.getCself() == null;
    }

    private static void checkTypeArguments(@Nonnull RsProblemsHolder holder, @Nonnull RsMethodOrPath element) {
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

    @Nonnull
    private static List<LocalQuickFix> getFixes(
        @Nonnull RsGenericDeclaration declaration,
        @Nonnull RsMethodOrPath element,
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
    public static Pair<RsTypeArgumentList, RsGenericDeclaration> getTypeArgumentsAndDeclaration(@Nonnull RsMethodOrPath pathOrMethodCall) {
        RsTypeArgumentList arguments = pathOrMethodCall.getTypeArgumentList();
        PsiElement resolved = pathOrMethodCall.getReference() != null ? pathOrMethodCall.getReference().resolve() : null;
        if (!(resolved instanceof RsGenericDeclaration)) return null;
        return new Pair<>(arguments, (RsGenericDeclaration) resolved);
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.wrong.generic.arguments.number.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
