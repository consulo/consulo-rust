/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.todo;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.IndexPattern;
import com.intellij.psi.search.IndexPatternOccurrence;
import org.rust.lang.core.psi.RsFile;

public class RsTodoOccurrence implements IndexPatternOccurrence {

    private final RsFile myFile;
    private final TextRange myTextRange;
    private final IndexPattern myPattern;

    public RsTodoOccurrence(RsFile file, TextRange textRange, IndexPattern pattern) {
        myFile = file;
        myTextRange = textRange;
        myPattern = pattern;
    }

    @Override
    public PsiFile getFile() {
        return myFile;
    }

    @Override
    public TextRange getTextRange() {
        return myTextRange;
    }

    @Override
    public IndexPattern getPattern() {
        return myPattern;
    }
}
