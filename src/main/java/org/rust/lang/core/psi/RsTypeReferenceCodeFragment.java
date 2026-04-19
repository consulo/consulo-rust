/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemsOwner;

public class RsTypeReferenceCodeFragment extends RsCodeFragment {

    public RsTypeReferenceCodeFragment(
        @NotNull Project project,
        @NotNull CharSequence text,
        @NotNull RsElement context
    ) {
        super(project, text, RsCodeFragmentElementType.TYPE_REF, context);
    }

    public RsTypeReferenceCodeFragment(
        @NotNull Project project,
        @NotNull CharSequence text,
        @NotNull RsElement context,
        @Nullable RsItemsOwner importTarget
    ) {
        super(project, text, RsCodeFragmentElementType.TYPE_REF, context, importTarget);
    }

    @Nullable
    public RsTypeReference getTypeReference() {
        return findChildByClass(RsTypeReference.class);
    }
}
