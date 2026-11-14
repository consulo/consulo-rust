/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.protobuf;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The Rust names prost-build gives to protobuf names - a port of its {@code ident.rs}, with heck's word splitting. */
public final class ProstNames {
    private static final Set<String> RAW_KEYWORDS = Set.of(
        "as", "break", "const", "continue", "else", "enum", "false", "fn", "for", "if", "impl", "in", "let", "loop",
        "match", "mod", "move", "mut", "pub", "ref", "return", "static", "struct", "trait", "true", "type", "unsafe",
        "use", "where", "while", "dyn", "abstract", "become", "box", "do", "final", "macro", "override", "priv",
        "typeof", "unsized", "virtual", "yield", "async", "await", "try", "gen"
    );
    private static final Set<String> SUFFIXED_KEYWORDS = Set.of("_", "super", "self", "Self", "extern", "crate");

    private ProstNames() {
    }

    /** A field, module or method name: {@code agentId} is {@code agent_id}. */
    @Nonnull
    public static String toSnake(@Nonnull String name) {
        return sanitize(String.join("_", words(name, false)));
    }

    /** A type or variant name: {@code web_socket} is {@code WebSocket}. */
    @Nonnull
    public static String toUpperCamel(@Nonnull String name) {
        return sanitize(String.join("", words(name, true)));
    }

    /** The name of an enum value with the name of its enum stripped, as prost-build does by default. */
    @Nonnull
    public static String enumValueName(@Nonnull String enumName, @Nonnull String valueName) {
        String prefix = String.join("", words(enumName, true));
        String name = String.join("", words(valueName, true));
        String stripped = name.startsWith(prefix) ? name.substring(prefix.length()) : name;
        if (stripped.isEmpty() || !Character.isUpperCase(stripped.charAt(0))) stripped = name;
        return sanitize(stripped);
    }

    /** A raw identifier for a keyword, an underscore after the names that cannot be raw. */
    @Nonnull
    public static String sanitize(@Nonnull String ident) {
        if (RAW_KEYWORDS.contains(ident)) return "r#" + ident;
        if (SUFFIXED_KEYWORDS.contains(ident)) return ident + "_";
        if (!ident.isEmpty() && Character.isDigit(ident.charAt(0))) return "_" + ident;
        return ident;
    }

    /**
     * The words of {@code name} like heck splits them: at non-alphanumeric characters, before an upper case letter
     * that follows a lower case one, and before the last upper case letter of an acronym followed by a lower case one.
     */
    @Nonnull
    private static List<String> words(@Nonnull String name, boolean capitalize) {
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                flush(word, words, capitalize);
                continue;
            }
            if (word.length() > 0 && Character.isUpperCase(c)) {
                char previous = name.charAt(i - 1);
                boolean afterLower = Character.isLowerCase(previous) || Character.isDigit(previous);
                boolean acronymEnd = Character.isUpperCase(previous) && i + 1 < name.length()
                    && Character.isLowerCase(name.charAt(i + 1));
                if (afterLower || acronymEnd) flush(word, words, capitalize);
            }
            word.append(c);
        }
        flush(word, words, capitalize);
        return words;
    }

    private static void flush(@Nonnull StringBuilder word, @Nonnull List<String> words, boolean capitalize) {
        if (word.length() == 0) return;
        String lower = word.toString().toLowerCase();
        words.add(capitalize ? Character.toUpperCase(lower.charAt(0)) + lower.substring(1) : lower);
        word.setLength(0);
    }
}
