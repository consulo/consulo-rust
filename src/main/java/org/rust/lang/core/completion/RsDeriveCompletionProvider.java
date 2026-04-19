/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.utils.imports.ImportCandidate;
import org.rust.ide.utils.imports.ImportCandidatesCollector;
import org.rust.ide.utils.imports.ImportContext;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.types.TraitRef;

import java.util.*;
import java.util.stream.Collectors;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

public class RsDeriveCompletionProvider extends RsCompletionProvider {
    public static final RsDeriveCompletionProvider INSTANCE = new RsDeriveCompletionProvider();

    private RsDeriveCompletionProvider() {
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        PsiElement position = Utils.safeGetOriginalOrSelf(parameters.getPosition());
        addCompletionsForStdlibBuiltinDerives(position, result);

        PsiElement parent = position.getParent();
        if (!(parent instanceof RsPath)) return;
        RsPath path = (RsPath) parent;
        MultiMap<String, RsElement> processedElements = new MultiMap<>();
        addCompletionsForInScopeDerives(path, result, processedElements);
        addCompletionsForOutOfScopeDerives(path, result, processedElements);
    }

    private void addCompletionsForStdlibBuiltinDerives(PsiElement position, CompletionResultSet result) {
        RsStructOrEnumItemElement owner = RsElementUtil.ancestorStrict(position, RsStructOrEnumItemElement.class);
        if (owner == null) return;
        org.rust.lang.core.types.ty.Ty ownerType = RsStructOrEnumItemElementUtil.getDeclaredType(owner);
        ImplLookup lookup = ImplLookup.relativeTo(owner);

        List<KnownDerivableTrait> stdDerivables = Arrays.stream(KnownDerivableTrait.values())
            .filter(KnownDerivableTrait::isStd)
            .filter(it -> {
                org.rust.lang.core.psi.RsTraitItem trait = it.findTrait(RsElementUtil.getKnownItems(owner));
                if (trait == null) return false;
                org.rust.lang.core.types.BoundElement<org.rust.lang.core.psi.RsTraitItem> boundTrait;
                if (it == KnownDerivableTrait.PartialOrd || it == KnownDerivableTrait.PartialEq) {
                    boundTrait = RsGenericDeclarationUtil.withSubst(trait, ownerType);
                } else {
                    boundTrait = RsGenericDeclarationUtil.withDefaultSubst(trait);
                }
                return boundTrait != null && !lookup.canSelect(new TraitRef(ownerType, boundTrait));
            })
            .collect(Collectors.toList());

        for (KnownDerivableTrait derivable : stdDerivables) {
            List<KnownDerivableTrait> traitWithDependencies = Arrays.stream(derivable.getWithDependencies())
                .filter(dep -> {
                    org.rust.lang.core.psi.RsTraitItem trait = dep.findTrait(RsElementUtil.getKnownItems(owner));
                    if (trait == null) return false;
                    org.rust.lang.core.types.BoundElement<org.rust.lang.core.psi.RsTraitItem> boundTrait;
                    if (dep == KnownDerivableTrait.PartialOrd || dep == KnownDerivableTrait.PartialEq) {
                        boundTrait = RsGenericDeclarationUtil.withSubst(trait, ownerType);
                    } else {
                        boundTrait = RsGenericDeclarationUtil.withDefaultSubst(trait);
                    }
                    return boundTrait != null && !lookup.canSelect(new TraitRef(ownerType, boundTrait));
                })
                .collect(Collectors.toList());

            if (traitWithDependencies.size() > 1) {
                String joinedNames = traitWithDependencies.stream()
                    .map(KnownDerivableTrait::name)
                    .collect(Collectors.joining(", "));
                LookupElementBuilder element = LookupElementBuilder.create(joinedNames)
                    .withIcon(RsIcons.PROC_MACRO);
                result.addElement(LookupElements.toRsLookupElement(element,
                    new RsLookupElementProperties(RsLookupElementProperties.ElementKind.DERIVE_GROUP)));
            }
            result.addElement(createLookupElement(derivable.name(), null));
        }
    }

    private void addCompletionsForInScopeDerives(RsPath path, CompletionResultSet result, MultiMap<String, RsElement> processedElements) {
        RsResolveProcessor processor = ResolveUtil.createProcessor(e -> {
            result.addElement(createLookupElement(e.getName(), null));
            processedElements.putValue(e.getName(), e.getElement());
        });
        RsResolveProcessor filtered = Processors.filterDeriveProcMacros(processor);
        NameResolutionUtil.processProcMacroResolveVariants(path, filtered, true);
    }

    private void addCompletionsForOutOfScopeDerives(RsPath path, CompletionResultSet result, MultiMap<String, RsElement> processedElements) {
        ImportContext importContext = ImportContext.from(path, ImportContext.Type.COMPLETION);
        if (importContext == null) return;
        List<ImportCandidate> candidates = ImportCandidatesCollector.getCompletionCandidates(importContext, result.getPrefixMatcher(), processedElements);
        for (ImportCandidate candidate : candidates) {
            RsElement item = candidate.getItem();
            if (!(item instanceof RsFunction) || !RsFunctionUtil.isCustomDeriveProcMacroDef((RsFunction) item)) continue;
            String name = RsFunctionUtil.getProcMacroName((RsFunction) item);
            if (name == null) continue;
            result.addElement(createLookupElement(name, candidate));
        }
    }

    private LookupElement createLookupElement(String name, @Nullable ImportCandidate candidate) {
        LookupElementBuilder builder = LookupElementBuilder.create(name).withIcon(RsIcons.PROC_MACRO);
        RsLookupElementProperties properties = new RsLookupElementProperties(RsLookupElementProperties.ElementKind.DERIVE);
        if (candidate != null) {
            ImportCandidate finalCandidate = candidate;
            LookupElement element = LookupElements.toRsLookupElement(
                builder
                    .withInsertHandler((ctx, item) -> LookupElements.importInContext(ctx, finalCandidate))
                    .appendTailText(" (" + candidate.getInfo().getUsePath() + ")", true),
                properties
            );
            return LookupElements.withImportCandidate(element, candidate);
        } else {
            return LookupElements.toRsLookupElement(builder, properties);
        }
    }

    @NotNull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement()
            .withLanguage(RsLanguage.INSTANCE)
            .withParent(psiElement(RsPath.class)
                .with(new com.intellij.patterns.PatternCondition<PsiElement>("PrimitivePath") {
                    @Override
                    public boolean accepts(@NotNull PsiElement path, ProcessingContext ctx) {
                        if (!(path instanceof RsPath)) return false;
                        return ((RsPath) path).getPath() == null;
                    }
                })
                .withParent(RsPsiPattern.derivedTraitMetaItem)
            );
    }
}
