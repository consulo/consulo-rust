/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.resolve.KnownItems;

import java.util.HashMap;
import java.util.Map;

public enum FormatTraitType {
    Display(KnownItems::getDisplay, "", null, null),
    Debug(KnownItems::getDebug, "?", "x?", "X?"),
    Octal(KnownItems::getOctal, "o", null, null),
    LowerHex(KnownItems::getLowerHex, "x", null, null),
    UpperHex(KnownItems::getUpperHex, "X", null, null),
    Pointer(KnownItems::getPointer, "p", null, null),
    Binary(KnownItems::getBinary, "b", null, null),
    LowerExp(KnownItems::getLowerExp, "e", null, null),
    UpperExp(KnownItems::getUpperExp, "E", null, null);

    @FunctionalInterface
    public interface TraitResolver {
        @Nullable
        RsTraitItem resolve(@NotNull KnownItems knownItems);
    }

    @NotNull
    private final TraitResolver myResolver;
    @NotNull
    private final String[] myNames;

    FormatTraitType(@NotNull TraitResolver resolver, @Nullable String name1, @Nullable String name2, @Nullable String name3) {
        this.myResolver = resolver;
        int count = 0;
        if (name1 != null) count++;
        if (name2 != null) count++;
        if (name3 != null) count++;
        this.myNames = new String[count];
        int idx = 0;
        if (name1 != null) myNames[idx++] = name1;
        if (name2 != null) myNames[idx++] = name2;
        if (name3 != null) myNames[idx] = name3;
    }

    @NotNull
    public String[] getNames() {
        return myNames;
    }

    @Nullable
    public RsTraitItem resolveTrait(@NotNull KnownItems knownItems) {
        return myResolver.resolve(knownItems);
    }

    private static final Map<String, FormatTraitType> NAME_TO_TRAIT_MAP = new HashMap<>();
    static {
        for (FormatTraitType trait : values()) {
            for (String name : trait.myNames) {
                NAME_TO_TRAIT_MAP.put(name, trait);
            }
        }
    }

    @Nullable
    public static FormatTraitType forString(@NotNull String name) {
        return NAME_TO_TRAIT_MAP.get(name);
    }
}
