/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.annotation.AnnotationBuilder;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.AnnotationSession;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElementUtil;

public class RsAnnotationHolder {
    @Nonnull
    private final AnnotationHolder myHolder;
    @Nonnull
    private final AnnotationSession myCurrentAnnotationSession;

    public RsAnnotationHolder(@Nonnull AnnotationHolder holder) {
        this.myHolder = holder;
        this.myCurrentAnnotationSession = holder.getCurrentAnnotationSession();
    }

    @Nonnull
    public AnnotationHolder getHolder() {
        return myHolder;
    }

    @Nonnull
    public AnnotationSession getCurrentAnnotationSession() {
        return myCurrentAnnotationSession;
    }

    public void createErrorAnnotation(@Nonnull PsiElement element, @Nullable  String message, @Nonnull IntentionAction... fixes) {
        AnnotationBuilder builder = newErrorAnnotation(element, message, fixes);
        if (builder != null) {
            builder.create();
        }
    }

    public void createWeakWarningAnnotation(@Nonnull PsiElement element, @Nullable  String message, @Nonnull IntentionAction... fixes) {
        AnnotationBuilder builder = newWeakWarningAnnotation(element, message, fixes);
        if (builder != null) {
            builder.create();
        }
    }

    @Nullable
    public AnnotationBuilder newErrorAnnotation(@Nonnull PsiElement element, @Nullable  String message, @Nonnull IntentionAction... fixes) {
        return newAnnotation(element, HighlightSeverity.ERROR, message, fixes);
    }

    @Nullable
    public AnnotationBuilder newWeakWarningAnnotation(@Nonnull PsiElement element, @Nullable  String message, @Nonnull IntentionAction... fixes) {
        return newAnnotation(element, HighlightSeverity.WEAK_WARNING, message, fixes);
    }

    @Nullable
    public AnnotationBuilder newAnnotation(@Nonnull PsiElement element, @Nonnull HighlightSeverity severity, @Nullable  String message, @Nonnull IntentionAction... fixes) {
        if (!RsElementUtil.existsAfterExpansion(element, AnnotationSessionEx.currentCrate(myCurrentAnnotationSession))) {
            return null;
        }
        AnnotationBuilder builder;
        if (message == null) {
            builder = myHolder.newSilentAnnotation(severity);
        } else {
            builder = myHolder.newAnnotation(severity, message);
        }
        builder.range(element);
        for (IntentionAction fix : fixes) {
            builder.withFix(fix);
        }
        return builder;
    }
}
