/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import com.intellij.psi.PsiComment;

/**
 * A skipped {@code ///}, {@code //!} or {@code *} (or other kind of documentation comment decorations)
 * is treated as a comment leaf in the markdown tree.
 */
public interface RsDocGap extends PsiComment {
}
