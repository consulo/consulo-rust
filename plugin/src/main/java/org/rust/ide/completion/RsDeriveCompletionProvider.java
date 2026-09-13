/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.icons.RsIcons;
import org.rust.lang.core.imports.ImportCandidate;
import org.rust.lang.core.imports.ImportCandidatesCollector;
import org.rust.lang.core.imports.ImportContext;
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
import consulo.language.pattern.PatternCondition;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.psi.ext.impl.*;
import org.rust.lang.core.completion.LookupElements;
import org.rust.lang.core.completion.RsCompletionProvider;
import org.rust.lang.core.completion.RsLookupElementProperties;
import org.rust.lang.core.completion.Utils;

public class RsDeriveCompletionProvider extends RsCompletionProvider {
    public static final RsDeriveCompletionProvider INSTANCE = new RsDeriveCompletionProvider();

    private RsDeriveCompletionProvider() {
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
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

    @Nonnull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement()
            .withLanguage(RsLanguage.INSTANCE)
            .withParent(psiElement(RsPath.class)
                .with(new consulo.language.pattern.PatternCondition<PsiElement>("PrimitivePath") {
                    @Override
                    public boolean accepts(@Nonnull PsiElement path, ProcessingContext ctx) {
                        if (!(path instanceof RsPath)) return false;
                        return ((RsPath) path).getPath() == null;
                    }
                })
                .withParent(RsPsiPattern.derivedTraitMetaItem)
            );
    }
}
