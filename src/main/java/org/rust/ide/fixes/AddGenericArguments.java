/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.inspections.RsWrongGenericArgumentsNumberInspection;
import org.rust.ide.utils.template.EditorExt;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.rust.lang.core.psi.ext.RsElement;

public class AddGenericArguments extends RsQuickFixBase<RsMethodOrPath> {
    @SafeFieldForPreview
    private final SmartPsiElementPointer<RsGenericDeclaration> declaration;

    public AddGenericArguments(@NotNull SmartPsiElementPointer<RsGenericDeclaration> declaration,
                               @NotNull RsMethodOrPath element) {
        super(element);
        this.declaration = declaration;
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.missing", getArgumentsName());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.add.missing.generic.arguments");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsMethodOrPath element) {
        List<RsElement> inserted = insertGenericArgumentsIfNeeded(element);
        if (inserted == null) return;
        if (editor != null) {
            EditorExt.buildAndRunTemplate(editor, element, new ArrayList<>(inserted));
        }
    }

    @NotNull
    private String getArgumentsName() {
        RsGenericDeclaration element = declaration.getElement();
        if (element == null) return "generic arguments";
        boolean hasTypeParams = !element.getTypeParameters().isEmpty();
        boolean hasConstParams = !element.getConstParameters().isEmpty();
        if (hasTypeParams && hasConstParams) return "generic arguments";
        if (hasTypeParams) return "type arguments";
        if (hasConstParams) return "const arguments";
        return "generic arguments";
    }

    @Nullable
    public static List<RsElement> insertGenericArgumentsIfNeeded(@NotNull RsMethodOrPath pathOrMethodCall) {
        var result = RsWrongGenericArgumentsNumberInspection.getTypeArgumentsAndDeclaration(pathOrMethodCall);
        if (result == null) return null;
        RsTypeArgumentList typeArgumentsExisting = result.getFirst();
        RsGenericDeclaration declaration = result.getSecond();

        var requiredParameters = RsGenericDeclarationUtil.getRequiredGenericParameters(declaration);
        if (requiredParameters.isEmpty()) return null;

        RsPsiFactory factory = new RsPsiFactory(pathOrMethodCall.getProject());
        RsTypeArgumentList typeArguments = typeArgumentsExisting;
        if (typeArguments == null) {
            if (!(pathOrMethodCall instanceof RsPath)) return null;
            typeArguments = addEmptyTypeArguments((RsPath) pathOrMethodCall, factory);
        }

        int argumentCount = typeArguments.getTypeReferenceList().size() + typeArguments.getExprList().size();
        if (argumentCount >= requiredParameters.size()) return null;

        List<String> missingParams = requiredParameters.subList(argumentCount, requiredParameters.size()).stream()
            .map(p -> {
                String name = p.getName();
                return name != null ? name : "_";
            })
            .collect(Collectors.toList());

        List<PsiElement> allElements = new ArrayList<>();
        allElements.addAll(typeArguments.getLifetimeList());
        allElements.addAll(typeArguments.getTypeReferenceList());
        allElements.addAll(typeArguments.getExprList());

        PsiElement lastArgument = allElements.stream()
            .max(Comparator.comparingInt(e -> RsElementUtil.getStartOffset(e)))
            .orElse(typeArguments.getLt());

        List<RsTypeReference> types = missingParams.stream()
            .map(factory::createType)
            .collect(Collectors.toList());

        return new ArrayList<>(addElements(typeArguments, types, lastArgument, factory));
    }

    @NotNull
    public static RsTypeArgumentList addEmptyTypeArguments(@NotNull RsPath path, @NotNull RsPsiFactory factory) {
        RsTypeArgumentList list = factory.createTypeArgumentList(Collections.emptyList());
        return (RsTypeArgumentList) path.addAfter(list, path.getIdentifier());
    }

    @NotNull
    public static <T extends RsElement> List<T> addElements(
        @NotNull RsTypeArgumentList typeArgumentList,
        @NotNull List<T> elements,
        @NotNull PsiElement anchor,
        @NotNull RsPsiFactory factory
    ) {
        PsiElement nextSibling = RsElementUtil.getNextNonCommentSibling(anchor);
        if (nextSibling == null) return Collections.emptyList();
        boolean addCommaAfter = isComma(nextSibling);

        PsiElement currentAnchor = addCommaAfter ? nextSibling : anchor;

        List<T> added = new ArrayList<>();
        for (T element : elements) {
            if (currentAnchor.getNode().getElementType() != RsElementTypes.LT && !isComma(currentAnchor)) {
                currentAnchor = typeArgumentList.addAfter(factory.createComma(), currentAnchor);
            }
            currentAnchor = typeArgumentList.addAfter(element, currentAnchor);
            @SuppressWarnings("unchecked")
            T typedAnchor = (T) currentAnchor;
            added.add(typedAnchor);
        }
        if (addCommaAfter) {
            typeArgumentList.addAfter(factory.createComma(), currentAnchor);
        }
        return added;
    }

    private static boolean isComma(@NotNull PsiElement element) {
        return element.getNode().getElementType() == RsElementTypes.COMMA;
    }
}
