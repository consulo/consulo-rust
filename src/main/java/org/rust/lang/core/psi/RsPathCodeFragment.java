/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.parser.RustParserUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;
import org.rust.lang.core.resolve.Namespace;

import java.util.Set;

/**
 * Code fragment for Rust paths.
 */
public class RsPathCodeFragment extends RsCodeFragment implements RsInferenceContextOwner {

    @NotNull
    private final Set<Namespace> ns;

    public RsPathCodeFragment(
        @NotNull Project project,
        @NotNull CharSequence text,
        boolean isPhysical,
        @Nullable RsElement context,
        @NotNull RustParserUtil.PathParsingMode mode,
        @NotNull Set<Namespace> ns
    ) {
        super(project, text, getContentElementType(mode), context);
        this.ns = ns;
    }

    @NotNull
    private static IElementType getContentElementType(@NotNull RustParserUtil.PathParsingMode mode) {
        if (mode == RustParserUtil.PathParsingMode.TYPE) {
            return RsCodeFragmentElementType.TYPE_PATH;
        }
        return RsCodeFragmentElementType.VALUE_PATH;
    }

    @NotNull
    public Set<Namespace> getNs() {
        return ns;
    }

    /** {@code val path: RsPath? get() = childOfType()}. */
    @Nullable
    public RsPath getPath() {
        return org.rust.lang.core.psi.ext.PsiElementUtil.childOfType(this, RsPath.class);
    }
}
