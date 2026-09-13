/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.editor.completion.CompletionSorter;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.ide.impl.idea.codeInsight.completion.impl.RealPrefixMatchingWeigher;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.LookupElementWeigher;
import consulo.application.AllIcons;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.experiments.RsExperiments;
import org.rust.lang.core.completion.CompletionUtilsUtil;
import org.rust.lang.core.psi.ext.impl.PsiElementExt;
import org.rust.lang.core.psi.ext.impl.RsElementExtUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;
import org.rust.toml.Util;
import org.rust.toml.crates.local.CratesLocalIndexService;
import org.rust.toml.crates.local.CratesLocalIndexService.CargoRegistryCrate;
import org.rust.toml.crates.local.CratesLocalIndexService.CargoRegistryCrateVersion;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlKeyExt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class LocalCargoTomlDependencyCompletionProviders {
    private LocalCargoTomlDependencyCompletionProviders() {}

    @Nonnull
    public static TomlKeySegment getDependencyKey(@Nonnull TomlKeyValue keyValue) {
        PsiElement parent = keyValue.getParent();
        if (parent instanceof TomlTable) {
            TomlTable table = (TomlTable) parent;
            TomlKey headerKey = table.getHeader().getKey();
            if (headerKey != null) {
                List<TomlKeySegment> segments = headerKey.getSegments();
                if (!segments.isEmpty()) {
                    return segments.get(segments.size() - 1);
                }
            }
        }
        if (parent instanceof TomlInlineTable) {
            PsiElement prevSibling = PsiElementExt.getPrevNonWhitespaceSibling(parent);
            if (prevSibling != null) {
                PsiElement prevPrev = PsiElementExt.getPrevNonWhitespaceSibling(prevSibling);
                if (prevPrev != null) {
                    TomlKeySegment seg = RsElementExtUtil.childOfType(prevPrev, TomlKeySegment.class);
                    if (seg != null) return seg;
                }
            }
        }
        throw new IllegalStateException("PsiElementPattern must not allow keys outside of TomlTable or TomlInlineTable");
    }

    @Nonnull
    static List<LookupElement> makeVersionCompletions(@Nonnull List<CargoRegistryCrateVersion> sortedVersions,
                                                       @Nonnull TomlKeyValue keyValue) {
        List<LookupElement> result = new ArrayList<>();
        for (int index = 0; index < sortedVersions.size(); index++) {
            CargoRegistryCrateVersion variant = sortedVersions.get(index);
            LookupElementBuilder lookupElement = LookupElementBuilder.create(variant.getVersion())
                .withInsertHandler(new Util.StringValueInsertionHandler(keyValue))
                .withTailText(variant.isYanked() ? " yanked" : null);
            result.add(PrioritizedLookupElement.withPriority(lookupElement, (double) index));
        }
        return result;
    }

    static final CompletionSorter VERSIONS_SORTER = CompletionSorter.emptySorter()
        .weigh(new RealPrefixMatchingWeigher())
        .weigh(new LookupElementWeigher("priority", true, false) {
            @Override
            public @Nonnull Comparable<?> weigh(@Nonnull LookupElement element) {
                return ((PrioritizedLookupElement<?>) element).getPriority();
            }
        });
}

class LocalCargoTomlDependencyCompletionProvider extends TomlKeyValueCompletionProviderBase {
    @Override
    protected void completeKey(@Nonnull TomlKeyValue keyValue, @Nonnull CompletionResultSet result) {
        List<TomlKeySegment> segments = keyValue.getKey().getSegments();
        if (segments.size() != 1) return;
        TomlKeySegment keySegment = segments.get(0);
        PsiElement original = CompletionUtilCore.getOriginalElement(keySegment);
        if (original == null) return;
        String prefix = original.getText();

        @SuppressWarnings("unchecked")
        RsResult<Collection<String>, ?> crateNamesResult = (RsResult<Collection<String>, ?>) (RsResult) CratesLocalIndexService.getInstance().getAllCrateNames();
        if (crateNamesResult.isErr()) return;
        Collection<String> crateNames = crateNamesResult.unwrap();

        List<LookupElement> elements = new ArrayList<>();
        for (String crateName : crateNames) {
            elements.add(PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create(crateName)
                    .withIcon(AllIcons.Nodes.PpLib)
                    .withInsertHandler(new KeyInsertHandlerWithCompletion()),
                (double) -crateName.length()
            ));
        }
        result.withPrefixMatcher(new CargoDependenciesPrefixMatcher(prefix)).addAllElements(elements);
    }

    @Override
    protected void completeValue(@Nonnull TomlKeyValue keyValue, @Nonnull CompletionResultSet result) {
        List<TomlKeySegment> segments = keyValue.getKey().getSegments();
        if (segments.size() != 1) return;
        TomlKeySegment keySegment = segments.get(0);
        PsiElement original = CompletionUtilCore.getOriginalElement(keySegment);
        if (original == null) return;
        String name = original.getText();

        RsResult<CargoRegistryCrate, ?> crateResult = CratesLocalIndexService.getInstance().getCrate(name);
        if (crateResult.isErr()) return;
        CargoRegistryCrate crate = crateResult.unwrap();
        if (crate == null) return;
        List<CargoRegistryCrateVersion> sortedVersions = crate.getSortedVersions();
        List<LookupElement> elements = LocalCargoTomlDependencyCompletionProviders.makeVersionCompletions(sortedVersions, keyValue);
        result.withRelevanceSorter(LocalCargoTomlDependencyCompletionProviders.VERSIONS_SORTER).addAllElements(elements);
    }
}

class LocalCargoTomlSpecificDependencyHeaderCompletionProvider implements CompletionProvider {
    @Override
    public void addCompletions(@Nonnull CompletionParameters parameters,
                                  @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        if (!(parameters.getPosition().getParent() instanceof TomlKeySegment)) return;
        TomlKeySegment keySegment = (TomlKeySegment) parameters.getPosition().getParent();
        PsiElement original = CompletionUtilCore.getOriginalElement(keySegment);
        if (original == null) return;
        String prefix = original.getText();

        @SuppressWarnings("unchecked")
        RsResult<Collection<String>, ?> crateNamesResult = (RsResult<Collection<String>, ?>) (RsResult) CratesLocalIndexService.getInstance().getAllCrateNames();
        if (crateNamesResult.isErr()) return;
        Collection<String> crateNames = crateNamesResult.unwrap();

        List<LookupElement> elements = new ArrayList<>();
        for (String variant : crateNames) {
            elements.add(LookupElementBuilder.create(variant)
                .withIcon(AllIcons.Nodes.PpLib)
                .withInsertHandler((ctx, item) -> {
                    TomlTable table = PsiElementExt.ancestorStrict(keySegment, TomlTable.class);
                    if (table == null) return;
                    if (table.getEntries().isEmpty()) {
                        ctx.getDocument().insertString(
                            ctx.getSelectionEndOffset() + 1,
                            "\nversion = \"\""
                        );
                        EditorModificationUtil.moveCaretRelatively(ctx.getEditor(), 13);
                        AutoPopupController.getInstance(ctx.getProject()).scheduleAutoPopup(ctx.getEditor());
                    }
                })
            );
        }
        result.withPrefixMatcher(new CargoDependenciesPrefixMatcher(prefix)).addAllElements(elements);
    }
}

class LocalCargoTomlSpecificDependencyVersionCompletionProvider extends TomlKeyValueCompletionProviderBase {
    @Override
    protected void completeKey(@Nonnull TomlKeyValue keyValue, @Nonnull CompletionResultSet result) {
        result.addElement(
            LookupElementBuilder.create("version")
                .withInsertHandler(new KeyInsertHandlerWithCompletion())
        );
    }

    @Override
    protected void completeValue(@Nonnull TomlKeyValue keyValue, @Nonnull CompletionResultSet result) {
        String keyName = TomlKeyExt.getName(keyValue.getKey());
        if (!"version".equals(keyName)) return;

        TomlKeySegment dependencyNameKey = LocalCargoTomlDependencyCompletionProviders.getDependencyKey(keyValue);
        RsResult<CargoRegistryCrate, ?> crateResult = CratesLocalIndexService.getInstance().getCrate(dependencyNameKey.getText());
        if (crateResult.isErr()) return;
        CargoRegistryCrate crate = crateResult.unwrap();
        if (crate == null) return;
        List<CargoRegistryCrateVersion> sortedVersions = crate.getSortedVersions();
        List<LookupElement> elements = LocalCargoTomlDependencyCompletionProviders.makeVersionCompletions(sortedVersions, keyValue);
        result.withRelevanceSorter(LocalCargoTomlDependencyCompletionProviders.VERSIONS_SORTER).addAllElements(elements);
    }
}

class CargoTomlDependencyKeysCompletionProvider implements CompletionProvider {
    private final InsertHandler<LookupElement> myDefaultKeyInsertHandler = new KeyInsertHandlerWithCompletion();

    private final Map<String, InsertHandler<LookupElement>> myDependencyKeys = Map.ofEntries(
        Map.entry("branch", myDefaultKeyInsertHandler),
        Map.entry("default-features", new KeyInsertHandlerWithCompletion(" = ", 3)),
        Map.entry("features", new KeyInsertHandlerWithCompletion(" = []", 4)),
        Map.entry("git", myDefaultKeyInsertHandler),
        Map.entry("optional", new KeyInsertHandlerWithCompletion(" = ", 3)),
        Map.entry("package", myDefaultKeyInsertHandler),
        Map.entry("path", myDefaultKeyInsertHandler),
        Map.entry("registry", myDefaultKeyInsertHandler),
        Map.entry("rev", myDefaultKeyInsertHandler),
        Map.entry("tag", myDefaultKeyInsertHandler)
    );

    @Override
    public void addCompletions(@Nonnull CompletionParameters parameters,
                                  @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        for (Map.Entry<String, InsertHandler<LookupElement>> entry : myDependencyKeys.entrySet()) {
            result.addElement(
                LookupElementBuilder.create(entry.getKey())
                    .withInsertHandler(entry.getValue())
            );
        }
    }
}

class LocalCargoTomlInlineTableVersionCompletionProvider implements CompletionProvider {
    @Override
    public void addCompletions(@Nonnull CompletionParameters parameters,
                                  @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        if (!OpenApiUtil.isFeatureEnabled(RsExperiments.CRATES_LOCAL_INDEX)) return;

        PsiElement parent = parameters.getPosition().getParent();
        if (parent == null) return;
        PsiElement kvCandidate = parent.getParent();
        if (!(kvCandidate instanceof TomlKeyValue)) return;
        TomlKeyValue keyValue = (TomlKeyValue) kvCandidate;
        PsiElement inlineParent = keyValue.getParent();
        if (inlineParent == null) return;
        PsiElement depKvCandidate = inlineParent.getParent();
        if (!(depKvCandidate instanceof TomlKeyValue)) return;
        TomlKey dependencyNameKey = ((TomlKeyValue) depKvCandidate).getKey();

        RsResult<CargoRegistryCrate, ?> crateResult = CratesLocalIndexService.getInstance().getCrate(dependencyNameKey.getText());
        if (crateResult.isErr()) return;
        CargoRegistryCrate crate = crateResult.unwrap();
        if (crate == null) return;
        List<CargoRegistryCrateVersion> sortedVersions = crate.getSortedVersions();
        List<LookupElement> elements = LocalCargoTomlDependencyCompletionProviders.makeVersionCompletions(sortedVersions, keyValue);
        result.withRelevanceSorter(LocalCargoTomlDependencyCompletionProviders.VERSIONS_SORTER).addAllElements(elements);
    }
}

class KeyInsertHandlerWithCompletion implements InsertHandler<LookupElement> {
    private final String myInsertedValue;
    private final int myCaretShift;

    KeyInsertHandlerWithCompletion() {
        this(" = \"\"", 4);
    }

    KeyInsertHandlerWithCompletion(@Nonnull String insertedValue) {
        this(insertedValue, 4);
    }

    KeyInsertHandlerWithCompletion(@Nonnull String insertedValue, int caretShift) {
        myInsertedValue = insertedValue;
        myCaretShift = caretShift;
    }

    @Override
    public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement item) {
        boolean alreadyHasValue = CompletionUtilsUtil.nextCharIs(context, '=');

        if (!alreadyHasValue) {
            context.getDocument().insertString(context.getSelectionEndOffset(), myInsertedValue);
        }

        EditorModificationUtil.moveCaretRelatively(context.getEditor(), myCaretShift);

        if (!alreadyHasValue) {
            AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
        }
    }
}
