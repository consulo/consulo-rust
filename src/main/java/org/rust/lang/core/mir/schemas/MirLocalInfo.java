/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class MirLocalInfo {
    private MirLocalInfo() {
    }

    public static final class User extends MirLocalInfo {
        @NotNull
        private final MirClearCrossCrate<MirBindingForm> form;

        public User(@NotNull MirClearCrossCrate<MirBindingForm> form) {
            this.form = form;
        }

        @NotNull
        public MirClearCrossCrate<MirBindingForm> getForm() {
            return form;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return Objects.equals(form, user.form);
        }

        @Override
        public int hashCode() {
            return Objects.hash(form);
        }

        @Override
        public String toString() {
            return "User(form=" + form + ")";
        }
    }

    public static final class StaticRef extends MirLocalInfo {
        private final boolean isThreadLocal;

        public StaticRef(boolean isThreadLocal) {
            this.isThreadLocal = isThreadLocal;
        }

        public boolean isThreadLocal() {
            return isThreadLocal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StaticRef staticRef = (StaticRef) o;
            return isThreadLocal == staticRef.isThreadLocal;
        }

        @Override
        public int hashCode() {
            return Objects.hash(isThreadLocal);
        }

        @Override
        public String toString() {
            return "StaticRef(isThreadLocal=" + isThreadLocal + ")";
        }
    }
}
