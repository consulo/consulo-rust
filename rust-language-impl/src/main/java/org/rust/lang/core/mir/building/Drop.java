/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.MirLocal;
import org.rust.lang.core.mir.schemas.MirSourceInfo;

public class Drop {
    @Nonnull
    private final MirLocal local;
    @Nonnull
    private final Kind kind;
    @Nonnull
    private final MirSourceInfo source;

    public static final Drop fake = new Drop(MirLocal.fake, Kind.STORAGE, MirSourceInfo.fake);

    public Drop(@Nonnull MirLocal local, @Nonnull Kind kind, @Nonnull MirSourceInfo source) {
        this.local = local;
        this.kind = kind;
        this.source = source;
    }

    @Nonnull
    public MirLocal getLocal() {
        return local;
    }

    @Nonnull
    public Kind getKind() {
        return kind;
    }

    @Nonnull
    public MirSourceInfo getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "Drop(local=" + local + ", kind=" + kind + ", source=" + source + ")"; // TODO
    }

    public enum Kind {
        VALUE,
        STORAGE
    }
}
