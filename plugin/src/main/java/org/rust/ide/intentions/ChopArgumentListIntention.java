/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsValueArgumentList;

public class ChopArgumentListIntention extends ChopListIntentionBase<RsValueArgumentList, RsExpr> {
    public ChopArgumentListIntention() {
        super(RsValueArgumentList.class, RsExpr.class,
            RsBundle.message("intention.name.put.arguments.on.separate.lines"));
    }
}
