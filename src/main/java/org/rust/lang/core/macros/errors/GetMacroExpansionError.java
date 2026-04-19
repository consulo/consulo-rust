/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.errors;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.lang.core.macros.MacroExpansionContext;
import org.rust.lang.core.psi.RsProcMacroKind;
import org.rust.openapiext.RsPathManager;

/**
 * An error type for {@code org.rust.lang.core.psi.ext.expansionResult}
 */
public abstract class GetMacroExpansionError {
    private GetMacroExpansionError() {}

    @Nls
    @NotNull
    public abstract String toUserViewableMessage();

    @Override
    public String toString() {
        return "GetMacroExpansionError." + getClass().getSimpleName();
    }

    public static final GetMacroExpansionError MacroExpansionIsDisabled = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.MacroExpansionIsDisabled.message"); }
        @Override public String toString() { return "GetMacroExpansionError.MacroExpansionIsDisabled"; }
    };

    public static final GetMacroExpansionError MacroExpansionEngineIsNotReady = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.MacroExpansionEngineIsNotReady.message"); }
        @Override public String toString() { return "GetMacroExpansionError.MacroExpansionEngineIsNotReady"; }
    };

    public static final GetMacroExpansionError IncludingFileNotFound = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.IncludingFileNotFound.message"); }
        @Override public String toString() { return "GetMacroExpansionError.IncludingFileNotFound"; }
    };

    public static final GetMacroExpansionError FileIncludedIntoMultiplePlaces = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.FileIncludedIntoMultiplePlaces.message"); }
        @Override public String toString() { return "GetMacroExpansionError.FileIncludedIntoMultiplePlaces"; }
    };

    public static final GetMacroExpansionError OldEngineStd = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.OldEngineStd.message"); }
        @Override public String toString() { return "GetMacroExpansionError.OldEngineStd"; }
    };

    public static final GetMacroExpansionError MemExpAttrMacro = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.MemExpAttrMacro.message"); }
        @Override public String toString() { return "GetMacroExpansionError.MemExpAttrMacro"; }
    };

    public static final GetMacroExpansionError ModDataNotFound = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.ModDataNotFound.message"); }
        @Override public String toString() { return "GetMacroExpansionError.ModDataNotFound"; }
    };

    public static final GetMacroExpansionError InconsistentExpansionExpandedFrom = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.InconsistentExpansionExpandedFrom.message"); }
        @Override public String toString() { return "GetMacroExpansionError.InconsistentExpansionExpandedFrom"; }
    };

    public static final GetMacroExpansionError TooDeepExpansion = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.TooDeepExpansion.message"); }
        @Override public String toString() { return "GetMacroExpansionError.TooDeepExpansion"; }
    };

    public static final GetMacroExpansionError NoMacroIndex = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.NoMacroIndex.message"); }
        @Override public String toString() { return "GetMacroExpansionError.NoMacroIndex"; }
    };

    public static final GetMacroExpansionError ExpansionNameNotFound = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.ExpansionNameNotFound.message"); }
        @Override public String toString() { return "GetMacroExpansionError.ExpansionNameNotFound"; }
    };

    public static final GetMacroExpansionError ExpansionFileNotFound = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.ExpansionFileNotFound.message"); }
        @Override public String toString() { return "GetMacroExpansionError.ExpansionFileNotFound"; }
    };

    public static final GetMacroExpansionError InconsistentExpansionCacheAndVfs = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.InconsistentExpansionCacheAndVfs.message"); }
        @Override public String toString() { return "GetMacroExpansionError.InconsistentExpansionCacheAndVfs"; }
    };

    public static final GetMacroExpansionError VirtualFileFoundButPsiIsNull = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.VirtualFileFoundButPsiIsNull.message"); }
        @Override public String toString() { return "GetMacroExpansionError.VirtualFileFoundButPsiIsNull"; }
    };

    public static final GetMacroExpansionError VirtualFileFoundButPsiIsUnknown = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.VirtualFileFoundButPsiIsUnknown.message"); }
        @Override public String toString() { return "GetMacroExpansionError.VirtualFileFoundButPsiIsUnknown"; }
    };

    public static final GetMacroExpansionError TooLargeExpansion = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.TooLargeExpansion.message"); }
        @Override public String toString() { return "GetMacroExpansionError.TooLargeExpansion"; }
    };

    public static final GetMacroExpansionError CfgDisabled = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.CfgDisabled.message"); }
        @Override public String toString() { return "GetMacroExpansionError.CfgDisabled"; }
    };

    public static final GetMacroExpansionError Skipped = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.Skipped.message"); }
        @Override public String toString() { return "GetMacroExpansionError.Skipped"; }
    };

    public static final GetMacroExpansionError Unresolved = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.Unresolved.message"); }
        @Override public String toString() { return "GetMacroExpansionError.Unresolved"; }
    };

    public static final GetMacroExpansionError NoProcMacroArtifact = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.NoProcMacroArtifact.message"); }
        @Override public String toString() { return "GetMacroExpansionError.NoProcMacroArtifact"; }
    };

    public static final GetMacroExpansionError MacroCallSyntax = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.MacroCallSyntax.message"); }
        @Override public String toString() { return "GetMacroExpansionError.MacroCallSyntax"; }
    };

    public static final GetMacroExpansionError MacroDefSyntax = new GetMacroExpansionError() {
        @Override public @NotNull String toUserViewableMessage() { return RsBundle.message("macro.expansion.error.MacroDefSyntax.message"); }
        @Override public String toString() { return "GetMacroExpansionError.MacroDefSyntax"; }
    };

    public static final class MemExpParsingError extends GetMacroExpansionError {
        private final CharSequence myExpansionText;
        private final MacroExpansionContext myContext;

        public MemExpParsingError(@NotNull CharSequence expansionText, @NotNull MacroExpansionContext context) {
            myExpansionText = expansionText;
            myContext = context;
        }

        @NotNull
        public CharSequence getExpansionText() {
            return myExpansionText;
        }

        @NotNull
        public MacroExpansionContext getContext() {
            return myContext;
        }

        @Override
        @NotNull
        public String toUserViewableMessage() {
            return RsBundle.message("macro.expansion.error.MemExpParsingError.message", myExpansionText, myContext);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MemExpParsingError that = (MemExpParsingError) o;
            return myExpansionText.toString().equals(that.myExpansionText.toString()) && myContext == that.myContext;
        }

        @Override
        public int hashCode() {
            int result = myExpansionText.toString().hashCode();
            result = 31 * result + myContext.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "GetMacroExpansionError.MemExpParsingError";
        }
    }

    public static final class UnmatchedProcMacroKind extends GetMacroExpansionError {
        private final RsProcMacroKind myCallKind;
        private final RsProcMacroKind myDefKind;

        public UnmatchedProcMacroKind(@NotNull RsProcMacroKind callKind, @NotNull RsProcMacroKind defKind) {
            myCallKind = callKind;
            myDefKind = defKind;
        }

        @NotNull
        public RsProcMacroKind getCallKind() {
            return myCallKind;
        }

        @NotNull
        public RsProcMacroKind getDefKind() {
            return myDefKind;
        }

        @Override
        @NotNull
        public String toUserViewableMessage() {
            return RsBundle.message("macro.expansion.error.UnmatchedProcMacroKind.message", myDefKind, myCallKind);
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
            return "GetMacroExpansionError.UnmatchedProcMacroKind";
        }
    }

    public static final class ExpansionError extends GetMacroExpansionError {
        private final MacroExpansionError myError;

        public ExpansionError(@NotNull MacroExpansionError error) {
            myError = error;
        }

        @NotNull
        public MacroExpansionError getError() {
            return myError;
        }

        @Override
        @NotNull
        public String toUserViewableMessage() {
            MacroExpansionError e = myError;
            if (e instanceof BuiltinMacroExpansionError) {
                return RsBundle.message("macro.expansion.error.BuiltinMacroExpansionError.message");
            } else if (e == DeclMacroExpansionError.DefSyntax) {
                return RsBundle.message("macro.expansion.error.DeclMacroExpansionError.DefSyntax.message");
            } else if (e == DeclMacroExpansionError.TooLargeExpansion) {
                return RsBundle.message("macro.expansion.error.DeclMacroExpansionError.TooLargeExpansion.message");
            } else if (e instanceof DeclMacroExpansionError.Matching) {
                return RsBundle.message("macro.expansion.error.DeclMacroExpansionError.Matching.message");
            } else if (e instanceof ProcMacroExpansionError.ServerSideError) {
                return RsBundle.message("macro.expansion.error.ProcMacroExpansionError.ServerSideError.message", ((ProcMacroExpansionError.ServerSideError) e).getMessage());
            } else if (e instanceof ProcMacroExpansionError.Timeout) {
                return RsBundle.message("macro.expansion.error.ProcMacroExpansionError.Timeout.message", ((ProcMacroExpansionError.Timeout) e).getTimeout());
            } else if (e instanceof ProcMacroExpansionError.UnsupportedExpanderVersion) {
                return RsBundle.message("macro.expansion.error.ProcMacroExpansionError.UnsupportedExpanderVersion.message", ((ProcMacroExpansionError.UnsupportedExpanderVersion) e).getVersion());
            } else if (e instanceof ProcMacroExpansionError.ProcessAborted) {
                return RsBundle.message("macro.expansion.error.ProcMacroExpansionError.ProcessAborted.message", ((ProcMacroExpansionError.ProcessAborted) e).getExitCode());
            } else if (e == ProcMacroExpansionError.IOExceptionThrown) {
                return RsBundle.message("macro.expansion.error.ProcMacroExpansionError.IOExceptionThrown.message");
            } else if (e == ProcMacroExpansionError.CantRunExpander) {
                return RsBundle.message("macro.expansion.error.ProcMacroExpansionError.CantRunExpander.message", RsPathManager.INTELLIJ_RUST_NATIVE_HELPER);
            } else if (e == ProcMacroExpansionError.ExecutableNotFound) {
                return RsBundle.message("macro.expansion.error.ProcMacroExpansionError.ExecutableNotFound.message", RsPathManager.INTELLIJ_RUST_NATIVE_HELPER);
            } else if (e == ProcMacroExpansionError.ProcMacroExpansionIsDisabled) {
                return RsBundle.message("macro.expansion.error.ProcMacroExpansionError.ProcMacroExpansionIsDisabled.message");
            }
            return "Unknown expansion error: " + e;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExpansionError that = (ExpansionError) o;
            return myError.equals(that.myError);
        }

        @Override
        public int hashCode() {
            return myError.hashCode();
        }

        @Override
        public String toString() {
            return "GetMacroExpansionError.ExpansionError";
        }
    }
}
