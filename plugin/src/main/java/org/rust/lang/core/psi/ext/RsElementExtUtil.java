/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.stubs.RsFunctionStub;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.psi.PsiUtilCore;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiNameIdentifierOwner;
import org.rust.cargo.project.workspace.PackageOrigin;

/**
 * Delegates to {@link PsiElementKt}, {@link RsPsiJavaUtil}, {@link CfgUtils}, etc.
 */
public final class RsElementExtUtil {

    public static final StubbedAttributeProperty<RsFunction, RsFunctionStub> IS_PROC_MACRO_DEF_PROP = RsFunctionUtil.IS_PROC_MACRO_DEF_PROP;

    private RsElementExtUtil() {
    }

    // --- PSI tree traversal ---

    @Nullable
    public static <T extends PsiElement> T ancestorStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiElementUtil.ancestorStrict(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorOrSelf(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiElementUtil.ancestorOrSelf(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T parentOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz);
    }

    @Nonnull
    public static <T extends PsiElement> List<T> childrenOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiElementUtil.childrenOfType(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T childOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getChildOfType(element, clazz);
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
        result.addAll(PsiTreeUtil.findChildrenOfType(element, clazz));
        return result;
    }

    @Nullable
    public static <T extends PsiElement> T descendantsOfTypeFirst(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T descendantOfTypeStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return RsPsiJavaUtil.descendantOfTypeStrict(element, clazz);
    }

    @Nonnull
    public static <T extends PsiElement> List<T> descendantsWithMacrosOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        // Simplified: just return descendantsOfType without macro expansion
        return PsiElementUtil.descendantsOfType(element, clazz);
    }

    @Nullable
    public static <T extends PsiElement> T stubChildOfElementType(@Nonnull PsiElement element, @Nonnull IElementType type, @Nonnull Class<T> clazz) {
        return (T) PsiElementUtil.stubChildOfElementType(element, consulo.language.ast.TokenSet.create(type), clazz);
    }

    // --- Element type ---

    @Nonnull
    public static IElementType getElementType(@Nonnull PsiElement element) {
        return PsiUtilCore.getElementType(element);
    }

    @Nullable
    public static IElementType getElementTypeOrNull(@Nullable PsiElement element) {
        if (element == null) return null;
        return PsiUtilCore.getElementType(element);
    }

    // --- Sibling navigation ---

    @Nullable
    public static PsiElement getPrevNonCommentSibling(@Nonnull PsiElement element) {
        return PsiElementUtil.getPrevNonCommentSibling(element);
    }

    @Nullable
    public static PsiElement getPrevNonWhitespaceSibling(@Nonnull PsiElement element) {
        return PsiTreeUtil.skipWhitespacesBackward(element);
    }

    // --- Iteration ---

    @Nonnull
    public static Iterable<PsiElement> getAncestors(@Nonnull PsiElement element) {
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

    @Nonnull
    public static Iterable<PsiElement> getContexts(@Nonnull PsiElement element) {
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

    @Nonnull
    public static Iterable<PsiElement> getChildrenWithLeaves(@Nonnull PsiElement element) {
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

    public static boolean getExistsAfterExpansion(@Nonnull PsiElement element) {
        return CfgUtils.existsAfterExpansion(element);
    }

    public static boolean existsAfterExpansion(@Nonnull PsiElement element, @Nullable Crate crate) {
        return CfgUtils.existsAfterExpansion(element, crate);
    }

    public static boolean isEnabledByCfg(@Nonnull PsiElement element) {
        return CfgUtils.isEnabledByCfg(element);
    }

    public static boolean isCfgUnknown(@Nonnull PsiElement element) {
        return CfgUtils.isCfgUnknown(element);
    }

    @Nonnull
    public static RsCodeStatus getCodeStatus(@Nonnull PsiElement element) {
        return CfgUtils.getCodeStatus(element, null);
    }

    // --- Edition ---

    @Nullable
    public static CargoWorkspace.Edition getEdition(@Nonnull PsiElement element) {
        if (element instanceof RsElement) {
            Crate crate = ((RsElement) element).getContainingCrate();
            if (crate != null) {
                return crate.getEdition();
            }
        }
        return null;
    }

    @Nullable
    public static CargoWorkspace.Edition getContainingCrateEdition(@Nonnull PsiElement element) {
        return getEdition(element);
    }

    public static boolean isAtLeastEdition2018(@Nonnull PsiElement element) {
        CargoWorkspace.Edition edition = getEdition(element);
        return edition != null && edition.compareTo(CargoWorkspace.Edition.EDITION_2018) >= 0;
    }

    // --- Crate/Cargo ---

    @Nullable
    public static Crate getContainingCrate(@Nonnull PsiElement element) {
        if (element instanceof RsElement) {
            return ((RsElement) element).getContainingCrate();
        }
        return null;
    }

    @Nullable
    public static Crate getCrate(@Nonnull PsiElement element) {
        return getContainingCrate(element);
    }

    @Nullable
    public static org.rust.cargo.project.workspace.PackageOrigin getContainingCrateAsPackageOrigin(@Nonnull PsiElement element) {
        Crate crate = getContainingCrate(element);
        if (crate != null) {
            return crate.getOrigin();
        }
        return null;
    }

    @Nullable
    public static CargoWorkspace.Package getContainingCargoPackage(@Nonnull PsiElement element) {
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
    public static CargoWorkspace.Target getContainingCargoTarget(@Nonnull PsiElement element) {
        if (element instanceof RsElement) {
            Crate crate = ((RsElement) element).getContainingCrate();
            if (crate != null) {
                return crate.getCargoTarget();
            }
        }
        return null;
    }

    @Nullable
    public static CargoProject findCargoProject(@Nonnull PsiElement element) {
        if (element instanceof RsElement) {
            Crate crate = ((RsElement) element).getContainingCrate();
            if (crate != null) {
                return crate.getCargoProject();
            }
        }
        return null;
    }

    @Nullable
    public static CargoWorkspace.Package findCargoPackage(@Nonnull PsiElement element) {
        return getContainingCargoPackage(element);
    }

    @Nullable
    public static CargoProject getCargoProject(@Nonnull PsiElement element) {
        return findCargoProject(element);
    }

    // --- Mod ---

    @Nullable
    public static RsMod getContainingMod(@Nonnull PsiElement element) {
        return RsPsiJavaUtil.getContainingMod(element);
    }

    // --- Offset ---

    public static int getStartOffset(@Nonnull PsiElement element) {
        return element.getTextRange().getStartOffset();
    }

    public static int getEndOffset(@Nonnull PsiElement element) {
        return element.getTextRange().getEndOffset();
    }

    // --- File ---

    @Nullable
    public static RsFile contextualFile(@Nonnull PsiElement element) {
        PsiFile file = element.getContainingFile();
        return file instanceof RsFile ? (RsFile) file : null;
    }

    @Nullable
    public static RsFile getContextualFile(@Nonnull PsiElement element) {
        return contextualFile(element);
    }

    // --- Misc ---

    @Nullable
    public static PsiElement getIdentifyingElement(@Nonnull PsiElement element) {
        if (element instanceof consulo.language.psi.PsiNameIdentifierOwner) {
            return ((consulo.language.psi.PsiNameIdentifierOwner) element).getNameIdentifier();
        }
        return null;
    }

    @Nullable
    public static PsiElement getParentDotExpr(@Nonnull PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof RsDotExpr) {
            return parent;
        }
        return null;
    }

    @Nonnull
    public static String getUnescapedText(@Nonnull PsiElement element) {
        return element.getText();
    }

    public static boolean isConstantLike(@Nonnull PsiElement element) {
        return RsPsiJavaUtil.isConstantLike(element);
    }

    @Nullable
    public static PsiElement findModificationTrackerOwner(@Nonnull PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, RsItemElement.class, RsFile.class);
    }

    public static void deleteWithSurroundingComma(@Nonnull PsiElement element) {
        PsiElement next = element.getNextSibling();
        if (next != null && next.getText().contains(",")) {
            next.delete();
        }
        element.delete();
    }

    public static void deleteWithSurroundingCommaAndWhitespace(@Nonnull PsiElement element) {
        deleteWithSurroundingComma(element);
    }
}
