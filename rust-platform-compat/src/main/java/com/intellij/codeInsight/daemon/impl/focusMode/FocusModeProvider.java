package com.intellij.codeInsight.daemon.impl.focusMode;
import consulo.document.util.Segment;
import consulo.language.psi.PsiFile;
import java.util.List;
public interface FocusModeProvider {
    List<? extends Segment> calcFocusZones(PsiFile psiFile);
}
