/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsUseGroup;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.stubs.RsUseSpeckStub;

import java.util.function.Consumer;

public final class RsUseSpeckUtil {
    private RsUseSpeckUtil() {
    }

    public static boolean isStarImport(@NotNull RsUseSpeck useSpeck) {
        RsUseSpeckStub stub = RsPsiJavaUtil.getGreenStub(useSpeck);
        return stub != null ? stub.isStarImport() : useSpeck.getMul() != null;
    }

    public static boolean getHasColonColon(@NotNull RsUseSpeck useSpeck) {
        RsUseSpeckStub stub = RsPsiJavaUtil.getGreenStub(useSpeck);
        return stub != null ? stub.isHasColonColon() : useSpeck.getColoncolon() != null;
    }

    @Nullable
    public static RsPath getQualifier(@NotNull RsUseSpeck useSpeck) {
        com.intellij.psi.PsiElement context = useSpeck.getContext();
        if (!(context instanceof RsUseGroup)) return null;
        RsUseSpeck parentUseSpeck = RsUseGroupUtil.getParentUseSpeck((RsUseGroup) context);
        return getPathOrQualifier(parentUseSpeck);
    }

    @Nullable
    public static RsPath getPathOrQualifier(@NotNull RsUseSpeck useSpeck) {
        RsPath path = useSpeck.getPath();
        return path != null ? path : getQualifier(useSpeck);
    }

    @Nullable
    public static String getNameInScope(@NotNull RsUseSpeck useSpeck) {
        return itemName(useSpeck, true);
    }

    @Nullable
    public static String itemName(@NotNull RsUseSpeck useSpeck, boolean withAlias) {
        if (useSpeck.getUseGroup() != null) return null;
        if (withAlias) {
            org.rust.lang.core.psi.RsAlias alias = useSpeck.getAlias();
            if (alias != null) {
                String name = alias.getName();
                if (name != null) return name;
            }
        }
        RsPath path = useSpeck.getPath();
        if (path == null) return null;
        String baseName = path.getReferenceName();
        if (baseName == null) return null;
        if ("self".equals(baseName)) {
            RsPath qualifier = getQualifier(useSpeck);
            return qualifier != null ? qualifier.getReferenceName() : null;
        }
        return baseName;
    }

    public static boolean isIdentifier(@NotNull RsUseSpeck useSpeck) {
        RsPath path = useSpeck.getPath();
        if (path == null) return false;
        if (!(path == useSpeck.getFirstChild() && path == useSpeck.getLastChild())) return false;
        return path.getIdentifier() != null && path.getPath() == null && path.getColoncolon() == null;
    }

    public static void forEachLeafSpeck(@NotNull RsUseSpeck useSpeck, @NotNull Consumer<RsUseSpeck> consumer) {
        RsUseGroup group = useSpeck.getUseGroup();
        if (group == null) {
            consumer.accept(useSpeck);
        } else {
            for (RsUseSpeck child : group.getUseSpeckList()) {
                forEachLeafSpeck(child, consumer);
            }
        }
    }

    @NotNull
    public static RsUseSpeck getParentUseSpeck(@NotNull RsUseGroup useGroup) {
        return RsUseGroupUtil.getParentUseSpeck(useGroup);
    }

    /**
     * Forwarding method for compatibility with callers that referenced RsUseSpeckUtil.deleteWithSurroundingComma.
     * The actual implementation is in {@link RsElementExtKt#deleteWithSurroundingComma(com.intellij.psi.PsiElement)}.
     */
    public static void deleteWithSurroundingComma(@NotNull com.intellij.psi.PsiElement element) {
        RsElementUtil.deleteWithSurroundingComma(element);
    }
}
