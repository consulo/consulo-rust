/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo;

import java.util.List;

public final class CargoConstants {

    public static final String MANIFEST_FILE = "Cargo.toml";
    public static final String XARGO_MANIFEST_FILE = "Xargo.toml";
    public static final String LOCK_FILE = "Cargo.lock";

    // From https://doc.rust-lang.org/cargo/reference/config.html#hierarchical-structure:
    //  Cargo also reads config files without the .toml extension, such as .cargo/config.
    //  Support for the .toml extension was added in version 1.39 and is the preferred form.
    //  If both files exist, Cargo will use the file without the extension.
    public static final String CONFIG_FILE = "config";
    public static final String CONFIG_TOML_FILE = "config.toml";

    // https://rust-lang.github.io/rustup/overrides.html#the-toolchain-file
    public static final String TOOLCHAIN_FILE = "rust-toolchain";
    public static final String TOOLCHAIN_TOML_FILE = "rust-toolchain.toml";

    public static final String BUILD_FILE = "build.rs";

    public static final String RUST_BACKTRACE_ENV_VAR = "RUST_BACKTRACE";

    private CargoConstants() {
    }

    public static final class ProjectLayout {
        public static final List<String> sources = List.of("src", "examples");
        public static final List<String> tests = List.of("tests", "benches");
        public static final String target = "target";

        private ProjectLayout() {
        }
    }
}
