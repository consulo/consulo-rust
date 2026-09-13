/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class RsMoveReferenceInfo {
    @Nonnull
    private RsPath pathOld;
    @Nonnull
    private RsElement pathOldOriginal;
    @Nullable
    private final RsPath pathNewAccessible;
    @Nullable
    private final RsPath pathNewFallback;
    @Nonnull
    private RsQualifiedNamedElement target;
    private final boolean forceReplaceDirectly;

    public RsMoveReferenceInfo(
        @Nonnull RsPath pathOld,
        @Nonnull RsElement pathOldOriginal,
        @Nullable RsPath pathNewAccessible,
        @Nullable RsPath pathNewFallback,
        @Nonnull RsQualifiedNamedElement target
    ) {
        this(pathOld, pathOldOriginal, pathNewAccessible, pathNewFallback, target, false);
    }

    public RsMoveReferenceInfo(
        @Nonnull RsPath pathOld,
        @Nonnull RsElement pathOldOriginal,
        @Nullable RsPath pathNewAccessible,
        @Nullable RsPath pathNewFallback,
        @Nonnull RsQualifiedNamedElement target,
        boolean forceReplaceDirectly
    ) {
        this.pathOld = pathOld;
        this.pathOldOriginal = pathOldOriginal;
        this.pathNewAccessible = pathNewAccessible;
        this.pathNewFallback = pathNewFallback;
        this.target = target;
        this.forceReplaceDirectly = forceReplaceDirectly;
    }

    @Nonnull
    public RsPath getPathOld() {
        return pathOld;
    }

    public void setPathOld(@Nonnull RsPath pathOld) {
        this.pathOld = pathOld;
    }

    @Nonnull
    public RsElement getPathOldOriginal() {
        return pathOldOriginal;
    }

    public void setPathOldOriginal(@Nonnull RsElement pathOldOriginal) {
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

    @Nonnull
    public RsQualifiedNamedElement getTarget() {
        return target;
    }

    public void setTarget(@Nonnull RsQualifiedNamedElement target) {
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
