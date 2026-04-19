/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import org.jetbrains.annotations.Nullable;

/**
 * Represents literal kinds stored in stubs.
 */
public abstract class RsStubLiteralKind {

    public static class Boolean extends RsStubLiteralKind {
        private final boolean value;

        public Boolean(boolean value) {
            this.value = value;
        }

        public boolean getValue() {
            return value;
        }
    }

    public static class Integer extends RsStubLiteralKind {
        @Nullable
        private final Long value;
        @Nullable
        private final String suffix;

        public Integer(@Nullable Long value, @Nullable String suffix) {
            this.value = value;
            this.suffix = suffix;
        }

        @Nullable
        public Long getValue() {
            return value;
        }

        @Nullable
        public String getSuffix() {
            return suffix;
        }
    }

    public static class Float extends RsStubLiteralKind {
        @Nullable
        private final Double value;
        @Nullable
        private final String suffix;

        public Float(@Nullable Double value, @Nullable String suffix) {
            this.value = value;
            this.suffix = suffix;
        }

        @Nullable
        public Double getValue() {
            return value;
        }

        @Nullable
        public String getSuffix() {
            return suffix;
        }
    }

    public static class Char extends RsStubLiteralKind {
        @Nullable
        private final String value;
        private final boolean isByte;

        public Char(@Nullable String value, boolean isByte) {
            this.value = value;
            this.isByte = isByte;
        }

        @Nullable
        public String getValue() {
            return value;
        }

        public boolean isByte() {
            return isByte;
        }
    }

    public static class StringLiteral extends RsStubLiteralKind {
        @Nullable
        private final java.lang.String value;
        private final boolean isByte;
        private final boolean isCStr;

        public StringLiteral(@Nullable java.lang.String value, boolean isByte, boolean isCStr) {
            this.value = value;
            this.isByte = isByte;
            this.isCStr = isCStr;
        }

        @Nullable
        public java.lang.String getValue() {
            return value;
        }

        public boolean isByte() {
            return isByte;
        }

        public boolean isCStr() {
            return isCStr;
        }
    }
}
