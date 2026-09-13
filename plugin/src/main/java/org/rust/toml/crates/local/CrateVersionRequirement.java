/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import io.github.milkdrinkers.javasemver.Range;
import io.github.milkdrinkers.javasemver.Version;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A Cargo dependency version requirement, such as {@code ^1.2}, {@code ~0.3} or {@code >=1.0, <2.0}.
 * A comma separated requirement is a conjunction: a version matches only when every part accepts it.
 */
public class CrateVersionRequirement {
    private final List<Range> myRequirements;
    private final boolean myIsPinned;

    private CrateVersionRequirement(@Nonnull List<Range> requirements, boolean pinned) {
        myRequirements = requirements;
        myIsPinned = pinned;
    }

    public boolean isPinned() {
        return myIsPinned;
    }

    public boolean matches(@Nonnull Version version) {
        for (Range requirement : myRequirements) {
            if (!requirement.contains(version)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static CrateVersionRequirement build(@Nonnull String text) {
        String[] parts = text.split(",");
        List<String> requirements = new ArrayList<>();
        for (String part : parts) {
            requirements.add(part.trim());
        }
        if (requirements.size() > 1) {
            for (String req : requirements) {
                if (req.isEmpty()) return null;
            }
        }

        List<Range> parsed = new ArrayList<>();
        boolean pinned = false;
        for (String req : requirements) {
            String normalized = normalizeVersion(req);
            Optional<Range> range = Range.parseOptional(normalized);
            if (range.isEmpty()) return null;
            parsed.add(range.get());
            if (normalized.startsWith("=")) {
                pinned = true;
            }
        }
        if (parsed.size() != requirements.size()) return null;

        return new CrateVersionRequirement(parsed, pinned);
    }

    @Nonnull
    private static String normalizeVersion(@Nonnull String version) {
        if (version.isBlank()) return version;

        String normalized = version;
        char first = normalized.charAt(0);
        if (first == '<' || first == '>' || first == '=') {
            while (countChar(normalized, '.') < 2) {
                normalized += ".0";
            }
        }

        if (normalized.charAt(0) >= '0' && normalized.charAt(0) <= '9' && !normalized.contains("*")) {
            return "^" + normalized;
        } else {
            return normalized;
        }
    }

    private static int countChar(@Nonnull String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) count++;
        }
        return count;
    }
}
