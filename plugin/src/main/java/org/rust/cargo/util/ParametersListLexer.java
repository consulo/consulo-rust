/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

// Copy of com.intellij.openapi.externalSystem.service.execution.cmd.ParametersListLexer,
// which is not present in all IDEs.
public class ParametersListLexer {
    private final String myText;
    private int myTokenStart = -1;
    private int index = 0;

    public ParametersListLexer(String myText) {
        this.myText = myText;
    }

    public int getTokenEnd() {
        assert myTokenStart >= 0;
        return index;
    }

    public String getCurrentToken() {
        return myText.substring(myTokenStart, index);
    }

    public boolean nextToken() {
        int i = index;

        while (i < myText.length() && Character.isWhitespace(myText.charAt(i))) {
            i++;
        }

        if (i == myText.length()) return false;

        myTokenStart = i;
        boolean isInQuote = false;

        do {
            char a = myText.charAt(i);
            if (!isInQuote && Character.isWhitespace(a)) break;
            if (a == '\\' && i + 1 < myText.length() && myText.charAt(i + 1) == '"') {
                i += 2;
            } else if (a == '"') {
                i++;
                isInQuote = !isInQuote;
            } else {
                i++;
            }
        } while (i < myText.length());

        index = i;
        return true;
    }
}
