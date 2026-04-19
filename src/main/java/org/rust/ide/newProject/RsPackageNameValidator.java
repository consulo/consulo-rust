/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;

import java.util.Set;

public final class RsPackageNameValidator {

    public static final RsPackageNameValidator INSTANCE = new RsPackageNameValidator();

    private RsPackageNameValidator() {
    }

    /**
     * See <a href="https://github.com/rust-lang/cargo/blob/2c6711155232b2d6271bb7147610077c3a8cee65/src/cargo/util/restricted_names.rs#L13">is_keyword</a> function
     */
    private static final Set<String> KEYWORDS_BLACKLIST = Set.of(
        "Self", "abstract", "as", "async", "await", "become", "box", "break", "const", "continue",
        "crate", "do", "dyn", "else", "enum", "extern", "false", "final", "fn", "for", "if",
        "impl", "in", "let", "loop", "macro", "match", "mod", "move", "mut", "override", "priv",
        "pub", "ref", "return", "self", "static", "struct", "super", "trait", "true", "try",
        "type", "typeof", "unsafe", "unsized", "use", "virtual", "where", "while", "yield"
    );

    /**
     * See <a href="https://github.com/rust-lang/cargo/blob/2c6711155232b2d6271bb7147610077c3a8cee65/src/cargo/util/restricted_names.rs#L35">is_conflicting_artifact_name</a> function
     */
    private static final Set<String> BINARY_BLACKLIST = Set.of("deps", "examples", "build", "incremental");

    private static final Set<String> WINDOWS_BLACKLIST = Set.of(
        "con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6", "com7",
        "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
    );

    @SuppressWarnings("UnstableApiUsage")
    @NlsContexts.DialogMessage
    @Nullable
    public static String validate(String name, boolean isBinary) {
        if (name.isEmpty()) {
            return RsBundle.message("dialog.message.package.name.can.t.be.empty");
        }
        if (KEYWORDS_BLACKLIST.contains(name) || "test".equals(name)) {
            return RsBundle.message("dialog.message.name.cannot.be.used.as.crate.name2", name);
        }
        if (isBinary && BINARY_BLACKLIST.contains(name)) {
            return RsBundle.message("dialog.message.name.cannot.be.used.as.crate.name", name);
        }
        if (Character.isDigit(name.charAt(0))) {
            return RsBundle.message("dialog.message.package.names.starting.with.digit.cannot.be.used.as.crate.name");
        }
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_') {
                return RsBundle.message("dialog.message.package.names.should.contain.only.letters.digits");
            }
        }
        if (SystemInfo.isWindows && WINDOWS_BLACKLIST.contains(name.toLowerCase())) {
            return RsBundle.message("dialog.message.name.reserved.windows.filename", name);
        }
        return null;
    }
}
