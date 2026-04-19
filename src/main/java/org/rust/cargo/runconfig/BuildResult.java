/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;

import java.util.List;
import java.util.Objects;

public abstract class BuildResult {

    public static final class Binaries extends BuildResult {
        @NotNull
        private final List<String> myPaths;

        public Binaries(@NotNull List<String> paths) {
            myPaths = paths;
        }

        @NotNull
        public List<String> getPaths() {
            return myPaths;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Binaries binaries = (Binaries) o;
            return Objects.equals(myPaths, binaries.myPaths);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myPaths);
        }

        @Override
        public String toString() {
            return "Binaries(paths=" + myPaths + ")";
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public abstract static class ToolchainError extends BuildResult {
        @NotNull
        private final String myMessage;

        protected ToolchainError(@NotNull String message) {
            myMessage = message;
        }

        @NotNull
        public String getMessage() {
            return myMessage;
        }
    }

    public static final class UnsupportedMSVC extends ToolchainError {
        public static final UnsupportedMSVC INSTANCE = new UnsupportedMSVC();

        private UnsupportedMSVC() {
            super(RsBundle.message("dialog.message.msvc.toolchain.not.supported.please.use.gnu.toolchain"));
        }
    }

    public static final class UnsupportedGNU extends ToolchainError {
        public static final UnsupportedGNU INSTANCE = new UnsupportedGNU();

        private UnsupportedGNU() {
            super(RsBundle.message("dialog.message.gnu.toolchain.not.supported.please.use.msvc.toolchain"));
        }
    }

    public static final class UnsupportedWSL extends ToolchainError {
        public static final UnsupportedWSL INSTANCE = new UnsupportedWSL();

        private UnsupportedWSL() {
            super(RsBundle.message("dialog.message.wsl.toolchain.not.supported"));
        }
    }

    public static final class MSVCWithRustGNU extends ToolchainError {
        public static final MSVCWithRustGNU INSTANCE = new MSVCWithRustGNU();

        private MSVCWithRustGNU() {
            super(RsBundle.message("dialog.message.msvc.debugger.cannot.be.used.with.gnu.rust.toolchain"));
        }
    }

    public static final class GNUWithRustMSVC extends ToolchainError {
        public static final GNUWithRustMSVC INSTANCE = new GNUWithRustMSVC();

        private GNUWithRustMSVC() {
            super(RsBundle.message("dialog.message.gnu.debugger.cannot.be.used.with.msvc.rust.toolchain"));
        }
    }

    public static final class WSLWithNonWSL extends ToolchainError {
        public static final WSLWithNonWSL INSTANCE = new WSLWithNonWSL();

        private WSLWithNonWSL() {
            super(RsBundle.message("dialog.message.html.local.debugger.cannot.be.used.with.wsl.br.use.href.https.www.jetbrains.com.help.clion.how.to.use.wsl.development.environment.in.product.html.instructions.to.configure.wsl.toolchain.html"));
        }
    }

    public static final class NonWSLWithWSL extends ToolchainError {
        public static final NonWSLWithWSL INSTANCE = new NonWSLWithWSL();

        private NonWSLWithWSL() {
            super(RsBundle.message("dialog.message.wsl.debugger.cannot.be.used.with.non.wsl.rust.toolchain"));
        }
    }

    public static final class OtherToolchainError extends ToolchainError {
        public OtherToolchainError(@NotNull String message) {
            super(message);
        }
    }
}
