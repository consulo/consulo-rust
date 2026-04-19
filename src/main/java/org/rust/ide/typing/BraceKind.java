/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import com.intellij.psi.tree.IElementType;

public final class BraceKind {
    private final char myChar;
    private final IElementType myTokenType;

    public BraceKind(char c, IElementType tokenType) {
        myChar = c;
        myTokenType = tokenType;
    }

    public char getChar() {
        return myChar;
    }

    public IElementType getTokenType() {
        return myTokenType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BraceKind braceKind = (BraceKind) o;
        return myChar == braceKind.myChar && myTokenType.equals(braceKind.myTokenType);
    }

    @Override
    public int hashCode() {
        int result = Character.hashCode(myChar);
        result = 31 * result + myTokenType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BraceKind(char=" + myChar + ", tokenType=" + myTokenType + ")";
    }
}
