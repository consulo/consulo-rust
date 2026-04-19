/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;

public abstract class SelfSignature {

    public static final BasicSelfSignature NO_SELF = new BasicSelfSignature("no self");
    public static final BasicSelfSignature BY_VAL = new BasicSelfSignature("self by value");
    public static final BasicSelfSignature BY_REF = new BasicSelfSignature("self by reference");
    public static final BasicSelfSignature BY_MUT_REF = new BasicSelfSignature("self by mutable reference");

    public static class ArbitrarySelfSignature extends SelfSignature {
        public static final ArbitrarySelfSignature INSTANCE = new ArbitrarySelfSignature();
    }

    public static class BasicSelfSignature extends SelfSignature {
        private final String myDescription;

        public BasicSelfSignature(@NotNull String description) {
            myDescription = description;
        }

        @NotNull
        public String getDescription() {
            return myDescription;
        }
    }
}
