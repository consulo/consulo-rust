package com.intellij.codeInsight.hints.presentation;

import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.psi.PsiElement;

import java.util.function.Supplier;

/** IntelliJ-compat stub. */
public class PresentationFactory {
    public PresentationFactory(Object editor) {}
    public InlayPresentation text(String text) { return null; }
    public InlayPresentation smallText(String text) { return null; }
    public InlayPresentation roundWithBackground(InlayPresentation p) { return p; }
    public InlayPresentation inset(InlayPresentation p, int left, int right, int top, int bottom) { return p; }
    public InlayPresentation container(InlayPresentation p) { return p; }
    public InlayPresentation collapsible(InlayPresentation prefix, InlayPresentation collapsed,
                                          Supplier<InlayPresentation> content, InlayPresentation suffix, boolean startWithPlaceholder) { return collapsed; }
    public InlayPresentation psiSingleReference(InlayPresentation base, Supplier<PsiElement> resolver) { return base; }
    public InlayPresentation seq(InlayPresentation... presentations) { return presentations.length > 0 ? presentations[0] : null; }
    public InlayPresentation withReferenceAttributes(InlayPresentation p) { return p; }
    public InlayPresentation onClick(InlayPresentation p, Object mouseButton, Object onClickAction) { return p; }
    public InlayPresentation mouseHandling(InlayPresentation p, Object handler) { return p; }
    public InlayPresentation changeOnHover(InlayPresentation onHover, Supplier<InlayPresentation> normal) { return normal.get(); }
    public InlayPresentation referenceOnHover(InlayPresentation p, Object handler) { return p; }
    public InlayPresentation icon(Object icon) { return null; }
    public InlayPresentation folding(InlayPresentation placeholder, Supplier<InlayPresentation> content) { return placeholder; }
    public InlayPresentation withTooltip(String tooltip, InlayPresentation p) { return p; }
    public InlayPresentation withCursorOnHover(InlayPresentation p, java.awt.Cursor cursor) { return p; }
}
