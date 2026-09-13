/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.inspections.RsDiagnostic;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.utils.RsFeatureDiagnostic;

import java.util.Collections;
import java.util.List;

/**
 * Reports uses of compiler features that the toolchain of the containing crate does not provide.
 */
public final class CompilerFeatureCheck {
    private CompilerFeatureCheck() {
    }

    public static void check(
        @Nonnull CompilerFeature feature,
        @Nonnull RsAnnotationHolder holder,
        @Nonnull PsiElement element,
        @Nonnull String presentableFeatureName
    ) {
        check(feature, holder, element, presentableFeatureName, Collections.emptyList(), Collections.emptyList());
    }

    public static void check(
        @Nonnull CompilerFeature feature,
        @Nonnull RsAnnotationHolder holder,
        @Nonnull PsiElement element,
        @Nonnull String presentableFeatureName,
        @Nonnull List<LocalQuickFix> experimentalFixes,
        @Nonnull List<LocalQuickFix> removedFixes
    ) {
        check(
            feature,
            holder,
            element,
            null,
            RsBundle.message("inspection.message.experimental", presentableFeatureName),
            RsBundle.message("inspection.message.has.been.removed2", presentableFeatureName),
            experimentalFixes,
            removedFixes
        );
    }

    public static void check(
        @Nonnull CompilerFeature feature,
        @Nonnull RsAnnotationHolder holder,
        @Nonnull PsiElement startElement,
        @Nullable PsiElement endElement,
        @Nonnull String experimentalMessage,
        @Nonnull String removedMessage,
        @Nonnull List<LocalQuickFix> experimentalFixes,
        @Nonnull List<LocalQuickFix> removedFixes
    ) {
        RsDiagnostic diagnostic =
            prepare(feature, startElement, endElement, experimentalMessage, removedMessage, experimentalFixes, removedFixes);
        if (diagnostic != null) {
            RsDiagnostic.addToHolder(diagnostic, holder);
        }
    }

    public static void check(
        @Nonnull CompilerFeature feature,
        @Nonnull AnnotationHolder holder,
        @Nonnull PsiElement element,
        @Nonnull String presentableFeatureName
    ) {
        check(feature, holder, element, presentableFeatureName, Collections.emptyList(), Collections.emptyList());
    }

    public static void check(
        @Nonnull CompilerFeature feature,
        @Nonnull AnnotationHolder holder,
        @Nonnull PsiElement element,
        @Nonnull String presentableFeatureName,
        @Nonnull List<LocalQuickFix> experimentalFixes,
        @Nonnull List<LocalQuickFix> removedFixes
    ) {
        check(
            feature,
            holder,
            element,
            null,
            RsBundle.message("inspection.message.experimental", presentableFeatureName),
            RsBundle.message("inspection.message.has.been.removed2", presentableFeatureName),
            experimentalFixes,
            removedFixes
        );
    }

    public static void check(
        @Nonnull CompilerFeature feature,
        @Nonnull AnnotationHolder holder,
        @Nonnull PsiElement startElement,
        @Nullable PsiElement endElement,
        @Nonnull String experimentalMessage,
        @Nonnull String removedMessage,
        @Nonnull List<LocalQuickFix> experimentalFixes,
        @Nonnull List<LocalQuickFix> removedFixes
    ) {
        RsDiagnostic diagnostic =
            prepare(feature, startElement, endElement, experimentalMessage, removedMessage, experimentalFixes, removedFixes);
        if (diagnostic != null) {
            RsDiagnostic.addToHolder(diagnostic, holder);
        }
    }

    @Nullable
    private static RsDiagnostic prepare(
        @Nonnull CompilerFeature feature,
        @Nonnull PsiElement startElement,
        @Nullable PsiElement endElement,
        @Nonnull String experimentalMessage,
        @Nonnull String removedMessage,
        @Nonnull List<LocalQuickFix> experimentalFixes,
        @Nonnull List<LocalQuickFix> removedFixes
    ) {
        RsFeatureDiagnostic problem = feature.checkAvailability(startElement, endElement, experimentalMessage, removedMessage);
        if (problem == null) {
            return null;
        }
        List<LocalQuickFix> extraFixes =
            problem.getKind() == RsFeatureDiagnostic.Kind.REMOVED ? removedFixes : experimentalFixes;
        return RsDiagnostic.of(problem, extraFixes);
    }
}
