/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.*;

public class RsTypeReferenceCodeFragment extends RsCodeFragment {

    public RsTypeReferenceCodeFragment(
        @Nonnull Project project,
        @Nonnull CharSequence text,
        @Nonnull RsElement context
    ) {
        super(project, text, RsCodeFragmentElementType.TYPE_REF, context);
    }

    public RsTypeReferenceCodeFragment(
        @Nonnull Project project,
        @Nonnull CharSequence text,
        @Nonnull RsElement context,
        @Nullable RsItemsOwner importTarget
    ) {
        super(project, text, RsCodeFragmentElementType.TYPE_REF, context, importTarget);
    }

    @Nullable
    public RsTypeReference getTypeReference() {
        return findChildByClass(RsTypeReference.class);
    }
}
