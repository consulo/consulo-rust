/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.*;
import com.intellij.psi.impl.RenameableFakePsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenameDialog;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ProcessLocalVariablesUtil;
import org.rust.lang.core.resolve.ref.RsReferenceBase;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsStructLiteralFieldUtil;
import org.rust.lang.core.psi.ext.RsTraitItemUtil;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;
import org.rust.lang.core.psi.ext.RsMod;

public class RsRenameProcessor extends RenamePsiElementProcessor {

    @NotNull
    @Override
    public RenameDialog createRenameDialog(
        @NotNull Project project,
        @NotNull PsiElement element,
        @Nullable PsiElement nameSuggestionContext,
        @Nullable Editor editor
    ) {
        return new RenameDialog(project, element, nameSuggestionContext, editor) {
            @Override
            protected String getFullName() {
                if (element instanceof RsFile) {
                    String modName = ((RsFile) element).getModName();
                    if (modName != null) {
                        return "module " + modName;
                    }
                }
                return super.getFullName();
            }
        };
    }

    @Override
    public boolean canProcessElement(@NotNull PsiElement element) {
        return element instanceof RsNamedElement || element instanceof RsFakeMacroExpansionRenameablePsiElement;
    }

    @Override
    public void findExistingNameConflicts(
        @NotNull PsiElement element,
        @NotNull String newName,
        @NotNull MultiMap<PsiElement, String> conflicts
    ) {
        if (!(element instanceof RsPatBinding)) return;
        RsPatBinding binding = (RsPatBinding) element;
        RsFunction function = PsiTreeUtil.getParentOfType(binding, RsFunction.class);
        if (function == null) return;
        String functionName = function.getName();
        if (functionName == null) return;
        List<String> foundConflicts = new ArrayList<>();

        RsElement scope;
        if (PsiTreeUtil.getParentOfType(binding, RsValueParameter.class) != null) {
            PsiElement rbrace = RsFunctionUtil.getBlock(function) != null ? RsFunctionUtil.getBlock(function).getRbrace() : null;
            scope = rbrace != null ? (RsElement) RsElementUtil.getPrevNonCommentSibling(rbrace) : null;
        } else {
            scope = binding;
        }

        if (scope != null) {
            ProcessLocalVariablesUtil.processLocalVariables(scope, it -> {
                if (newName.equals(it.getName())) {
                    String type;
                    PsiElement parent = it.getParent();
                    if (parent instanceof RsPatIdent) {
                        if (PsiTreeUtil.getParentOfType(it, RsValueParameter.class) != null) {
                            type = "Parameter";
                        } else {
                            type = "Variable";
                        }
                    } else {
                        type = "Binding";
                    }
                    foundConflicts.add(type + " `" + newName + "` is already declared in function `" + functionName + "`");
                }
            });
        }

        if (!foundConflicts.isEmpty()) {
            conflicts.put(element, foundConflicts);
        }
    }

    @Override
    public void renameElement(
        @NotNull PsiElement element,
        @NotNull String newName,
        @NotNull UsageInfo[] usages,
        @Nullable RefactoringElementListener listener
    ) {
        RsPsiFactory psiFactory = new RsPsiFactory(element.getProject());
        if (!(element instanceof RsNamedFieldDecl)) {
            for (UsageInfo usage : usages) {
                PsiElement usageElement = usage.getElement();
                if (usageElement == null) continue;
                RsStructLiteralField field = PsiTreeUtil.getParentOfType(usageElement, RsStructLiteralField.class, false, RsBlock.class);
                if (field == null) continue;
                if (RsStructLiteralFieldUtil.isShorthand(field)) {
                    RsStructLiteralField newPatField = psiFactory.createStructLiteralField(field.getReferenceName(), newName);
                    field.replace(newPatField);
                } else if (newName.equals(field.getReferenceName()) && field.getExpr() instanceof RsPathExpr
                    && ((RsPathExpr) field.getExpr()).getPath() == usageElement) {
                    if (field.getExpr() != null) field.getExpr().delete();
                    if (field.getColon() != null) field.getColon().delete();
                }
            }
        }

        PsiElement newRenameElement;
        if (element instanceof RsPatBinding && element.getParent() != null && element.getParent().getParent() instanceof RsPatStruct) {
            RsPatBinding binding = (RsPatBinding) element;
            PsiElement newPatField = psiFactory.createPatFieldFull(binding.getIdentifier().getText(), binding.getText());
            PsiElement replaced = element.replace(newPatField);
            newRenameElement = PsiTreeUtil.findChildOfType(replaced, RsPatBinding.class);
            if (newRenameElement == null) newRenameElement = replaced;
        } else {
            newRenameElement = element;
        }
        super.renameElement(newRenameElement, newName, usages, listener);
    }

    @Override
    public void prepareRenaming(
        @NotNull PsiElement element,
        @NotNull String newName,
        @NotNull Map<PsiElement, String> allRenames,
        @NotNull SearchScope scope
    ) {
        PsiElement semanticElement = element instanceof RsFakeMacroExpansionRenameablePsiElement
            ? ((RsFakeMacroExpansionRenameablePsiElement) element).getExpandedElement()
            : element;

        String rename;
        if (semanticElement instanceof RsLifetime
            || semanticElement instanceof RsLifetimeParameter
            || semanticElement instanceof RsLabel
            || semanticElement instanceof RsLabelDecl) {
            rename = ensureQuote(newName);
        } else {
            rename = newName.startsWith("'") ? newName.substring(1) : newName;
        }

        allRenames.put(element, rename);
    }

    @NotNull
    @Override
    public PsiElement substituteElementToRename(@NotNull PsiElement element, @Nullable Editor editor) {
        PsiElement superElement = element instanceof RsAbstractable
            ? (((RsAbstractable) element).getSuperItem() != null ? ((RsAbstractable) element).getSuperItem() : element)
            : element;
        PsiElement fakeElement = findFakeElementForRenameInMacroBody(superElement);
        return fakeElement != null ? fakeElement : superElement;
    }

    @Override
    public void substituteElementToRename(@NotNull PsiElement element, @Nullable Editor editor, @NotNull Pass<? super PsiElement> renameCallback) {
        renameCallback.pass(substituteElementToRename(element, editor));
    }

    @NotNull
    @Override
    public Collection<PsiReference> findReferences(@NotNull PsiElement element, @NotNull SearchScope searchScope, boolean searchInCommentsAndStrings) {
        PsiElement refinedElement = element instanceof RsFakeMacroExpansionRenameablePsiElement
            ? ((RsFakeMacroExpansionRenameablePsiElement) element).getExpandedElement()
            : element;
        return super.findReferences(refinedElement, searchScope, searchInCommentsAndStrings);
    }

    @Override
    public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
        super.prepareRenaming(element, newName, allRenames);
        PsiElement semanticElement = element instanceof RsFakeMacroExpansionRenameablePsiElement
            ? ((RsFakeMacroExpansionRenameablePsiElement) element).getExpandedElement()
            : element;

        if (semanticElement instanceof RsAbstractable) {
            RsAbstractableOwner owner = RsAbstractableUtil.getOwner((RsAbstractable) semanticElement);
            if (!(owner instanceof RsAbstractableOwner.Trait)) return;
            RsTraitItem trait = ((RsAbstractableOwner.Trait) owner).getTrait();
            for (RsImplItem implItem : RsTraitItemUtil.searchForImplementations(trait)) {
                RsAbstractable corresponding = RsAbstractableUtil.findCorrespondingElement((RsTraitOrImpl) implItem, (RsAbstractable) semanticElement);
                if (corresponding != null) {
                    PsiElement fake = findFakeElementForRenameInMacroBody(corresponding);
                    allRenames.put(fake != null ? fake : corresponding, newName);
                }
            }
        } else if (semanticElement instanceof RsMod) {
            if (semanticElement instanceof RsFile && ((RsFile) semanticElement).getDeclaration() == null) return;
            if (((RsMod) semanticElement).getPathAttribute() != null) return;
            PsiDirectory ownedDir = ((RsMod) semanticElement).getOwnedDirectory();
            if (ownedDir == null) return;
            allRenames.put(ownedDir, newName);
        }
    }

    @Nullable
    private static PsiElement findFakeElementForRenameInMacroBody(@NotNull PsiElement element) {
        if (element instanceof RsNameIdentifierOwner) {
            RsNameIdentifierOwner namedElement = (RsNameIdentifierOwner) element;
            PsiElement identifier = namedElement.getNameIdentifier();
            if (identifier == null) return null;
            PsiElement sourceIdentifier = RsExpandedElementUtil.findElementExpandedFrom(identifier);
            if (sourceIdentifier == null) return null;
            PsiElement sourceIdentifierParent = sourceIdentifier.getParent();

            if (sourceIdentifierParent instanceof RsNameIdentifierOwner) {
                if (namedElement.getName() != null && namedElement.getName().equals(((RsNameIdentifierOwner) sourceIdentifierParent).getName())) {
                    return new RsFakeMacroExpansionRenameablePsiElement.AttrMacro(namedElement, (RsNameIdentifierOwner) sourceIdentifierParent);
                }
            } else if (sourceIdentifierParent instanceof RsMacroBodyIdent) {
                if (namedElement.getName() != null && namedElement.getName().equals(((RsMacroBodyIdent) sourceIdentifierParent).getReferenceName())) {
                    return new RsFakeMacroExpansionRenameablePsiElement.BangMacro(namedElement, (RsReferenceElementBase) sourceIdentifierParent);
                }
            } else if (sourceIdentifierParent instanceof RsMacroBodyQuoteIdent) {
                if (namedElement.getName() != null && namedElement.getName().equals(((RsMacroBodyQuoteIdent) sourceIdentifierParent).getReferenceName())) {
                    return new RsFakeMacroExpansionRenameablePsiElement.BangMacro(namedElement, (RsReferenceElementBase) sourceIdentifierParent);
                }
            } else if (sourceIdentifierParent instanceof RsPath) {
                if (namedElement.getName() != null && namedElement.getName().equals(((RsPath) sourceIdentifierParent).getReferenceName())) {
                    return new RsFakeMacroExpansionRenameablePsiElement.AttrPath(namedElement, sourceIdentifier);
                }
            }
        }
        return null;
    }

    @NotNull
    private static String ensureQuote(@NotNull String name) {
        return name.startsWith("'") ? name : "'" + name;
    }
}
