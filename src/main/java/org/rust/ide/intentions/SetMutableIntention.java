/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.rust.RsBundle;
import org.rust.lang.core.types.ty.Mutability;

/**
 * Set reference mutable
 *
 * <pre>
 * &amp;type
 * </pre>
 *
 * to this:
 *
 * <pre>
 * &amp;mut type
 * </pre>
 */
public class SetMutableIntention extends ChangeReferenceMutabilityIntention {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.set.reference.mutable");
    }

    @Override
    protected Mutability getNewMutability() {
        return Mutability.MUTABLE;
    }
}
