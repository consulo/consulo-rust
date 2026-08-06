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
import org.rust.ide.fixes.EncloseExprInBracesFix;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.utils.RsDiagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import consulo.util.lang.Pair;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Inspection that detects the E0747 error.
 */
public class RsWrongGenericArgumentsOrderInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMethodCall(@Nonnull RsMethodCall methodCall) {
                checkGenericArguments(holder, methodCall);
            }

            @Override
            public void visitPath(@Nonnull RsPath path) {
                if (!isPathValid(path)) return;
                checkGenericArguments(holder, path);
            }
        };
    }

    private static boolean isPathValid(@Nullable RsPath path) {
        return path != null && path.getValueParameterList() == null && path.getCself() == null;
    }

    private static void checkGenericArguments(@Nonnull RsProblemsHolder holder, @Nonnull RsMethodOrPath element) {
        Pair<RsTypeArgumentList, RsGenericDeclaration> result = RsWrongGenericArgumentsNumberInspection.getTypeArgumentsAndDeclaration(element);
        if (result == null) return;
        RsTypeArgumentList actualArguments = result.getFirst();
        RsGenericDeclaration declaration = result.getSecond();
        if (actualArguments == null) return;
        RsTypeParameterList parameterList = declaration.getTypeParameterList();
        if (parameterList == null) return;

        boolean explicitLifetimes = !RsTypeArgumentListUtil.getLifetimeArguments(actualArguments).isEmpty();

        List<RsGenericParameter> params = new ArrayList<>();
        for (RsGenericParameter param : PsiElementUtil.stubChildrenOfType(parameterList, RsGenericParameter.class)) {
            if (param instanceof RsTypeParameter) {
                params.add(param);
            } else if (param instanceof RsConstParameter) {
                params.add(param);
            } else if (param instanceof RsLifetimeParameter && explicitLifetimes) {
                params.add(param);
            }
        }

        List<RsElement> args = new ArrayList<>();
        for (RsElement arg : PsiElementUtil.stubChildrenOfType(actualArguments, RsElement.class)) {
            if (arg instanceof RsTypeReference) {
                args.add(arg);
            } else if (arg instanceof RsExpr) {
                args.add(arg);
            } else if (arg instanceof RsLifetime && explicitLifetimes) {
                args.add(arg);
            }
        }

        List<? extends RsElement> typeArguments = RsTypeArgumentListUtil.getTypeArguments(actualArguments);
        List<? extends RsElement> constArguments = RsTypeArgumentListUtil.getConstArguments(actualArguments);
        List<? extends RsElement> lifetimeArguments = RsTypeArgumentListUtil.getLifetimeArguments(actualArguments);

        int size = Math.min(params.size(), args.size());
        for (int i = 0; i < size; i++) {
            RsGenericParameter param = params.get(i);
            RsElement arg = args.get(i);

            String text;
            if (param instanceof RsTypeParameter && !typeArguments.contains(arg)) {
                text = RsBundle.message("inspection.message.provided.when.type.was.expected", kindName(arg, typeArguments, constArguments, lifetimeArguments));
            } else if (param instanceof RsConstParameter && !constArguments.contains(arg)) {
                text = RsBundle.message("inspection.message.provided.when.constant.was.expected", kindName(arg, typeArguments, constArguments, lifetimeArguments));
            } else if (param instanceof RsLifetimeParameter && !lifetimeArguments.contains(arg)) {
                text = RsBundle.message("inspection.message.provided.when.lifetime.was.expected", kindName(arg, typeArguments, constArguments, lifetimeArguments));
            } else {
                continue;
            }

            List<consulo.language.editor.inspection.LocalQuickFix> fixes;
            if (param instanceof RsConstParameter && arg instanceof RsTypeReference) {
                fixes = Collections.singletonList(new EncloseExprInBracesFix((RsTypeReference) arg));
            } else {
                fixes = Collections.emptyList();
            }
            new RsDiagnostic.WrongOrderOfGenericArguments(arg, text, fixes).addToHolder(holder);
        }
    }

    @Nonnull
    private static String kindName(
        @Nonnull RsElement arg,
        @Nonnull List<? extends RsElement> typeArguments,
        @Nonnull List<? extends RsElement> constArguments,
        @Nonnull List<? extends RsElement> lifetimeArguments
    ) {
        if (typeArguments.contains(arg)) return "Type";
        if (constArguments.contains(arg)) return "Constant";
        if (lifetimeArguments.contains(arg)) return "Lifetime";
        throw new IllegalStateException("impossible");
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.wrong.generic.arguments.order.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
