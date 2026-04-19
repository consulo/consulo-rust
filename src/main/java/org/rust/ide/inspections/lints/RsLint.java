/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.*;

import java.util.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.RsMod;

/**
 * Rust lints.
 */
public abstract class RsLint {

    private final String myId;
    private final List<String> myGroupIds;
    private final RsLintLevel myDefaultLevel;

    protected RsLint(@NotNull String id, @NotNull List<String> groupIds, @NotNull RsLintLevel defaultLevel) {
        myId = id;
        myGroupIds = groupIds;
        myDefaultLevel = defaultLevel;
    }

    protected RsLint(@NotNull String id, @NotNull List<String> groupIds) {
        this(id, groupIds, RsLintLevel.WARN);
    }

    protected RsLint(@NotNull String id) {
        this(id, Collections.emptyList(), RsLintLevel.WARN);
    }

    @NotNull
    public String getId() {
        return myId;
    }

    // Rustc lints - warnings
    public static final RsLint NonSnakeCase = new RsLintImpl("non_snake_case", Arrays.asList("bad_style", "nonstandard_style"));
    public static final RsLint NonCamelCaseTypes = new RsLintImpl("non_camel_case_types", Arrays.asList("bad_style", "nonstandard_style"));
    public static final RsLint NonUpperCaseGlobals = new RsLintImpl("non_upper_case_globals", Arrays.asList("bad_style", "nonstandard_style"));
    public static final RsLint Deprecated = new RsLintImpl("deprecated");
    public static final RsLint UnusedVariables = new RsLintImpl("unused_variables", Collections.singletonList("unused"));
    public static final RsLint UnusedImports = new RsLintImpl("unused_imports", Collections.singletonList("unused"));
    public static final RsLint UnusedMut = new RsLintImpl("unused_mut", Collections.singletonList("unused"));
    public static final RsLint UnreachablePattern = new RsLintImpl("unreachable_patterns", Collections.singletonList("unused"));
    public static final RsLint WhileTrue = new RsLintImpl("while_true");
    public static final RsLint UnreachableCode = new RsLintImpl("unreachable_code");
    public static final RsLint BareTraitObjects = new RsLintImpl("bare_trait_objects", Collections.singletonList("rust_2018_idioms"));
    public static final RsLint NonShorthandFieldPatterns = new RsLintImpl("non_shorthand_field_patterns");
    public static final RsLint UnusedQualifications = new RsLintImpl("unused_qualifications");
    public static final RsLint UnusedMustUse = new RsLintImpl("unused_must_use", Collections.singletonList("unused"));
    public static final RsLint RedundantSemicolons = new RsLintImpl("redundant_semicolons", Collections.singletonList("unused"));
    public static final RsLint UnusedLabels = new RsLintImpl("unused_labels", Collections.singletonList("unused"));
    public static final RsLint PathStatements = new RsLintImpl("path_statements", Collections.singletonList("unused"));
    // Rustc lints - errors
    public static final RsLint UnknownCrateTypes = new RsLintImpl("unknown_crate_types", Collections.emptyList(), RsLintLevel.DENY);
    // Clippy lints
    public static final RsLint NeedlessLifetimes = new RsLintImpl("clippy::needless_lifetimes", Arrays.asList("clippy::complexity", "clippy::all", "clippy"));
    public static final RsLint DoubleMustUse = new RsLintImpl("clippy::double_must_use", Arrays.asList("clippy::style", "clippy::all", "clippy"));
    public static final RsLint WrongSelfConvention = new RsLintImpl("clippy::wrong_self_convention", Arrays.asList("clippy::style", "clippy::all", "clippy"));
    public static final RsLint UnnecessaryCast = new RsLintImpl("clippy::unnecessary_cast", Arrays.asList("clippy::complexity", "clippy::all", "clippy"));

    /**
     * Returns the level of the lint for the given PSI element.
     */
    @NotNull
    public RsLintLevel levelFor(@NotNull PsiElement el) {
        RsLintLevel explicit = explicitLevel(el);
        if (explicit != null) return explicit;
        RsLintLevel superMods = superModsLevel(el);
        if (superMods != null) return superMods;
        return myDefaultLevel;
    }

    @Nullable
    public RsLintLevel explicitLevel(@NotNull PsiElement el) {
        for (PsiElement context : RsElementExtUtil.getContexts(el)) {
            if (context instanceof RsDocAndAttributeOwner) {
                RsDocAndAttributeOwner owner = (RsDocAndAttributeOwner) context;
                List<org.rust.lang.core.psi.RsMetaItem> metaItems = new ArrayList<>();
                for (org.rust.lang.core.psi.RsMetaItem mi : RsDocAndAttributeOwnerUtil.getQueryAttributes(owner).getMetaItems()) {
                    metaItems.add(mi);
                }
                Collections.reverse(metaItems);
                for (org.rust.lang.core.psi.RsMetaItem metaItem : metaItems) {
                    org.rust.lang.core.psi.RsMetaItemArgs args = metaItem.getMetaItemArgs();
                    if (args == null) continue;
                    List<org.rust.lang.core.psi.RsMetaItem> itemList = args.getMetaItemList();
                    if (itemList == null) continue;
                    boolean matches = false;
                    for (org.rust.lang.core.psi.RsMetaItem item : itemList) {
                        String itemId = RsMetaItemUtil.getId(item);
                        if (myId.equals(itemId) || myGroupIds.contains(itemId)) {
                            matches = true;
                            break;
                        }
                    }
                    if (matches) {
                        String name = metaItem.getName();
                        if (name != null) {
                            RsLintLevel level = RsLintLevel.valueForId(name);
                            if (level != null) return level;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private RsLintLevel superModsLevel(@NotNull PsiElement el) {
        RsMod lastMod = null;
        for (PsiElement context : RsElementExtUtil.getContexts(el)) {
            if (context instanceof RsMod) {
                lastMod = (RsMod) context;
            }
        }
        if (lastMod == null) return null;
        for (RsMod superMod : RsModExtUtil.getSuperMods(lastMod)) {
            RsLintLevel level = explicitLevel(superMod);
            if (level != null) return level;
        }
        return null;
    }

    private static class RsLintImpl extends RsLint {
        RsLintImpl(@NotNull String id, @NotNull List<String> groupIds, @NotNull RsLintLevel defaultLevel) {
            super(id, groupIds, defaultLevel);
        }

        RsLintImpl(@NotNull String id, @NotNull List<String> groupIds) {
            super(id, groupIds);
        }

        RsLintImpl(@NotNull String id) {
            super(id);
        }
    }

    /**
     * External linter lint.
     */
    public static class ExternalLinterLint extends RsLint {
        public ExternalLinterLint(@NotNull String id) {
            super(id);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            ExternalLinterLint that = (ExternalLinterLint) other;
            return getId().equals(that.getId());
        }

        @Override
        public int hashCode() {
            return getId().hashCode();
        }
    }
}
