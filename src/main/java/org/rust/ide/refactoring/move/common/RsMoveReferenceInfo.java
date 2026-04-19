/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.rust.lang.core.psi.ext.RsElementUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class RsMoveReferenceInfo {
    @NotNull
    private RsPath pathOld;
    @NotNull
    private RsElement pathOldOriginal;
    @Nullable
    private final RsPath pathNewAccessible;
    @Nullable
    private final RsPath pathNewFallback;
    @NotNull
    private RsQualifiedNamedElement target;
    private final boolean forceReplaceDirectly;

    public RsMoveReferenceInfo(
        @NotNull RsPath pathOld,
        @NotNull RsElement pathOldOriginal,
        @Nullable RsPath pathNewAccessible,
        @Nullable RsPath pathNewFallback,
        @NotNull RsQualifiedNamedElement target
    ) {
        this(pathOld, pathOldOriginal, pathNewAccessible, pathNewFallback, target, false);
    }

    public RsMoveReferenceInfo(
        @NotNull RsPath pathOld,
        @NotNull RsElement pathOldOriginal,
        @Nullable RsPath pathNewAccessible,
        @Nullable RsPath pathNewFallback,
        @NotNull RsQualifiedNamedElement target,
        boolean forceReplaceDirectly
    ) {
        this.pathOld = pathOld;
        this.pathOldOriginal = pathOldOriginal;
        this.pathNewAccessible = pathNewAccessible;
        this.pathNewFallback = pathNewFallback;
        this.target = target;
        this.forceReplaceDirectly = forceReplaceDirectly;
    }

    @NotNull
    public RsPath getPathOld() {
        return pathOld;
    }

    public void setPathOld(@NotNull RsPath pathOld) {
        this.pathOld = pathOld;
    }

    @NotNull
    public RsElement getPathOldOriginal() {
        return pathOldOriginal;
    }

    public void setPathOldOriginal(@NotNull RsElement pathOldOriginal) {
        this.pathOldOriginal = pathOldOriginal;
    }

    @Nullable
    public RsPath getPathNewAccessible() {
        return pathNewAccessible;
    }

    @Nullable
    public RsPath getPathNewFallback() {
        return pathNewFallback;
    }

    @Nullable
    public RsPath getPathNew() {
        return pathNewAccessible != null ? pathNewAccessible : pathNewFallback;
    }

    @NotNull
    public RsQualifiedNamedElement getTarget() {
        return target;
    }

    public void setTarget(@NotNull RsQualifiedNamedElement target) {
        this.target = target;
    }

    public boolean isForceReplaceDirectly() {
        return forceReplaceDirectly;
    }

    public boolean isInsideUseDirective() {
        return RsElementUtil.ancestorStrict(pathOldOriginal, RsUseItem.class) != null;
    }

    @Override
    public String toString() {
        return "'" + pathOld.getText() + "' -> '" + target.getQualifiedName() + "'";
    }
}
