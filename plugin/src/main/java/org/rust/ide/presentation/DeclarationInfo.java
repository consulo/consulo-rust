/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import jakarta.annotation.Nonnull;

public class DeclarationInfo {

    @Nonnull
    private final String myPrefix;
    @Nonnull
    private final String mySuffix;
    @Nonnull
    private final String myValue;
    private final boolean myIsAmbiguous;

    public DeclarationInfo() {
        this("", "", "", false);
    }

    public DeclarationInfo(@Nonnull String prefix) {
        this(prefix, "", "", false);
    }

    public DeclarationInfo(@Nonnull String prefix, @Nonnull String suffix, @Nonnull String value, boolean isAmbiguous) {
        myPrefix = prefix;
        mySuffix = suffix;
        myValue = value;
        myIsAmbiguous = isAmbiguous;
    }

    @Nonnull
    public String getPrefix() {
        return myPrefix;
    }

    @Nonnull
    public String getSuffix() {
        return mySuffix;
    }

    @Nonnull
    public String getValue() {
        return myValue;
    }

    public boolean isAmbiguous() {
        return myIsAmbiguous;
    }
}
