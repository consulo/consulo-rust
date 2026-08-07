/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.util.lang.StringUtil;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.utils.RsDiagnostic;

import java.util.List;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;

/**
 * Inspection that detects the E0049 error.
 */
@ExtensionImpl
public class RsWrongGenericParametersNumberInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitFunction2(@Nonnull RsFunction function) {
                checkTypeParameters(holder, function, "type");
                checkConstParameters(holder, function, "const");
            }

            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitTypeAlias2(@Nonnull RsTypeAlias alias) {
                checkTypeParameters(holder, alias, "type");
                checkConstParameters(holder, alias, "const");
            }
        };
    }

    private static <T extends RsAbstractable & RsGenericDeclaration> void checkTypeParameters(
        @Nonnull RsProblemsHolder holder,
        @Nonnull T item,
        @Nonnull String paramType
    ) {
        String itemName = item.getName();
        if (itemName == null) return;
        String itemType;
        if (item instanceof RsFunction) {
            itemType = "Method";
        } else if (item instanceof RsTypeAlias) {
            itemType = "Type";
        } else {
            return;
        }
        RsAbstractable superItemRaw = RsAbstractableUtil.getSuperItem(item);
        if (!(superItemRaw instanceof RsGenericDeclaration)) return;
        RsGenericDeclaration superItem = (RsGenericDeclaration) superItemRaw;
        PsiElement toHighlight = item.getTypeParameterList();
        if (toHighlight == null) toHighlight = item.getNameIdentifier();
        if (toHighlight == null) return;

        List<? extends RsGenericParameter> typeParameters = RsGenericDeclarationUtil.getTypeParameters(item);
        List<? extends RsGenericParameter> superTypeParameters = RsGenericDeclarationUtil.getTypeParameters(superItem);
        if (typeParameters.size() == superTypeParameters.size()) return;

        String paramName = paramType + " " + StringUtil.pluralize("parameter", typeParameters.size());
        String superParamName = paramType + " " + StringUtil.pluralize("parameter", superTypeParameters.size());
        String problemText = RsBundle.message("inspection.message.has.but.its.trait.declaration.has",
            itemType, itemName, typeParameters.size(), paramName, superTypeParameters.size(), superParamName);
        new RsDiagnostic.WrongNumberOfGenericParameters(toHighlight, problemText).addToHolder(holder);
    }

    private static <T extends RsAbstractable & RsGenericDeclaration> void checkConstParameters(
        @Nonnull RsProblemsHolder holder,
        @Nonnull T item,
        @Nonnull String paramType
    ) {
        String itemName = item.getName();
        if (itemName == null) return;
        String itemType;
        if (item instanceof RsFunction) {
            itemType = "Method";
        } else if (item instanceof RsTypeAlias) {
            itemType = "Type";
        } else {
            return;
        }
        RsAbstractable superItemRaw = RsAbstractableUtil.getSuperItem(item);
        if (!(superItemRaw instanceof RsGenericDeclaration)) return;
        RsGenericDeclaration superItem = (RsGenericDeclaration) superItemRaw;
        PsiElement toHighlight = item.getTypeParameterList();
        if (toHighlight == null) toHighlight = item.getNameIdentifier();
        if (toHighlight == null) return;

        List<? extends RsGenericParameter> constParameters = RsGenericDeclarationUtil.getConstParameters(item);
        List<? extends RsGenericParameter> superConstParameters = RsGenericDeclarationUtil.getConstParameters(superItem);
        if (constParameters.size() == superConstParameters.size()) return;

        String paramName = paramType + " " + StringUtil.pluralize("parameter", constParameters.size());
        String superParamName = paramType + " " + StringUtil.pluralize("parameter", superConstParameters.size());
        String problemText = RsBundle.message("inspection.message.has.but.its.trait.declaration.has",
            itemType, itemName, constParameters.size(), paramName, superConstParameters.size(), superParamName);
        new RsDiagnostic.WrongNumberOfGenericParameters(toHighlight, problemText).addToHolder(holder);
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.wrong.generic.parameters.number.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
