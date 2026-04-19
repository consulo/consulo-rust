/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.SimpleMultiLineTextEscaper;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.doc.psi.RsDocCodeFence;
import org.rust.lang.doc.psi.RsDocCodeFenceLang;
import org.rust.lang.doc.psi.RsDocCodeFenceStartEnd;
import org.rust.lang.doc.psi.RsDocKind;

import java.util.List;

public class RsDocCodeFenceImpl extends RsDocElementImpl implements RsDocCodeFence {

    public RsDocCodeFenceImpl(@NotNull IElementType type) {
        super(type);
    }

    @Override
    public boolean isValidHost() {
        return true;
    }

    @Override
    @NotNull
    public RsDocCodeFenceStartEnd getStart() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocCodeFenceStartEnd.class));
    }

    @Override
    @Nullable
    public RsDocCodeFenceStartEnd getEnd() {
        List<RsDocCodeFenceStartEnd> children = PsiTreeUtil.getChildrenOfTypeAsList(this, RsDocCodeFenceStartEnd.class);
        return children.size() > 1 ? children.get(1) : null;
    }

    @Override
    @Nullable
    public RsDocCodeFenceLang getLang() {
        return PsiTreeUtil.getChildOfType(this, RsDocCodeFenceLang.class);
    }

    /**
     * Handles changes in PSI injected to the comment (see {@code RsDoctestLanguageInjector}).
     * It is not used on typing. Instead, it's used on direct PSI changes (performed by
     * intentions/quick fixes).
     * <p>
     * Each line of doc comment should start with some prefix (see {@link RsDocKind#getInfix()}). For example, with {@code ///}.
     * But if some intention inserts newline to PSI, there will not be such prefix after that newline.
     * Here we ensure that every comment line is started from appropriate prefix.
     */
    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        RsDocKind docKind = RsDocKind.of(getContainingDoc().getNode().getElementType());
        String infix = docKind.getInfix();

        PsiElement prevSibling = RsPsiJavaUtil.getPrevNonWhitespaceSibling(this); // Should be an `infix` (e.g. `///`)

        StringBuilder newText = new StringBuilder();

        if (prevSibling != null && !prevSibling.getText().equals(docKind.getPrefix())) {
            // `newText` must be parsed in an empty file, so append a prefix if it differs from `infix` (e.g. `/**`)
            newText.append(docKind.getPrefix());

            // Then add a proper whitespace between the prefix (`/**`) and the first (`*`)
            PsiElement prevPrevSibling = prevSibling.getPrevSibling();
            if (prevPrevSibling instanceof PsiWhiteSpace) {
                newText.append(prevPrevSibling.getText());
            } else {
                newText.append("\n");
            }
        }

        newText.append(docKind.getInfix());

        // Add a whitespace between `infix` and a code fence start (e.g. between "///" and "```").
        // The whitespace affects markdown escaping, hence markdown parsing
        if (prevSibling != null && prevSibling.getNextSibling() != this) {
            newText.append(prevSibling.getNextSibling().getText());
        }

        String prevIndent = "";
        int index = 0;
        while (index < text.length()) {
            int linebreakIndex = text.indexOf("\n", index);
            if (linebreakIndex == -1) {
                newText.append(text, index, text.length());
                break;
            } else {
                int nextLineStart = linebreakIndex + 1;
                newText.append(text, index, nextLineStart);
                index = nextLineStart;

                int firstNonWhitespace = CharArrayUtil.shiftForward(text, nextLineStart, " \t");
                if (firstNonWhitespace == text.length()) continue;
                boolean isStartCorrect = text.startsWith(infix, firstNonWhitespace) ||
                    (docKind.isBlock() && text.startsWith("*/", firstNonWhitespace));
                if (!isStartCorrect) {
                    newText.append(prevIndent);
                    newText.append(infix);
                    newText.append(" ");
                } else {
                    prevIndent = text.substring(nextLineStart, firstNonWhitespace);
                }
            }
        }

        if (docKind.isBlock() && !newText.toString().endsWith("*/")) {
            newText.append("\n*/");
        }

        // There are some problems with indentation if we just use replaceWithText(text).
        // copied from PsiCommentManipulator
        RsPsiFactory psiFactory = new RsPsiFactory(getProject(), true);
        PsiElement fromText = psiFactory.createFile(newText.toString());
        RsDocCodeFenceImpl newElement = PsiTreeUtil.findChildOfType(fromText, RsDocCodeFenceImpl.class, false);
        if (newElement == null) {
            throw new IllegalStateException(newText.toString());
        }
        return (RsDocCodeFenceImpl) replace(newElement);
    }

    @Override
    @NotNull
    public LiteralTextEscaper<RsDocCodeFenceImpl> createLiteralTextEscaper() {
        return new SimpleMultiLineTextEscaper<>(this);
    }
}
