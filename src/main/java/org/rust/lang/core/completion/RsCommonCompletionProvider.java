/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.ml.MLRankingIgnorable;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.settings.RsCodeInsightSettings;
import org.rust.ide.utils.imports.*;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.macros.MacroExpansionUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.resolve.ref.DotExprResolveVariant;
import org.rust.lang.core.resolve.ref.FieldResolveVariant;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.infer.ExpectedType;
import org.rust.lang.core.types.ty.*;
import org.rust.openapiext.Testmark;

import java.util.*;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

public class RsCommonCompletionProvider extends RsCompletionProvider {
    public static final RsCommonCompletionProvider INSTANCE = new RsCommonCompletionProvider();

    private RsCommonCompletionProvider() {
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext processingContext,
        @NotNull CompletionResultSet result
    ) {
        PsiElement position = parameters.getPosition();
        PsiElement parent = position.getParent();
        if (!(parent instanceof RsReferenceElement)) return;
        RsReferenceElement element = (RsReferenceElement) parent;
        if (position != element.getReferenceNameElement()) return;

        MultiMap<String, RsElement> processedPathElements = new MultiMap<>();

        PsiElement elementContext = element.getContext();
        if (elementContext instanceof RsVisRestriction && element instanceof RsPath && ((RsVisRestriction) elementContext).getIn() == null) {
            return;
        }

        RsCompletionContext context = new RsCompletionContext(
            element,
            getExpectedTypeForEnclosingPathOrDotExpr(element),
            RsPsiPattern.INSTANCE.getSimplePathPattern().accepts(parameters.getPosition())
        );

        addCompletionVariants(element, result, context, processedPathElements);

        if (element instanceof RsMethodOrField) {
            addMethodAndFieldCompletion((RsMethodOrField) element, result, context);
        }

        if (element instanceof RsPath && RsCodeInsightSettings.getInstance().suggestOutOfScopeItems) {
            if (context.isSimplePath() && !RsPathUtil.isInsideDocLink((RsPath) element)) {
                addCompletionsForOutOfScopeItems(position, (RsPath) element, result, processedPathElements, context.getExpectedTy());
            }
            if (processedPathElements.isEmpty()) {
                addCompletionsForOutOfScopeFirstPathSegment((RsPath) element, result, context);
            }
        }
    }

    public void addCompletionVariants(
        RsReferenceElement element,
        CompletionResultSet result,
        RsCompletionContext context,
        MultiMap<String, RsElement> processedElements
    ) {
        RsResolveProcessor finalProcessor = ResolveUtil.createProcessor(entry -> {
            processedElements.putValue(entry.getName(), entry.getElement());
            result.addElement(LookupElements.createLookupElement(entry, context));
        });

        RsResolveProcessor filtered = Processors.filterNotCfgDisabledItemsAndTestFunctions(finalProcessor);
        if (element instanceof RsPath) {
            RsPath path = (RsPath) element;
            RsResolveProcessor processor2 = filtered;
            processPathVariants(path, processor2);
            processUnresolvedImports(path, result, context);
        } else if (element instanceof RsAssocTypeBinding) {
            NameResolutionUtil.processAssocTypeVariants((RsAssocTypeBinding) element, filtered);
        } else if (element instanceof RsExternCrateItem) {
            NameResolutionUtil.processExternCrateResolveVariants((RsExternCrateItem) element, true, filtered);
        } else if (element instanceof RsLabel) {
            NameResolutionUtil.processLabelResolveVariants((RsLabel) element, filtered);
        } else if (element instanceof RsLifetime) {
            NameResolutionUtil.processLifetimeResolveVariants((RsLifetime) element, filtered);
        } else if (element instanceof RsMacroReference) {
            NameResolutionUtil.processMacroReferenceVariants((RsMacroReference) element, filtered);
        } else if (element instanceof RsModDeclItem) {
            NameResolutionUtil.processModDeclResolveVariants((RsModDeclItem) element, filtered);
        } else if (element instanceof RsPatBinding) {
            NameResolutionUtil.processPatBindingResolveVariants((RsPatBinding) element, true, filtered);
        } else if (element instanceof RsStructLiteralField) {
            NameResolutionUtil.processStructLiteralFieldResolveVariants((RsStructLiteralField) element, true, filtered);
        }
    }

    private void processPathVariants(RsPath element, RsResolveProcessor processor) {
        PsiElement parent = element.getParent();
        if (parent instanceof RsMacroCall) {
            RsResolveProcessor filtered = Processors.filterNotAttributeAndDeriveProcMacros(processor);
            NameResolutionUtil.processMacroCallPathResolveVariants(element, true, filtered);
        } else if (parent instanceof RsMetaItem) {
            if (!RsProcMacroPsiUtil.canBeProcMacroAttributeCall((RsMetaItem) parent, CustomAttributes.EMPTY)) return;
            RsResolveProcessor filtered = Processors.filterAttributeProcMacros(processor);
            NameResolutionUtil.processProcMacroResolveVariants(element, filtered, true);
        } else if (parent instanceof RsVisRestriction && ((RsVisRestriction) parent).getIn() == null) {
            return;
        } else {
            ImplLookup lookup = ImplLookup.relativeTo(element);
            RsResolveProcessor filtered = Processors.filterPathCompletionVariantsByTraitBounds(processor, lookup);
            if (!(parent instanceof RsUseSpeck)) {
                filtered = Processors.filterNotAttributeAndDeriveProcMacros(filtered);
            }
            filtered = filterCompletionVariantsByVisibility(element, filtered);
            filtered = filterAssocTypes(element, filtered);
            filtered = filterVisRestrictionPaths(element, filtered);
            filtered = filterTraitRefPaths(element, filtered);

            if (element.getParent() instanceof RsPathExpr && !(element.getHasColonColon() && RsElementUtil.isAtLeastEdition2018(element))) {
                NameResolutionUtil.processMacroCallPathResolveVariants(element, true, filtered);
            }

            PsiElement possibleTypeArgs = parent != null ? parent.getParent() : null;
            if (possibleTypeArgs instanceof RsTypeArgumentList) {
                PsiElement resolved = ((RsPath) possibleTypeArgs.getParent()).getReference() != null
                    ? ((RsPath) possibleTypeArgs.getParent()).getReference().resolve() : null;
                if (resolved instanceof RsTraitItem) {
                    NameResolutionUtil.processAssocTypeVariants((RsTraitItem) resolved, filtered);
                }
            }

            NameResolutionUtil.processPathResolveVariants(lookup, element, true, true, filtered);
        }
    }

    private void processUnresolvedImports(RsPath path, CompletionResultSet result, RsCompletionContext context) {
        if (!context.isSimplePath()) return;
        Set<String> unresolvedImports = new HashSet<>();
        NameResolutionUtil.processUnresolvedImports(path, useSpeck -> {
            String name = RsUseSpeckUtil.getNameInScope(useSpeck);
            if (name != null && !"_".equals(name)) {
                unresolvedImports.add(name);
            }
        });
        for (String unresolvedImport : unresolvedImports) {
            LookupElement element = LookupElements.toRsLookupElement(
                LookupElementBuilder.create(unresolvedImport),
                new RsLookupElementProperties(RsLookupElementProperties.ElementKind.FROM_UNRESOLVED_IMPORT)
            );
            @SuppressWarnings("UnstableApiUsage")
            LookupElement wrapped = MLRankingIgnorable.wrap(element);
            result.addElement(wrapped);
        }
    }

    public void addMethodAndFieldCompletion(
        RsMethodOrField element,
        CompletionResultSet result,
        RsCompletionContext context
    ) {
        RsExpr receiver = Utils.safeGetOriginalOrSelf(RsMethodOrFieldUtil.getReceiver(element));
        ImplLookup lookup = ImplLookup.relativeTo(receiver);
        Ty receiverTy = RsTypesUtil.getType(receiver);

        RsResolveProcessor processor = ResolveUtil.createProcessor(entry -> {
            if (entry instanceof FieldResolveVariant) {
                result.addElement(LookupElements.createLookupElement(entry, context));
            } else if (entry instanceof MethodResolveVariant) {
                if (((MethodResolveVariant) entry).getElement() instanceof RsFunction && RsFunctionUtil.isTest((RsFunction) ((MethodResolveVariant) entry).getElement())) return;
                result.addElement(LookupElements.createLookupElement(entry, context, null, new RsDefaultInsertHandler() {
                    @Override
                    protected void handleInsert(RsElement rsElement, String scopeName, InsertionContext ctx, LookupElement item) {
                        ImportCandidate traitImportCandidate = findTraitImportCandidate(element, (MethodResolveVariant) entry);
                        super.handleInsert(rsElement, scopeName, ctx, item);
                        if (traitImportCandidate != null) {
                            ctx.commitDocument();
                            RsElement rsElem = LookupElements.getElementOfType(ctx, RsElement.class);
                            if (rsElem != null) {
                                ImportUtil.import_(traitImportCandidate, rsElem);
                            }
                        }
                    }
                }));
            }
        });

        RsResolveProcessor deduped = Processors.deduplicateMethodCompletionVariants(processor);
        RsResolveProcessor boundFiltered = Processors.filterMethodCompletionVariantsByTraitBounds(lookup, receiverTy, deduped);
        ImportContext importCtx = ImportContext.from(element, ImportContext.Type.COMPLETION);
        RsResolveProcessor traitFiltered = importCtx != null ? ImportCandidatesCollector.filterAccessibleTraits(importCtx, boundFiltered) : boundFiltered;
        RsResolveProcessor visFiltered = Processors.filterCompletionVariantsByVisibility(element, traitFiltered);

        @SuppressWarnings("unchecked")
        RsResolveProcessorBase<MethodResolveVariant> methodProcessor = (RsResolveProcessorBase<MethodResolveVariant>) (RsResolveProcessorBase<?>) visFiltered;
        @SuppressWarnings("unchecked")
        RsResolveProcessorBase<DotExprResolveVariant> dotProcessor = (RsResolveProcessorBase<DotExprResolveVariant>) (RsResolveProcessorBase<?>) visFiltered;
        if (element instanceof RsMethodCall) {
            NameResolutionUtil.processMethodCallExprResolveVariants(lookup, receiverTy, element, methodProcessor);
        } else {
            NameResolutionUtil.processDotExprResolveVariants(lookup, receiverTy, element, dotProcessor);
        }
    }

    private void addCompletionsForOutOfScopeItems(
        PsiElement position,
        RsPath path,
        CompletionResultSet result,
        MultiMap<String, RsElement> processedPathElements,
        @org.jetbrains.annotations.Nullable ExpectedType expectedTy
    ) {
        PsiElement originalFile = position.getContainingFile().getOriginalFile();
        boolean ignoreCodeFragment = originalFile instanceof RsExpressionCodeFragment
            && originalFile.getUserData(RsMacroCompletionProvider.FORCE_OUT_OF_SCOPE_COMPLETION) != Boolean.TRUE;
        if (ignoreCodeFragment) return;

        PsiElement positionInMacroArgument = MacroExpansionUtil.findElementExpandedFrom(position);
        if (positionInMacroArgument != null && !isInSameRustMod(positionInMacroArgument, position)) return;
        if (TyPrimitive.fromPath(path) != null) return;
        PsiElement parent = path.getParent();
        if (parent instanceof RsMetaItem && !RsProcMacroPsiUtil.canBeProcMacroAttributeCall((RsMetaItem) parent, CustomAttributes.EMPTY)) return;
        Testmarks.OutOfScopeItemsCompletion.hit();

        RsCompletionContext context = new RsCompletionContext(path, expectedTy, true);
        ImportContext importContext = ImportContext.from(path, ImportContext.Type.COMPLETION);
        if (importContext == null) return;
        List<ImportCandidate> candidates = ImportCandidatesCollector.getCompletionCandidates(importContext, result.getPrefixMatcher(), processedPathElements);

        RsMod contextMod = path.getContainingMod();

        for (ImportCandidate candidate : candidates) {
            RsElement item = candidate.getItem();
            if (item instanceof RsOuterAttributeOwner) {
                if (Processors.shouldHideElementInCompletion((RsOuterAttributeOwner) item, path, contextMod)) {
                    continue;
                }
            }
            ScopeEntry scopeEntry = new SimpleScopeEntry(candidate.getItemName(), item, Namespace.TYPES_N_VALUES_N_MACROS);

            if (item instanceof RsEnumItem
                && context.getExpectedTy() != null
                && TyUtil.stripReferences(context.getExpectedTy().getTy()) instanceof TyAdt
                && ((TyAdt) TyUtil.stripReferences(context.getExpectedTy().getTy())).getItem() == ((TyAdt) RsStructOrEnumItemElementUtil.getDeclaredType((RsEnumItem) item)).getItem()) {
                // create one lookup entry per `Enum::Variant` so the user can complete the variant
                // directly from a context expecting the enum type.
                List<LookupElement> variants = collectVariantsForEnumCompletion(
                    (RsEnumItem) item, context, candidate, contextMod);
                result.addAllElements(variants);
            }

            LookupElement lookupElement = createLookupElementWithImportCandidate(scopeEntry, context, candidate);
            result.addElement(lookupElement);
        }
    }

    private void addCompletionsForOutOfScopeFirstPathSegment(RsPath path, CompletionResultSet result, RsCompletionContext context) {
        RsPath qualifier = path.getPath();
        if (qualifier == null) return;
        boolean isApplicablePath = qualifier.getPath() == null && qualifier.getTypeQual() == null && !qualifier.getHasColonColon()
            && RsPathUtil.getResolveStatus(qualifier) == PathResolveStatus.UNRESOLVED;
        if (!isApplicablePath) return;

        ImportContext importContext = ImportContext.from(qualifier, ImportContext.Type.AUTO_IMPORT);
        if (importContext == null) return;
        String referenceName = qualifier.getReferenceName();
        if (referenceName == null) return;
        List<ImportCandidate> candidates = ImportCandidatesCollector.getImportCandidates(importContext, referenceName);
        Map<RsElement, List<ImportCandidate>> itemToCandidates = new HashMap<>();
        for (ImportCandidate c : candidates) {
            itemToCandidates.computeIfAbsent(c.getItem(), k -> new ArrayList<>()).add(c);
        }
        for (List<ImportCandidate> candidateList : itemToCandidates.values()) {
            String firstUsePath = candidateList.get(0).getInfo().getUsePath();
            RsPath newPath = new RsCodeFragmentFactory(path.getProject()).createPathInTmpMod(
                path.getText(),
                importContext.getRootMod(),
                ImportContext.getPathParsingMode(path),
                RsPathUtil.allowedNamespaces(path, true),
                firstUsePath,
                null
            );
            if (newPath != null) {
                RsResolveProcessor collector = ResolveUtil.createProcessor(e -> {
                    for (ImportCandidate c : candidateList) {
                        result.addElement(createLookupElementWithImportCandidate(e, context, c));
                    }
                });
                RsResolveProcessor proc = filterCompletionVariantsByVisibility(
                    path,
                    Processors.filterNotCfgDisabledItemsAndTestFunctions(collector)
                );
                processPathVariants(newPath, proc);
            }
        }
    }

    /**
     * Emits one {@link LookupElement} per {@code Enum::Variant} and (when {@code candidate} is
     * non-null) attaches it as an import candidate so accepting the variant inserts the use item.
     */
    @NotNull
    private List<LookupElement> collectVariantsForEnumCompletion(
        @NotNull RsEnumItem element,
        @NotNull RsCompletionContext context,
        @org.jetbrains.annotations.Nullable ImportCandidate candidate,
        @org.jetbrains.annotations.Nullable RsMod contextMod
    ) {
        String enumName = element.getName();
        if (enumName == null) return Collections.emptyList();
        org.rust.lang.core.psi.RsEnumBody body = element.getEnumBody();
        if (body == null) return Collections.emptyList();

        List<LookupElement> result = new ArrayList<>();
        for (org.rust.lang.core.psi.RsEnumVariant variant : body.getEnumVariantList()) {
            String variantName = variant.getName();
            if (variantName == null) continue;
            if (contextMod != null
                && Processors.shouldHideElementInCompletion(variant, context.getContext(), contextMod)) {
                continue;
            }
            ScopeEntry entry = new SimpleScopeEntry(
                enumName + "::" + variantName, variant, Namespace.ENUM_VARIANT_NS);
            LookupElement lookup = LookupElements.createLookupElement(
                new ScopedBaseCompletionEntity(entry), context, null, new RsDefaultInsertHandler() {
                    @Override
                    protected void handleInsert(RsElement el, String scopeName, InsertionContext ctx, LookupElement item) {
                        super.handleInsert(el, scopeName, ctx, item);
                        if (candidate != null) LookupElements.importInContext(ctx, candidate);
                    }
                });
            result.add(candidate != null ? new RsImportLookupElement(lookup, candidate) : lookup);
        }
        return result;
    }

    private RsImportLookupElement createLookupElementWithImportCandidate(
        ScopeEntry scopeEntry,
        RsCompletionContext context,
        ImportCandidate candidate
    ) {
        LookupElement element = LookupElements.createLookupElement(
            new ScopedBaseCompletionEntity(scopeEntry),
            context,
            candidate.getInfo().getUsePath(),
            new RsDefaultInsertHandler() {
                @Override
                protected void handleInsert(RsElement rsElement, String scopeName, InsertionContext ctx, LookupElement item) {
                    super.handleInsert(rsElement, scopeName, ctx, item);
                    LookupElements.importInContext(ctx, candidate);
                }
            }
        );
        return new RsImportLookupElement(element, candidate);
    }

    private static RsResolveProcessor filterCompletionVariantsByVisibility(RsPath path, RsResolveProcessor processor) {
        return Processors.filterCompletionVariantsByVisibility(path, processor);
    }

    private static RsResolveProcessor filterVisRestrictionPaths(RsPath path, RsResolveProcessor processor) {
        if (path.getParent() instanceof RsVisRestriction) {
            List<RsMod> allowedModules = path.getContainingMod().getSuperMods();
            return processor.wrapWithFilter(it -> {
                if (!(it.getElement() instanceof RsMod)) return false;
                return allowedModules.contains(it.getElement());
            });
        }
        return processor;
    }

    private static RsResolveProcessor filterTraitRefPaths(RsPath path, RsResolveProcessor processor) {
        if (path.getParent() instanceof RsTraitRef) {
            return processor.wrapWithFilter(it ->
                it.getElement() instanceof RsTraitItem || it.getElement() instanceof RsMod);
        }
        return processor;
    }

    private static RsResolveProcessor filterAssocTypes(RsPath path, RsResolveProcessor processor) {
        RsPath qualifier = path.getPath();
        boolean allAssocItemsAllowed = qualifier == null
            || RsPathUtil.getHasCself(qualifier)
            || (qualifier.getReference() != null && qualifier.getReference().resolve() instanceof RsTypeParameter);
        if (allAssocItemsAllowed) return processor;
        return processor.wrapWithFilter(it ->
            !(it instanceof AssocItemScopeEntry && it.getElement() instanceof RsTypeAlias));
    }

    private static boolean isInSameRustMod(PsiElement element1, PsiElement element2) {
        RsElement rs1 = PsiTreeUtil.getContextOfType(element1, RsElement.class, false);
        RsElement rs2 = PsiTreeUtil.getContextOfType(element2, RsElement.class, false);
        if (rs1 == null || rs2 == null) return false;
        return rs1.getContainingMod() == rs2.getContainingMod();
    }

    @org.jetbrains.annotations.Nullable
    private static ExpectedType getExpectedTypeForEnclosingPathOrDotExpr(RsReferenceElement element) {
        for (PsiElement ancestor : RsElementUtil.getAncestors(element)) {
            if (element.getTextRange().getEndOffset() < ancestor.getTextRange().getEndOffset()) continue;
            if (element.getTextRange().getEndOffset() > ancestor.getTextRange().getEndOffset()) break;
            if (ancestor instanceof RsPathExpr) {
                return ExtensionsUtil.getExpectedTypeCoercable((RsPathExpr) ancestor);
            } else if (ancestor instanceof RsDotExpr) {
                return ExtensionsUtil.getExpectedTypeCoercable((RsDotExpr) ancestor);
            }
        }
        return null;
    }

    @org.jetbrains.annotations.Nullable
    private static ImportCandidate findTraitImportCandidate(RsMethodOrField methodOrField, MethodResolveVariant resolveVariant) {
        if (!RsCodeInsightSettings.getInstance().importOutOfScopeItems) return null;
        PsiElement ancestor = PsiTreeUtil.getParentOfType(methodOrField, RsBlock.class, RsMod.class);
        if (ancestor == null) return null;
        PsiElement original = com.intellij.codeInsight.completion.CompletionUtil.getOriginalElement(ancestor);
        if (!(original instanceof RsElement)) return null;
        List<ImportCandidate> candidates = ImportCandidatesCollector.getImportCandidates(
            (RsElement) original, Collections.singletonList(resolveVariant));
        if (candidates == null) return null;
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    @NotNull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement().withParent(psiElement(RsReferenceElement.class));
    }

    public static final class Testmarks {
        public static final Testmark OutOfScopeItemsCompletion = new Testmark();
    }
}
