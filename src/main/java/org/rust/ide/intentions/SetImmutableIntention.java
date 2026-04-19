/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.rust.RsBundle;
import org.rust.lang.core.types.ty.Mutability;

/**
 * Set reference immutable
 *
 * <pre>
 * &amp;mut type
 * </pre>
 *
 * to this:
 *
 * <pre>
 * &amp;type
 * </pre>
 */
public class SetImmutableIntention extends ChangeReferenceMutabilityIntention {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.set.reference.immutable");
    }

    @Override
    protected Mutability getNewMutability() {
        return Mutability.IMMUTABLE;
    }
}
