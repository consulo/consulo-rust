/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.util.macros;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.psi.PsiFile;
import org.rust.lang.core.macros.RangeMap;

import java.util.ArrayList;
import java.util.List;

public class RsIntentionInsideMacroExpansionContext {
    private final PsiFile myOriginalFile;
    private final Document myDocumentCopy;
    private final RangeMap myRangeMap;
    private final RangeMarker myRootMacroBodyRange;
    private final List<RangeMarker> myChangedRanges;
    private boolean myFinished;
    private boolean myBroken;
    private boolean myApplyChangesToOriginalDoc;

    public RsIntentionInsideMacroExpansionContext(
        PsiFile originalFile,
        Document documentCopy,
        RangeMap rangeMap,
        RangeMarker rootMacroBodyRange
    ) {
        this(originalFile, documentCopy, rangeMap, rootMacroBodyRange, new ArrayList<>(), false, false, true);
    }

    public RsIntentionInsideMacroExpansionContext(
        PsiFile originalFile,
        Document documentCopy,
        RangeMap rangeMap,
        RangeMarker rootMacroBodyRange,
        List<RangeMarker> changedRanges,
        boolean finished,
        boolean broken,
        boolean applyChangesToOriginalDoc
    ) {
        this.myOriginalFile = originalFile;
        this.myDocumentCopy = documentCopy;
        this.myRangeMap = rangeMap;
        this.myRootMacroBodyRange = rootMacroBodyRange;
        this.myChangedRanges = changedRanges;
        this.myFinished = finished;
        this.myBroken = broken;
        this.myApplyChangesToOriginalDoc = applyChangesToOriginalDoc;
    }

    public PsiFile getOriginalFile() {
        return myOriginalFile;
    }

    public Document getDocumentCopy() {
        return myDocumentCopy;
    }

    public RangeMap getRangeMap() {
        return myRangeMap;
    }

    public RangeMarker getRootMacroBodyRange() {
        return myRootMacroBodyRange;
    }

    public List<RangeMarker> getChangedRanges() {
        return myChangedRanges;
    }

    public boolean isFinished() {
        return myFinished;
    }

    public void setFinished(boolean finished) {
        this.myFinished = finished;
    }

    public boolean isBroken() {
        return myBroken;
    }

    public void setBroken(boolean broken) {
        this.myBroken = broken;
    }

    public boolean isApplyChangesToOriginalDoc() {
        return myApplyChangesToOriginalDoc;
    }

    public void setApplyChangesToOriginalDoc(boolean applyChangesToOriginalDoc) {
        this.myApplyChangesToOriginalDoc = applyChangesToOriginalDoc;
    }

    public int getRootMacroCallBodyOffset() {
        return myRootMacroBodyRange.getStartOffset();
    }
}
