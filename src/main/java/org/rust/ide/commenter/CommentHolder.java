/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.commenter;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.generation.CommenterDataHolder;
import com.intellij.psi.PsiFile;
import org.rust.lang.RsLanguage;

public class CommentHolder extends CommenterDataHolder {
    private final PsiFile myFile;

    public CommentHolder(PsiFile file) {
        this.myFile = file;
    }

    public PsiFile getFile() {
        return myFile;
    }

    public boolean useSpaceAfterLineComment() {
        return CodeStyle.getLanguageSettings(myFile, RsLanguage.INSTANCE).LINE_COMMENT_ADD_SPACE;
    }
}
