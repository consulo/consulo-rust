/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion.lint;

import java.util.List;

public class RsRustcLintCompletionProvider extends RsLintCompletionProvider {
    public static final RsRustcLintCompletionProvider INSTANCE = new RsRustcLintCompletionProvider();

    private RsRustcLintCompletionProvider() {
    }

    @Override
    protected List<Lint> getLints() {
        return RustcLints.RUSTC_LINTS;
    }
}
