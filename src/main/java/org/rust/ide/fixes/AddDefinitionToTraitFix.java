/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.extractTrait.RsExtractTraitProcessor;
import org.rust.ide.utils.imports.ImportBridge;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class AddDefinitionToTraitFix extends RsQuickFixBase<RsAbstractable> {

    public AddDefinitionToTraitFix(@NotNull RsAbstractable member) {
        super(member);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.definition.to.trait");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsAbstractable element) {
        var impl = element.getParent() != null ? element.getParent().getParent() : null;
        if (!(impl instanceof RsImplItem)) return;
        RsImplItem rsImpl = (RsImplItem) impl;
        RsTraitRef traitRef = rsImpl.getTraitRef();
        if (traitRef == null) return;
        RsTraitItem trait = RsTraitRefUtil.resolveToTrait(traitRef);
        if (trait == null) return;
        trait = (RsTraitItem) RsElementUtil.findPreviewCopyIfNeeded(trait);

        RsPsiFactory factory = new RsPsiFactory(project);
        RsAbstractable newMember = (RsAbstractable) RsExtractTraitProcessor.makeAbstract(element.copy(), factory);
        RsMembers traitMembers = trait.getMembers();
        if (traitMembers == null) return;

        RsAbstractable anchor = findAnchor(rsImpl, trait, element);
        if (anchor == null) {
            var rbrace = traitMembers.getRbrace();
            if (rbrace == null) return;
            anchor = null;
            var prevSibling = rbrace.getPrevSibling();
            traitMembers.addAfter(newMember, prevSibling);
        } else {
            traitMembers.addAfter(newMember, anchor);
        }

        if (traitMembers.getLbrace() != null) {
            traitMembers.addAfter(factory.createNewline(), traitMembers.getLbrace());
        }

        if (!RsElementUtil.getContainingMod(trait).equals(RsElementUtil.getContainingMod(rsImpl))) {
            ImportBridge.importTypeReferencesFromElement(trait, element);
        }
    }

    @Nullable
    private static RsAbstractable findAnchor(@NotNull RsImplItem impl, @NotNull RsTraitItem trait, @NotNull RsAbstractable member) {
        List<RsAbstractable> traitMembers = RsMembersUtil.getExplicitMembers(trait);
        Map<RsAbstractable, RsAbstractable> mapping = new LinkedHashMap<>();
        for (RsAbstractable implMember : RsMembersUtil.getExplicitMembers(impl)) {
            RsAbstractable found = null;
            for (RsAbstractable traitMember : traitMembers) {
                if (implMember.getNode().getElementType() == traitMember.getNode().getElementType()
                    && java.util.Objects.equals(implMember.getName(), traitMember.getName())) {
                    found = traitMember;
                    break;
                }
            }
            mapping.put(implMember, found);
        }

        var filteredValues = mapping.values().stream()
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());
        boolean areMembersInOrder = filteredValues.equals(traitMembers);
        if (!areMembersInOrder) return null;

        RsAbstractable result = null;
        for (var entry : mapping.entrySet()) {
            if (entry.getKey() == member) break;
            if (entry.getValue() != null) {
                result = entry.getValue();
            }
        }
        return result;
    }

    @Nullable
    public static AddDefinitionToTraitFix createIfCompatible(@NotNull RsAbstractable member) {
        var parent = member.getParent();
        if (parent == null) return null;
        var grandParent = parent.getParent();
        if (!(grandParent instanceof RsImplItem)) return null;
        RsImplItem impl = (RsImplItem) grandParent;
        RsTraitRef traitRef = impl.getTraitRef();
        if (traitRef == null) return null;
        if (!checkTraitRef(traitRef)) return null;
        if (!checkMember(member)) return null;
        return new AddDefinitionToTraitFix(member);
    }

    private static boolean checkTraitRef(@NotNull RsTraitRef traitRef) {
        RsTypeArgumentList typeArgumentList = traitRef.getPath().getTypeArgumentList();
        if (typeArgumentList == null) return true;
        var generics = RsElementUtil.stubChildrenOfType(typeArgumentList, RsElement.class);
        return generics.stream().allMatch(generic ->
            generic instanceof RsLifetime ||
            (generic instanceof RsPathType && isGeneric(((RsPathType) generic).getPath()))
        );
    }

    private static boolean checkMember(@NotNull RsAbstractable member) {
        if (member instanceof RsTypeAlias) return true;

        java.util.List<com.intellij.psi.PsiElement> elementsToCheck;
        if (member instanceof RsFunction) {
            RsFunction fn = (RsFunction) member;
            elementsToCheck = new java.util.ArrayList<>(fn.getValueParameters());
            if (fn.getRetType() != null) {
                elementsToCheck.add(fn.getRetType());
            }
        } else if (member instanceof RsConstant) {
            RsConstant constant = (RsConstant) member;
            elementsToCheck = constant.getTypeReference() != null
                ? java.util.List.of(constant.getTypeReference())
                : java.util.Collections.emptyList();
        } else {
            throw new IllegalStateException("unreachable");
        }

        return elementsToCheck.stream().allMatch(element ->
            RsElementUtil.stubDescendantsOfTypeOrSelf(element, RsPath.class).stream().allMatch(path ->
                path.getHasColonColon() || !isGeneric(path)
            )
        );
    }

    private static boolean isGeneric(@NotNull RsPath path) {
        var ref = path.getReference();
        if (ref == null) return false;
        var target = ref.resolve();
        if (target == null) return false;
        return target instanceof RsTypeParameter || target instanceof RsConstParameter;
    }
}
