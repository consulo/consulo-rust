/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.project.Project;
import consulo.language.file.FileViewProvider;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.parser.RustParserUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;
import org.rust.lang.core.resolve.Namespace;

import java.util.Set;

/**
 * Code fragment for Rust paths.
 */
public class RsPathCodeFragment extends RsCodeFragment implements RsInferenceContextOwner {

    @Nonnull
    private final Set<Namespace> ns;

    public RsPathCodeFragment(
        @Nonnull Project project,
        @Nonnull CharSequence text,
        boolean isPhysical,
        @Nullable RsElement context,
        @Nonnull RustParserUtil.PathParsingMode mode,
        @Nonnull Set<Namespace> ns
    ) {
        super(project, text, getContentElementType(mode), context);
        this.ns = ns;
    }

    @Nonnull
    private static IElementType getContentElementType(@Nonnull RustParserUtil.PathParsingMode mode) {
        if (mode == RustParserUtil.PathParsingMode.TYPE) {
            return RsCodeFragmentElementType.TYPE_PATH;
        }
        return RsCodeFragmentElementType.VALUE_PATH;
    }

    @Nonnull
    public Set<Namespace> getNs() {
        return ns;
    }

    /** {@code val path: RsPath? get() = childOfType()}. */
    @Nullable
    public RsPath getPath() {
        return org.rust.lang.core.psi.ext.PsiElementUtil.childOfType(this, RsPath.class);
    }
}
