/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.MirLocal;
import org.rust.lang.core.mir.schemas.MirSourceInfo;

public class Drop {
    @NotNull
    private final MirLocal local;
    @NotNull
    private final Kind kind;
    @NotNull
    private final MirSourceInfo source;

    public static final Drop fake = new Drop(MirLocal.fake, Kind.STORAGE, MirSourceInfo.fake);

    public Drop(@NotNull MirLocal local, @NotNull Kind kind, @NotNull MirSourceInfo source) {
        this.local = local;
        this.kind = kind;
        this.source = source;
    }

    @NotNull
    public MirLocal getLocal() {
        return local;
    }

    @NotNull
    public Kind getKind() {
        return kind;
    }

    @NotNull
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
