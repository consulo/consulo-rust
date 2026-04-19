/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.TokenType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.doc.DocElementUtil;
import org.rust.lang.doc.psi.RsDocCodeFence;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.doc.psi.RsDocElementTypes;
import org.rust.lang.doc.psi.RsDocGap;
import org.rust.openapiext.Testmark;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.rust.lang.core.psi.ext.RsElementExtUtil;

public class DoctestInfo {

    private static final Pattern LANG_SPLIT_REGEX = Pattern.compile("[^\\w-]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final List<String> RUST_LANG_ALIASES = List.of(
        "rust", "allow_fail", "should_panic", "no_run", "test_harness",
        "edition2015", "edition2018", "edition2021"
    );

    private final int myDocIndent;
    private final int myFenceIndent;
    private final List<Content> myContents;
    private final String myText;

    private DoctestInfo(int docIndent, int fenceIndent, @NotNull List<Content> contents, @NotNull String text) {
        myDocIndent = docIndent;
        myFenceIndent = fenceIndent;
        myContents = contents;
        myText = text;
    }

    @NotNull
    public String getText() {
        return myText;
    }

    @NotNull
    public List<TextRange> getRangesForInjection() {
        List<TextRange> result = new ArrayList<>();
        for (Content c : myContents) {
            if (!(c instanceof Content.DocData)) continue;
            PsiElement psi = ((Content.DocData) c).myPsi;
            TextRange range = psi.getTextRangeInParent();
            int add = range.getEndOffset() < myText.length() ? 1 : 0;
            int startOffset = CharArrayUtil.shiftForward(myText, range.getStartOffset(), range.getStartOffset() + myFenceIndent, " \t");
            if (startOffset < range.getEndOffset()) {
                result.add(new TextRange(startOffset, range.getEndOffset() + add));
            }
        }
        return result;
    }

    @NotNull
    public List<TextRange> getRangesForBackgroundHighlighting() {
        List<TextRange> result = new ArrayList<>();
        for (Content c : myContents) {
            if (c instanceof Content.DocData) {
                PsiElement psi = ((Content.DocData) c).myPsi;
                TextRange range = psi.getTextRangeInParent();
                int add = range.getEndOffset() < myText.length() ? 1 : 0;
                result.add(new TextRange(range.getStartOffset() - myDocIndent, range.getEndOffset() + add));
            } else if (c instanceof Content.EmptyLine) {
                result.add(((Content.EmptyLine) c).myRange);
            }
        }
        return result;
    }

    @Nullable
    public static DoctestInfo fromCodeFence(@NotNull RsDocCodeFence codeFence) {
        if (!RsProjectSettingsServiceUtil.getRustSettings(codeFence.getProject()).getDoctestInjectionEnabled()) return null;
        if (!RsElementUtil.getContainingCrate(codeFence).getAreDoctestsEnabled()) return null;
        if (hasUnbalancedCodeFencesBefore(codeFence)) return null;

        PsiElement langElement = codeFence.getLang();
        String lang = langElement != null ? langElement.getText() : "";
        String[] parts = LANG_SPLIT_REGEX.split(lang);
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!RUST_LANG_ALIASES.contains(part)) return null;
        }

        PsiElement start = codeFence.getStart();
        String startText = start.getText();
        int fenceIndent = 0;
        for (int i = 0; i < startText.length(); i++) {
            char ch = startText.charAt(i);
            if (ch == '`' || ch == '~') {
                fenceIndent = i;
                break;
            }
        }

        PsiElement prevLeaf = PsiTreeUtil.prevLeaf(codeFence);
        int docIndent;
        if (prevLeaf instanceof PsiWhiteSpace && PsiTreeUtil.prevLeaf(prevLeaf) instanceof RsDocGap) {
            docIndent = prevLeaf.getTextLength();
        } else {
            docIndent = 0;
        }

        boolean isAfterNewLine = false;
        List<Content> contents = new ArrayList<>();

        for (PsiElement element : RsElementExtUtil.getChildrenWithLeaves(codeFence)) {
            if (element.getNode().getElementType() == RsDocElementTypes.DOC_DATA) {
                isAfterNewLine = false;
                contents.add(new Content.DocData(element));
            } else if (element.getNode().getElementType() == TokenType.WHITE_SPACE) {
                String text = element.getText();
                int prevIdx = -1;
                int idx = text.indexOf("\n");
                while (idx != -1) {
                    if (isAfterNewLine) {
                        int startOffset = prevIdx != -1 ? prevIdx + 1 : 0;
                        int endOffset = idx + 1;
                        TextRange range = new TextRange(startOffset, endOffset).shiftRight(element.getStartOffsetInParent());
                        contents.add(new Content.EmptyLine(range));
                    }
                    isAfterNewLine = true;
                    prevIdx = idx;
                    idx = text.indexOf("\n", idx + 1);
                }
            }
        }

        return new DoctestInfo(docIndent, fenceIndent, contents, codeFence.getText());
    }

    public static boolean hasUnbalancedCodeFencesBefore(@NotNull RsDocCodeFence context) {
        RsDocComment containingDoc = context.getContainingDoc();
        PsiElement docOwner = containingDoc.getOwner();
        if (docOwner == null) return false;
        for (PsiElement docElement : org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil.docElements((org.rust.lang.core.psi.ext.RsDocAndAttributeOwner) docOwner, true)) {
            if (!(docElement instanceof RsDocComment)) continue;
            if (docElement == containingDoc) return false;
            RsDocComment docComment = (RsDocComment) docElement;
            for (RsDocCodeFence fence : docComment.getCodeFences()) {
                if (fence.getEnd() == null) {
                    Testmarks.UNBALANCED_CODE_FENCE.hit();
                    return true;
                }
            }
        }
        return false;
    }

    private static abstract class Content {
        static class DocData extends Content {
            final PsiElement myPsi;

            DocData(@NotNull PsiElement psi) {
                myPsi = psi;
            }
        }

        static class EmptyLine extends Content {
            final TextRange myRange;

            EmptyLine(@NotNull TextRange range) {
                myRange = range;
            }
        }
    }

    public static final class Testmarks {
        public static final Testmark UNBALANCED_CODE_FENCE = new Testmark();

        private Testmarks() {}
    }
}
