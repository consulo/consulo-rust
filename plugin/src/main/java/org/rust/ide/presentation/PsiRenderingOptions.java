/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import java.util.Objects;

public final class PsiRenderingOptions {
    private final boolean renderLifetimes;
    /** Related to {@link RsPsiRenderer#appendFunctionSignature} */
    private final boolean renderGenericsAndWhere;
    /** if {@code true}, renders {@code Bar} instead of {@code foo::Bar} */
    private final boolean shortPaths;

    public PsiRenderingOptions() {
        this(true, true, true);
    }

    public PsiRenderingOptions(boolean renderLifetimes) {
        this(renderLifetimes, true, true);
    }

    public PsiRenderingOptions(boolean renderLifetimes, boolean renderGenericsAndWhere, boolean shortPaths) {
        this.renderLifetimes = renderLifetimes;
        this.renderGenericsAndWhere = renderGenericsAndWhere;
        this.shortPaths = shortPaths;
    }

    public boolean getRenderLifetimes() {
        return renderLifetimes;
    }

    public boolean getRenderGenericsAndWhere() {
        return renderGenericsAndWhere;
    }

    public boolean getShortPaths() {
        return shortPaths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PsiRenderingOptions that = (PsiRenderingOptions) o;
        return renderLifetimes == that.renderLifetimes
            && renderGenericsAndWhere == that.renderGenericsAndWhere
            && shortPaths == that.shortPaths;
    }

    @Override
    public int hashCode() {
        return Objects.hash(renderLifetimes, renderGenericsAndWhere, shortPaths);
    }

    @Override
    public String toString() {
        return "PsiRenderingOptions(renderLifetimes=" + renderLifetimes
            + ", renderGenericsAndWhere=" + renderGenericsAndWhere
            + ", shortPaths=" + shortPaths + ")";
    }
}
