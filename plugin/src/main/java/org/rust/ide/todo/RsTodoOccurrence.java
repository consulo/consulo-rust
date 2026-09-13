/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.todo;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.search.IndexPatternOccurrence;
import org.rust.lang.core.psi.impl.RsFile;

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
