/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.RsInferenceResult;

import java.util.*;

/**
 * <p>
 * be called directly from Java. This class provides static method equivalents
 * that accept explicit {@code Class<T>} parameters.
 */
public final class RsPsiJavaUtil {

    private RsPsiJavaUtil() {
    }

    // --- Stub access ---

    /**
     * Safe accessor for getGreenStub() that works on PSI interfaces.
     * The generated PSI interfaces (like RsFunction) don't declare getGreenStub(),
     * only StubBasedPsiElementBase has it. This method does the cast safely.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <S extends StubElement<?>> S getGreenStub(@Nullable PsiElement element) {
        if (element instanceof StubBasedPsiElementBase<?>) {
            return (S) ((StubBasedPsiElementBase<?>) element).getGreenStub();
        }
        return null;
    }

    // --- PSI tree traversal methods ---

    @Nullable
    public static <T extends PsiElement> T parentOfType(PsiElement element, Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    public static <T extends PsiElement> T ancestorStrict(PsiElement element, Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    public static <T extends PsiElement> T ancestorStrict(PsiElement element, Class<T> clazz, Class<? extends PsiElement> stopAt) {
        return PsiTreeUtil.getParentOfType(element, clazz, true, stopAt);
    }

    public static <T extends PsiElement> T ancestorOrSelf(PsiElement element, Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, false);
    }

    public static <T extends PsiElement> T contextStrict(PsiElement element, Class<T> clazz) {
        return PsiTreeUtil.getContextOfType(element, clazz, true);
    }

    /**
     * Checks if {@code this} is an ancestor of the given element.
     */
    public static boolean isAncestorOf(@Nullable PsiElement ancestor, @Nullable PsiElement descendant) {
        if (ancestor == null || descendant == null) return false;
        return PsiTreeUtil.isAncestor(ancestor, descendant, false);
    }

    /**
     * Returns the first child element of the given type, or null.
     */
    @Nullable
    public static <T extends PsiElement> T childOfType(@Nullable PsiElement element, Class<T> clazz) {
        if (element == null) return null;
        return PsiTreeUtil.findChildOfType(element, clazz);
    }

    // --- Element type ---

    public static IElementType elementType(PsiElement element) {
        return PsiUtilCore.getElementType(element);
    }

    // --- Sibling navigation ---

    public static PsiElement getNextNonCommentSibling(PsiElement element) {
        return PsiTreeUtil.skipWhitespacesAndCommentsForward(element);
    }

    public static PsiElement getPrevNonWhitespaceSibling(PsiElement element) {
        return PsiTreeUtil.skipWhitespacesBackward(element);
    }

    public static PsiElement getNextNonWhitespaceSibling(PsiElement element) {
        return PsiTreeUtil.skipWhitespacesForward(element);
    }

    // --- Descendants and children ---

    public static <T extends PsiElement> List<T> descendantsOfType(PsiElement element, Class<T> clazz) {
        Collection<T> result = PsiTreeUtil.findChildrenOfType(element, clazz);
        return new ArrayList<>(result);
    }

    @Nullable
    public static <T extends PsiElement> T descendantOfTypeStrict(PsiElement element, Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz, true);
    }

    public static <T extends PsiElement> List<T> childrenOfType(PsiElement element, Class<T> clazz) {
        return PsiTreeUtil.getChildrenOfTypeAsList(element, clazz);
    }

    /**
     * Returns all descendants of the given type, including those inside macro expansions.
     * Simplified version that delegates to standard PsiTreeUtil.
     */
    public static <T extends PsiElement> List<T> descendantsWithMacrosOfType(@Nullable PsiElement element, Class<T> clazz) {
        if (element == null) return Collections.emptyList();
        Collection<T> result = PsiTreeUtil.findChildrenOfType(element, clazz);
        return new ArrayList<>(result);
    }

    // --- RsElement-specific methods ---

    public static RsMod getContainingMod(PsiElement element) {
        if (element instanceof RsElement) {
            return ((RsElement) element).getContainingMod();
        }
        RsMod mod = PsiTreeUtil.getContextOfType(element, RsMod.class, true);
        if (mod != null) {
            return mod;
        }
        throw new IllegalStateException("Element outside of module: " + element.getText());
    }

    public static PsiElement firstKeyword(RsStructOrEnumItemElement item) {
        if (item instanceof RsStructItem) {
            RsStructItem structItem = (RsStructItem) item;
            PsiElement vis = structItem.getVis();
            return vis != null ? vis : structItem.getStruct();
        }
        if (item instanceof RsEnumItem) {
            RsEnumItem enumItem = (RsEnumItem) item;
            PsiElement vis = enumItem.getVis();
            return vis != null ? vis : enumItem.getEnum();
        }
        return null;
    }

    public static RsOuterAttr findOuterAttr(RsOuterAttributeOwner item, String name) {
        for (RsOuterAttr attr : item.getOuterAttrList()) {
            RsMetaItem metaItem = attr.getMetaItem();
            if (metaItem != null && name.equals(RsMetaItemUtil.getName(metaItem))) {
                return attr;
            }
        }
        return null;
    }

    public static List<RsMatchArm> getArms(RsMatchExpr matchExpr) {
        RsMatchBody matchBody = matchExpr.getMatchBody();
        if (matchBody == null) {
            return Collections.emptyList();
        }
        return matchBody.getMatchArmList();
    }

    public static Set<String> getAllVisibleBindings(PsiElement element) {
        if (element instanceof RsElement) {
            return RsElementUtil.getAllVisibleBindings((RsElement) element);
        }
        return Collections.emptySet();
    }

    public static PsiFile getOrCreateModuleFile(RsModDeclItem decl) {
        return RsModDeclItemUtil.getOrCreateModuleFile(decl);
    }

    public static List<MethodResolveVariant> getResolvedMethod(RsMethodCall methodCall) {
        RsInferenceResult inference = ExtensionsUtil.getInference(methodCall);
        if (inference == null) {
            return Collections.emptyList();
        }
        return inference.getResolvedMethod(methodCall);
    }

    public static RsExpr unwrapReference(RsExpr expr) {
        if (expr instanceof RsUnaryExpr) {
            RsUnaryExpr unaryExpr = (RsUnaryExpr) expr;
            UnaryOperator op = RsExprUtil.getOperatorType(unaryExpr);
            if (op == UnaryOperator.REF || op == UnaryOperator.REF_MUT) {
                RsExpr inner = unaryExpr.getExpr();
                if (inner != null) {
                    return inner;
                }
            }
        }
        return expr;
    }

    /**
     * Checks if the given element is constant-like: a real constant, static variable,
     * or enum variant without fields.
     * <p>
     */
    public static boolean isConstantLike(PsiElement element) {
        if (element instanceof RsConstant) return true;
        if (element instanceof RsFieldsOwner) {
            RsFieldsOwner fieldsOwner = (RsFieldsOwner) element;
            return fieldsOwner.getBlockFields() == null && fieldsOwner.getTupleFields() == null;
        }
        return false;
    }

    public static RsPat skipUnnecessaryTupDown(RsPat pat) {
        RsPat current = pat;
        while (current instanceof RsPatTup) {
            List<RsPat> patList = ((RsPatTup) current).getPatList();
            if (patList.size() != 1) {
                return current;
            }
            current = patList.get(0);
        }
        return current;
    }
}
