/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.rust.RsBundle;
import org.rust.lang.core.psi.RsEnumBody;
import org.rust.lang.core.psi.RsEnumVariant;

public class ChopVariantListIntention extends ChopListIntentionBase<RsEnumBody, RsEnumVariant> {
    public ChopVariantListIntention() {
        super(RsEnumBody.class, RsEnumVariant.class,
            RsBundle.message("intention.name.put.variants.on.separate.lines"));
    }
}
