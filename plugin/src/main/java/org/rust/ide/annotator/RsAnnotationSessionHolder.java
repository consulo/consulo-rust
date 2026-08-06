/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.AnnotationSession;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

/**
 * An {@link AnnotationHolder} that carries nothing but the {@link AnnotationSession}.
 * <p>
 * {@code AnnotationHolderImpl} is platform-internal, and the plugin needs a holder in exactly one
 * place that does not annotate: {@code RsCfgDisabledCodeAnnotator.shouldHighlightAsCfsDisabled}
 * reads the current crate off the session. Annotators are actually run through
 * {@code LanguageEditorInternalHelperImpl.runAnnotator}, which builds its own real holder.
 * <p>
 * The annotation-creating methods therefore throw: reaching them would mean this holder escaped the
 * session-lookup path it was written for, and silently dropping annotations would hide that.
 */
public class RsAnnotationSessionHolder implements AnnotationHolder {

    @Nonnull
    private final AnnotationSession mySession;

    public RsAnnotationSessionHolder(@Nonnull AnnotationSession session) {
        mySession = session;
    }

    @Nonnull
    @Override
    public AnnotationSession getCurrentAnnotationSession() {
        return mySession;
    }

    @Override
    public boolean isBatchMode() {
        return false;
    }

    private static Annotation unsupported() {
        throw new UnsupportedOperationException(
            "RsAnnotationSessionHolder only carries the AnnotationSession; run annotators via "
                + "LanguageEditorInternalHelperImpl.runAnnotator instead");
    }

    @Override
    public Annotation createErrorAnnotation(@Nonnull PsiElement elt, String message) {
        return unsupported();
    }

    @Override
    public Annotation createErrorAnnotation(@Nonnull ASTNode node, String message) {
        return unsupported();
    }

    @Override
    public Annotation createErrorAnnotation(@Nonnull TextRange range, String message) {
        return unsupported();
    }

    @Override
    public Annotation createWarningAnnotation(@Nonnull PsiElement elt, String message) {
        return unsupported();
    }

    @Override
    public Annotation createWarningAnnotation(@Nonnull ASTNode node, String message) {
        return unsupported();
    }

    @Override
    public Annotation createWarningAnnotation(@Nonnull TextRange range, String message) {
        return unsupported();
    }

    @Override
    public Annotation createWeakWarningAnnotation(@Nonnull PsiElement elt, String message) {
        return unsupported();
    }

    @Override
    public Annotation createWeakWarningAnnotation(@Nonnull ASTNode node, String message) {
        return unsupported();
    }

    @Override
    public Annotation createWeakWarningAnnotation(@Nonnull TextRange range, String message) {
        return unsupported();
    }

    @Override
    public Annotation createInfoAnnotation(@Nonnull PsiElement elt, String message) {
        return unsupported();
    }

    @Override
    public Annotation createInfoAnnotation(@Nonnull ASTNode node, String message) {
        return unsupported();
    }

    @Override
    public Annotation createInfoAnnotation(@Nonnull TextRange range, String message) {
        return unsupported();
    }

    @Override
    public Annotation createAnnotation(@Nonnull HighlightSeverity severity, @Nonnull TextRange range, String message) {
        return unsupported();
    }

    @Override
    public Annotation createAnnotation(@Nonnull HighlightSeverity severity,
                                       @Nonnull TextRange range,
                                       String message,
                                       String htmlTooltip) {
        return unsupported();
    }
}
