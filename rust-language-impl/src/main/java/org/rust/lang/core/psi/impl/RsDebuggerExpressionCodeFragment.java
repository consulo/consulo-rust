/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;

public class RsDebuggerExpressionCodeFragment extends RsExpressionCodeFragment {
    public RsDebuggerExpressionCodeFragment(@Nonnull Project project, @Nonnull CharSequence text, @Nonnull RsElement context) {
        super(project, text, context);
    }
}
