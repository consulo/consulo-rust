/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PanicMessage {
    @NotNull
    private final String myMessage;

    public PanicMessage(@NotNull String message) {
        myMessage = message;
    }

    @NotNull
    public String getMessage() {
        return myMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PanicMessage)) return false;
        return Objects.equals(myMessage, ((PanicMessage) o).myMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myMessage);
    }

    @Override
    public String toString() {
        return "PanicMessage(message=" + myMessage + ")";
    }
}
