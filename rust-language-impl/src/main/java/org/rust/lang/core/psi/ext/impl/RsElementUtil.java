/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import consulo.component.util.ModificationTracker;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.psi.PsiUtilCore;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.RsInferenceResult;

import java.util.*;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;
import org.rust.lang.core.crate.impl.FakeInvalidCrate;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiWhiteSpace;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve.Processors;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.*;

/**
 * Delegates to {@link RsElementExtKt}, {@link PsiElementKt}, {@link PsiElementExt},
 * {@link RsPsiJavaUtil}, {@link CfgUtils}, {@link RsDocAndAttributeOwnerKt}, etc.
 */
public final class RsElementUtil {

    private RsElementUtil() {
    }

    // ========================
    // PSI tree traversal
    // ========================

    @Nullable
    public static <T extends PsiElement> T ancestorStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorOrSelf(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, false);
    }

    @Nullable
    public static <T extends PsiElement> T contextStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiElementUtil.contextStrict(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T stubAncestorStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        // In the stub-aware version, we try to walk stubs first, falling back to PSI
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T childOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiElementUtil.childOfType(element, clazz);
    }

    @Nonnull
    public static <T extends PsiElement> List<T> childrenOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiElementUtil.childrenOfType(element, clazz);
    }

    @Nonnull
    public static <T extends PsiElement> List<T> stubChildrenOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiElementUtil.stubChildrenOfType(element, clazz);
    }

    @Nonnull
    public static <T extends PsiElement> List<T> descendantsOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiElementUtil.descendantsOfType(element, clazz);
    }

    @Nonnull
    public static <T extends PsiElement> List<T> descendantsOfTypeOrSelf(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        List<T> result = new ArrayList<>();
        if (clazz.isInstance(element)) {
            result.add(clazz.cast(element));
        }
        result.addAll(PsiElementUtil.descendantsOfType(element, clazz));
        return result;
    }

    @Nullable
    public static <T extends PsiElement> T descendantOfTypeStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz);
    }

    /**
     * Same as descendantsOfTypeOrSelf but uses "stub" traversal
     */
    @Nonnull
    public static <T extends PsiElement> List<T> stubDescendantsOfTypeOrSelf(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return descendantsOfTypeOrSelf(element, clazz);
    }

    /**
     * Returns the first descendant of the given type (strict - not self).
     * "Stub" variant falls back to regular PsiTreeUtil.
     */
    @Nullable
    public static <T extends PsiElement> T stubDescendantOfTypeOrStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz);
    }

    @Nullable
    public static PsiElement getStubParent(@Nonnull PsiElement element) {
        return PsiElementUtil.getStubParent(element);
    }

    // ========================
    // Sibling navigation
    // ========================

    @Nullable
    public static PsiElement getNextNonCommentSibling(@Nonnull PsiElement element) {
        return PsiElementUtil.getNextNonCommentSibling(element);
    }

    @Nullable
    public static PsiElement getPrevNonCommentSibling(@Nonnull PsiElement element) {
        return PsiElementUtil.getPrevNonCommentSibling(element);
    }

    @Nullable
    public static PsiElement getPrevNonWhitespaceSibling(@Nonnull PsiElement element) {
        return PsiTreeUtil.skipWhitespacesBackward(element);
    }

    // ========================
    // Element type
    // ========================

    @Nonnull
    public static IElementType getElementType(@Nonnull PsiElement element) {
        return PsiUtilCore.getElementType(element);
    }

    // ========================
    // Offsets
    // ========================

    public static int getStartOffset(@Nonnull PsiElement element) {
        return PsiElementUtil.getStartOffset(element);
    }

    public static int getEndOffset(@Nonnull PsiElement element) {
        return PsiElementUtil.getEndOffset(element);
    }

    public static boolean containsOffset(@Nonnull PsiElement element, int offset) {
        return element.getTextRange().containsOffset(offset);
    }

    // ========================
    // Iteration
    // ========================

    @Nonnull
    public static Iterable<PsiElement> getAncestors(@Nonnull PsiElement element) {
        return () -> new java.util.Iterator<PsiElement>() {
            PsiElement current = element;
            public boolean hasNext() { return current != null; }
            public PsiElement next() { PsiElement r = current; current = current.getParent(); return r; }
        };
    }

    @Nonnull
    public static Iterable<PsiElement> getContexts(@Nonnull PsiElement element) {
        return () -> new java.util.Iterator<PsiElement>() {
            PsiElement current = element;
            public boolean hasNext() { return current != null; }
            public PsiElement next() { PsiElement r = current; current = current.getContext(); return r; }
        };
    }

    // ========================
    // Cfg / expansion
    // ========================

    public static boolean existsAfterExpansion(@Nonnull PsiElement element) {
        return CfgUtils.existsAfterExpansion(element);
    }

    public static boolean existsAfterExpansion(@Nonnull PsiElement element, @Nullable Crate crate) {
        return CfgUtils.existsAfterExpansion(element, crate);
    }

    public static boolean existsAfterExpansionSelf(@Nonnull RsDocAndAttributeOwner self, @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(self, crate);
    }

    public static boolean isEnabledByCfg(@Nonnull PsiElement element) {
        return CfgUtils.isEnabledByCfg(element);
    }

    public static boolean isEnabledByCfg(@Nonnull PsiElement element, @Nullable org.rust.lang.core.crate.Crate crate) {
        if (element instanceof RsDocAndAttributeOwner && crate != null) {
            return isEnabledByCfgSelf((RsDocAndAttributeOwner) element, crate);
        }
        return isEnabledByCfg(element);
    }

    public static boolean isEnabledByCfgSelf(@Nonnull RsDocAndAttributeOwner self, @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.isEnabledByCfgSelf(self, crate);
    }

    @Nonnull
    public static RsCodeStatus getCodeStatus(@Nonnull PsiElement element) {
        return CfgUtils.getCodeStatus(element, null);
    }

    @Nonnull
    public static RsCodeStatus getCodeStatus(@Nonnull PsiElement element, @Nullable Crate crate) {
        return CfgUtils.getCodeStatus(element, crate);
    }

    // ========================
    // Edition
    // ========================

    @Nullable
    public static CargoWorkspace.Edition getEdition(@Nonnull PsiElement element) {
        Crate crate = getContainingCrate(element);
        if (crate == null) return null;
        return crate.getEdition();
    }

    public static boolean isAtLeastEdition2018(@Nonnull PsiElement element) {
        CargoWorkspace.Edition edition = getEdition(element);
        return edition != null && edition.compareTo(CargoWorkspace.Edition.EDITION_2018) >= 0;
    }

    // ========================
    // Crate / Cargo
    // ========================

    @Nonnull
    public static Crate getContainingCrate(@Nonnull PsiElement element) {
        RsFile file = PsiElementUtil.getContainingRsFileSkippingCodeFragments(element);
        Crate crate = file != null ? file.getCrate() : null;
        return crate != null ? crate : new FakeInvalidCrate(element.getProject());
    }

    @Nullable
    public static CargoWorkspace.Package getContainingCargoPackage(@Nonnull PsiElement element) {
        Crate crate = getContainingCrate(element);
        if (crate == null) return null;
        CargoWorkspace.Target target = crate.getCargoTarget();
        return target != null ? target.getPkg() : null;
    }

    @Nullable
    public static CargoWorkspace getCargoWorkspace(@Nonnull PsiElement element) {
        Crate crate = getContainingCrate(element);
        return crate != null ? crate.getCargoWorkspace() : null;
    }

    @Nullable
    public static org.rust.cargo.api.workspace.PackageOrigin getContainingCrateAsPackageOrigin(@Nonnull PsiElement element) {
        Crate crate = getContainingCrate(element);
        if (crate == null) return null;
        CargoWorkspace.Target target = crate.getCargoTarget();
        if (target == null) return null;
        return target.getPkg().getOrigin();
    }

    @Nullable
    public static CargoWorkspace.Edition getContainingCrateEdition(@Nonnull PsiElement element) {
        Crate crate = getContainingCrate(element);
        if (crate == null) return null;
        return crate.getEdition();
    }

    // ========================
    // Mod
    // ========================

    @Nullable
    public static RsMod getContainingMod(@Nonnull PsiElement element) {
        if (element instanceof RsElement) {
            return ((RsElement) element).getContainingMod();
        }
        return PsiTreeUtil.getContextOfType(element, RsMod.class, true);
    }

    /** Alias for getContainingMod */
    @Nullable
    public static RsMod containingMod(@Nonnull PsiElement element) {
        return getContainingMod(element);
    }

    @Nonnull
    public static RsMod getContainingModOrSelf(@Nonnull RsElement element) {
        if (element instanceof RsMod) return (RsMod) element;
        RsMod mod = getContainingMod((PsiElement) element);
        if (mod != null) return mod;
        throw new IllegalStateException("Element has no containing mod: " + element);
    }

    @Nullable
    public static RsMod getCrateRoot(@Nonnull PsiElement element) {
        if (element instanceof RsElement) {
            RsFile file = PsiElementUtil.getContainingRsFileSkippingCodeFragments(element);
            if (file != null) {
                return file.getCrateRoot();
            }
        }
        return null;
    }

    // ========================
    // File
    // ========================

    @Nullable
    public static RsFile contextualFile(@Nonnull PsiElement element) {
        return PsiElementUtil.getContainingRsFileSkippingCodeFragments(element);
    }

    @Nullable
    public static RsFile getContainingRsFileSkippingCodeFragments(@Nonnull PsiElement element) {
        return PsiElementUtil.getContainingRsFileSkippingCodeFragments(element);
    }

    // ========================
    // Attributes
    // ========================

    @Nonnull
    public static QueryAttributes<RsMetaItem> getQueryAttributes(@Nonnull RsDocAndAttributeOwner self) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(self);
    }

    @Nullable
    public static RsOuterAttr findOuterAttr(@Nonnull RsOuterAttributeOwner item, @Nonnull String name) {
        return RsDocAndAttributeOwnerUtil.findOuterAttr(item, name);
    }

    // ========================
    // Types / inference
    // ========================

    @Nullable
    public static RsInferenceResult getInference(@Nonnull PsiElement element) {
        return ExtensionsUtil.getInference(element);
    }

    @Nonnull
    public static KnownItems getKnownItems(@Nonnull RsElement element) {
        return KnownItems.getKnownItems(element);
    }

    // ========================
    // Misc utility
    // ========================

    public static boolean isConstantLike(@Nonnull PsiElement element) {
        return element instanceof RsConstant || element instanceof RsConstParameter;
    }

    public static boolean isContextOf(@Nonnull PsiElement ancestor, @Nonnull PsiElement child) {
        return PsiElementExt.isContextOf(ancestor, child);
    }

    public static boolean isIntentionPreviewElement(@Nonnull PsiElement element) {
        return PsiElementExt.isIntentionPreviewElement(element);
    }

    /**
     * Checks if a PsiElement is a keyword-like token (keyword or contextual keyword).
     */
    public static boolean isKeywordLike(@Nonnull PsiElement element) {
        IElementType type = PsiUtilCore.getElementType(element);
        return RsTokenSets.RS_KEYWORDS.contains(type) || RsTokenSets.RS_CONTEXTUAL_KEYWORDS.contains(type);
    }

    public static boolean isPublic(@Nonnull RsVisibilityOwner element) {
        return element.isPublic();
    }

    /**
     * Checks whether the element is under a #[cfg(test)] context.
     */
    public static boolean isUnderCfgTest(@Nonnull RsElement element) {
        RsMod mod = getContainingMod((PsiElement) element);
        while (mod != null) {
            if (mod instanceof RsDocAndAttributeOwner) {
                QueryAttributes<RsMetaItem> attrs = RsDocAndAttributeOwnerUtil.getQueryAttributes((RsDocAndAttributeOwner) mod);
                if (attrs.hasAttributeWithArg("cfg", "test")) {
                    return true;
                }
            }
            mod = mod.getSuper();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public static <T extends PsiElement> T findPreviewCopyIfNeeded(@Nonnull T element) {
        // Delegate to intention preview utility - in Java the element is returned as-is
        // unless we're in a preview context
        PsiFile file = element.getContainingFile();
        if (file != null && file.getOriginalFile() != file) {
            // We're in a copy/preview - try to find the equivalent element
            PsiElement original = file.getOriginalFile().findElementAt(element.getTextRange().getStartOffset());
            if (original != null) {
                PsiElement parent = PsiTreeUtil.getParentOfType(original, element.getClass(), false);
                if (parent != null && element.getClass().isInstance(parent)) {
                    return (T) parent;
                }
            }
        }
        return element;
    }

    @Nullable
    public static PsiElement firstKeyword(@Nonnull RsStructOrEnumItemElement item) {
        return RsPsiJavaUtil.firstKeyword(item);
    }

    /**
     * Names of every value binding visible at the given element's position, collected by walking
     * the nested scopes upwards from it.
     */
    @Nonnull
    public static Set<String> getAllVisibleBindings(@Nonnull RsElement element) {
        return Processors.collectNames(
            processor -> NameResolution.processNestedScopesUpwards(
                element, Namespace.VALUES, processor));
    }

    /**
     * Returns locally visible variable bindings at the given element position.
     */
    @Nonnull
    public static Map<String, RsPatBinding> getLocalVariableVisibleBindings(@Nonnull PsiElement element) {
        Map<String, RsPatBinding> result = new LinkedHashMap<>();
        PsiElement scope = element;
        while (scope != null) {
            if (scope instanceof RsBlock || scope instanceof RsFunction) {
                for (PsiElement child = scope.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getTextRange().getStartOffset() >= element.getTextRange().getStartOffset()) break;
                    if (child instanceof RsLetDecl) {
                        RsPat pat = ((RsLetDecl) child).getPat();
                        if (pat != null) {
                            for (RsPatBinding binding : PsiTreeUtil.findChildrenOfType(pat, RsPatBinding.class)) {
                                String name = binding.getName();
                                if (name != null) {
                                    result.putIfAbsent(name, binding);
                                }
                            }
                        }
                    }
                }
            }
            if (scope instanceof RsFunction) break;
            scope = scope.getParent();
        }
        return result;
    }

    @Nullable
    public static String getMacroName(@Nonnull RsMacroCall call) {
        return RsMacroCallUtil.getMacroName(call);
    }

    @Nonnull
    public static ModificationTracker getRustStructureOrAnyPsiModificationTracker(@Nonnull RsElement element) {
        return RsPsiUtilUtil.getRustStructureOrAnyPsiModificationTracker(element);
    }

    @Nullable
    public static RsPat getTopLevelPattern(@Nonnull RsPatBinding binding) {
        return RsPatBindingUtil.getTopLevelPattern(binding);
    }

    /**
     * Filters a list of elements to only include those that are in scope
     * relative to the given context element.
     */
    @Nonnull
    public static <T extends RsElement> List<T> filterInScope(@Nonnull List<T> elements, @Nonnull PsiElement context) {
        RsMod contextMod = getContainingMod(context);
        if (contextMod == null) return elements;
        List<T> result = new ArrayList<>();
        for (T element : elements) {
            if (element instanceof RsVisible) {
                if (RsVisibilityUtil.isVisibleFrom((RsVisible) element, contextMod)) {
                    result.add(element);
                }
            } else {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Returns children including leaf (token) elements.
     */
    @Nonnull
    public static List<PsiElement> getChildrenWithLeaves(@Nonnull PsiElement element) {
        List<PsiElement> children = new ArrayList<>();
        PsiElement child = element.getFirstChild();
        while (child != null) {
            children.add(child);
            child = child.getNextSibling();
        }
        return children;
    }

    /**
     * Returns the contextual file for the element, handling macro expansions.
     */
    @Nonnull
    public static PsiFile getContextualFile(@Nonnull PsiElement element) {
        return RsElementExtUtil.getContextualFile(element);
    }

    public static boolean isCfgUnknown(@Nonnull RsDocAndAttributeOwner element) {
        return getCodeStatus((PsiElement) element) == RsCodeStatus.CFG_UNKNOWN;
    }

    public static void deleteWithSurroundingComma(@Nonnull PsiElement element) {
        PsiElement nextSibling = element.getNextSibling();
        while (nextSibling instanceof consulo.language.psi.PsiWhiteSpace) {
            nextSibling = nextSibling.getNextSibling();
        }
        if (nextSibling != null && nextSibling.getText().equals(",")) {
            nextSibling.delete();
        }
        element.delete();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends PsiElement> T stubChildOfElementType(@Nonnull PsiElement parent, @Nonnull consulo.language.ast.IElementType type, @Nonnull Class<T> clazz) {
        return (T) PsiElementUtil.stubChildOfElementType(parent, consulo.language.ast.TokenSet.create(type), clazz);
    }

    @Nonnull
    public static String getUnescapedText(@Nullable PsiElement element) {
        if (element == null) return "";
        return element.getText();
    }

    @Nonnull
    public static QueryAttributes<RsMetaItem> queryAttributes(@Nonnull RsDocAndAttributeOwner element) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(element);
    }

    @Nullable
    public static PsiElement getIdentifyingElement(@Nonnull RsAbstractable element) {
        if (element instanceof consulo.language.psi.PsiNameIdentifierOwner) {
            return ((consulo.language.psi.PsiNameIdentifierOwner) element).getNameIdentifier();
        }
        return null;
    }
}
