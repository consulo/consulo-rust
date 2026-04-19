/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import org.jetbrains.annotations.NotNull;

public class DeclarationInfo {

    @NotNull
    private final String myPrefix;
    @NotNull
    private final String mySuffix;
    @NotNull
    private final String myValue;
    private final boolean myIsAmbiguous;

    public DeclarationInfo() {
        this("", "", "", false);
    }

    public DeclarationInfo(@NotNull String prefix) {
        this(prefix, "", "", false);
    }

    public DeclarationInfo(@NotNull String prefix, @NotNull String suffix, @NotNull String value, boolean isAmbiguous) {
        myPrefix = prefix;
        mySuffix = suffix;
        myValue = value;
        myIsAmbiguous = isAmbiguous;
    }

    @NotNull
    public String getPrefix() {
        return myPrefix;
    }

    @NotNull
    public String getSuffix() {
        return mySuffix;
    }

    @NotNull
    public String getValue() {
        return myValue;
    }

    public boolean isAmbiguous() {
        return myIsAmbiguous;
    }
}
