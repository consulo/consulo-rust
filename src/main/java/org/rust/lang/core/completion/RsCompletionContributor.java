/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.impl.CompletionSorterImpl;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementWeigher;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.completion.lint.RsClippyLintCompletionProvider;
import org.rust.lang.core.completion.lint.RsRustcLintCompletionProvider;
import org.rust.lang.core.completion.sort.RsCompletionWeigher;
import org.rust.lang.core.completion.sort.RsCompletionWeighers;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.*;

import static org.rust.lang.core.PsiElementPatternExtUtil.or;

public class RsCompletionContributor extends CompletionContributor {

    public RsCompletionContributor() {
        extend(CompletionType.BASIC, RsPrimitiveTypeCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsLiteralSuffixCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsBoolCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsSelfParameterCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsFragmentSpecifierCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsCommonCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsTupleFieldCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsDeriveCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsAttributeCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsMacroCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsPartialMacroArgumentCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsFullMacroArgumentCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsCfgAttributeCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsCfgPanicCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsAwaitCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsStructPatRestCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsClippyLintCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsRustcLintCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsImplTraitMemberCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsFnMainCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsVisRestrictionCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsLambdaExprCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsPsiPattern.fieldVisibility, new RsVisibilityCompletionProvider());
        extend(CompletionType.BASIC,
            or(RsPsiPattern.INSTANCE.declarationPattern(), RsPsiPattern.INSTANCE.inherentImplDeclarationPattern()),
            new RsVisibilityCompletionProvider());
        extend(CompletionType.BASIC, RsReprCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsCrateTypeAttrCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsExternAbiCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsStaticLifetimeCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsBuildScriptCargoInstructionCompletionProvider.INSTANCE);
        extend(CompletionType.BASIC, RsChronoFormatCompletionProvider.INSTANCE);
    }

    public void extend(CompletionType type, RsCompletionProvider provider) {
        extend(type, provider.getElementPattern(), provider);
    }

    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        super.beforeCompletion(context);
        PsiElement element = context.getFile().findElementAt(context.getStartOffset());
        if (element != null && element.getNode().getElementType() == RsElementTypes.IDENTIFIER) {
            context.setDummyIdentifier(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED);
        }
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        super.fillCompletionVariants(parameters, withRustSorter(parameters, result));
    }

    private static final List<AnchoredWeigherGroup> RS_COMPLETION_WEIGHERS_GROUPED = splitIntoGroups(RsCompletionWeighers.RS_COMPLETION_WEIGHERS);

    public static CompletionResultSet withRustSorter(CompletionParameters parameters, CompletionResultSet result) {
        CompletionSorterImpl sorter = (CompletionSorterImpl) CompletionSorter.defaultSorter(parameters, result.getPrefixMatcher());
        sorter = sorter.withoutClassifiers(it -> "liftShorter".equals(it.getId()));
        for (AnchoredWeigherGroup weigherGroup : RS_COMPLETION_WEIGHERS_GROUPED) {
            sorter = sorter.weighAfter(weigherGroup.myAnchor, weigherGroup.myWeighers);
        }
        return result.withRelevanceSorter(sorter);
    }

    private static List<AnchoredWeigherGroup> splitIntoGroups(List<Object> weighersWithAnchors) {
        if (weighersWithAnchors.isEmpty()) return Collections.emptyList();
        Object firstEntry = weighersWithAnchors.get(0);
        if (!(firstEntry instanceof String)) {
            throw new IllegalStateException("The first element in the weigher list must be a string placeholder like \"priority\"");
        }

        List<AnchoredWeigherGroup> groups = new ArrayList<>();
        Set<String> weigherIds = new HashSet<>();
        String currentAnchor = (String) firstEntry;
        List<LookupElementWeigher> currentWeighers = new ArrayList<>();

        List<Object> items = new ArrayList<>(weighersWithAnchors.subList(1, weighersWithAnchors.size()));
        items.add("dummy weigher");

        for (Object weigherOrAnchor : items) {
            if (weigherOrAnchor instanceof String) {
                if (!currentWeighers.isEmpty()) {
                    groups.add(new AnchoredWeigherGroup(currentAnchor, currentWeighers.toArray(new LookupElementWeigher[0])));
                    currentWeighers = new ArrayList<>();
                }
                currentAnchor = (String) weigherOrAnchor;
            } else if (weigherOrAnchor instanceof RsCompletionWeigher) {
                RsCompletionWeigher weigher = (RsCompletionWeigher) weigherOrAnchor;
                if (!weigherIds.add(weigher.getId())) {
                    throw new IllegalStateException("Found a RsCompletionWeigher.id duplicate: " + weigher.getId());
                }
                currentWeighers.add(new RsCompletionWeigherAsLookupElementWeigher(weigher));
            } else {
                throw new IllegalStateException("The weigher list must consist of String placeholders and instances of RsCompletionWeigher");
            }
        }
        return groups;
    }

    private static final class AnchoredWeigherGroup {
        final String myAnchor;
        final LookupElementWeigher[] myWeighers;

        AnchoredWeigherGroup(String anchor, LookupElementWeigher[] weighers) {
            myAnchor = anchor;
            myWeighers = weighers;
        }
    }

    private static final class RsCompletionWeigherAsLookupElementWeigher extends LookupElementWeigher {
        private final RsCompletionWeigher myWeigher;

        RsCompletionWeigherAsLookupElementWeigher(RsCompletionWeigher weigher) {
            super(weigher.getId(), false, false);
            myWeigher = weigher;
        }

        @Override
        public Comparable<?> weigh(@NotNull LookupElement element) {
            LookupElement rsElement = element.as(RsLookupElement.class);
            return myWeigher.weigh(rsElement != null ? rsElement : element);
        }
    }
}
