/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import io.github.z4kn4fein.semver.Version;
import io.github.z4kn4fein.semver.constraints.Constraint;
import io.github.z4kn4fein.semver.constraints.ConstraintExtensionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CrateVersionRequirement {
    private final List<Constraint> myRequirements;
    private final boolean myIsPinned;

    private CrateVersionRequirement(@NotNull List<Constraint> requirements) {
        myRequirements = requirements;
        boolean pinned = false;
        for (Constraint c : requirements) {
            if (c.toString().startsWith("=")) {
                pinned = true;
                break;
            }
        }
        myIsPinned = pinned;
    }

    public boolean isPinned() {
        return myIsPinned;
    }

    public boolean matches(@NotNull Version version) {
        for (Constraint requirement : myRequirements) {
            if (!ConstraintExtensionsKt.satisfiedBy(requirement, version)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static CrateVersionRequirement build(@NotNull String text) {
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

        List<Constraint> parsed = new ArrayList<>();
        for (String req : requirements) {
            Constraint constraint = ConstraintExtensionsKt.toConstraintOrNull(normalizeVersion(req));
            if (constraint == null) return null;
            parsed.add(constraint);
        }
        if (parsed.size() != requirements.size()) return null;

        return new CrateVersionRequirement(parsed);
    }

    @NotNull
    private static String normalizeVersion(@NotNull String version) {
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

    private static int countChar(@NotNull String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) count++;
        }
        return count;
    }
}
