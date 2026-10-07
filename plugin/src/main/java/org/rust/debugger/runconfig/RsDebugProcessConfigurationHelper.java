/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.debugger.runconfig;

import consulo.logging.Logger;
import consulo.nativeDev.debugger.NativeDebuggerProvider;
import consulo.nativeDev.debugger.driver.NativeDebuggerSetup;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.RustcInfo;
import org.rust.cargo.api.toolchain.RustcVersion;
import org.rust.cargo.project.model.CargoProjectLocator;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustc;
import org.rust.openapiext.RsPathManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RsDebugProcessConfigurationHelper implements NativeDebuggerSetup {
    private static final Logger LOG = Logger.getInstance(RsDebugProcessConfigurationHelper.class);

    private static final String LLDB_LOOKUP = "lldb_lookup";
    private static final String GDB_LOOKUP = "gdb_formatters.gdb_lookup";
    private static final String COMPILER_GDB_LOOKUP = "gdb_lookup";
    private static final String RUST_LANGUAGE = "rust";
    // the id of the CodeLLDB debugger of native-dev
    private static final String CODELLDB_ID = "codelldb";

    private static final boolean BREAK_ON_PANIC = true;
    private static final boolean SKIP_STDLIB_IN_STEPPING = false;

    @Nullable
    private final String myCommitHash;
    @Nullable
    private final String myPrettyPrintersPath;
    private final Supplier<String> mySysroot;

    public RsDebugProcessConfigurationHelper(@Nonnull RsToolchainBase toolchain, @Nullable CargoProject cargoProject) {
        RustcInfo rustcInfo = cargoProject == null ? null : cargoProject.getRustcInfo();
        RustcVersion version = rustcInfo == null ? null : rustcInfo.getVersion();
        myCommitHash = version == null ? null : version.getCommitHash();
        Path prettyPrintersDir = RsPathManager.prettyPrintersDir();
        myPrettyPrintersPath = prettyPrintersDir != null && Files.isDirectory(prettyPrintersDir) ? prettyPrintersDir.toString() : null;
        mySysroot = LazyValue.nullable(() -> {
            if (rustcInfo != null) {
                return rustcInfo.getSysroot();
            }
            if (cargoProject == null) {
                return null;
            }
            return Rustc.create(toolchain).getSysroot(CargoProjectLocator.getWorkingDirectory(cargoProject));
        });
    }

    @Nonnull
    @Override
    public List<String> getSourceLanguages() {
        return List.of(RUST_LANGUAGE);
    }

    @Nonnull
    @Override
    public List<String> getInitCommands(@Nonnull NativeDebuggerProvider debugger) {
        String family = debugger.getFamilyId();
        List<String> commands = new ArrayList<>();
        loadRustcSources(family, commands);
        // CodeLLDB brings its own Rust formatters
        if (!CODELLDB_ID.equals(debugger.getId())) {
            loadPrettyPrinters(family, commands);
        }
        if (BREAK_ON_PANIC) {
            setBreakOnPanic(family, commands);
        }
        setSteppingFilters(family, commands);
        return commands;
    }

    private static void setBreakOnPanic(String family, List<String> commands) {
        switch (family) {
            case NativeDebuggerProvider.LLDB_FAMILY -> commands.add("breakpoint set -n rust_panic");
            case NativeDebuggerProvider.GDB_FAMILY -> {
                commands.add("set breakpoint pending on");
                commands.add("break rust_panic");
            }
            default -> {
            }
        }
    }

    private static void setSteppingFilters(String family, List<String> commands) {
        List<String> regexes = new ArrayList<>();
        if (SKIP_STDLIB_IN_STEPPING) {
            regexes.add("^(std|core|alloc)::.*");
        }
        String command = switch (family) {
            case NativeDebuggerProvider.LLDB_FAMILY -> "settings set target.process.thread.step-avoid-regexp";
            case NativeDebuggerProvider.GDB_FAMILY -> "skip -rfu";
            default -> null;
        };
        if (command == null) {
            return;
        }
        for (String regex : regexes) {
            commands.add(command + " " + regex);
        }
    }

    private void loadRustcSources(String family, List<String> commands) {
        if (myCommitHash == null) {
            return;
        }
        String sourceMapCommand = switch (family) {
            case NativeDebuggerProvider.LLDB_FAMILY -> "settings set target.source-map";
            case NativeDebuggerProvider.GDB_FAMILY -> "set substitute-path";
            default -> null;
        };
        if (sourceMapCommand == null) {
            return;
        }
        String sysroot = checkSysroot();
        if (sysroot == null) {
            return;
        }
        String rustcHash = systemDependentAndEscaped("/rustc/" + myCommitHash + "/");
        String rustcSources = systemDependentAndEscaped(sysroot + "/lib/rustlib/src/rust/");
        commands.add(sourceMapCommand + " \"" + rustcHash + "\" \"" + rustcSources + "\" ");
    }

    private void loadPrettyPrinters(String family, List<String> commands) {
        switch (family) {
            case NativeDebuggerProvider.LLDB_FAMILY -> loadLldbPrettyPrinters(commands);
            case NativeDebuggerProvider.GDB_FAMILY -> loadGdbPrettyPrinters(commands);
            default -> {
            }
        }
    }

    private void loadLldbPrettyPrinters(List<String> commands) {
        String basePath = compilerPrettyPrintersPath(LLDB_LOOKUP);
        if (basePath != null) {
            String lldbLookupPath = systemDependentAndEscaped(basePath + "/" + LLDB_LOOKUP + ".py");
            String lldbCommandsPath = systemDependentAndEscaped(basePath + "/lldb_commands");
            commands.add("command script import \"" + lldbLookupPath + "\" ");
            commands.add("command source \"" + lldbCommandsPath + "\" ");
            return;
        }
        if (myPrettyPrintersPath != null) {
            String path = systemDependentAndEscaped(myPrettyPrintersPath);
            commands.add("command script import \"" + path + "/lldb_formatters\" ");
        }
    }

    private void loadGdbPrettyPrinters(List<String> commands) {
        String path;
        String lookup;
        String basePath = compilerPrettyPrintersPath(COMPILER_GDB_LOOKUP);
        if (basePath != null) {
            path = systemDependentAndEscaped(basePath);
            lookup = COMPILER_GDB_LOOKUP;
        }
        else if (myPrettyPrintersPath != null) {
            path = systemDependentAndEscaped(myPrettyPrintersPath);
            lookup = GDB_LOOKUP;
        }
        else {
            return;
        }
        // Avoid multiline Python scripts due to https://youtrack.jetbrains.com/issue/CPP-9090
        commands.add("python " +
            "sys.path.insert(0, \"" + path + "\"); " +
            "import " + lookup + "; " +
            lookup + ".register_printers(gdb); ");
    }

    @Nullable
    private String compilerPrettyPrintersPath(String lookupModule) {
        String sysroot = checkSysroot();
        if (sysroot == null) {
            return null;
        }
        Path basePath = Path.of(sysroot, "lib", "rustlib", "etc");
        // MSVC toolchain does not contain Python pretty-printers.
        return Files.isRegularFile(basePath.resolve(lookupModule + ".py")) ? sysroot + "/lib/rustlib/etc" : null;
    }

    @Nullable
    private String checkSysroot() {
        String sysroot = mySysroot.get();
        if (sysroot == null) {
            LOG.warn("Can't find the Rust sysroot: the debugger shows raw values and no Rust standard library sources");
        }
        return sysroot;
    }

    @Nonnull
    private static String systemDependentAndEscaped(@Nonnull String value) {
        return StringUtil.escapeStringCharacters(FileUtil.toSystemDependentName(value));
    }
}
