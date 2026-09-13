/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.errors;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.impl.RsProcMacroKind;

/**
 * Sealed class hierarchy for macro resolution errors without PSI.
 */
public abstract class ResolveMacroWithoutPsiError {
    private ResolveMacroWithoutPsiError() {}

    public static final ResolveMacroWithoutPsiError Unresolved = new ResolveMacroWithoutPsiError() {
        @Override
        public String toString() {
            return "ResolveMacroWithoutPsiError.Unresolved";
        }
    };

    public static final ResolveMacroWithoutPsiError NoProcMacroArtifact = new ResolveMacroWithoutPsiError() {
        @Override
        public String toString() {
            return "ResolveMacroWithoutPsiError.NoProcMacroArtifact";
        }
    };

    /** @see org.rust.lang.core.psi.RS_HARDCODED_PROC_MACRO_ATTRIBUTES */
    public static final ResolveMacroWithoutPsiError HardcodedProcMacroAttribute = new ResolveMacroWithoutPsiError() {
        @Override
        public String toString() {
            return "ResolveMacroWithoutPsiError.HardcodedProcMacroAttribute";
        }
    };

    public static final class UnmatchedProcMacroKind extends ResolveMacroWithoutPsiError {
        private final RsProcMacroKind myCallKind;
        private final RsProcMacroKind myDefKind;

        public UnmatchedProcMacroKind(@Nonnull RsProcMacroKind callKind, @Nonnull RsProcMacroKind defKind) {
            myCallKind = callKind;
            myDefKind = defKind;
        }

        @Nonnull
        public RsProcMacroKind getCallKind() {
            return myCallKind;
        }

        @Nonnull
        public RsProcMacroKind getDefKind() {
            return myDefKind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnmatchedProcMacroKind that = (UnmatchedProcMacroKind) o;
            return myCallKind == that.myCallKind && myDefKind == that.myDefKind;
        }

        @Override
        public int hashCode() {
            int result = myCallKind.hashCode();
            result = 31 * result + myDefKind.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ResolveMacroWithoutPsiError.UnmatchedProcMacroKind(" +
                "callKind=" + myCallKind +
                ", defKind=" + myDefKind +
                ")";
        }
    }
}
