/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Iconable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.colors.RsColor;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ExtensionsUtil;

import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.RsImplItemUtil;
import org.rust.lang.core.psi.ext.RsTypeParameterUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public final class PresentationUtil {

    private PresentationUtil() {
    }

    @NotNull
    public static ItemPresentation getPresentation(@NotNull RsElement psi) {
        RsMod mod = psi.getContainingMod();
        String qualifiedName = mod.getQualifiedName();
        String modName = mod.getModName();
        String fileName = psi.getContainingFile().getName();
        String location = "(in " + (qualifiedName != null ? qualifiedName : (modName != null ? modName : fileName)) + ")";

        String name = presentableName(psi);
        return new PresentationData(name, location, psi.getIcon(0), null);
    }

    @NotNull
    public static ItemPresentation getPresentationForStructure(@NotNull RsElement psi) {
        StringBuilder sb = new StringBuilder();
        String name = presentableName(psi);
        if (name != null) {
            sb.append(name);
        }
        if (psi instanceof RsFunction) {
            RsFunction fn = (RsFunction) psi;
            sb.append('(');
            sb.append(fn.getValueParameters().stream()
                .map(p -> p.getTypeReference() != null ? RsPsiRenderer.getStubOnlyText(p.getTypeReference()) : "")
                .collect(Collectors.joining(", ")));
            sb.append(')');
            RsRetType ret = fn.getRetType();
            if (ret != null && ret.getTypeReference() != null) {
                sb.append(" -> ").append(RsPsiRenderer.getStubOnlyText(ret.getTypeReference()));
            }
        } else if (psi instanceof RsConstant) {
            RsConstant c = (RsConstant) psi;
            if (c.getTypeReference() != null) {
                sb.append(": ").append(RsPsiRenderer.getStubOnlyText(c.getTypeReference()));
            }
        } else if (psi instanceof RsNamedFieldDecl) {
            RsNamedFieldDecl f = (RsNamedFieldDecl) psi;
            if (f.getTypeReference() != null) {
                sb.append(": ").append(RsPsiRenderer.getStubOnlyText(f.getTypeReference()));
            }
        } else if (psi instanceof RsPatBinding) {
            RsPatBinding binding = (RsPatBinding) psi;
            var inference = ExtensionsUtil.getInference(binding);
            if (inference != null) {
                sb.append(": ").append(inference.getBindingType(binding));
            }
        } else if (psi instanceof RsEnumVariant) {
            RsEnumVariant variant = (RsEnumVariant) psi;
            RsTupleFields fields = variant.getTupleFields();
            if (fields != null) {
                sb.append('(');
                sb.append(fields.getTupleFieldDeclList().stream()
                    .map(f -> RsPsiRenderer.getStubOnlyText(f.getTypeReference()))
                    .collect(Collectors.joining(", ")));
                sb.append(')');
            }
        }

        var icon = psi.getIcon(Iconable.ICON_FLAG_VISIBILITY);
        TextAttributesKey textAttributes = null;
        if (psi instanceof RsDocAndAttributeOwner && !RsDocAndAttributeOwnerUtil.isEnabledByCfgSelfOrInAttrProcMacroBody((RsDocAndAttributeOwner) psi)) {
            textAttributes = RsColor.CFG_DISABLED_CODE.getTextAttributesKey();
        } else if (RsExpandedElementUtil.isExpandedFromMacro(psi)) {
            textAttributes = RsColor.GENERATED_ITEM.getTextAttributesKey();
        }

        return new PresentationData(sb.toString(), null, icon, textAttributes);
    }

    @Nullable
    public static String getPresentableQualifiedName(@NotNull RsDocAndAttributeOwner owner) {
        if (owner instanceof RsQualifiedNamedElement) {
            String qName = ((RsQualifiedNamedElement) owner).getQualifiedName();
            if (qName != null) return qName;
        }
        if (owner instanceof RsMod) {
            return ((RsMod) owner).getModName();
        }
        return owner.getName();
    }

    @Nullable
    private static String presentableName(@NotNull RsElement psi) {
        if (psi instanceof RsFunction) {
            return RsFunctionUtil.getFunctionName((RsFunction) psi);
        }
        if (psi instanceof RsNamedElement) {
            return ((RsNamedElement) psi).getName();
        }
        if (psi instanceof RsImplItem) {
            RsImplItem impl = (RsImplItem) psi;
            if (impl.getTypeReference() == null) return null;
            String type = impl.getTypeReference().getText();
            RsTraitRef traitRef = impl.getTraitRef();
            StringBuilder sb = new StringBuilder();
            if (traitRef != null) {
                if (RsImplItemUtil.isNegativeImpl(impl)) {
                    sb.append("!");
                }
                sb.append(traitRef.getText()).append(" for ");
            }
            sb.append(type);
            sb.append(typeParameterBounds(impl));
            return sb.toString();
        }
        return null;
    }

    @NotNull
    private static String typeParameterBounds(@NotNull RsImplItem impl) {
        List<String> allBounds = impl.getTypeParameters().stream()
            .map(param -> {
                String paramName = param.getName();
                if (paramName == null) return null;
                List<String> bounds = RsTypeParameterUtil.getBounds(param).stream()
                    .map(it -> {
                        RsTraitRef traitRef = it.getBound().getTraitRef();
                        if (traitRef == null || traitRef.getPath() == null) return null;
                        String bound = traitRef.getPath().getReferenceName();
                        if (bound == null) return null;
                        return RsPolyboundUtil.getHasQ(it) ? "?" + bound : bound;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
                if (!bounds.isEmpty()) {
                    return paramName + ": " + String.join(" + ", bounds);
                }
                return null;
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
        if (!allBounds.isEmpty()) {
            return " where " + String.join(", ", allBounds);
        }
        return "";
    }
}
