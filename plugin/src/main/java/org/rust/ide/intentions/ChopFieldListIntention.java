/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.rust.RsBundle;
import org.rust.lang.core.psi.RsBlockFields;
import org.rust.lang.core.psi.RsNamedFieldDecl;

public class ChopFieldListIntention extends ChopListIntentionBase<RsBlockFields, RsNamedFieldDecl> {
    public ChopFieldListIntention() {
        super(RsBlockFields.class, RsNamedFieldDecl.class,
            RsBundle.message("intention.name.put.fields.on.separate.lines"));
    }
}
