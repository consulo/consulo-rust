/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineTypeAlias;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.DeleteUseSpeckUtil;
import org.rust.ide.presentation.RsPsiRendererUtil;
import org.rust.ide.refactoring.RsInlineUsageViewDescriptor;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.infer.SubstituteUtil;
import org.rust.lang.core.types.ty.TyTypeParameter;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

public class RsInlineTypeAliasProcessor extends BaseRefactoringProcessor {
    @NotNull
    private final RsTypeAlias myTypeAlias;
    @Nullable
    private final RsReference myReference;
    private final boolean myInlineThisOnly;

    public RsInlineTypeAliasProcessor(
        @NotNull Project project,
        @NotNull RsTypeAlias typeAlias,
        @Nullable RsReference reference,
        boolean inlineThisOnly
    ) {
        super(project);
        myTypeAlias = typeAlias;
        myReference = reference;
        myInlineThisOnly = inlineThisOnly;
    }

    @NotNull
    @Override
    protected UsageInfo[] findUsages() {
        List<? extends com.intellij.psi.PsiReference> usages;
        if (myInlineThisOnly && myReference != null) {
            usages = Collections.singletonList(myReference);
        } else {
            usages = new ArrayList<>(RsSearchableUtil.searchReferences(myTypeAlias, myTypeAlias.getUseScope()));
        }
        return usages.stream()
            .map(UsageInfo::new)
            .toArray(UsageInfo[]::new);
    }

    @Override
    protected void performRefactoring(@NotNull UsageInfo[] usages) {
        List<PathUsage> pathUsages = new ArrayList<>();
        List<RsUseSpeck> useSpecks = new ArrayList<>();
        int totalProcessed = 0;

        for (UsageInfo usage : usages) {
            PsiElement element = usage.getElement();
            if (!(element instanceof RsPath)) continue;
            RsPath path = (RsPath) element;
            if (RsExpandedElementUtil.isExpandedFromMacro(path)) continue;

            RsUseSpeck useSpeck = RsElementUtil.ancestorOrSelf(path, RsUseSpeck.class);
            if (useSpeck != null) {
                useSpecks.add(useSpeck);
                totalProcessed++;
            } else {
                if (path.getReference() == null) continue;
                BoundElement<RsElement> resolved = path.getReference().advancedResolve();
                if (resolved == null) continue;
                Substitution substitution = tryGetTypeAliasSubstitutionUsingParent(path, myTypeAlias);
                if (substitution == null) {
                    substitution = resolved.getSubst();
                }
                pathUsages.add(new PathUsage(path, substitution));
                totalProcessed++;
            }
        }

        RsTypeReference typeReference = myTypeAlias.getTypeReference();
        if (typeReference == null) return;

        List<RsElement> inlined = new ArrayList<>();
        for (PathUsage pathUsage : pathUsages) {
            RsElement result = fillPathWithActualType(pathUsage.myPath, typeReference, pathUsage.mySubstitution);
            if (result != null) {
                inlined.add(result);
            }
        }
        for (RsUseSpeck useSpeck : useSpecks) {
            DeleteUseSpeckUtil.deleteUseSpeck(useSpeck);
        }
        addNeededImports(inlined);

        if (!myInlineThisOnly && useSpecks.size() + inlined.size() == usages.length && inlined.size() == pathUsages.size()) {
            PsiElement prev = myTypeAlias.getPrevSibling();
            if (prev instanceof PsiWhiteSpace) prev.delete();
            myTypeAlias.delete();
        }
    }

    private void addNeededImports(@NotNull List<RsElement> inlined) {
        RsTypeReference typeReference = myTypeAlias.getTypeReference();
        if (typeReference == null) return;
        Set<RsMod> handledMods = new HashSet<>();
        handledMods.add(RsElementUtil.containingMod(myTypeAlias));
        for (RsElement context : inlined) {
            RsMod mod = RsElementUtil.containingMod(context);
            if (handledMods.add(mod)) {
                RsImportHelper.importTypeReferencesFromElement(context, typeReference);
            }
        }
    }

    @NotNull
    @Override
    protected String getCommandName() {
        String name = myTypeAlias.getName() != null ? myTypeAlias.getName() : "";
        return RsBundle.message("command.name.inline.type.alias", name);
    }

    @NotNull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
        return new RsInlineUsageViewDescriptor(myTypeAlias, RsBundle.message("list.item.type.alias.to.inline"));
    }

    @Nullable
    public static Substitution tryGetTypeAliasSubstitutionUsingParent(@NotNull RsPath path, @NotNull RsTypeAlias typeAlias) {
        PsiElement parent = path.getParent();
        if (!(parent instanceof RsPath)) return null;
        RsPath parentPath = (RsPath) parent;
        if (!(parentPath.getParent() instanceof RsPathExpr)) return null;
        if (parentPath.getReference() == null) return null;
        BoundElement<RsElement> resolved = parentPath.getReference().advancedResolve();
        if (resolved == null) return null;
        org.rust.lang.core.types.ty.Ty selfTy = resolved.getSubst().get(TyTypeParameter.self());
        if (selfTy == null) return null;
        ImplLookup implLookup = RsTypesUtil.getImplLookup(path);
        org.rust.lang.core.types.infer.RsInferenceContext inference = implLookup.getCtx();
        Substitution subst = inference.instantiateBounds(typeAlias, selfTy, org.rust.lang.core.types.SubstitutionUtil.EMPTY_SUBSTITUTION);
        RsTypeReference typeReference = typeAlias.getTypeReference();
        if (typeReference == null) return null;
        org.rust.lang.core.types.ty.Ty type = org.rust.lang.core.types.infer.FoldUtil.substitute(RsTypesUtil.normType(typeReference, implLookup), subst);
        inference.combineTypes(type, selfTy);
        return subst.mapTypeValues(entry -> inference.resolveTypeVarsIfPossible(entry.getValue()));
    }

    @Nullable
    public static RsElement fillPathWithActualType(
        @NotNull RsPath path,
        @NotNull RsTypeReference typeReference,
        @NotNull Substitution substitution
    ) {
        String typeText = RsPsiRendererUtil.getStubOnlyText(typeReference, substitution, false);
        return fillPathWithActualTypeText(path, typeText);
    }

    @Nullable
    private static RsElement fillPathWithActualTypeText(@NotNull RsPath path, @NotNull String typeText) {
        RsPsiFactory factory = new RsPsiFactory(path.getProject());
        RsTypeReference typeRef = factory.tryCreateType(typeText);
        if (typeRef == null) return null;
        RsPath typeAsPath = typeRef instanceof RsPathType ? ((RsPathType) typeRef).getPath() : null;

        PsiElement parent = path.getParent();
        if (parent instanceof RsTypeReference) {
            return (RsElement) parent.replace(typeRef);
        } else if (typeAsPath != null) {
            if (RsElementUtil.ancestorStrict(path, RsTypeReference.class) == null) {
                if (typeAsPath.getTypeArgumentList() != null) {
                    typeAsPath.getTypeArgumentList().addAfter(factory.createColonColon(), null);
                }
            }
            return (RsElement) path.replace(typeAsPath);
        } else if (parent instanceof RsPath) {
            RsPath parentPath = (RsPath) parent;
            String parentName = parentPath.getReferenceName();
            if (parentName == null) return null;
            String parentTypeArguments = parentPath.getTypeArgumentList() != null ? parentPath.getTypeArgumentList().getText() : "";
            String parentNewText = "<" + typeText + ">::" + parentName + parentTypeArguments;
            RsPath parentNew = factory.tryCreatePath(parentNewText);
            if (parentNew == null) return null;
            return (RsElement) parentPath.replace(parentNew);
        }
        return null;
    }

    private static class PathUsage {
        @NotNull
        final RsPath myPath;
        @NotNull
        final Substitution mySubstitution;

        PathUsage(@NotNull RsPath path, @NotNull Substitution substitution) {
            myPath = path;
            mySubstitution = substitution;
        }
    }
}
