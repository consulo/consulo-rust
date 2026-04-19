/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElementUtil;
import org.rust.lang.core.resolve.RsResolveProcessor;
import org.rust.lang.core.resolve.TraitImplSource;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Finds {@link ImportCandidate}s for auto-import and completion features. {@link
 * #findImportCandidate} constructs a candidate from an already-resolved named element by
 * reading its crate-relative / qualified path; the by-name / completion / trait-import
 * entry points are not wired yet (full def-map enumeration is part of the resolve2 subsystem).
 */
public final class ImportCandidatesCollector {
    private ImportCandidatesCollector() {
    }

    /**
     * Enumerate all items named {@code targetName} reachable from {@code context} and wrap each
     * as an {@link ImportCandidate}. Looks up every {@link RsQualifiedNamedElement} with a
     * matching name in the project-wide index, filters out elements already in scope, and
     * builds a candidate for each remaining hit.
     */
    @NotNull
    public static List<ImportCandidate> getImportCandidates(@NotNull ImportContext context, @NotNull String targetName) {
        java.util.Collection<org.rust.lang.core.psi.ext.RsNamedElement> byName =
            org.rust.lang.core.stubs.index.RsNamedElementIndex.findElementsByName(
                context.getRootMod().getProject(), targetName);
        List<ImportCandidate> result = new ArrayList<>();
        for (org.rust.lang.core.psi.ext.RsNamedElement el : byName) {
            if (!(el instanceof RsQualifiedNamedElement)) continue;
            ImportCandidate candidate = findImportCandidate(context, (RsQualifiedNamedElement) el);
            if (candidate != null) result.add(candidate);
        }
        Collections.sort(result);
        return result;
    }

    @Nullable
    public static List<ImportCandidate> getImportCandidates(@NotNull RsElement scope, @NotNull List<MethodResolveVariant> resolvedMethods) {
        List<Object> sources = new ArrayList<>();
        for (MethodResolveVariant m : resolvedMethods) sources.add(m.getSource());
        return getTraitImportCandidates(scope, sources);
    }

    /**
     * For each method-resolution source that refers to a non-inherent trait, produce an
     * {@link ImportCandidate} that makes that trait visible at {@code scope}. Traits already
     * in scope are skipped (returning {@code null} in that case communicates "nothing to import").
     */
    @Nullable
    public static List<ImportCandidate> getTraitImportCandidates(@NotNull RsElement scope, @NotNull List<Object> sources) {
        java.util.Set<org.rust.lang.core.psi.RsTraitItem> traits = new java.util.LinkedHashSet<>();
        for (Object src : sources) {
            if (!(src instanceof TraitImplSource)) continue;
            TraitImplSource source = (TraitImplSource) src;
            if (source.isInherent()) return null;
            org.rust.lang.core.psi.RsTraitItem requiredTrait = source.getRequiredTraitInScope();
            if (requiredTrait != null) traits.add(requiredTrait);
        }
        if (traits.isEmpty()) return null;
        ImportContext context = ImportContext.from(scope, ImportContext.Type.AUTO_IMPORT);
        if (context == null) return Collections.emptyList();
        List<ImportCandidate> result = new ArrayList<>();
        for (org.rust.lang.core.psi.RsTraitItem trait : traits) {
            ImportCandidate candidate = findImportCandidate(context, trait);
            if (candidate != null) result.add(candidate);
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Produce one {@link ImportCandidate} that imports {@code target} into {@code context}.
     * Derived from the target's context-relative qualified name (or, failing that, its
     * crate-relative path); picks a {@link ImportInfo.LocalImportInfo} when the target is in
     * the same crate and an {@link ImportInfo.ExternCrateImportInfo} otherwise.
     */
    @Nullable
    public static ImportCandidate findImportCandidate(@NotNull ImportContext context, @NotNull RsQualifiedNamedElement target) {
        String usePath = findUsePath(context, target);
        if (usePath == null || usePath.isEmpty()) return null;

        Crate targetCrate = RsElementUtil.getContainingCrate(target);
        if (targetCrate == null) return null;

        String[] pathSegments = usePath.split("::");
        if (pathSegments.length == 0) return null;

        ImportInfo info;
        if (sameCrate(context.getRootMod(), targetCrate)) {
            info = new ImportInfo.LocalImportInfo(usePath);
        } else {
            String crateName = targetCrate.getNormName();
            if (crateName == null) return null;
            String crateRelative = target.getCrateRelativePath();
            if (crateRelative != null && crateRelative.startsWith("::")) {
                crateRelative = crateRelative.substring(2);
            }
            if (crateRelative == null) crateRelative = "";
            boolean needExternCrate =
                targetCrate.getOrigin() != PackageOrigin.STDLIB
                    && targetCrate.getOrigin() != PackageOrigin.STDLIB_DEPENDENCY;
            info = new ImportInfo.ExternCrateImportInfo(
                targetCrate, crateName, needExternCrate, crateRelative);
        }
        return new ImportCandidate(target, pathSegments, targetCrate, info, true);
    }

    /**
     * Wrap {@code processor} so it only accepts {@link MethodResolveVariant}s whose owning
     * trait can be imported (i.e. not excluded by user preference). Non-method entries are
     * passed through unchanged.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public static RsResolveProcessor filterAccessibleTraits(@NotNull ImportContext context, @NotNull RsResolveProcessor processor) {
        return (RsResolveProcessor) org.rust.lang.core.resolve.Processors.wrapWithFilter(processor, entry -> {
            if (!(entry instanceof MethodResolveVariant)) return true;
            TraitImplSource source = ((MethodResolveVariant) entry).getSource();
            if (source.isInherent()) return true;
            org.rust.lang.core.psi.RsTraitItem trait = source.getRequiredTraitInScope();
            if (trait == null) return true;
            return findImportCandidate(context, trait) != null;
        });
    }

    /**
     * Enumerate completion-time import candidates — names matching {@code prefixMatcher} that
     * aren't already {@linkplain MultiMap present in} {@code processedElements}.
     */
    @NotNull
    public static List<ImportCandidate> getCompletionCandidates(
        @NotNull ImportContext context,
        @NotNull PrefixMatcher prefixMatcher,
        @NotNull MultiMap<String, RsElement> processedElements
    ) {
        com.intellij.psi.stubs.StubIndex stubIndex = com.intellij.psi.stubs.StubIndex.getInstance();
        java.util.Collection<String> allKeys = stubIndex.getAllKeys(
            org.rust.lang.core.stubs.index.RsNamedElementIndex.KEY,
            context.getRootMod().getProject());
        List<ImportCandidate> result = new ArrayList<>();
        for (String name : allKeys) {
            if (!prefixMatcher.prefixMatches(name)) continue;
            for (org.rust.lang.core.psi.ext.RsNamedElement el :
                org.rust.lang.core.stubs.index.RsNamedElementIndex.findElementsByName(
                    context.getRootMod().getProject(), name)) {
                if (!(el instanceof RsQualifiedNamedElement)) continue;
                if (processedElements.get(name).contains((RsElement) el)) continue;
                ImportCandidate candidate = findImportCandidate(context, (RsQualifiedNamedElement) el);
                if (candidate != null) result.add(candidate);
            }
        }
        Collections.sort(result);
        return result;
    }

    @Nullable
    private static String findUsePath(@NotNull ImportContext context, @NotNull RsQualifiedNamedElement target) {
        String qname = RsQualifiedNamedElementUtil.qualifiedNameInCrate(context.getRootMod(), target);
        if (qname != null) return qname;
        String crateRel = target.getCrateRelativePath();
        if (crateRel == null) return null;
        if (crateRel.startsWith("::")) crateRel = crateRel.substring(2);
        Crate targetCrate = RsElementUtil.getContainingCrate(target);
        if (targetCrate != null) {
            String normName = targetCrate.getNormName();
            if (normName != null && !crateRel.startsWith(normName + "::")) {
                return normName + "::" + crateRel;
            }
        }
        return crateRel;
    }

    private static boolean sameCrate(@NotNull RsElement here, @NotNull Crate there) {
        Crate hereCrate = RsElementUtil.getContainingCrate(here);
        if (hereCrate == null) return false;
        if (hereCrate == there) return true;
        Integer a = hereCrate.getId();
        Integer b = there.getId();
        return a != null && a.equals(b);
    }
}
