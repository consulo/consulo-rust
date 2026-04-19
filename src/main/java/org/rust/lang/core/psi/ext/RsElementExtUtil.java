/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.stubs.RsFunctionStub;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Delegates to {@link PsiElementKt}, {@link RsPsiJavaUtil}, {@link CfgUtils}, etc.
 */
public final class RsElementExtUtil {

    public static final StubbedAttributeProperty<RsFunction, RsFunctionStub> IS_PROC_MACRO_DEF_PROP = RsFunctionUtil.IS_PROC_MACRO_DEF_PROP;

    private RsElementExtUtil() {
    }

    // --- PSI tree traversal ---

    @Nullable
    public static <T extends PsiElement> T ancestorStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiElementUtil.ancestorStrict(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorOrSelf(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiElementUtil.ancestorOrSelf(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T parentOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz);
    }

    @NotNull
    public static <T extends PsiElement> List<T> childrenOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiElementUtil.childrenOfType(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T childOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getChildOfType(element, clazz);
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
        result.addAll(PsiTreeUtil.findChildrenOfType(element, clazz));
        return result;
    }

    @Nullable
    public static <T extends PsiElement> T descendantsOfTypeFirst(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T descendantOfTypeStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return RsPsiJavaUtil.descendantOfTypeStrict(element, clazz);
    }

    @NotNull
    public static <T extends PsiElement> List<T> descendantsWithMacrosOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        // Simplified: just return descendantsOfType without macro expansion
        return PsiElementUtil.descendantsOfType(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T stubChildOfElementType(@NotNull PsiElement element, @NotNull IElementType type, @NotNull Class<T> clazz) {
        return (T) PsiElementUtil.stubChildOfElementType(element, com.intellij.psi.tree.TokenSet.create(type), clazz);
    }

    // --- Element type ---

    @NotNull
    public static IElementType getElementType(@NotNull PsiElement element) {
        return PsiUtilCore.getElementType(element);
    }

    @Nullable
    public static IElementType getElementTypeOrNull(@Nullable PsiElement element) {
        if (element == null) return null;
        return PsiUtilCore.getElementType(element);
    }

    // --- Sibling navigation ---

    @Nullable
    public static PsiElement getPrevNonCommentSibling(@NotNull PsiElement element) {
        return PsiElementUtil.getPrevNonCommentSibling(element);
    }

    @Nullable
    public static PsiElement getPrevNonWhitespaceSibling(@NotNull PsiElement element) {
        return PsiTreeUtil.skipWhitespacesBackward(element);
    }

    // --- Iteration ---

    @NotNull
    public static Iterable<PsiElement> getAncestors(@NotNull PsiElement element) {
        return () -> new Iterator<PsiElement>() {
            PsiElement current = element.getParent();

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public PsiElement next() {
                PsiElement result = current;
                current = current.getParent();
                return result;
            }
        };
    }

    @NotNull
    public static Iterable<PsiElement> getContexts(@NotNull PsiElement element) {
        return () -> new Iterator<PsiElement>() {
            PsiElement current = element.getContext();

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public PsiElement next() {
                PsiElement result = current;
                current = current.getContext();
                return result;
            }
        };
    }

    @NotNull
    public static Iterable<PsiElement> getChildrenWithLeaves(@NotNull PsiElement element) {
        return () -> new Iterator<PsiElement>() {
            PsiElement current = element.getFirstChild();

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public PsiElement next() {
                PsiElement result = current;
                current = current.getNextSibling();
                return result;
            }
        };
    }

    // --- Cfg/expansion ---

    public static boolean getExistsAfterExpansion(@NotNull PsiElement element) {
        return CfgUtils.existsAfterExpansion(element);
    }

    public static boolean existsAfterExpansion(@NotNull PsiElement element, @Nullable Crate crate) {
        return CfgUtils.existsAfterExpansion(element, crate);
    }

    public static boolean isEnabledByCfg(@NotNull PsiElement element) {
        return CfgUtils.isEnabledByCfg(element);
    }

    public static boolean isCfgUnknown(@NotNull PsiElement element) {
        return CfgUtils.isCfgUnknown(element);
    }

    @NotNull
    public static RsCodeStatus getCodeStatus(@NotNull PsiElement element) {
        return CfgUtils.getCodeStatus(element, null);
    }

    // --- Edition ---

    @Nullable
    public static CargoWorkspace.Edition getEdition(@NotNull PsiElement element) {
        if (element instanceof RsElement) {
            Crate crate = ((RsElement) element).getContainingCrate();
            if (crate != null) {
                return crate.getEdition();
            }
        }
        return null;
    }

    @Nullable
    public static CargoWorkspace.Edition getContainingCrateEdition(@NotNull PsiElement element) {
        return getEdition(element);
    }

    public static boolean isAtLeastEdition2018(@NotNull PsiElement element) {
        CargoWorkspace.Edition edition = getEdition(element);
        return edition != null && edition.compareTo(CargoWorkspace.Edition.EDITION_2018) >= 0;
    }

    // --- Crate/Cargo ---

    @Nullable
    public static Crate getContainingCrate(@NotNull PsiElement element) {
        if (element instanceof RsElement) {
            return ((RsElement) element).getContainingCrate();
        }
        return null;
    }

    @Nullable
    public static Crate getCrate(@NotNull PsiElement element) {
        return getContainingCrate(element);
    }

    @Nullable
    public static org.rust.cargo.project.workspace.PackageOrigin getContainingCrateAsPackageOrigin(@NotNull PsiElement element) {
        Crate crate = getContainingCrate(element);
        if (crate != null) {
            return crate.getOrigin();
        }
        return null;
    }

    @Nullable
    public static CargoWorkspace.Package getContainingCargoPackage(@NotNull PsiElement element) {
        if (element instanceof RsElement) {
            Crate crate = ((RsElement) element).getContainingCrate();
            if (crate != null) {
                CargoWorkspace.Target target = crate.getCargoTarget();
                return target != null ? target.getPkg() : null;
            }
        }
        return null;
    }

    @Nullable
    public static CargoWorkspace.Target getContainingCargoTarget(@NotNull PsiElement element) {
        if (element instanceof RsElement) {
            Crate crate = ((RsElement) element).getContainingCrate();
            if (crate != null) {
                return crate.getCargoTarget();
            }
        }
        return null;
    }

    @Nullable
    public static CargoProject findCargoProject(@NotNull PsiElement element) {
        if (element instanceof RsElement) {
            Crate crate = ((RsElement) element).getContainingCrate();
            if (crate != null) {
                return crate.getCargoProject();
            }
        }
        return null;
    }

    @Nullable
    public static CargoWorkspace.Package findCargoPackage(@NotNull PsiElement element) {
        return getContainingCargoPackage(element);
    }

    @Nullable
    public static CargoProject getCargoProject(@NotNull PsiElement element) {
        return findCargoProject(element);
    }

    // --- Mod ---

    @Nullable
    public static RsMod getContainingMod(@NotNull PsiElement element) {
        return RsPsiJavaUtil.getContainingMod(element);
    }

    // --- Offset ---

    public static int getStartOffset(@NotNull PsiElement element) {
        return element.getTextRange().getStartOffset();
    }

    public static int getEndOffset(@NotNull PsiElement element) {
        return element.getTextRange().getEndOffset();
    }

    // --- File ---

    @Nullable
    public static RsFile contextualFile(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        return file instanceof RsFile ? (RsFile) file : null;
    }

    @Nullable
    public static RsFile getContextualFile(@NotNull PsiElement element) {
        return contextualFile(element);
    }

    // --- Misc ---

    @Nullable
    public static PsiElement getIdentifyingElement(@NotNull PsiElement element) {
        if (element instanceof com.intellij.psi.PsiNameIdentifierOwner) {
            return ((com.intellij.psi.PsiNameIdentifierOwner) element).getNameIdentifier();
        }
        return null;
    }

    @Nullable
    public static PsiElement getParentDotExpr(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof RsDotExpr) {
            return parent;
        }
        return null;
    }

    @NotNull
    public static String getUnescapedText(@NotNull PsiElement element) {
        return element.getText();
    }

    public static boolean isConstantLike(@NotNull PsiElement element) {
        return RsPsiJavaUtil.isConstantLike(element);
    }

    @Nullable
    public static PsiElement findModificationTrackerOwner(@NotNull PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, RsItemElement.class, RsFile.class);
    }

    public static void deleteWithSurroundingComma(@NotNull PsiElement element) {
        PsiElement next = element.getNextSibling();
        if (next != null && next.getText().contains(",")) {
            next.delete();
        }
        element.delete();
    }

    public static void deleteWithSurroundingCommaAndWhitespace(@NotNull PsiElement element) {
        deleteWithSurroundingComma(element);
    }
}
