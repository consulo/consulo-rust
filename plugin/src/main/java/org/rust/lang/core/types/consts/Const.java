/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.consts;

import org.rust.lang.core.types.Kind;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

public abstract class Const implements Kind, TypeFoldable<Const> {
    private final int myFlags;

    protected Const() {
        this(0);
    }

    protected Const(int flags) {
        myFlags = flags;
    }

    @Override
    public int getFlags() {
        return myFlags;
    }

    @Override
    public Const foldWith(TypeFolder folder) {
        return folder.foldConst(this);
    }

    @Override
    public Const superFoldWith(TypeFolder folder) {
        return this;
    }

    @Override
    public boolean visitWith(TypeVisitor visitor) {
        return visitor.visitConst(this);
    }

    @Override
    public boolean superVisitWith(TypeVisitor visitor) {
        return false;
    }
}
