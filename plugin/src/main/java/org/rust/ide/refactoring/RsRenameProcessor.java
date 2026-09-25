/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.editor.Pass;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.impl.psi.RenameableFakePsiElement;
import consulo.content.scope.SearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.editor.refactoring.rename.RenameDialog;
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewUtil;
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.psi.ext.impl.RsStructLiteralFieldUtil;
import org.rust.lang.core.psi.ext.impl.RsTraitItemUtil;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;
import org.rust.lang.core.psi.ext.RsMod;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.impl.*;

@ExtensionImpl(id = "rsRenameProcessor")
public class RsRenameProcessor extends RenamePsiElementProcessor {

    @Nonnull
    @Override
    public RenameDialog createRenameDialog(
        @Nonnull Project project,
        @Nonnull PsiElement element,
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
    public boolean canProcessElement(@Nonnull PsiElement element) {
        return element instanceof RsNamedElement || element instanceof RsFakeMacroExpansionRenameablePsiElement;
    }

    @Override
    public void findExistingNameConflicts(
        @Nonnull PsiElement element,
        @Nonnull String newName,
        @Nonnull MultiMap<PsiElement, consulo.localize.LocalizeValue> conflicts
    ) {
        if (!(element instanceof RsPatBinding)) return;
        RsPatBinding binding = (RsPatBinding) element;
        RsFunction function = PsiTreeUtil.getParentOfType(binding, RsFunction.class);
        if (function == null) return;
        String functionName = function.getName();
        if (functionName == null) return;
        List<consulo.localize.LocalizeValue> foundConflicts = new ArrayList<>();

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
                    foundConflicts.add(consulo.localize.LocalizeValue.of(type + " `" + newName + "` is already declared in function `" + functionName + "`"));
                }
            });
        }

        if (!foundConflicts.isEmpty()) {
            conflicts.put(element, foundConflicts);
        }
    }

    @Override
    public void renameElement(
        @Nonnull PsiElement element,
        @Nonnull String newName,
        @Nonnull UsageInfo[] usages,
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
        @Nonnull PsiElement element,
        @Nonnull String newName,
        @Nonnull Map<PsiElement, String> allRenames,
        @Nonnull SearchScope scope
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

    @Nonnull
    @Override
    public PsiElement substituteElementToRename(@Nonnull PsiElement element, @Nullable Editor editor) {
        PsiElement superElement = element instanceof RsAbstractable
            ? (((RsAbstractable) element).getSuperItem() != null ? ((RsAbstractable) element).getSuperItem() : element)
            : element;
        PsiElement fakeElement = findFakeElementForRenameInMacroBody(superElement);
        return fakeElement != null ? fakeElement : superElement;
    }

    @Override
    public void substituteElementToRename(@Nonnull PsiElement element, @Nullable Editor editor, @Nonnull java.util.function.Consumer<PsiElement> renameCallback) {
        renameCallback.accept(substituteElementToRename(element, editor));
    }

    @Nonnull
    @Override
    public Collection<PsiReference> findReferences(@Nonnull PsiElement element, boolean searchInCommentsAndStrings) {
        PsiElement refinedElement = element instanceof RsFakeMacroExpansionRenameablePsiElement
            ? ((RsFakeMacroExpansionRenameablePsiElement) element).getExpandedElement()
            : element;
        return super.findReferences(refinedElement, searchInCommentsAndStrings);
    }

    @Override
    public void prepareRenaming(@Nonnull PsiElement element, @Nonnull String newName, @Nonnull Map<PsiElement, String> allRenames) {
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
    private static PsiElement findFakeElementForRenameInMacroBody(@Nonnull PsiElement element) {
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

    @Nonnull
    private static String ensureQuote(@Nonnull String name) {
        return name.startsWith("'") ? name : "'" + name;
    }
}
