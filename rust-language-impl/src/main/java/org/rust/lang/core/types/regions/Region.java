/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.regions;

import org.rust.lang.core.types.Kind;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

/**
 * We use the terms `region` and `lifetime` interchangeably.
 * The name `Region` inspired by the Rust compiler.
 */
public abstract class Region implements Kind, TypeFoldable<Region> {
    @Override
    public int getFlags() {
        return 0;
    }

    @Override
    public Region superFoldWith(TypeFolder folder) {
        return folder.foldRegion(this);
    }

    @Override
    public boolean superVisitWith(TypeVisitor visitor) {
        return visitor.visitRegion(this);
    }
}
