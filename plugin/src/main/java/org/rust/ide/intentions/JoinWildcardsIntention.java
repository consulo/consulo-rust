/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;

import java.util.ArrayList;
import java.util.List;
import consulo.localize.LocalizeValue;

public class JoinWildcardsIntention extends RsElementBaseIntentionAction<List<RsPatWild>> {
    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.replace.successive.with"));
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    @Override
    public List<RsPatWild> findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsPat patUnderCaret = PsiElementExt.ancestorStrict(element, RsPat.class);
        if (patUnderCaret == null) return null;
        if (patUnderCaret instanceof RsPatWild) {
            patUnderCaret = PsiElementExt.ancestorStrict(patUnderCaret, RsPat.class);
            if (patUnderCaret == null) return null;
        }

        List<RsPat> patList;
        if (patUnderCaret instanceof RsPatTup) {
            patList = ((RsPatTup) patUnderCaret).getPatList();
        } else if (patUnderCaret instanceof RsPatTupleStruct) {
            patList = ((RsPatTupleStruct) patUnderCaret).getPatList();
        } else if (patUnderCaret instanceof RsPatSlice) {
            patList = ((RsPatSlice) patUnderCaret).getPatList();
        } else {
            return null;
        }

        // Unavailable if `..` is already there
        for (RsPat pat : patList) {
            if (pat instanceof RsPatRest) return null;
        }

        List<RsPatWild> patWildSeq = new ArrayList<>();
        int patListSize = patList.size();
        for (int i = 0; i <= patListSize; i++) {
            RsPat pat = i < patListSize ? patList.get(i) : null;
            if (pat instanceof RsPatWild) {
                patWildSeq.add((RsPatWild) pat);
            } else {
                if (!patWildSeq.isEmpty()) {
                    int seqStart = PsiElementExt.getStartOffset(patWildSeq.get(0));
                    int seqEnd = PsiElementExt.getEndOffset(patWildSeq.get(patWildSeq.size() - 1));
                    TextRange patWildSeqRange = new TextRange(seqStart, seqEnd);
                    if (patWildSeqRange.containsOffset(PsiElementExt.getStartOffset(element))) {
                        if (patWildSeq.size() == 1) {
                            setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.replace.with")));
                        } else {
                            setText(getFamilyName());
                        }
                        if (!PsiModificationUtil.canReplaceAll(new ArrayList<>(patWildSeq))) return null;
                        return new ArrayList<>(patWildSeq);
                    }
                }
                patWildSeq.clear();
            }
        }
        return null;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull List<RsPatWild> ctx) {
        int startOffset = PsiElementExt.getStartOffset(ctx.get(0));
        int endOffset = PsiElementExt.getEndOffset(ctx.get(ctx.size() - 1));
        editor.getDocument().replaceString(startOffset, endOffset, "..");
        editor.getCaretModel().moveToOffset(startOffset + 2);
    }
}
