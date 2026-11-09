/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;

/**
 * A stand-in for a Rust installation: a toolchain is a directory with two executables, and these
 * answer only what the plugin asks. The plugin runs its real command lines against them and parses
 * real {@code cargo metadata} output, so a sync can be tested without Rust being installed.
 */
public final class CargoProxyToolchain {

    private CargoProxyToolchain() {
    }

    /** A single-package project with both a lib and a bin target, matching the metadata fixture. */
    public static Path writeProject(String prefix) throws Exception {
        Path directory = Files.createTempDirectory(prefix);
        Files.writeString(directory.resolve("Cargo.toml"),
            "[package]\nname = \"proxied\"\nversion = \"0.1.0\"\nedition = \"2021\"\n");
        Files.createDirectories(directory.resolve("src"));
        Files.writeString(directory.resolve("src/lib.rs"), "pub fn f() {}\n");
        Files.writeString(directory.resolve("src/main.rs"), "fn main() {}\n");
        return directory;
    }

    public static Path writeToolchain(Path projectDirectory) throws Exception {
        return writeToolchain(projectDirectory, Map.of("core", "// core\n"));
    }

    /** The same, with {@code stdlibSources} as the {@code lib.rs} of the standard library crates, by crate name. */
    public static Path writeToolchain(Path projectDirectory, Map<String, String> stdlibSources) throws Exception {
        Path home = Files.createTempDirectory("consulo-rust-it-toolchain");
        Path bin = Files.createDirectories(home.resolve("bin"));
        // the home is the sysroot, as in a real toolchain, so the sync finds the stdlib sources under it
        for (Map.Entry<String, String> crate : stdlibSources.entrySet()) {
            Path src = Files.createDirectories(home.resolve("lib/rustlib/src/rust/library/" + crate.getKey() + "/src"));
            Files.writeString(src.resolve("lib.rs"), crate.getValue());
        }

        String metadata = new String(
            CargoProxyToolchain.class.getResourceAsStream("/cargo-metadata.json").readAllBytes(),
            StandardCharsets.UTF_8
        ).replace("@ROOT@", projectDirectory.toString().replace('\\', '/'));
        Path metadataFile = home.resolve("metadata.json");
        Files.writeString(metadataFile, metadata);

        write(bin.resolve("rustc"), """
            #!/bin/sh
            case "$*" in
              *"--print sysroot"*) echo "%SYSROOT%" ;;
              *--version*)         echo "rustc 1.99.0 (deadbeef 2026-01-01)"
                                   echo "binary: rustc"
                                   echo "commit-hash: deadbeef"
                                   echo "commit-date: 2026-01-01"
                                   echo "host: x86_64-unknown-linux-gnu"
                                   echo "release: 1.99.0" ;;
              *) exit 0 ;;
            esac
            """.replace("%SYSROOT%", home.toString()));

        // the subcommand is not always the first argument - "-Z unstable-options" comes before it
        write(bin.resolve("cargo"), """
            #!/bin/sh
            for arg in "$@"; do
              case "$arg" in
                metadata) cat "%METADATA%"; exit 0 ;;
                config)   echo '[env]'; exit 0 ;;
                rustc)    echo 'debug_assertions'
                          echo 'target_arch="x86_64"'
                          echo 'target_os="linux"'
                          exit 0 ;;
              esac
            done
            echo "cargo 1.99.0 (deadbeef 2026-01-01)"
            """.replace("%METADATA%", metadataFile.toString()));

        return home;
    }

    private static void write(Path script, String body) throws Exception {
        Files.writeString(script, body);
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
    }
}
