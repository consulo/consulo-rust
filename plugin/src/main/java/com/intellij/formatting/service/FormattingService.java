package com.intellij.formatting.service;

import consulo.language.codeStyle.FormattingContext;
import consulo.language.psi.PsiFile;

import java.util.Set;

/** IntelliJ-compat stub — formatting service extension point. */
public interface FormattingService {
    enum Feature {
        FORMAT_FRAGMENTS,
        AD_HOC_FORMATTING,
        FORMAT_SELECTION,
        OPTIMIZE_IMPORTS
    }
    Set<Feature> getFeatures();
    boolean canFormat(PsiFile file);
}
