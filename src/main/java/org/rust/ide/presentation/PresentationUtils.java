/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Iconable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.colors.RsColor;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.RsInferenceResult;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsImplItemUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.RsTypeParameterUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public final class PresentationUtils {

    private PresentationUtils() {
    }

    @NotNull
    public static ItemPresentation getPresentation(@NotNull RsElement psi) {
        RsMod mod = psi.getContainingMod();
        String qualifiedName = mod instanceof RsQualifiedNamedElement ? ((RsQualifiedNamedElement) mod).qualifiedName() : null;
        String modName = mod.getModName();
        String fileName = psi.getContainingFile() != null ? psi.getContainingFile().getName() : "";
        String locationBase = qualifiedName != null ? qualifiedName : (modName != null ? modName : fileName);
        String location = "(in " + locationBase + ")";

        String name = presentableName(psi);
        Icon icon = psi.getIcon(0);
        return new PresentationData(name, location, icon, null);
    }

    @NotNull
    public static ItemPresentation getPresentationForStructure(@NotNull RsElement psi) {
        StringBuilder sb = new StringBuilder();
        String name = presentableName(psi);
        if (name != null) {
            sb.append(name);
        }

        if (psi instanceof RsFunction fn) {
            sb.append('(');
            List<RsValueParameter> params = RsFunctionUtil.getValueParameters(fn);
            sb.append(params.stream()
                .map(p -> {
                    RsTypeReference typeRef = p.getTypeReference();
                    return typeRef != null ? RsPsiRenderer.getStubOnlyText(typeRef) : "";
                })
                .collect(Collectors.joining(", ")));
            sb.append(')');

            RsRetType retType = fn.getRetType();
            if (retType != null && retType.getTypeReference() != null) {
                sb.append(" -> ").append(RsPsiRenderer.getStubOnlyText(retType.getTypeReference()));
            }
        } else if (psi instanceof RsConstant constant) {
            RsTypeReference typeRef = constant.getTypeReference();
            if (typeRef != null) {
                sb.append(": ").append(RsPsiRenderer.getStubOnlyText(typeRef));
            }
        } else if (psi instanceof RsNamedFieldDecl field) {
            RsTypeReference typeRef = field.getTypeReference();
            if (typeRef != null) {
                sb.append(": ").append(RsPsiRenderer.getStubOnlyText(typeRef));
            }
        } else if (psi instanceof RsPatBinding binding) {
            RsInferenceResult inference = RsTypesUtil.getInference(binding);
            if (inference != null) {
                sb.append(": ").append(inference.getBindingType(binding));
            }
        } else if (psi instanceof RsEnumVariant variant) {
            RsTupleFields fields = variant.getTupleFields();
            if (fields != null) {
                sb.append('(');
                sb.append(fields.getTupleFieldDeclList().stream()
                    .map(f -> RsPsiRenderer.getStubOnlyText(f.getTypeReference()))
                    .collect(Collectors.joining(", ")));
                sb.append(')');
            }
        }

        String presentation = sb.toString();
        Icon icon = psi.getIcon(Iconable.ICON_FLAG_VISIBILITY);

        com.intellij.openapi.editor.colors.TextAttributesKey textAttributes = null;
        if (psi instanceof RsDocAndAttributeOwner owner
            && !RsDocAndAttributeOwnerUtil.isEnabledByCfgSelfOrInAttrProcMacroBody(owner)) {
            textAttributes = RsColor.CFG_DISABLED_CODE.getTextAttributesKey();
        } else if (RsExpandedElementUtil.isExpandedFromMacro(psi)) {
            textAttributes = RsColor.GENERATED_ITEM.getTextAttributesKey();
        }

        return new PresentationData(presentation, null, icon, textAttributes);
    }

    @Nullable
    private static String presentableName(@NotNull RsElement psi) {
        if (psi instanceof RsFunction fn) {
            return RsFunctionUtil.getFunctionName(fn);
        } else if (psi instanceof RsNamedElement named) {
            return named.getName();
        } else if (psi instanceof RsImplItem impl) {
            RsTypeReference typeRef = impl.getTypeReference();
            if (typeRef == null) return null;
            String type = typeRef.getText();
            RsTraitRef traitRef = impl.getTraitRef();
            String trait = traitRef != null ? traitRef.getText() : null;
            StringBuilder sb = new StringBuilder();
            if (trait != null) {
                if (RsImplItemUtil.isNegativeImpl(impl)) {
                    sb.append("!");
                }
                sb.append(trait).append(" for ");
            }
            sb.append(type);
            sb.append(typeParameterBounds(impl));
            return sb.toString();
        }
        return null;
    }

    @NotNull
    private static String typeParameterBounds(@NotNull RsImplItem impl) {
        List<RsTypeParameter> typeParams = RsGenericDeclarationUtil.getTypeParameters(impl);
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (RsTypeParameter param : typeParams) {
            String paramName = param.getName();
            if (paramName == null) continue;
            List<RsPolybound> bounds = RsTypeParameterUtil.getBounds(param);
            List<String> boundStrings = bounds.stream().map(bound -> {
                RsTraitRef traitRef = bound.getBound().getTraitRef();
                String traitName = traitRef != null && traitRef.getPath() != null ? traitRef.getPath().getReferenceName() : null;
                if (traitName == null) return null;
                return RsPolyboundUtil.getHasQ(bound) ? "?" + traitName : traitName;
            }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
            if (boundStrings.isEmpty()) continue;
            if (first) {
                result.append(" where ");
                first = false;
            } else {
                result.append(", ");
            }
            result.append(paramName).append(": ").append(String.join(" + ", boundStrings));
        }
        return result.toString();
    }

    @Nullable
    public static String getPresentableQualifiedName(@NotNull RsDocAndAttributeOwner element) {
        if (element instanceof RsQualifiedNamedElement qNamedElement) {
            String qName = qNamedElement.qualifiedName();
            if (qName != null) return qName;
        }
        if (element instanceof RsMod mod) {
            return mod.getModName();
        }
        if (element instanceof RsNamedElement named) {
            return named.getName();
        }
        return null;
    }
}
