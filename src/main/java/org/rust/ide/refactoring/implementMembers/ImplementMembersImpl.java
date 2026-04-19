/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.presentation.ImportingPsiRenderer;
import org.rust.ide.presentation.PsiRenderingOptions;
import org.rust.ide.settings.RsCodeInsightSettings;
import org.rust.ide.utils.imports.ImportCandidate;
import org.rust.ide.utils.imports.ImportCandidateUtil;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.SubstituteUtil;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public final class ImplementMembersImpl {

    private ImplementMembersImpl() {
    }

    public static void generateTraitMembers(@NotNull RsImplItem impl, @Nullable Editor editor) {
        org.rust.openapiext.OpenApiUtil.checkWriteAccessNotAllowed();
        RsTraitRef traitRef = impl.getTraitRef();
        BoundElement<RsTraitItem> trait = traitRef != null ? RsTraitRefUtil.resolveToBoundTrait(traitRef) : null;
        if (trait == null) {
            if (editor != null) {
                org.rust.openapiext.OpenApiUtil.showErrorHint(editor, RsBundle.message("hint.text.no.members.to.implement.have.been.found"));
            }
            return;
        }

        TraitImplementationInfo implInfo = TraitImplementationInfo.create((RsTraitItem) trait.getElement(), impl);
        if (implInfo == null || implInfo.declared.isEmpty()) {
            if (editor != null) {
                org.rust.openapiext.OpenApiUtil.showErrorHint(editor, RsBundle.message("hint.text.no.members.to.implement.have.been.found"));
            }
            return;
        }

        Collection<RsAbstractable> chosen = TraitMemberChooserUi.showTraitMemberChooser(implInfo, impl.getProject());
        if (chosen.isEmpty()) return;
        ApplicationManager.getApplication().runWriteAction(() -> {
            insertNewTraitMembers(chosen, impl, trait, editor);
        });
    }

    public static void generateMissingTraitMembers(@NotNull RsImplItem impl, @NotNull RsTraitRef traitRef, @Nullable Editor editor) {
        org.rust.openapiext.OpenApiUtil.checkReadAccessAllowed();
        BoundElement<RsTraitItem> trait = RsTraitRefUtil.resolveToBoundTrait(traitRef);
        if (trait == null) return;
        TraitImplementationInfo implInfo = TraitImplementationInfo.create((RsTraitItem) trait.getElement(), impl);
        if (implInfo == null || implInfo.declared.isEmpty()) return;

        IntentionPreviewUtils.write(() -> {
            insertNewTraitMembers(implInfo.missingImplementations, impl, trait, editor);
        });
    }

    private static void insertNewTraitMembers(
        @NotNull Collection<RsAbstractable> selected,
        @NotNull RsImplItem impl,
        @NotNull BoundElement<RsTraitItem> trait,
        @Nullable Editor editor
    ) {
        if (!RsPsiImplUtil.isIntentionPreviewElement(impl)) {
            org.rust.openapiext.OpenApiUtil.checkWriteAccessAllowed();
        }
        if (selected.isEmpty()) return;

        MembersGenerator gen = new MembersGenerator(new RsPsiFactory(impl.getProject()), impl, trait);
        RsMembers templateImpl = gen.createTraitMembers(selected);

        List<RsAbstractable> traitMembers = RsTraitOrImplUtil.getExpandedMembers((RsTraitItem) trait.getElement());
        List<RsAbstractable> newMembers = PsiElementUtil.childrenOfType(templateImpl, RsAbstractable.class);

        RsMembers existingMembers = impl.getMembers();
        if (existingMembers == null) return;
        List<RsAbstractable> existingMembersList = RsMembersUtil.getExpandedMembers(existingMembers);

        List<Map.Entry<RsAbstractable, Integer>> existingMembersWithPosInTrait = new ArrayList<>();
        for (RsAbstractable existingMember : existingMembersList) {
            int pos = -1;
            for (int i = 0; i < traitMembers.size(); i++) {
                RsAbstractable tm = traitMembers.get(i);
                if (tm.getClass().equals(existingMember.getClass()) && Objects.equals(tm.getName(), existingMember.getName())) {
                    pos = i;
                    break;
                }
            }
            existingMembersWithPosInTrait.add(new AbstractMap.SimpleEntry<>(existingMember, pos));
        }

        List<Integer> existingMembersOrder = existingMembersWithPosInTrait.stream()
            .map(Map.Entry::getValue).collect(Collectors.toList());
        List<Integer> sortedOrder = new ArrayList<>(existingMembersOrder);
        Collections.sort(sortedOrder);
        boolean areExistingMembersInTheRightOrder = existingMembersOrder.equals(sortedOrder);

        RsElement needToSelect = null;
        List<RsAbstractable> insertedMembers = new ArrayList<>();

        for (int index = 0; index < newMembers.size(); index++) {
            RsAbstractable newMember = newMembers.get(index);
            int posInTrait = -1;
            for (int i = 0; i < traitMembers.size(); i++) {
                RsAbstractable tm = traitMembers.get(i);
                if (tm.getClass().equals(newMember.getClass()) && Objects.equals(tm.getName(), newMember.getName())) {
                    posInTrait = i;
                    break;
                }
            }

            int anchorIndex = -1;
            PsiElement anchorElement = existingMembers.getLbrace();

            if (areExistingMembersInTheRightOrder || index > 0) {
                for (int i = existingMembersWithPosInTrait.size() - 1; i >= 0; i--) {
                    Map.Entry<RsAbstractable, Integer> entry = existingMembersWithPosInTrait.get(i);
                    if (entry.getValue() < posInTrait) {
                        anchorIndex = i;
                        RsAbstractable member = entry.getKey();
                        PsiElement expanded = RsExpandedElementUtil.expandedFromRecursively(member);
                        anchorElement = expanded != null ? expanded : member;
                        break;
                    }
                }
            } else {
                if (!existingMembersWithPosInTrait.isEmpty()) {
                    int lastIdx = existingMembersWithPosInTrait.size() - 1;
                    anchorIndex = lastIdx;
                    RsAbstractable member = existingMembersWithPosInTrait.get(lastIdx).getKey();
                    PsiElement expanded = RsExpandedElementUtil.expandedFromRecursively(member);
                    anchorElement = expanded != null ? expanded : member;
                }
            }

            RsAbstractable addedMember = (RsAbstractable) existingMembers.addAfter(newMember, anchorElement);
            existingMembersWithPosInTrait.add(anchorIndex + 1, new AbstractMap.SimpleEntry<>(addedMember, posInTrait));

            PsiElement prev = findAbstractableSibling(addedMember, true);
            if (prev != null && (prev instanceof RsFunction || addedMember instanceof RsFunction)) {
                PsiElement whitespaces = createExtraWhitespacesAroundFunction(prev, addedMember);
                existingMembers.addBefore(whitespaces, addedMember);
            }

            PsiElement next = findAbstractableSibling(addedMember, false);
            if (next != null && (next instanceof RsFunction || addedMember instanceof RsFunction)) {
                PsiElement whitespaces = createExtraWhitespacesAroundFunction(addedMember, next);
                existingMembers.addAfter(whitespaces, addedMember);
            }

            if (needToSelect == null) {
                if (addedMember instanceof RsFunction) {
                    RsBlock block = RsFunctionUtil.getBlock((RsFunction) addedMember);
                    needToSelect = block != null ? RsBlockUtil.syntaxTailStmt(block) : null;
                } else if (addedMember instanceof RsTypeAlias) {
                    needToSelect = ((RsTypeAlias) addedMember).getTypeReference();
                } else if (addedMember instanceof RsConstant) {
                    needToSelect = ((RsConstant) addedMember).getExpr();
                }
            }
            insertedMembers.add(addedMember);
        }

        if (RsCodeInsightSettings.getInstance().importOutOfScopeItems) {
            for (ImportCandidate importCandidate : gen.getItemsToImport()) {
                ImportCandidateUtil.doImport(importCandidate, existingMembers);
            }
        }

        simplifyConstExprs(insertedMembers);

        if (needToSelect != null && editor != null) {
            org.rust.openapiext.OpenApiUtil.selectElement(needToSelect, editor);
        }
    }

    @Nullable
    private static PsiElement findAbstractableSibling(@NotNull PsiElement element, boolean left) {
        PsiElement sibling = left ? element.getPrevSibling() : element.getNextSibling();
        while (sibling != null) {
            if (sibling instanceof RsAbstractable || sibling instanceof RsMacroCall) {
                return sibling;
            }
            sibling = left ? sibling.getPrevSibling() : sibling.getNextSibling();
        }
        return null;
    }

    private static void simplifyConstExprs(@NotNull List<RsAbstractable> insertedMembers) {
        for (RsAbstractable member : insertedMembers) {
            List<RsTypeArgumentList> typeArgLists = PsiElementUtil.descendantsOfType(member, RsTypeArgumentList.class);
            List<RsExpr> constExprs = new ArrayList<>();
            for (RsTypeArgumentList tal : typeArgLists) {
                constExprs.addAll(tal.getExprList());
            }
            List<RsArrayType> arrayTypes = PsiElementUtil.descendantsOfType(member, RsArrayType.class);
            for (RsArrayType at : arrayTypes) {
                if (at.getExpr() != null) {
                    constExprs.add(at.getExpr());
                }
            }
            for (RsExpr expr : constExprs) {
                if (expr instanceof RsBlockExpr) {
                    RsBlock block = ((RsBlockExpr) expr).getBlock();
                    RsExpr wrappingExpr = RsBlockUtil.singleTailStmt(block) != null ? RsBlockUtil.singleTailStmt(block).getExpr() : null;
                    if (wrappingExpr != null && (!(wrappingExpr instanceof RsPathExpr) || expr.getParent() instanceof RsArrayType)) {
                        expr.replace(wrappingExpr);
                    }
                }
            }
        }
    }

    @NotNull
    private static PsiElement createExtraWhitespacesAroundFunction(@NotNull PsiElement left, @NotNull PsiElement right) {
        int lineCount = 0;
        PsiElement sibling = left.getNextSibling();
        while (sibling != null && sibling != right) {
            if (sibling instanceof PsiWhiteSpace) {
                String text = sibling.getText();
                for (char c : text.toCharArray()) {
                    if (c == '\n') lineCount++;
                }
            }
            sibling = sibling.getNextSibling();
        }
        int extraLineCount = Math.max(0, 2 - lineCount);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < extraLineCount; i++) {
            sb.append('\n');
        }
        return new RsPsiFactory(left.getProject()).createWhitespace(sb.toString());
    }
}
