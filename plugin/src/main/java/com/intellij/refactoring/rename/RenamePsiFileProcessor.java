package com.intellij.refactoring.rename;
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor;
/** IntelliJ-compat stub — RenamePsiFileProcessor derivation from RenamePsiElementProcessor. */
public class RenamePsiFileProcessor extends RenamePsiElementProcessor {
    @Override public boolean canProcessElement(consulo.language.psi.PsiElement element) {
        return element instanceof consulo.language.psi.PsiFile;
    }
}
