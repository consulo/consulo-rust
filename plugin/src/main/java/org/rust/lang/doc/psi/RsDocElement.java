/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import org.rust.lang.core.psi.ext.RsElement;

public interface RsDocElement extends RsElement {
    RsDocComment getContainingDoc();

    String getMarkdownValue();
}
