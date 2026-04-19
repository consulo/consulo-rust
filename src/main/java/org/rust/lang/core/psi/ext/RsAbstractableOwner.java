/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsTraitItem;

public abstract class RsAbstractableOwner {

    public static final RsAbstractableOwner Free = new RsAbstractableOwner() {};
    public static final RsAbstractableOwner Foreign = new RsAbstractableOwner() {};

    private RsAbstractableOwner() {
    }

    public static final class Trait extends RsAbstractableOwner {
        private final RsTraitItem myTrait;

        public Trait(RsTraitItem trait) {
            this.myTrait = trait;
        }

        public RsTraitItem getTrait() {
            return myTrait;
        }
    }

    public static final class Impl extends RsAbstractableOwner {
        private final RsImplItem myImpl;
        private final boolean myIsInherent;

        public Impl(RsImplItem impl, boolean isInherent) {
            this.myImpl = impl;
            this.myIsInherent = isInherent;
        }

        public RsImplItem getImpl() {
            return myImpl;
        }

        public boolean isInherent() {
            return myIsInherent;
        }
    }

    public boolean isInherentImpl() {
        return this instanceof Impl && ((Impl) this).isInherent();
    }

    public boolean isTraitImpl() {
        return this instanceof Impl && !((Impl) this).isInherent();
    }

    public boolean isImplOrTrait() {
        return this instanceof Impl || this instanceof Trait;
    }
}
