/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RsMembersUtil {
    private RsMembersUtil() {
    }

    /**
     * Returns all members including those produced by macros.
     */
    @NotNull
    public static List<RsAbstractable> getExpandedMembers(@NotNull RsMembers members) {
        // Iterative DFS for macros
        List<RsAttrProcMacroOwner> stack = new ArrayList<>();
        List<RsAbstractable> result = new ArrayList<>();

        for (PsiElement member : reversedChildren(members)) {
            if (member instanceof RsAttrProcMacroOwner) {
                stack.add((RsAttrProcMacroOwner) member);
            }
        }

        while (!stack.isEmpty()) {
            RsAttrProcMacroOwner memberOrMacroCall = stack.remove(stack.size() - 1);
            ProcMacroAttribute<?> attr = ProcMacroAttribute.getProcMacroAttribute(memberOrMacroCall);
            RsPossibleMacroCall macroCall;
            if (attr instanceof ProcMacroAttribute.Attr) {
                macroCall = (RsPossibleMacroCall) ((ProcMacroAttribute.Attr<?>) attr).getAttr();
            } else if (attr instanceof ProcMacroAttribute.Derive) {
                macroCall = null;
            } else {
                // null attr
                if (memberOrMacroCall instanceof RsMacroCall) {
                    macroCall = (RsMacroCall) memberOrMacroCall;
                } else if (memberOrMacroCall instanceof RsAbstractable) {
                    result.add((RsAbstractable) memberOrMacroCall);
                    macroCall = null;
                } else {
                    macroCall = null;
                }
            }
            if (macroCall == null) continue;
            MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion(macroCall);
            if (expansion == null) continue;
            List<org.rust.lang.core.macros.RsExpandedElement> elements = expansion.getElements();
            for (int i = elements.size() - 1; i >= 0; i--) {
                org.rust.lang.core.macros.RsExpandedElement expandedElement = elements.get(i);
                if (expandedElement instanceof RsAttrProcMacroOwner) {
                    stack.add((RsAttrProcMacroOwner) expandedElement);
                }
            }
        }

        return result;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static List<PsiElement> reversedChildren(@NotNull RsMembers members) {
        if (members instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) members).getGreenStub();
            if (stub != null) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                List<StubElement<?>> childStubs = (List<StubElement<?>>) (List) stub.getChildrenStubs();
                List<PsiElement> result = new ArrayList<>(childStubs.size());
                for (int i = childStubs.size() - 1; i >= 0; i--) {
                    result.add(childStubs.get(i).getPsi());
                }
                return result;
            }
        }
        List<PsiElement> result = new ArrayList<>();
        PsiElement child = members.getLastChild();
        while (child != null) {
            result.add(child);
            child = child.getPrevSibling();
        }
        return result;
    }

    /**
     * Convenience for getting expanded members from a {@link RsTraitOrImpl}.
     * Equivalent to {@code traitOrImpl.getMembers().expandedMembers}.
     */
    @NotNull
    public static List<RsAbstractable> getExpandedMembers(@NotNull RsTraitOrImpl traitOrImpl) {
        RsMembers members = traitOrImpl.getMembers();
        return members != null ? getExpandedMembers(members) : Collections.emptyList();
    }

    /**
     * Explicit (non-macro-expanded) members of a trait or impl.
     */
    @NotNull
    public static List<RsAbstractable> getExplicitMembers(@NotNull RsTraitOrImpl traitOrImpl) {
        RsMembers members = traitOrImpl.getMembers();
        if (members == null) return Collections.emptyList();
        return PsiElementUtil.stubChildrenOfType(members, RsAbstractable.class);
    }

    @NotNull
    public static List<RsFunction> getFunctions(@NotNull List<RsAbstractable> members) {
        List<RsFunction> result = new ArrayList<>();
        for (RsAbstractable member : members) {
            if (member instanceof RsFunction) result.add((RsFunction) member);
        }
        return result;
    }

    @NotNull
    public static List<RsConstant> getConstants(@NotNull List<RsAbstractable> members) {
        List<RsConstant> result = new ArrayList<>();
        for (RsAbstractable member : members) {
            if (member instanceof RsConstant) result.add((RsConstant) member);
        }
        return result;
    }

    @NotNull
    public static List<RsTypeAlias> getTypes(@NotNull List<RsAbstractable> members) {
        List<RsTypeAlias> result = new ArrayList<>();
        for (RsAbstractable member : members) {
            if (member instanceof RsTypeAlias) result.add((RsTypeAlias) member);
        }
        return result;
    }
}
