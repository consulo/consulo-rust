package com.intellij.ide;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
/** IntelliJ-compat stub — IconProvider extension point. */
public abstract class IconProvider {
    public abstract Image getIcon(PsiElement element, int flags);
}
