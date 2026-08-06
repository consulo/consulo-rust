/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractTrait;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.usage.BaseUsageViewDescriptor;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.GenericConstraints;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.stdext.StdextUtil;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsTraitItemUtil;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.PsiElementUtil;

/**
 * This refactoring can be applied to either inherent impl or trait.
 */
public class RsExtractTraitProcessor extends BaseRefactoringProcessor {
    @Nonnull
    private final RsTraitOrImpl myTraitOrImpl;
    @Nonnull
    private final String myTraitName;
    @Nonnull
    private final List<RsItemElement> myMembers;
    @Nonnull
    private final RsPsiFactory myPsiFactory;

    public RsExtractTraitProcessor(
        @Nonnull RsTraitOrImpl traitOrImpl,
        @Nonnull String traitName,
        @Nonnull List<RsItemElement> members
    ) {
        super(traitOrImpl.getProject());
        myTraitOrImpl = traitOrImpl;
        myTraitName = traitName;
        myMembers = members;
        myPsiFactory = new RsPsiFactory(traitOrImpl.getProject());
    }

    @Nonnull
    @Override
    protected consulo.localize.LocalizeValue getCommandName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("command.name.extract.trait"));
    }

    @Nonnull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
        List<PsiElement> elements = new ArrayList<>();
        elements.add(myTraitOrImpl);
        for (UsageInfo usage : usages) {
            if (usage.getElement() != null) {
                elements.add(usage.getElement());
            }
        }
        return new BaseUsageViewDescriptor(elements.toArray(PsiElement.EMPTY_ARRAY));
    }

    @Nonnull
    @Override
    protected UsageInfo[] findUsages() {
        List<UsageInfo> result = new ArrayList<>();

        List<ImplUsage> implUsages = new ArrayList<>();
        if (myTraitOrImpl instanceof RsTraitItem) {
            RsTraitItem traitItem = (RsTraitItem) myTraitOrImpl;
            for (RsImplItem impl : RsTraitItemUtil.searchForImplementations(traitItem)) {
                ImplUsage implUsage = new ImplUsage(impl);
                implUsages.add(implUsage);
                result.add(implUsage);
            }
        }

        Set<String> membersNames = new HashSet<>();
        for (RsItemElement member : myMembers) {
            if (member.getName() != null) {
                membersNames.add(member.getName());
            }
        }

        List<RsItemElement> membersInImpls = new ArrayList<>();
        for (ImplUsage implUsage : implUsages) {
            membersInImpls.addAll(getMembersWithNames(implUsage.myImpl, membersNames));
        }

        List<RsItemElement> membersAll = new ArrayList<>(myMembers);
        membersAll.addAll(membersInImpls);

        for (RsItemElement member : membersAll) {
            ReferencesSearch.search(member, member.getUseScope()).forEach(ref -> {
                result.add(new MemberUsage(ref));
            });
        }

        return result.toArray(UsageInfo.EMPTY_ARRAY);
    }

    @Override
    protected void performRefactoring(@Nonnull UsageInfo[] usages) {
        GenericConstraints newTraitConstraints = createNewTraitConstraints();
        RsTraitItem newTrait = createNewTrait(newTraitConstraints);
        if (newTrait == null) return;

        if (myTraitOrImpl instanceof RsTraitItem) {
            addDerivedBound((RsTraitItem) myTraitOrImpl);
        }

        addTraitImports(usages, newTrait);

        List<RsImplItem> impls = new ArrayList<>();
        for (UsageInfo usage : usages) {
            if (usage instanceof ImplUsage) {
                impls.add(((ImplUsage) usage).myImpl);
            }
        }
        if (myTraitOrImpl instanceof RsImplItem) {
            impls.add((RsImplItem) myTraitOrImpl);
        }

        for (RsImplItem impl : impls) {
            RsImplItem newImpl = createNewImpl(impl, newTraitConstraints);
            if (newImpl == null) continue;
            if (!Objects.equals(RsElementUtil.containingMod(newImpl), RsElementUtil.containingMod(newTrait))) {
                RsImportHelper.importElement(newImpl, newTrait);
            }

            if (impl.getTraitRef() == null && RsTraitOrImplUtil.getExplicitMembers(impl).isEmpty()) {
                impl.delete();
            }
        }
    }

    @Nonnull
    private GenericConstraints createNewTraitConstraints() {
        List<RsTypeReference> typesInsideMembers = new ArrayList<>();
        for (RsItemElement member : myMembers) {
            if (member instanceof RsFunction) {
                RsFunction function = (RsFunction) member;
                if (function.getRetType() != null) {
                    typesInsideMembers.addAll(PsiElementUtil.descendantsOfType(function.getRetType(), RsTypeReference.class));
                }
                if (function.getValueParameterList() != null) {
                    typesInsideMembers.addAll(PsiElementUtil.descendantsOfType(function.getValueParameterList(), RsTypeReference.class));
                }
            }
        }
        return GenericConstraints.create(myTraitOrImpl).filterByTypeReferences(typesInsideMembers);
    }

    @Nullable
    private RsTraitItem createNewTrait(@Nonnull GenericConstraints constraints) {
        String typeParameters = constraints.buildTypeParameters();
        String whereClause = constraints.buildWhereClause();

        List<PsiElement> membersCopy;
        if (myTraitOrImpl instanceof RsImplItem) {
            membersCopy = myMembers.stream()
                .map(it -> makeAbstract((PsiElement) it.copy(), myPsiFactory))
                .collect(Collectors.toList());
        } else if (myTraitOrImpl instanceof RsTraitItem) {
            membersCopy = new ArrayList<>();
            for (RsItemElement member : myMembers) {
                membersCopy.add(member.copy());
                member.delete();
            }
        } else {
            return null;
        }

        String traitBody = membersCopy.stream().map(PsiElement::getText).collect(Collectors.joining("\n"));
        String visibility = getTraitVisibility();
        RsTraitItem trait = myPsiFactory.tryCreateTraitItem(
            visibility + "trait " + myTraitName + " " + typeParameters + " " + whereClause + " {\n" + traitBody + "\n}"
        );
        if (trait == null) return null;

        copyAttributes(myTraitOrImpl, trait);
        return (RsTraitItem) myTraitOrImpl.getParent().addAfter(trait, myTraitOrImpl);
    }

    @Nullable
    private RsImplItem createNewImpl(@Nonnull RsImplItem impl, @Nonnull GenericConstraints traitConstraints) {
        RsTypeReference typeRef = impl.getTypeReference();
        if (typeRef == null) return null;
        String typeText = typeRef.getText();
        String typeParametersStruct = impl.getTypeParameterList() != null ? impl.getTypeParameterList().getText() : "";
        String whereClauseStruct = impl.getWhereClause() != null ? impl.getWhereClause().getText() : "";
        String typeArgumentsTrait = traitConstraints.buildTypeArguments();

        String newImplBody = extractMembersFromOldImpl(impl);
        RsImplItem newImpl = myPsiFactory.tryCreateImplItem(
            "impl " + typeParametersStruct + " " + myTraitName + " " + typeArgumentsTrait + " for " + typeText + " " + whereClauseStruct + " {\n" + newImplBody + "\n}"
        );
        if (newImpl == null) return null;

        copyAttributes(myTraitOrImpl, newImpl);
        return (RsImplItem) impl.getParent().addAfter(newImpl, impl);
    }

    @Nonnull
    private String getTraitVisibility() {
        RsVisibility unitedVisibility;
        if (myTraitOrImpl instanceof RsTraitItem) {
            unitedVisibility = RsVisibilityUtil.getVisibility((RsTraitItem) myTraitOrImpl);
        } else if (myTraitOrImpl instanceof RsImplItem) {
            unitedVisibility = myMembers.stream()
                .map(it -> RsVisibilityUtil.getVisibility(it))
                .reduce(RsVisibilityUtil::unite)
                .orElse(RsVisibility.Private.INSTANCE);
        } else {
            throw new IllegalStateException("unreachable");
        }
        return RsVisibilityUtil.format(unitedVisibility);
    }

    @Nonnull
    private String extractMembersFromOldImpl(@Nonnull RsImplItem impl) {
        Set<String> membersNames = new HashSet<>();
        for (RsItemElement member : myMembers) {
            if (member.getName() != null) {
                membersNames.add(member.getName());
            }
        }
        List<RsItemElement> membersToMove = getMembersWithNames(impl, membersNames);
        StringBuilder sb = new StringBuilder();
        for (RsItemElement member : membersToMove) {
            if (member instanceof RsVisibilityOwner) {
                RsVis vis = ((RsVisibilityOwner) member).getVis();
                if (vis != null) vis.delete();
            }
            PsiElement prevSibling = member.getPrevSibling();
            if (prevSibling instanceof PsiWhiteSpace) {
                prevSibling.delete();
            }
            if (sb.length() > 0) sb.append("\n");
            sb.append(member.getText());
            member.delete();
        }
        return sb.toString();
    }

    private void copyAttributes(@Nonnull RsTraitOrImpl source, @Nonnull RsTraitOrImpl target) {
        for (RsOuterAttr attr : source.getOuterAttrList()) {
            if ("doc".equals(attr.getMetaItem().getName())) continue;
            PsiElement inserted = target.addAfter(attr, null);
            target.addAfter(myPsiFactory.createNewline(), inserted);
        }

        RsMembers targetMembers = target.getMembers();
        if (targetMembers == null) return;
        for (RsInnerAttr attr : source.getInnerAttrList()) {
            if ("doc".equals(attr.getMetaItem().getName())) continue;
            targetMembers.addAfter(attr, targetMembers.getLbrace());
            targetMembers.addAfter(myPsiFactory.createNewline(), targetMembers.getLbrace());
        }
    }

    private void addDerivedBound(@Nonnull RsTraitItem trait) {
        RsTypeParamBounds typeParamBounds = trait.getTypeParamBounds();
        if (typeParamBounds == null) {
            PsiElement anchor = trait.getIdentifier();
            if (anchor == null) anchor = trait.getTypeParameterList();
            trait.addAfter(myPsiFactory.createTypeParamBounds(myTraitName), anchor);
        } else {
            typeParamBounds.addAfter(myPsiFactory.createPlus(), typeParamBounds.getColon());
            typeParamBounds.addAfter(myPsiFactory.createPolybound(myTraitName), typeParamBounds.getColon());
        }
    }

    private void addTraitImports(@Nonnull UsageInfo[] usages, @Nonnull RsTraitItem trait) {
        Set<RsMod> mods = new HashSet<>();
        for (UsageInfo usage : usages) {
            if (usage instanceof MemberUsage) {
                PsiElement element = usage.getElement();
                if (element instanceof RsElement) {
                    RsMod mod = RsElementUtil.containingMod((RsElement) element);
                    if (mod != null) {
                        mods.add(mod);
                    }
                }
            }
        }
        for (RsMod mod : mods) {
            RsElement context = PsiElementUtil.childOfType(mod, RsElement.class);
            if (context != null) {
                RsImportHelper.importElement(context, trait);
            }
        }
    }

    @Nonnull
    private static List<RsItemElement> getMembersWithNames(@Nonnull RsImplItem impl, @Nonnull Set<String> names) {
        RsMembers members = impl.getMembers();
        if (members == null) return Collections.emptyList();
        List<RsItemElement> itemElements = PsiElementUtil.childrenOfType(members, RsItemElement.class);
        return itemElements.stream()
            .filter(it -> it.getName() != null && names.contains(it.getName()))
            .collect(Collectors.toList());
    }

    @Nonnull
    public static PsiElement makeAbstract(@Nonnull PsiElement element, @Nonnull RsPsiFactory psiFactory) {
        if (element instanceof RsVisibilityOwner) {
            RsVis vis = ((RsVisibilityOwner) element).getVis();
            if (vis != null) vis.delete();
        }
        if (element instanceof RsFunction) {
            RsFunction function = (RsFunction) element;
            if (RsFunctionUtil.getBlock(function) != null) RsFunctionUtil.getBlock(function).delete();
            if (function.getSemicolon() == null) function.add(psiFactory.createSemicolon());
        } else if (element instanceof RsConstant) {
            RsConstant constant = (RsConstant) element;
            if (constant.getEq() != null) constant.getEq().delete();
            if (constant.getExpr() != null) constant.getExpr().delete();
        } else if (element instanceof RsTypeAlias) {
            RsTypeAlias typeAlias = (RsTypeAlias) element;
            if (typeAlias.getEq() != null) typeAlias.getEq().delete();
            if (typeAlias.getTypeReference() != null) typeAlias.getTypeReference().delete();
        }
        return element;
    }

    private static class MemberUsage extends UsageInfo {
        MemberUsage(@Nonnull PsiReference reference) {
            super(reference);
        }
    }

    private static class ImplUsage extends UsageInfo {
        @Nonnull
        final RsImplItem myImpl;

        ImplUsage(@Nonnull RsImplItem impl) {
            super(impl);
            myImpl = impl;
        }
    }
}
