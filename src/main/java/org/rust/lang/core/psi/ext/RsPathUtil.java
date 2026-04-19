/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.stubs.common.RsPathPsiOrStub;
import org.rust.lang.core.types.ty.TyPrimitive;
import org.rust.lang.doc.psi.RsDocPathLinkParent;

import java.util.List;
import java.util.Set;

public final class RsPathUtil {
    private RsPathUtil() {
    }

    public static boolean getHasCself(@NotNull RsPath path) {
        return path.getKind() == PathKind.CSELF;
    }

    /**
     * For {@code Foo::bar::baz::quux} path returns {@code Foo}.
     */
    @NotNull
    public static <T extends RsPathPsiOrStub> T basePath(@NotNull T path) {
        @SuppressWarnings("unchecked")
        T qualifier = (T) path.getPath();
        while (qualifier != null) {
            path = qualifier;
            //noinspection unchecked
            qualifier = (T) path.getPath();
        }
        return path;
    }

    /**
     * For {@code Foo::bar} in {@code Foo::bar::baz::quux} returns {@code Foo::bar::baz::quux}.
     */
    @NotNull
    public static RsPath rootPath(@NotNull RsPath path) {
        PsiElement parent = path.getParent();
        while (parent instanceof RsPath) {
            path = (RsPath) parent;
            parent = path.getParent();
        }
        return path;
    }

    @Nullable
    public static TextRange getTextRangeOfLastSegment(@NotNull RsPath path) {
        PsiElement refNameElement = path.getReferenceNameElement();
        if (refNameElement == null) return null;
        RsTypeArgumentList typeArgList = path.getTypeArgumentList();
        int endOffset = typeArgList != null ? typeArgList.getTextRange().getEndOffset() : refNameElement.getTextRange().getEndOffset();
        return new TextRange(refNameElement.getTextRange().getStartOffset(), endOffset);
    }

    @Nullable
    public static RsPath getQualifier(@NotNull RsPath path) {
        RsPath subPath = path.getPath();
        if (subPath != null) return subPath;
        PsiElement ctx = path.getContext();
        while (ctx instanceof RsPath) {
            ctx = ctx.getContext();
        }
        if (ctx instanceof RsUseSpeck) {
            return RsUseSpeckUtil.getQualifier((RsUseSpeck) ctx);
        }
        return null;
    }

    @NotNull
    public static RsPath getBasePath(@NotNull RsPath path) {
        RsPath qualifier = getQualifier(path);
        while (qualifier != null) {
            RsPath next = getQualifier(qualifier);
            if (next == null) return qualifier;
            qualifier = next;
        }
        return path;
    }

    public static boolean isInsideDocLink(@NotNull RsPath path) {
        PsiElement parent = rootPath(path).getParent();
        if (parent instanceof RsDocPathLinkParent) return true;
        if (parent instanceof RsTypeReference) {
            RsPath enclosing = RsPsiJavaUtil.ancestorStrict((RsTypeReference) parent, RsPath.class);
            return enclosing != null && isInsideDocLink(enclosing);
        }
        return false;
    }

    @NotNull
    public static Set<Namespace> allowedNamespaces(@NotNull RsPath path, boolean isCompletion) {
        return allowedNamespaces(path, isCompletion, path.getParent());
    }

    @NotNull
    public static Set<Namespace> allowedNamespaces(@NotNull RsPath path, boolean isCompletion, @Nullable PsiElement parent) {
        if (parent instanceof RsPath || parent instanceof RsTraitRef || parent instanceof RsStructLiteral || parent instanceof RsPatStruct) {
            return Namespace.TYPES;
        }
        if (parent instanceof RsTypeReference) {
            PsiElement stubParent = RsElementUtil.getStubParent((PsiElement) parent);
            if (stubParent instanceof RsTypeArgumentList) {
                if (path.getTypeArgumentList() == null && path.getValueParameterList() == null) {
                    return Namespace.TYPES_N_VALUES;
                }
            }
            return Namespace.TYPES;
        }
        if (parent instanceof RsUseSpeck) {
            RsUseSpeck useSpeck = (RsUseSpeck) parent;
            if (useSpeck.getUseGroup() != null || RsUseSpeckUtil.isStarImport(useSpeck)) {
                return Namespace.TYPES;
            }
            return Namespace.TYPES_N_VALUES_N_MACROS;
        }
        if (parent instanceof RsPathExpr) {
            if (isCompletion && getQualifier(path) != null) return Namespace.TYPES_N_VALUES_N_MACROS;
            if (isCompletion && getQualifier(path) == null) return Namespace.TYPES_N_VALUES;
            return Namespace.VALUES;
        }
        if (parent instanceof RsPatTupleStruct) {
            return isCompletion ? Namespace.TYPES_N_VALUES : Namespace.VALUES;
        }
        if (parent instanceof RsMacroCall) {
            return Namespace.MACROS;
        }
        if (parent instanceof RsPathCodeFragment) {
            return ((RsPathCodeFragment) parent).getNs();
        }
        if (parent instanceof RsDocPathLinkParent) {
            return Namespace.TYPES_N_VALUES_N_MACROS;
        }
        return Namespace.TYPES_N_VALUES;
    }

    @NotNull
    public static PathResolveStatus getResolveStatus(@NotNull RsPath path) {
        if (path.getReference() == null) return PathResolveStatus.NO_REFERENCE;
        if (TyPrimitive.fromPath(path) == null && path.getReference().multiResolve().isEmpty()) {
            return PathResolveStatus.UNRESOLVED;
        }
        return PathResolveStatus.RESOLVED;
    }

    public static boolean getHasColonColon(@NotNull RsPath path) {
        return path.getHasColonColon();
    }

    @NotNull
    public static List<org.rust.lang.core.psi.RsLifetime> getLifetimeArguments(@NotNull RsPath path) {
        return RsMethodOrPathUtil.getLifetimeArguments(path);
    }

    /**
     * Returns the path segments adjusted for macro resolution.
     * Walks from the base path to the root path, collecting reference names.
     */
    @Nullable
    public static String[] getPathSegmentsAdjusted(@NotNull RsPath path) {
        java.util.List<String> segments = new java.util.ArrayList<>();
        RsPath current = path;
        while (current != null) {
            String name = current.getReferenceName();
            if (name == null) return null;
            segments.add(0, name);
            current = current.getPath();
        }
        if (segments.isEmpty()) return null;
        return segments.toArray(new String[0]);
    }

    /**
     * Returns the path segments adjusted for attribute macro resolution.
     * Similar to getPathSegmentsAdjusted but handles the attribute macro context.
     */
    @Nullable
    public static String[] getPathSegmentsAdjustedForAttrMacro(@NotNull RsPath path) {
        return getPathSegmentsAdjusted(path);
    }

    /**
     * Returns context chain for the path.
     */
    @NotNull
    public static Iterable<PsiElement> getContexts(@NotNull RsPath path) {
        return RsElementUtil.getContexts(path);
    }
}
