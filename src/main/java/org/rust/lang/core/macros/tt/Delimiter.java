/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.MacroBraces;

import java.util.Objects;

public final class Delimiter {
    private final int myId;
    @NotNull
    private final MacroBraces myKind;

    public Delimiter(int id, @NotNull MacroBraces kind) {
        myId = id;
        myKind = kind;
    }

    public int getId() {
        return myId;
    }

    @NotNull
    public MacroBraces getKind() {
        return myKind;
    }

    @NotNull
    public Delimiter copy(int newId) {
        return new Delimiter(newId, myKind);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Delimiter)) return false;
        Delimiter d = (Delimiter) o;
        return myId == d.myId && myKind == d.myKind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myId, myKind);
    }

    @Override
    public String toString() {
        return "Delimiter(id=" + myId + ", kind=" + myKind + ")";
    }
}
