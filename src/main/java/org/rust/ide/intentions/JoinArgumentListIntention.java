/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsValueArgumentList;

public class JoinArgumentListIntention extends JoinListIntentionBase<RsValueArgumentList, RsExpr> {
    public JoinArgumentListIntention() {
        super(RsValueArgumentList.class, RsExpr.class,
            RsBundle.message("intention.name.put.arguments.on.one.line"));
    }
}
