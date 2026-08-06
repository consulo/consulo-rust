/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.ast.TokenSet;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.macros.MacroExpansionContextUtil;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.stubs.RsMacroCallStub;
import org.rust.stdext.HashCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RsMacroCallUtil {
    private RsMacroCallUtil() {
    }

    private static final TokenSet MACRO_ARGUMENT_TYPES = RsTokenType.tokenSetOf(
        RsElementTypes.MACRO_ARGUMENT, RsElementTypes.FORMAT_MACRO_ARGUMENT,
        RsElementTypes.ASSERT_MACRO_ARGUMENT, RsElementTypes.EXPR_MACRO_ARGUMENT,
        RsElementTypes.VEC_MACRO_ARGUMENT, RsElementTypes.CONCAT_MACRO_ARGUMENT,
        RsElementTypes.ENV_MACRO_ARGUMENT, RsElementTypes.ASM_MACRO_ARGUMENT
    );

    @Nonnull
    public static String getMacroName(@Nonnull RsMacroCall macroCall) {
        String name = macroCall.getPath().getReferenceName();
        return name != null ? name : "";
    }

    public static boolean isTopLevelExpansion(@Nonnull RsMacroCall macroCall) {
        return macroCall.getParent() instanceof RsMod;
    }

    @Nullable
    public static MacroBraces getBracesKind(@Nonnull RsMacroCall macroCall) {
        RsElement argElement = getMacroArgumentElement(macroCall);
        if (argElement == null) return null;
        PsiElement first = argElement.getFirstChild();
        if (first == null) return null;
        return MacroBraces.fromToken(first.getNode().getElementType());
    }

    @Nullable
    public static String getMacroBody(@Nonnull RsMacroCall macroCall) {
        RsMacroCallStub stub = RsPsiJavaUtil.getGreenStub(macroCall) instanceof RsMacroCallStub ? (RsMacroCallStub) RsPsiJavaUtil.getGreenStub(macroCall) : null;
        if (stub != null) return stub.getMacroBody();
        RsElement argElement = getMacroArgumentElement(macroCall);
        if (argElement == null) return null;
        CharSequence chars = argElement.getNode().getChars();
        int len = chars.length();
        return chars.subSequence(1, len - (len == 1 ? 0 : 1)).toString();
    }

    @Nullable
    public static TextRange getBodyTextRange(@Nonnull RsMacroCall macroCall) {
        Object greenStub = RsPsiJavaUtil.getGreenStub(macroCall);
        if (greenStub instanceof RsMacroCallStub) {
            return getBodyTextRange((RsMacroCallStub) greenStub);
        } else {
            RsElement argElement = getMacroArgumentElement(macroCall);
            if (argElement == null) return null;
            TextRange range = argElement.getTextRange();
            return new TextRange(range.getStartOffset() + 1, range.getEndOffset() - (range.getLength() == 1 ? 0 : 1));
        }
    }

    @Nullable
    public static TextRange getBodyTextRange(@Nonnull RsMacroCallStub stub) {
        int bodyStartOffset = stub.getBodyStartOffset();
        String macroBody = stub.getMacroBody();
        if (bodyStartOffset != -1 && macroBody != null) {
            return new TextRange(bodyStartOffset, bodyStartOffset + macroBody.length());
        }
        return null;
    }

    @Nullable
    public static RsElement getMacroArgumentElement(@Nonnull RsMacroCall macroCall) {
        consulo.language.ast.ASTNode child = macroCall.getNode().findChildByType(MACRO_ARGUMENT_TYPES);
        if (child == null) return null;
        PsiElement psi = child.getPsi();
        return psi instanceof RsElement ? (RsElement) psi : null;
    }

    @Nullable
    public static HashCode getBodyHash(@Nonnull RsMacroCall macroCall) {
        Object greenStub = RsPsiJavaUtil.getGreenStub(macroCall);
        if (greenStub instanceof RsMacroCallStub) {
            return ((RsMacroCallStub) greenStub).getBodyHash();
        }
        return CachedValuesManager.getManager(macroCall.getProject()).getCachedValue(macroCall, () -> {
            String body = getMacroBody(macroCall);
            HashCode hash = body != null ? HashCode.compute(body) : null;
            return CachedValueProvider.Result.create(hash, macroCall.getModificationTracker());
        });
    }

    @Nullable
    public static RsMacroDefinitionBase resolveToMacro(@Nonnull RsMacroCall macroCall) {
        PsiElement resolved = macroCall.getPath().getReference() != null
            ? macroCall.getPath().getReference().resolve()
            : null;
        return resolved instanceof RsMacroDefinitionBase ? (RsMacroDefinitionBase) resolved : null;
    }

    @Nonnull
    public static List<RsExpandedElement> getExpansionFlatten(@Nonnull RsMacroCall macroCall) {
        List<RsExpandedElement> list = new ArrayList<>();
        processExpansionRecursively(macroCall, element -> {
            list.add(element);
            return false;
        });
        return list;
    }

    public static boolean processExpansionRecursively(@Nonnull RsMacroCall macroCall,
                                                       @Nonnull java.util.function.Function<RsExpandedElement, Boolean> processor) {
        MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion(macroCall);
        if (expansion == null) return false;
        for (RsExpandedElement element : expansion.getElements()) {
            if (processRecursively(element, processor)) return true;
        }
        return false;
    }

    private static boolean processRecursively(@Nonnull RsExpandedElement element,
                                               @Nonnull java.util.function.Function<RsExpandedElement, Boolean> processor) {
        if (element instanceof RsMacroCall) {
            RsMacroCall call = (RsMacroCall) element;
            return RsPossibleMacroCallUtil.getExistsAfterExpansion(call) && processExpansionRecursively(call, processor);
        } else {
            return processor.apply(element);
        }
    }

    @Nonnull
    public static RsElement replaceWithExpr(@Nonnull RsMacroCall macroCall, @Nonnull RsExpr expr) {
        org.rust.lang.core.macros.MacroExpansionContext context = MacroExpansionContextUtil.getExpansionContext(macroCall);
        PsiElement result;
        if (context == org.rust.lang.core.macros.MacroExpansionContext.EXPR) {
            result = macroCall.getParent().replace(expr);
        } else if (context == org.rust.lang.core.macros.MacroExpansionContext.STMT) {
            RsExprStmt exprStmt = (RsExprStmt) new RsPsiFactory(macroCall.getProject()).createStatement("();");
            exprStmt.getExpr().replace(expr);
            result = macroCall.replace(exprStmt);
        } else {
            throw new IllegalStateException("`replaceWithExpr` can only be used for expr or stmt context macros; got " + context + " context");
        }
        return (RsElement) result;
    }

    public static boolean isStdTryMacro(@Nonnull RsMacroCall macroCall) {
        if (!"try".equals(getMacroName(macroCall))) return false;
        RsMacroDefinitionBase macro = resolveToMacro(macroCall);
        if (macro == null) return false;
        org.rust.cargo.project.workspace.CargoWorkspace.Package pkg = RsElementUtil.getContainingCargoPackage(macro);
        return pkg != null && pkg.getOrigin() == PackageOrigin.STDLIB;
    }

    @Nullable
    public static MacroExpansion getExpansion(@Nonnull RsMacroCall macroCall) {
        return RsPossibleMacroCallUtil.getExpansion(macroCall);
    }
}
