/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

public enum MacroExpansionContext {
    EXPR, PAT, TYPE, STMT, META_ITEM_VALUE, ITEM;

    public CharSequence prepareExpandedTextForParsing(CharSequence expandedText) {
        switch (this) {
            case EXPR: return "const C:T=" + expandedText + ";";
            case PAT: return "fn f(" + expandedText + ":())";
            case TYPE: return "type T=" + expandedText + ";";
            case STMT: return "fn f(){" + expandedText + "}";
            case META_ITEM_VALUE: return "#[a=" + expandedText + "]fn f(){}";
            case ITEM: return expandedText;
            default: throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public int getExpansionFileStartOffset() {
        switch (this) {
            case EXPR: return 10;
            case PAT: return 5;
            case TYPE: return 7;
            case STMT: return 7;
            case META_ITEM_VALUE: return 4;
            case ITEM: return 0;
            default: throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
