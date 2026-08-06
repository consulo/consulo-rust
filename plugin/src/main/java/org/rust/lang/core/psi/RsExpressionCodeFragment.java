/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemsOwner;

public class RsExpressionCodeFragment extends RsCodeFragment {

    public RsExpressionCodeFragment(
        @Nonnull Project project,
        @Nonnull CharSequence text,
        @Nonnull RsElement context
    ) {
        super(project, text, RsCodeFragmentElementType.EXPR, context);
    }

    public RsExpressionCodeFragment(
        @Nonnull Project project,
        @Nonnull CharSequence text,
        @Nonnull RsElement context,
        @Nullable RsItemsOwner importTarget
    ) {
        super(project, text, RsCodeFragmentElementType.EXPR, context, importTarget);
    }

    @Nullable
    public RsExpr getExpr() {
        return findChildByClass(RsExpr.class);
    }
}
