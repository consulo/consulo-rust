/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.rust.RsBundle;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.psi.RsValueParameterList;

public class JoinParameterListIntention extends JoinListIntentionBase<RsValueParameterList, RsValueParameter> {
    public JoinParameterListIntention() {
        super(RsValueParameterList.class, RsValueParameter.class,
            RsBundle.message("intention.name.put.parameters.on.one.line"));
    }
}
