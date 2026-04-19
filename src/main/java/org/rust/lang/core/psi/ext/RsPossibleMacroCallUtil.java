/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.*;
import org.rust.lang.core.macros.errors.GetMacroExpansionError;
import org.rust.lang.core.psi.*;
import org.rust.stdext.HashCode;
import org.rust.stdext.RsResult;

import java.util.function.Function;

public final class RsPossibleMacroCallUtil {

    @NotNull
    public static org.rust.lang.core.macros.MacroExpansionContext getExpansionContext(@NotNull RsPossibleMacroCall call) {
        return org.rust.lang.core.macros.RsExpandedElementUtil.getExpansionContext(call);
    }

    @NotNull
    public static String expandMacrosRecursively(
        @NotNull RsPossibleMacroCall call,
        int depthLimit,
        boolean replaceDollarCrate
    ) {
        return expandMacrosRecursively(call, depthLimit, replaceDollarCrate,
            c -> getExpansion(c));
    }
    private RsPossibleMacroCallUtil() {
    }

    private static final Key<CachedValue<RsResult<MacroExpansion, GetMacroExpansionError>>> RS_MACRO_CALL_EXPANSION_RESULT =
        Key.create("org.rust.lang.core.psi.ext.RS_MACRO_CALL_EXPANSION_RESULT");

    @NotNull
    public static RsPossibleMacroCallKind getKind(@NotNull RsPossibleMacroCall call) {
        if (call instanceof RsMacroCall) {
            return new RsPossibleMacroCallKind.MacroCall((RsMacroCall) call);
        }
        if (call instanceof RsMetaItem) {
            return new RsPossibleMacroCallKind.MetaItem((RsMetaItem) call);
        }
        throw new IllegalStateException("unreachable");
    }

    @Nullable
    public static RsPossibleMacroCall getContextMacroCall(@NotNull PsiElement element) {
        for (PsiElement ctx : RsElementUtil.getContexts(element)) {
            if (ctx instanceof RsPossibleMacroCall && isMacroCall((RsPossibleMacroCall) ctx)) {
                return (RsPossibleMacroCall) ctx;
            }
        }
        return null;
    }

    public static boolean isMacroCall(@NotNull RsPossibleMacroCall call) {
        RsPossibleMacroCallKind kind = getKind(call);
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) return true;
        if (kind instanceof RsPossibleMacroCallKind.MetaItem) {
            RsMetaItem meta = ((RsPossibleMacroCallKind.MetaItem) kind).meta;
            RsDocAndAttributeOwner owner = RsMetaItemUtil.getOwner(meta);
            if (!(owner instanceof RsAttrProcMacroOwner)) return false;
            RsAttrProcMacroOwner procMacroOwner = (RsAttrProcMacroOwner) owner;
            ProcMacroAttribute<?> attr = ProcMacroAttribute.getProcMacroAttribute(procMacroOwner, RsDocAndAttributeOwnerUtil.getAttributeStub(procMacroOwner), null, true, false);
            if (attr instanceof ProcMacroAttribute.Attr) {
                return ((ProcMacroAttribute.Attr<?>) attr).getAttr() == call;
            }
            if (attr instanceof ProcMacroAttribute.Derive) {
                return RsProcMacroPsiUtil.canBeCustomDerive(meta);
            }
            // null attr - check without resolve
            for (ProcMacroAttribute<?> attr1 : ProcMacroAttribute.getProcMacroAttributeWithoutResolve(procMacroOwner, RsDocAndAttributeOwnerUtil.getAttributeStub(procMacroOwner), null, true, null, false)) {
                if (attr1 instanceof ProcMacroAttribute.Attr) {
                    if (((ProcMacroAttribute.Attr<?>) attr1).getAttr() == call) {
                        return true;
                    }
                }
                if (attr1 instanceof ProcMacroAttribute.Derive) {
                    return RsProcMacroPsiUtil.canBeCustomDerive(meta);
                }
            }
            return false;
        }
        return false;
    }

    public static boolean getCanBeMacroCall(@NotNull RsPossibleMacroCall call) {
        RsPossibleMacroCallKind kind = getKind(call);
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) return true;
        if (kind instanceof RsPossibleMacroCallKind.MetaItem) {
            return RsProcMacroPsiUtil.canBeProcMacroCall(((RsPossibleMacroCallKind.MetaItem) kind).meta);
        }
        return false;
    }

    public static boolean isTopLevelExpansion(@NotNull RsPossibleMacroCall call) {
        RsPossibleMacroCallKind kind = getKind(call);
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) {
            return RsMacroCallUtil.isTopLevelExpansion(((RsPossibleMacroCallKind.MacroCall) kind).call);
        }
        if (kind instanceof RsPossibleMacroCallKind.MetaItem) {
            RsMetaItem meta = ((RsPossibleMacroCallKind.MetaItem) kind).meta;
            if (!getCanBeMacroCall(meta)) return false;
            RsDocAndAttributeOwner owner = RsMetaItemUtil.getOwner(meta);
            return owner != null && owner.getParent() instanceof RsMod;
        }
        return false;
    }

    @Nullable
    public static MacroCallBody getMacroBody(@NotNull RsPossibleMacroCall call) {
        RsPossibleMacroCallKind kind = getKind(call);
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) {
            String body = RsMacroCallUtil.getMacroBody(((RsPossibleMacroCallKind.MacroCall) kind).call);
            return body != null ? new MacroCallBody.FunctionLike(body) : null;
        }
        return null;
    }

    @Nullable
    public static TextRange getBodyTextRange(@NotNull RsPossibleMacroCall call) {
        if (call instanceof RsMacroCall) {
            return RsMacroCallUtil.getBodyTextRange((RsMacroCall) call);
        }
        if (call instanceof RsMetaItem) {
            RsDocAndAttributeOwner owner = RsMetaItemUtil.getOwner((RsMetaItem) call);
            if (owner == null) return null;
            return getOwnerBodyTextRange(owner);
        }
        return null;
    }

    @Nullable
    private static TextRange getOwnerBodyTextRange(@NotNull RsDocAndAttributeOwner owner) {
        if (owner instanceof com.intellij.extapi.psi.StubBasedPsiElementBase) {
            Object stub = ((com.intellij.extapi.psi.StubBasedPsiElementBase<?>) owner).getGreenStub();
            if (stub instanceof org.rust.lang.core.stubs.RsAttrProcMacroOwnerStub) {
                org.rust.lang.core.stubs.RsAttrProcMacroOwnerStub procStub = (org.rust.lang.core.stubs.RsAttrProcMacroOwnerStub) stub;
                int startOffset = procStub.getStartOffset();
                String text = procStub.getStubbedText();
                if (startOffset != -1 && text != null) {
                    return new TextRange(startOffset, startOffset + text.length());
                }
                return null;
            }
        }
        return owner.getTextRange();
    }

    @Nullable
    public static HashCode getBodyHash(@NotNull RsPossibleMacroCall call) {
        RsPossibleMacroCallKind kind = getKind(call);
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) {
            return RsMacroCallUtil.getBodyHash(((RsPossibleMacroCallKind.MacroCall) kind).call);
        }
        // MetaItem case
        return null;
    }

    @Nullable
    public static MacroExpansion getExpansion(@NotNull RsPossibleMacroCall call) {
        RsResult<MacroExpansion, GetMacroExpansionError> result = getExpansionResult(call);
        return result instanceof RsResult.Ok ? ((RsResult.Ok<MacroExpansion, GetMacroExpansionError>) result).getOk() : null;
    }

    @NotNull
    public static RsResult<MacroExpansion, GetMacroExpansionError> getExpansionResult(@NotNull RsPossibleMacroCall call) {
        return CachedValuesManager.getCachedValue(call, RS_MACRO_CALL_EXPANSION_RESULT, () -> {
            RsPossibleMacroCall originalOrSelf = CompletionUtil.getOriginalElement(call) instanceof RsPossibleMacroCall
                ? (RsPossibleMacroCall) CompletionUtil.getOriginalElement(call)
                : null;
            if (originalOrSelf != null) {
                MacroCallBody origBody = getMacroBody(originalOrSelf);
                MacroCallBody thisBody = getMacroBody(call);
                boolean bodiesEqual = (origBody == null && thisBody == null) ||
                    (origBody != null && thisBody != null && origBody.equals(thisBody));
                if (!bodiesEqual) originalOrSelf = null;
            }
            if (originalOrSelf == null) originalOrSelf = call;
            return call.getProject().getService(MacroExpansionManager.class).getExpansionFor(originalOrSelf);
        });
    }

    @Nullable
    public static PsiElement getContextToSetForExpansion(@NotNull RsPossibleMacroCall call) {
        RsPossibleMacroCallKind kind = getKind(call);
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) {
            return ((RsPossibleMacroCallKind.MacroCall) kind).call.getContext();
        }
        if (kind instanceof RsPossibleMacroCallKind.MetaItem) {
            RsDocAndAttributeOwner owner = RsMetaItemUtil.getOwner(((RsPossibleMacroCallKind.MetaItem) kind).meta);
            return owner != null ? owner.getContext() : null;
        }
        return null;
    }

    @NotNull
    public static String expandMacrosRecursively(
        @NotNull RsPossibleMacroCall call,
        int depthLimit,
        boolean replaceDollarCrate,
        @NotNull Function<RsPossibleMacroCall, MacroExpansion> expander
    ) {
        if (depthLimit == 0) return textIfNotExpanded(call);

        MacroExpansion expansionElements = expander.apply(call);
        if (expansionElements != null) {
            StringBuilder sb = new StringBuilder();
            for (RsExpandedElement element : expansionElements.getElements()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(toExpandedText(element, depthLimit, replaceDollarCrate, expander));
            }
            return sb.toString();
        }
        return textIfNotExpanded(call);
    }

    private static String toExpandedText(PsiElement element, int depthLimit, boolean replaceDollarCrate,
                                          Function<RsPossibleMacroCall, MacroExpansion> expander) {
        if (element instanceof RsMacroCall) {
            return expandMacrosRecursively((RsMacroCall) element, depthLimit - 1, replaceDollarCrate, expander);
        }
        if (element instanceof RsElement) {
            if (replaceDollarCrate && element instanceof RsPath) {
                RsPath path = (RsPath) element;
                String refName = path.getReferenceName();
                if (org.rust.lang.core.macros.decl.DeclMacroExpander.MACRO_DOLLAR_CRATE_IDENTIFIER.equals(refName)
                    && RsPathUtil.getQualifier(path) == null
                    && path.getTypeQual() == null
                    && !path.getHasColonColon()) {
                    return "::" + (refName != null ? refName : "");
                }
            }
            StringBuilder sb = new StringBuilder();
            for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(toExpandedText(child, depthLimit, replaceDollarCrate, expander));
            }
            return sb.toString();
        }
        return element.getText();
    }

    private static String textIfNotExpanded(@NotNull RsPossibleMacroCall call) {
        if (call instanceof RsMacroCall) return call.getText();
        if (call instanceof RsMetaItem) {
            RsDocAndAttributeOwner owner = RsMetaItemUtil.getOwner((RsMetaItem) call);
            return owner != null ? owner.getText() : "";
        }
        return "";
    }

    public static boolean getExistsAfterExpansion(@NotNull RsPossibleMacroCall call) {
        return RsElementUtil.existsAfterExpansion(call);
    }

    /**
     * Convenience to get the path from a possible macro call.
     */
    @Nullable
    public static RsPath getPath(@NotNull RsPossibleMacroCall call) {
        return call.getPath();
    }
}
