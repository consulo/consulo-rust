/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

/**
 * Utility methods for move refactoring. Placed in a class to decrease their priority in completion.
 */
public final class RsMoveUtil {

    public static final Logger LOG = Logger.getInstance(RsMoveUtil.class);

    public static final String TMP_MOD_NAME = "__tmp__";

    private RsMoveUtil() {
    }

    /**
     * Returns shallow descendants of moved elements matching the given type.
     * When {@code includeSelf} is true, the moved items themselves are included if they match.
     */
    @NotNull
    public static <T extends PsiElement> List<T> movedElementsShallowDescendantsOfType(
        @NotNull List<ElementToMove> elementsToMove,
        @NotNull Class<T> aClass,
        boolean includeSelf
    ) {
        List<T> result = new ArrayList<>();
        for (ElementToMove e : elementsToMove) {
            PsiElement element = e.getElement();
            if (includeSelf && aClass.isInstance(element)) {
                result.add(aClass.cast(element));
            }
            for (PsiElement child : element.getChildren()) {
                if (aClass.isInstance(child)) {
                    result.add(aClass.cast(child));
                }
                collectDescendantsOfType(child, aClass, result);
            }
        }
        return result;
    }

    /**
     * Returns deep descendants of moved elements matching the given type.
     */
    @NotNull
    public static <T extends PsiElement> List<T> movedElementsDeepDescendantsOfType(
        @NotNull List<ElementToMove> elementsToMove,
        @NotNull Class<T> aClass
    ) {
        List<T> result = new ArrayList<>();
        for (ElementToMove e : elementsToMove) {
            PsiElement element = e.getElement();
            if (aClass.isInstance(element)) {
                result.add(aClass.cast(element));
            }
            collectDescendantsOfType(element, aClass, result);
        }
        return result;
    }

    private static <T extends PsiElement> void collectDescendantsOfType(
        @NotNull PsiElement element,
        @NotNull Class<T> aClass,
        @NotNull List<T> result
    ) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (aClass.isInstance(child)) {
                result.add(aClass.cast(child));
            }
            collectDescendantsOfType(child, aClass, result);
        }
    }

    /**
     * Updates the vis restriction scope when moving elements between modules.
     */
    public static void updateScopeIfNecessary(
        @NotNull RsVisRestriction visRestriction,
        @NotNull RsPsiFactory psiFactory,
        @NotNull RsMod targetMod
    ) {
        RsPath path = visRestriction.getPath();
        if (path == null) return;
        PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
        if (resolved == null) return;
        // If the restriction already refers to a parent of targetMod, keep it
        if (resolved instanceof RsMod) {
            RsMod restrictedMod = (RsMod) resolved;
            List<RsMod> targetSuperMods = targetMod.getSuperMods();
            if (targetSuperMods.contains(restrictedMod)) return;
        }
        // Otherwise update to pub(crate)
        RsVis newVis = psiFactory.createPubCrateRestricted();
        if (newVis != null) {
            PsiElement parent = visRestriction.getParent();
            if (parent instanceof RsVis) {
                parent.replace(newVis);
            }
        }
    }

    @Nullable
    public static RsPath toRsPath(@NotNull String text, @NotNull RsPsiFactory psiFactory) {
        RsPath path = psiFactory.tryCreatePath(text);
        if (path == null) {
            LOG.error("Can't create RsPath from '" + text + "'");
        }
        return path;
    }

    @Nullable
    public static RsPath toRsPath(@NotNull String text, @NotNull RsCodeFragmentFactory codeFragmentFactory, @NotNull RsElement context) {
        RsPath path = codeFragmentFactory.createPath(text, context);
        if (path == null) {
            LOG.error("Can't create RsPath from '" + text + "' in context " + context);
        }
        return path;
    }

    @Nullable
    public static RsPath toRsPathInEmptyTmpMod(
        @NotNull String text,
        @NotNull RsCodeFragmentFactory codeFragmentFactory,
        @NotNull RsPsiFactory psiFactory,
        @NotNull RsMod context
    ) {
        RsModItem mod = psiFactory.createModItem(RsMoveUtil.TMP_MOD_NAME, "");
        org.rust.lang.core.macros.MacrosUtil.setContext(mod, context);
        return toRsPath(text, codeFragmentFactory, mod);
    }

    public static boolean isAbsolute(@NotNull RsPath path) {
        if (RsPathUtil.basePath(path).getHasColonColon()) return true;
        if (startsWithSuper(path)) return false;

        if (path.getContainingFile() instanceof DummyHolder) LOG.error("Path '" + path.getText() + "' is inside dummy holder");
        PsiElement basePathTarget = RsPathUtil.basePath(path).getReference() != null ? RsPathUtil.basePath(path).getReference().resolve() : null;
        if (!(basePathTarget instanceof RsMod)) return false;
        return ((RsMod) basePathTarget).isCrateRoot();
    }

    public static boolean startsWithSuper(@NotNull RsPath path) {
        return "super".equals(RsPathUtil.basePath(path).getReferenceName());
    }

    public static boolean startsWithSelf(@NotNull RsPath path) {
        return "self".equals(RsPathUtil.basePath(path).getReferenceName());
    }

    private static boolean startsWithCSelf(@NotNull RsPath path) {
        return "Self".equals(RsPathUtil.basePath(path).getReferenceName());
    }

    @NotNull
    public static String getTextNormalized(@NotNull RsPath path) {
        StringBuilder sb = new StringBuilder();
        if (path.getPath() != null) {
            sb.append(getTextNormalized(path.getPath()));
        }
        if (path.getColoncolon() != null) {
            sb.append(path.getColoncolon().getText());
        }
        if (path.getReferenceName() != null) {
            sb.append(path.getReferenceName());
        }
        return sb.toString();
    }

    public static boolean isSimplePath(@NotNull RsPath path) {
        if (startsWithSelf(path) || startsWithCSelf(path)) return false;
        PsiElement target = path.getReference() != null ? path.getReference().resolve() : null;
        if (target == null) return false;
        if (target instanceof RsMod && path.getParent() instanceof RsPath) return false;

        RsPath subpath = path.getPath();
        while (subpath != null) {
            PsiElement resolved = subpath.getReference() != null ? subpath.getReference().resolve() : null;
            if (!(resolved instanceof RsMod)) return false;
            subpath = subpath.getPath();
        }
        return true;
    }

    @NotNull
    public static RsPath convertFromPathOriginal(@NotNull RsElement pathOriginal, @NotNull RsCodeFragmentFactory codeFragmentFactory) {
        if (pathOriginal instanceof RsPath) {
            return removeTypeArguments((RsPath) pathOriginal, codeFragmentFactory);
        }
        if (pathOriginal instanceof RsPatIdent) {
            RsElement context = pathOriginal.getContext() instanceof RsElement ? (RsElement) pathOriginal.getContext() : pathOriginal;
            RsPath result = codeFragmentFactory.createPath(pathOriginal.getText(), context);
            if (result == null) throw new IllegalStateException("Can't create path from " + pathOriginal.getText());
            return result;
        }
        throw new IllegalStateException("unexpected pathOriginal: " + pathOriginal + ", text=" + pathOriginal.getText());
    }

    @NotNull
    private static RsPath removeTypeArguments(@NotNull RsPath path, @NotNull RsCodeFragmentFactory codeFragmentFactory) {
        if (path.getTypeArgumentList() == null) return path;
        RsPath pathCopy = (RsPath) path.copy();
        if (pathCopy.getTypeArgumentList() != null) {
            pathCopy.getTypeArgumentList().delete();
        }
        RsElement context = path.getContext() instanceof RsElement ? (RsElement) path.getContext() : path;
        RsPath result = toRsPath(pathCopy.getText(), codeFragmentFactory, context);
        return result != null ? result : path;
    }

    public static boolean resolvesToAndAccessible(@NotNull RsPath path, @NotNull RsQualifiedNamedElement target) {
        if (path.getContainingFile() instanceof DummyHolder) LOG.error("Path '" + path.getText() + "' is inside dummy holder");
        if (target.getContainingFile() instanceof DummyHolder) LOG.error("Target " + target + " of path '" + path.getText() + "' is inside dummy holder");
        PsiElement ref = path.getReference() != null ? path.getReference().resolve() : null;
        if (ref == null || !path.getReference().isReferenceTo(target)) return false;
        return isTargetOfEachSubpathAccessible(path);
    }

    public static boolean isTargetOfEachSubpathAccessible(@NotNull RsPath path) {
        RsPath subpath = path;
        while (subpath != null) {
            PsiElement subpathTarget = subpath.getReference() != null ? subpath.getReference().resolve() : null;
            if (subpathTarget instanceof RsVisible) {
                if (!((RsVisible) subpathTarget).isVisibleFrom(path.getContainingMod())) return false;
            }
            subpath = subpath.getPath();
        }
        return true;
    }

    @NotNull
    public static RsMod getContainingModStrict(@NotNull RsElement element) {
        if (element instanceof RsMod) {
            RsMod superMod = ((RsMod) element).getSuper();
            return superMod != null ? superMod : (RsMod) element;
        }
        return element.getContainingMod();
    }

    @NotNull
    public static PsiElement addInner(@NotNull RsMod mod, @NotNull PsiElement element) {
        if (mod instanceof RsModItem) {
            return mod.addBefore(element, ((RsModItem) mod).getRbrace());
        }
        return mod.add(element);
    }

    public static boolean isInsideMovedElements(@NotNull PsiElement element, @NotNull List<ElementToMove> elementsToMove) {
        if (element.getContainingFile() instanceof RsCodeFragment) {
            LOG.error("Unexpected containingFile: " + element.getContainingFile());
        }
        for (ElementToMove e : elementsToMove) {
            if (e instanceof ItemToMove) {
                if (PsiTreeUtil.isAncestor(((ItemToMove) e).getItem(), element, false)) return true;
            } else if (e instanceof ModToMove) {
                RsElement rsElement = element instanceof RsElement ? (RsElement) element : null;
                if (rsElement != null && RsElementUtil.getContainingModOrSelf(rsElement).getSuperMods().contains(((ModToMove) e).getMod())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Converts an absolute path to a relative path if possible.
     * If the path starts with the mod's crate-relative path, strip that prefix.
     */
    @Nullable
    public static String convertPathToRelativeIfPossible(@NotNull RsMod mod, @NotNull String path) {
        String crateRelative = mod.getCrateRelativePath();
        if (crateRelative != null) {
            String prefix = "crate" + crateRelative + "::";
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
        }
        return path;
    }

    /**
     * Adds a use import statement to the file of the given context element.
     */
    public static void addImport(
        @NotNull RsPsiFactory psiFactory,
        @NotNull PsiElement context,
        @NotNull String usePath,
        @Nullable String alias
    ) {
        RsElement rsContext = context instanceof RsElement ? (RsElement) context : null;
        if (rsContext == null) return;
        RsMod containingMod = rsContext.getContainingMod();
        String useText = alias != null ? usePath + " as " + alias : usePath;
        RsUseItem useItem = psiFactory.createUseItem(useText);
        if (useItem != null) {
            addInner(containingMod, useItem);
        }
    }

    /**
     * Checks orphan rules for the given impl.
     * Returns true if orphan rules are satisfied according to the given predicate.
     */
    public static boolean checkOrphanRules(
        @NotNull RsImplItem impl,
        @NotNull java.util.function.Predicate<RsElement> isLocal
    ) {
        // Simplified orphan rule check: either the trait or the type must be local
        RsTraitRef traitRef = impl.getTraitRef();
        if (traitRef != null) {
            PsiElement traitResolved = traitRef.getPath().getReference() != null
                ? traitRef.getPath().getReference().resolve() : null;
            if (traitResolved instanceof RsElement && isLocal.test((RsElement) traitResolved)) {
                return true;
            }
        }
        RsTypeReference typeRef = impl.getTypeReference();
        if (typeRef != null) {
            // Check if any descendant path in the type reference resolves to a local element
            for (RsPath typePath : PsiTreeUtil.findChildrenOfType(typeRef, RsPath.class)) {
                PsiElement typeResolved = typePath.getReference() != null ? typePath.getReference().resolve() : null;
                if (typeResolved instanceof RsElement && isLocal.test((RsElement) typeResolved)) {
                    return true;
                }
            }
        }
        return false;
    }
}
