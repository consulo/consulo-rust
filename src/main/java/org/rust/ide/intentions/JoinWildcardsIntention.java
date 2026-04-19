/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;

import java.util.ArrayList;
import java.util.List;

public class JoinWildcardsIntention extends RsElementBaseIntentionAction<List<RsPatWild>> {
    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.replace.successive.with");
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    @Override
    public List<RsPatWild> findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
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
                            setText(RsBundle.message("intention.name.replace.with"));
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
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull List<RsPatWild> ctx) {
        int startOffset = PsiElementExt.getStartOffset(ctx.get(0));
        int endOffset = PsiElementExt.getEndOffset(ctx.get(ctx.size() - 1));
        editor.getDocument().replaceString(startOffset, endOffset, "..");
        editor.getCaretModel().moveToOffset(startOffset + 2);
    }
}
