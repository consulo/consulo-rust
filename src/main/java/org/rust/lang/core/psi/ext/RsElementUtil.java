/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.RsInferenceResult;

import java.util.*;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

/**
 * <p>
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
    public static <T extends PsiElement> T ancestorStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorOrSelf(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, false);
    }

    @Nullable
    public static <T extends PsiElement> T contextStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiElementUtil.contextStrict(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T stubAncestorStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        // In the stub-aware version, we try to walk stubs first, falling back to PSI
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T childOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiElementUtil.childOfType(element, clazz);
    }

    @NotNull
    public static <T extends PsiElement> List<T> childrenOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiElementUtil.childrenOfType(element, clazz);
    }

    @NotNull
    public static <T extends PsiElement> List<T> stubChildrenOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiElementUtil.stubChildrenOfType(element, clazz);
    }

    @NotNull
    public static <T extends PsiElement> List<T> descendantsOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiElementUtil.descendantsOfType(element, clazz);
    }

    @NotNull
    public static <T extends PsiElement> List<T> descendantsOfTypeOrSelf(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        List<T> result = new ArrayList<>();
        if (clazz.isInstance(element)) {
            result.add(clazz.cast(element));
        }
        result.addAll(PsiElementUtil.descendantsOfType(element, clazz));
        return result;
    }

    @Nullable
    public static <T extends PsiElement> T descendantOfTypeStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz);
    }

    /**
     * Same as descendantsOfTypeOrSelf but uses "stub" traversal
     */
    @NotNull
    public static <T extends PsiElement> List<T> stubDescendantsOfTypeOrSelf(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return descendantsOfTypeOrSelf(element, clazz);
    }

    /**
     * Returns the first descendant of the given type (strict - not self).
     * "Stub" variant falls back to regular PsiTreeUtil.
     */
    @Nullable
    public static <T extends PsiElement> T stubDescendantOfTypeOrStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz);
    }

    @Nullable
    public static PsiElement getStubParent(@NotNull PsiElement element) {
        return PsiElementUtil.getStubParent(element);
    }

    // ========================
    // Sibling navigation
    // ========================

    @Nullable
    public static PsiElement getNextNonCommentSibling(@NotNull PsiElement element) {
        return PsiElementUtil.getNextNonCommentSibling(element);
    }

    @Nullable
    public static PsiElement getPrevNonCommentSibling(@NotNull PsiElement element) {
        return PsiElementUtil.getPrevNonCommentSibling(element);
    }

    @Nullable
    public static PsiElement getPrevNonWhitespaceSibling(@NotNull PsiElement element) {
        return PsiTreeUtil.skipWhitespacesBackward(element);
    }

    // ========================
    // Element type
    // ========================

    @NotNull
    public static IElementType getElementType(@NotNull PsiElement element) {
        return PsiUtilCore.getElementType(element);
    }

    // ========================
    // Offsets
    // ========================

    public static int getStartOffset(@NotNull PsiElement element) {
        return PsiElementUtil.getStartOffset(element);
    }

    public static int getEndOffset(@NotNull PsiElement element) {
        return PsiElementUtil.getEndOffset(element);
    }

    public static boolean containsOffset(@NotNull PsiElement element, int offset) {
        return element.getTextRange().containsOffset(offset);
    }

    // ========================
    // Iteration
    // ========================

    @NotNull
    public static Iterable<PsiElement> getAncestors(@NotNull PsiElement element) {
        return () -> new java.util.Iterator<PsiElement>() {
            PsiElement current = element;
            public boolean hasNext() { return current != null; }
            public PsiElement next() { PsiElement r = current; current = current.getParent(); return r; }
        };
    }

    @NotNull
    public static Iterable<PsiElement> getContexts(@NotNull PsiElement element) {
        return () -> new java.util.Iterator<PsiElement>() {
            PsiElement current = element;
            public boolean hasNext() { return current != null; }
            public PsiElement next() { PsiElement r = current; current = current.getContext(); return r; }
        };
    }

    // ========================
    // Cfg / expansion
    // ========================

    public static boolean existsAfterExpansion(@NotNull PsiElement element) {
        return CfgUtils.existsAfterExpansion(element);
    }

    public static boolean existsAfterExpansion(@NotNull PsiElement element, @Nullable Crate crate) {
        return CfgUtils.existsAfterExpansion(element, crate);
    }

    public static boolean existsAfterExpansionSelf(@NotNull RsDocAndAttributeOwner self, @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(self, crate);
    }

    public static boolean isEnabledByCfg(@NotNull PsiElement element) {
        return CfgUtils.isEnabledByCfg(element);
    }

    public static boolean isEnabledByCfg(@NotNull PsiElement element, @Nullable org.rust.lang.core.crate.Crate crate) {
        if (element instanceof RsDocAndAttributeOwner && crate != null) {
            return isEnabledByCfgSelf((RsDocAndAttributeOwner) element, crate);
        }
        return isEnabledByCfg(element);
    }

    public static boolean isEnabledByCfgSelf(@NotNull RsDocAndAttributeOwner self, @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.isEnabledByCfgSelf(self, crate);
    }

    @NotNull
    public static RsCodeStatus getCodeStatus(@NotNull PsiElement element) {
        return CfgUtils.getCodeStatus(element, null);
    }

    @NotNull
    public static RsCodeStatus getCodeStatus(@NotNull PsiElement element, @Nullable Crate crate) {
        return CfgUtils.getCodeStatus(element, crate);
    }

    // ========================
    // Edition
    // ========================

    @Nullable
    public static CargoWorkspace.Edition getEdition(@NotNull PsiElement element) {
        Crate crate = getContainingCrate(element);
        if (crate == null) return null;
        return crate.getEdition();
    }

    public static boolean isAtLeastEdition2018(@NotNull PsiElement element) {
        CargoWorkspace.Edition edition = getEdition(element);
        return edition != null && edition.compareTo(CargoWorkspace.Edition.EDITION_2018) >= 0;
    }

    // ========================
    // Crate / Cargo
    // ========================

    @Nullable
    public static Crate getContainingCrate(@NotNull PsiElement element) {
        if (element instanceof RsElement) {
            return ((RsElement) element).getContainingCrate();
        }
        RsFile file = PsiElementUtil.getContainingRsFileSkippingCodeFragments(element);
        if (file != null) {
            return file.getCrate();
        }
        return null;
    }

    @Nullable
    public static CargoWorkspace.Package getContainingCargoPackage(@NotNull PsiElement element) {
        Crate crate = getContainingCrate(element);
        if (crate == null) return null;
        CargoWorkspace.Target target = crate.getCargoTarget();
        return target != null ? target.getPkg() : null;
    }

    @Nullable
    public static CargoWorkspace getCargoWorkspace(@NotNull PsiElement element) {
        Crate crate = getContainingCrate(element);
        return crate != null ? crate.getCargoWorkspace() : null;
    }

    @Nullable
    public static org.rust.cargo.project.workspace.PackageOrigin getContainingCrateAsPackageOrigin(@NotNull PsiElement element) {
        Crate crate = getContainingCrate(element);
        if (crate == null) return null;
        CargoWorkspace.Target target = crate.getCargoTarget();
        if (target == null) return null;
        return target.getPkg().getOrigin();
    }

    @Nullable
    public static CargoWorkspace.Edition getContainingCrateEdition(@NotNull PsiElement element) {
        Crate crate = getContainingCrate(element);
        if (crate == null) return null;
        return crate.getEdition();
    }

    // ========================
    // Mod
    // ========================

    @Nullable
    public static RsMod getContainingMod(@NotNull PsiElement element) {
        if (element instanceof RsElement) {
            return ((RsElement) element).getContainingMod();
        }
        return PsiTreeUtil.getContextOfType(element, RsMod.class, true);
    }

    /** Alias for getContainingMod */
    @Nullable
    public static RsMod containingMod(@NotNull PsiElement element) {
        return getContainingMod(element);
    }

    @NotNull
    public static RsMod getContainingModOrSelf(@NotNull RsElement element) {
        if (element instanceof RsMod) return (RsMod) element;
        RsMod mod = getContainingMod((PsiElement) element);
        if (mod != null) return mod;
        throw new IllegalStateException("Element has no containing mod: " + element);
    }

    @Nullable
    public static RsMod getCrateRoot(@NotNull PsiElement element) {
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
    public static RsFile contextualFile(@NotNull PsiElement element) {
        return PsiElementUtil.getContainingRsFileSkippingCodeFragments(element);
    }

    @Nullable
    public static RsFile getContainingRsFileSkippingCodeFragments(@NotNull PsiElement element) {
        return PsiElementUtil.getContainingRsFileSkippingCodeFragments(element);
    }

    // ========================
    // Attributes
    // ========================

    @NotNull
    public static QueryAttributes<RsMetaItem> getQueryAttributes(@NotNull RsDocAndAttributeOwner self) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(self);
    }

    @Nullable
    public static RsOuterAttr findOuterAttr(@NotNull RsOuterAttributeOwner item, @NotNull String name) {
        return RsDocAndAttributeOwnerUtil.findOuterAttr(item, name);
    }

    // ========================
    // Types / inference
    // ========================

    @Nullable
    public static RsInferenceResult getInference(@NotNull PsiElement element) {
        return ExtensionsUtil.getInference(element);
    }

    @NotNull
    public static KnownItems getKnownItems(@NotNull RsElement element) {
        return KnownItems.getKnownItems(element);
    }

    // ========================
    // Misc utility
    // ========================

    public static boolean isConstantLike(@NotNull PsiElement element) {
        return element instanceof RsConstant || element instanceof RsConstParameter;
    }

    public static boolean isContextOf(@NotNull PsiElement ancestor, @NotNull PsiElement child) {
        return PsiElementExt.isContextOf(ancestor, child);
    }

    public static boolean isIntentionPreviewElement(@NotNull PsiElement element) {
        return PsiElementExt.isIntentionPreviewElement(element);
    }

    /**
     * Checks if a PsiElement is a keyword-like token (keyword or contextual keyword).
     */
    public static boolean isKeywordLike(@NotNull PsiElement element) {
        IElementType type = PsiUtilCore.getElementType(element);
        return RsTokenType.RS_KEYWORDS.contains(type) || RsTokenType.RS_CONTEXTUAL_KEYWORDS.contains(type);
    }

    public static boolean isPublic(@NotNull RsVisibilityOwner element) {
        return element.isPublic();
    }

    /**
     * Checks whether the element is under a #[cfg(test)] context.
     */
    public static boolean isUnderCfgTest(@NotNull RsElement element) {
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
    @NotNull
    public static <T extends PsiElement> T findPreviewCopyIfNeeded(@NotNull T element) {
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
    public static PsiElement firstKeyword(@NotNull RsStructOrEnumItemElement item) {
        return RsPsiJavaUtil.firstKeyword(item);
    }

    @NotNull
    public static Set<String> getAllVisibleBindings(@NotNull PsiElement element) {
        return RsPsiJavaUtil.getAllVisibleBindings(element);
    }

    /**
     * Returns locally visible variable bindings at the given element position.
     */
    @NotNull
    public static Map<String, RsPatBinding> getLocalVariableVisibleBindings(@NotNull PsiElement element) {
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
    public static String getMacroName(@NotNull RsMacroCall call) {
        return RsMacroCallUtil.getMacroName(call);
    }

    @NotNull
    public static ModificationTracker getRustStructureOrAnyPsiModificationTracker(@NotNull RsElement element) {
        return RsPsiUtilUtil.getRustStructureOrAnyPsiModificationTracker(element);
    }

    @Nullable
    public static RsPat getTopLevelPattern(@NotNull RsPatBinding binding) {
        return RsPatBindingUtil.getTopLevelPattern(binding);
    }

    /**
     * Filters a list of elements to only include those that are in scope
     * relative to the given context element.
     */
    @NotNull
    public static <T extends RsElement> List<T> filterInScope(@NotNull List<T> elements, @NotNull PsiElement context) {
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
    @NotNull
    public static List<PsiElement> getChildrenWithLeaves(@NotNull PsiElement element) {
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
    @NotNull
    public static PsiFile getContextualFile(@NotNull PsiElement element) {
        return RsElementExtUtil.getContextualFile(element);
    }

    public static boolean isCfgUnknown(@NotNull RsDocAndAttributeOwner element) {
        return getCodeStatus((PsiElement) element) == RsCodeStatus.CFG_UNKNOWN;
    }

    public static void deleteWithSurroundingComma(@NotNull PsiElement element) {
        PsiElement nextSibling = element.getNextSibling();
        while (nextSibling instanceof com.intellij.psi.PsiWhiteSpace) {
            nextSibling = nextSibling.getNextSibling();
        }
        if (nextSibling != null && nextSibling.getText().equals(",")) {
            nextSibling.delete();
        }
        element.delete();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends PsiElement> T stubChildOfElementType(@NotNull PsiElement parent, @NotNull com.intellij.psi.tree.IElementType type, @NotNull Class<T> clazz) {
        return (T) PsiElementUtil.stubChildOfElementType(parent, com.intellij.psi.tree.TokenSet.create(type), clazz);
    }

    @NotNull
    public static String getUnescapedText(@Nullable PsiElement element) {
        if (element == null) return "";
        return element.getText();
    }

    @NotNull
    public static QueryAttributes<RsMetaItem> queryAttributes(@NotNull RsDocAndAttributeOwner element) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(element);
    }

    @Nullable
    public static PsiElement getIdentifyingElement(@NotNull RsAbstractable element) {
        if (element instanceof com.intellij.psi.PsiNameIdentifierOwner) {
            return ((com.intellij.psi.PsiNameIdentifierOwner) element).getNameIdentifier();
        }
        return null;
    }
}
