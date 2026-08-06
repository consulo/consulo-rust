/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import java.util.Collections;
import java.util.List;

public enum CargoCommands {
    BENCH(
        "Execute benchmarks of a package",
        List.of(
            new CargoOption("no-run", "Compile, but don't run benchmarks"),
            new CargoOption("no-fail-fast", "Run all benchmarks regardless of failure"),
            new CargoOption("package", "Benchmark only the specified packages"),
            new CargoOption("workspace", "Benchmark all members in the workspace"),
            new CargoOption("all", "Deprecated alias for --workspace"),
            new CargoOption("exclude", "Exclude the specified packages"),
            new CargoOption("lib", "Benchmark the package's library"),
            new CargoOption("bin", "Benchmark the specified binary"),
            new CargoOption("bins", "Benchmark all binary targets"),
            new CargoOption("example", "Benchmark the specified example"),
            new CargoOption("examples", "Benchmark all example targets"),
            new CargoOption("test", "Benchmark the specified integration test"),
            new CargoOption("tests", "Benchmark all targets in test mode that have the test = true manifest flag set"),
            new CargoOption("bench", "Benchmark the specified benchmark"),
            new CargoOption("benches", "Benchmark all targets in benchmark mode that have the bench = true manifest flag set"),
            new CargoOption("all-targets", "Benchmark all targets"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("target", "Benchmark for the given architecture"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("message-format", "The output format for diagnostic messages"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("jobs", "Number of parallel jobs to run")
        )
    ),

    BUILD(
        "Compile the current package",
        List.of(
            new CargoOption("package", "Build only the specified packages"),
            new CargoOption("workspace", "Build all members in the workspace"),
            new CargoOption("all", "Deprecated alias for --workspace"),
            new CargoOption("exclude", "Exclude the specified packages"),
            new CargoOption("lib", "Build the package's library"),
            new CargoOption("bin", "Build the specified binary"),
            new CargoOption("bins", "Build all binary targets"),
            new CargoOption("example", "Build the specified example"),
            new CargoOption("examples", "Build all example targets"),
            new CargoOption("test", "Build the specified integration test"),
            new CargoOption("tests", "Build all targets in test mode that have the test = true manifest flag set"),
            new CargoOption("bench", "Build the specified benchmark"),
            new CargoOption("benches", "Build all targets in benchmark mode that have the bench = true manifest flag set"),
            new CargoOption("all-targets", "Build all targets"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("target", "Build for the given architecture"),
            new CargoOption("release", "Build optimized artifacts with the release profile"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("out-dir", "Copy final artifacts to this directory"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("message-format", "The output format for diagnostic messages"),
            new CargoOption("build-plan", "Outputs a series of JSON messages to stdout that indicate the commands to run the build"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("jobs", "Number of parallel jobs to run")
        )
    ),

    CHECK(
        "Check the current package",
        List.of(
            new CargoOption("package", "Check only the specified packages"),
            new CargoOption("workspace", "Check all members in the workspace"),
            new CargoOption("all", "Deprecated alias for --workspace"),
            new CargoOption("exclude", "Exclude the specified packages"),
            new CargoOption("lib", "Check the package's library"),
            new CargoOption("bin", "Check the specified binary"),
            new CargoOption("bins", "Check all binary targets"),
            new CargoOption("example", "Check the specified example"),
            new CargoOption("examples", "Check all example targets"),
            new CargoOption("test", "Check the specified integration test"),
            new CargoOption("tests", "Check all targets in test mode that have the test = true manifest flag set"),
            new CargoOption("bench", "Check the specified benchmark"),
            new CargoOption("benches", "Check all targets in benchmark mode that have the bench = true manifest flag set"),
            new CargoOption("all-targets", "Check all targets"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("target", "Check for the given architecture"),
            new CargoOption("release", "Check optimized artifacts with the release profile"),
            new CargoOption("profile", "Changes check behavior"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("message-format", "The output format for diagnostic messages"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("jobs", "Number of parallel jobs to run")
        )
    ),

    CLEAN(
        "Remove generated artifacts",
        List.of(
            new CargoOption("package", "Clean only the specified packages"),
            new CargoOption("doc", "This option will cause cargo clean to remove only the doc directory in the target directory"),
            new CargoOption("release", "Clean all artifacts that were built with the release or bench profiles"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("target", "Clean for the given architecture"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information")
        )
    ),

    DOC(
        "Build a package's documentation",
        List.of(
            new CargoOption("open", "Open the docs in a browser after building them"),
            new CargoOption("no-deps", "Do not build documentation for dependencies"),
            new CargoOption("document-private-items", "Include non-public items in the documentation"),
            new CargoOption("package", "Document only the specified packages"),
            new CargoOption("workspace", "Document all members in the workspace"),
            new CargoOption("all", "Deprecated alias for --workspace"),
            new CargoOption("exclude", "Exclude the specified packages"),
            new CargoOption("lib", "Document the package's library"),
            new CargoOption("bin", "Document the specified binary"),
            new CargoOption("bins", "Document all binary targets"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("target", "Document for the given architecture"),
            new CargoOption("release", "Document optimized artifacts with the release profile"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("message-format", "The output format for diagnostic messages"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("jobs", "Number of parallel jobs to run")
        )
    ),

    FETCH(
        "Fetch dependencies of a package from the network",
        List.of(
            new CargoOption("target", "Fetch for the given architecture"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information")
        )
    ),

    FIX(
        "Automatically fix lint warnings reported by rustc",
        List.of(
            new CargoOption("broken-code", "Fix code even if it already has compiler errors"),
            new CargoOption("edition", "Apply changes that will update the code to the latest edition"),
            new CargoOption("edition-idioms", "Apply suggestions that will update code to the preferred style for the current edition"),
            new CargoOption("allow-no-vcs", "Fix code even if a VCS was not detected"),
            new CargoOption("allow-dirty", "Fix code even if the working directory has changes"),
            new CargoOption("allow-staged", "Fix code even if the working directory has staged changes"),
            new CargoOption("package", "Fix only the specified packages"),
            new CargoOption("workspace", "Fix all members in the workspace"),
            new CargoOption("all", "Deprecated alias for --workspace"),
            new CargoOption("exclude", "Exclude the specified packages"),
            new CargoOption("lib", "Fix the package's library"),
            new CargoOption("bin", "Fix the specified binary"),
            new CargoOption("bins", "Fix all binary targets"),
            new CargoOption("example", "Fix the specified example"),
            new CargoOption("examples", "Fix all example targets"),
            new CargoOption("test", "Fix the specified integration test"),
            new CargoOption("tests", "Fix all targets in test mode that have the test = true manifest flag set"),
            new CargoOption("bench", "Fix the specified benchmark"),
            new CargoOption("benches", "Fix all targets in benchmark mode that have the bench = true manifest flag set"),
            new CargoOption("all-targets", "Fix all targets"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("target", "Fix for the given architecture"),
            new CargoOption("release", "Fix optimized artifacts with the release profile"),
            new CargoOption("profile", "Changes fix behavior"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("message-format", "The output format for diagnostic messages"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("jobs", "Number of parallel jobs to run")
        )
    ),

    RUN(
        "Run the current package",
        List.of(
            new CargoOption("package", "The package to run"),
            new CargoOption("bin", "Run the specified binary"),
            new CargoOption("example", "Run the specified example"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("target", "Run for the given architecture"),
            new CargoOption("release", "Run optimized artifacts with the release profile"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("message-format", "The output format for diagnostic messages"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("jobs", "Number of parallel jobs to run")
        )
    ),

    RUSTC(
        "Compile the current package, and pass extra options to the compiler",
        List.of(
            new CargoOption("package", "The package to build"),
            new CargoOption("lib", "Build the package's library"),
            new CargoOption("bin", "Build the specified binary"),
            new CargoOption("bins", "Build all binary targets"),
            new CargoOption("example", "Build the specified example"),
            new CargoOption("examples", "Build all example targets"),
            new CargoOption("test", "Build the specified integration test"),
            new CargoOption("tests", "Build all targets in test mode that have the test = true manifest flag set"),
            new CargoOption("bench", "Build the specified benchmark"),
            new CargoOption("benches", "Build all targets in benchmark mode that have the bench = true manifest flag set"),
            new CargoOption("all-targets", "Build all targets"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("target", "Build for the given architecture"),
            new CargoOption("release", "Build optimized artifacts with the release profile"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("message-format", "The output format for diagnostic messages"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("jobs", "Number of parallel jobs to run")
        )
    ),

    RUSTDOC(
        "Build a package's documentation, using specified custom flags",
        List.of(
            new CargoOption("open", "Open the docs in a browser after building them"),
            new CargoOption("package", "The package to document"),
            new CargoOption("lib", "Document the package's library"),
            new CargoOption("bin", "Document the specified binary"),
            new CargoOption("bins", "Document all binary targets"),
            new CargoOption("example", "Document the specified example"),
            new CargoOption("examples", "Document all example targets"),
            new CargoOption("test", "Document the specified integration test"),
            new CargoOption("tests", "Document all targets in test mode that have the test = true manifest flag set"),
            new CargoOption("bench", "Document the specified benchmark"),
            new CargoOption("benches", "Document all targets in benchmark mode that have the bench = true manifest flag set"),
            new CargoOption("all-targets", "Document all targets"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("target", "Document for the given architecture"),
            new CargoOption("release", "Document optimized artifacts with the release profile"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("message-format", "The output format for diagnostic messages"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("jobs", "Number of parallel jobs to run")
        )
    ),

    TEST(
        "Execute unit and integration tests of a package",
        List.of(
            new CargoOption("no-run", "Compile, but don't run tests"),
            new CargoOption("no-fail-fast", "Run all tests regardless of failure"),
            new CargoOption("package", "Test only the specified packages"),
            new CargoOption("workspace", "Test all members in the workspace"),
            new CargoOption("all", "Deprecated alias for --workspace"),
            new CargoOption("exclude", "Exclude the specified packages"),
            new CargoOption("lib", "Test the package's library"),
            new CargoOption("bin", "Test the specified binary"),
            new CargoOption("bins", "Test all binary targets"),
            new CargoOption("example", "Test the specified example"),
            new CargoOption("examples", "Test all example targets"),
            new CargoOption("test", "Test the specified integration test"),
            new CargoOption("tests", "Test all targets in test mode that have the test = true manifest flag set"),
            new CargoOption("bench", "Test the specified benchmark"),
            new CargoOption("benches", "Test all targets in benchmark mode that have the bench = true manifest flag set"),
            new CargoOption("all-targets", "Test all targets"),
            new CargoOption("doc", "Test only the library's documentation"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("target", "Test for the given architecture"),
            new CargoOption("release", "Test optimized artifacts with the release profile"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("message-format", "The output format for diagnostic messages"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("jobs", "Number of parallel jobs to run")
        )
    ),

    GENERATE_LOCKFILE(
        "Generate the lockfile for a package",
        List.of(
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information")
        )
    ),

    LOCATE_PROJECT(
        "Print a JSON representation of a Cargo.toml file's location",
        List.of(
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("help", "Prints help information")
        )
    ),

    METADATA(
        "Machine-readable metadata about the current package",
        List.of(
            new CargoOption("no-deps", "Output information only about the workspace members and don't fetch dependencies"),
            new CargoOption("format-version", "Specify the version of the output format to use"),
            new CargoOption("filter-platform", "This filters the resolve output to only include dependencies for the given target triple"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information")
        )
    ),

    PKGID(
        "Print a fully qualified package specification",
        List.of(
            new CargoOption("package", "Get the package ID for the given package instead of the current package"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information")
        )
    ),

    TREE(
        "Display a tree visualization of a dependency graph",
        List.of(
            new CargoOption("invert", "Show the reverse dependencies for the given package"),
            new CargoOption("no-dedupe", "Do not de-duplicate repeated dependencies"),
            new CargoOption("duplicates", "Show only dependencies which come in multiple versions (implies --invert)"),
            new CargoOption("edges", "The dependency kinds to display"),
            new CargoOption("target", "Filter dependencies matching the given target-triple"),
            new CargoOption("charset", "Chooses the character set to use for the tree"),
            new CargoOption("format", "Set the format string for each package"),
            new CargoOption("prefix", "Sets how each line is displayed"),
            new CargoOption("package", "Display only the specified packages"),
            new CargoOption("workspace", "Display all members in the workspace"),
            new CargoOption("exclude", "Exclude the specified packages"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason")
        )
    ),

    UPDATE(
        "Update dependencies as recorded in the local lock file",
        List.of(
            new CargoOption("package", "Update only the specified packages"),
            new CargoOption("aggressive", "When used with -p, dependencies of SPEC are forced to update as well"),
            new CargoOption("precise", "When used with -p, allows you to specify a specific version number to set the package to"),
            new CargoOption("dry-run", "Displays what would be updated, but doesn't actually write the lockfile"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information")
        )
    ),

    VENDOR(
        "Vendor all dependencies locally",
        List.of(
            new CargoOption("sync", "Specify extra Cargo.toml manifests to workspaces which should also be vendored and synced to the output"),
            new CargoOption("no-delete", "Don't delete the \"vendor\" directory when vendoring, but rather keep all existing contents of the vendor directory"),
            new CargoOption("respect-source-config", "Instead of ignoring [source] configuration by default in .cargo/config.toml read it and use it when downloading crates from crates.io, for example"),
            new CargoOption("versioned-dirs", "Normally versions are only added to disambiguate multiple versions of the same package"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason")
        )
    ),

    VERIFY_PROJECT(
        "Check correctness of crate manifest",
        List.of(
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("help", "Prints help information")
        )
    ),

    INIT(
        "Create a new Cargo package in an existing directory",
        List.of(
            new CargoOption("bin", "Create a package with a binary target (src/main.rs)"),
            new CargoOption("lib", "Create a package with a library target (src/lib.rs)"),
            new CargoOption("edition", "Specify the Rust edition to use"),
            new CargoOption("name", "Set the package name"),
            new CargoOption("vcs", "Initialize a new VCS repository for the given version control system (git, hg, pijul, or fossil) or do not initialize any version control at all (none)"),
            new CargoOption("registry", "This sets the publish field in Cargo.toml to the given registry name which will restrict publishing only to that registry"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information")
        )
    ),

    INSTALL(
        "Build and install a Rust binary",
        List.of(
            new CargoOption("vers", "Specify a version to install"),
            new CargoOption("version", "Specify a version to install"),
            new CargoOption("git", "Git URL to install the specified crate from"),
            new CargoOption("branch", "Branch to use when installing from git"),
            new CargoOption("tag", "Tag to use when installing from git"),
            new CargoOption("rev", "Specific commit to use when installing from git"),
            new CargoOption("path", "Filesystem path to local crate to install"),
            new CargoOption("list", "List all installed packages and their versions"),
            new CargoOption("force", "Force overwriting existing crates or binaries"),
            new CargoOption("no-track", "By default, Cargo keeps track of the installed packages with a metadata file stored in the installation root directory"),
            new CargoOption("bin", "Install only the specified binary"),
            new CargoOption("bins", "Install all binaries"),
            new CargoOption("example", "Install only the specified example"),
            new CargoOption("examples", "Install all examples"),
            new CargoOption("root", "Directory to install packages into"),
            new CargoOption("registry", "Name of the registry to use"),
            new CargoOption("index", "The URL of the registry index to use"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("target", "Install for the given architecture"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("debug", "Build with the dev profile instead the release profile"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("jobs", "Number of parallel jobs to run"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information")
        )
    ),

    NEW(
        "Create a new Cargo package",
        List.of(
            new CargoOption("bin", "Create a package with a binary target (src/main.rs)"),
            new CargoOption("lib", "Create a package with a library target (src/lib.rs)"),
            new CargoOption("edition", "Specify the Rust edition to use"),
            new CargoOption("name", "Set the package name"),
            new CargoOption("vcs", "Initialize a new VCS repository for the given version control system (git, hg, pijul, or fossil) or do not initialize any version control at all (none)"),
            new CargoOption("registry", "This sets the publish field in Cargo.toml to the given registry name which will restrict publishing only to that registry"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information")
        )
    ),

    SEARCH(
        "Search packages in crates.io",
        List.of(
            new CargoOption("limit", "Limit the number of results (default: 10, max: 100)"),
            new CargoOption("index", "The URL of the registry index to use"),
            new CargoOption("registry", "Name of the registry to use"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information")
        )
    ),

    UNINSTALL(
        "Remove a Rust binary",
        List.of(
            new CargoOption("package", "Package to uninstall"),
            new CargoOption("bin", "Only uninstall the binary NAME"),
            new CargoOption("root", "Directory to uninstall packages from"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information")
        )
    ),

    LOGIN(
        "Save an API token from the registry locally",
        List.of(
            new CargoOption("registry", "Name of the registry to use"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information")
        )
    ),

    OWNER(
        "Manage the owners of a crate on the registry",
        List.of(
            new CargoOption("add", "Invite the given user or team as an owner"),
            new CargoOption("remove", "Remove the given user or team as an owner"),
            new CargoOption("list", "List owners of a crate"),
            new CargoOption("token", "API token to use when authenticating"),
            new CargoOption("index", "The URL of the registry index to use"),
            new CargoOption("registry", "Name of the registry to use"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information")
        )
    ),

    PACKAGE(
        "Assemble the local package into a distributable tarball",
        List.of(
            new CargoOption("list", "Print files included in a package without making one"),
            new CargoOption("no-verify", "Don't verify the contents by building them"),
            new CargoOption("no-metadata", "Ignore warnings about a lack of human-usable metadata (such as the description or the license)"),
            new CargoOption("allow-dirty", "Allow working directories with uncommitted VCS changes to be packaged"),
            new CargoOption("target", "Package for the given architecture"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("jobs", "Number of parallel jobs to run"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information")
        )
    ),

    PUBLISH(
        "Upload a package to the registry",
        List.of(
            new CargoOption("dry-run", "Perform all checks without uploading"),
            new CargoOption("token", "API token to use when authenticating"),
            new CargoOption("no-verify", "Don't verify the contents by building them"),
            new CargoOption("allow-dirty", "Allow working directories with uncommitted VCS changes to be packaged"),
            new CargoOption("index", "The URL of the registry index to use"),
            new CargoOption("registry", "Name of the registry to use"),
            new CargoOption("target", "Publish for the given architecture"),
            new CargoOption("target-dir", "Directory for all generated artifacts and intermediate files"),
            new CargoOption("features", "Space or comma separated list of features to activate"),
            new CargoOption("all-features", "Activate all available features of all selected packages"),
            new CargoOption("no-default-features", "Do not activate the default feature of the current directory's package"),
            new CargoOption("manifest-path", "Path to the Cargo.toml file"),
            new CargoOption("frozen", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("locked", "Either of these flags requires that the Cargo.lock file is up-to-date"),
            new CargoOption("offline", "Prevents Cargo from accessing the network for any reason"),
            new CargoOption("jobs", "Number of parallel jobs to run"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information")
        )
    ),

    YANK(
        "Remove a pushed crate from the index",
        List.of(
            new CargoOption("vers", "The version to yank or un-yank"),
            new CargoOption("undo", "Undo a yank, putting a version back into the index"),
            new CargoOption("token", "API token to use when authenticating"),
            new CargoOption("index", "The URL of the registry index to use"),
            new CargoOption("registry", "Name of the registry to use"),
            new CargoOption("verbose", "Use verbose output"),
            new CargoOption("quiet", "No output printed to stdout"),
            new CargoOption("color", "Control when colored output is used"),
            new CargoOption("help", "Prints help information")
        )
    ),

    HELP(
        "Get help for a Cargo command",
        Collections.emptyList()
    ),

    VERSION(
        "Show version information",
        List.of(
            new CargoOption("verbose", "Display additional version information")
        )
    );

    private final String description;
    private final List<CargoOption> options;

    CargoCommands(String description, List<CargoOption> options) {
        this.description = description;
        this.options = options;
    }

    public String getDescription() {
        return description;
    }

    public List<CargoOption> getOptions() {
        return options;
    }

    public String getPresentableName() {
        return name().toLowerCase().replace('_', '-');
    }
}
