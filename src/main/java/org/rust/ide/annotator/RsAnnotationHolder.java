/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElementUtil;

public class RsAnnotationHolder {
    @NotNull
    private final AnnotationHolder myHolder;
    @NotNull
    private final AnnotationSession myCurrentAnnotationSession;

    public RsAnnotationHolder(@NotNull AnnotationHolder holder) {
        this.myHolder = holder;
        this.myCurrentAnnotationSession = holder.getCurrentAnnotationSession();
    }

    @NotNull
    public AnnotationHolder getHolder() {
        return myHolder;
    }

    @NotNull
    public AnnotationSession getCurrentAnnotationSession() {
        return myCurrentAnnotationSession;
    }

    public void createErrorAnnotation(@NotNull PsiElement element, @Nullable @InspectionMessage String message, @NotNull IntentionAction... fixes) {
        AnnotationBuilder builder = newErrorAnnotation(element, message, fixes);
        if (builder != null) {
            builder.create();
        }
    }

    public void createWeakWarningAnnotation(@NotNull PsiElement element, @Nullable @InspectionMessage String message, @NotNull IntentionAction... fixes) {
        AnnotationBuilder builder = newWeakWarningAnnotation(element, message, fixes);
        if (builder != null) {
            builder.create();
        }
    }

    @Nullable
    public AnnotationBuilder newErrorAnnotation(@NotNull PsiElement element, @Nullable @InspectionMessage String message, @NotNull IntentionAction... fixes) {
        return newAnnotation(element, HighlightSeverity.ERROR, message, fixes);
    }

    @Nullable
    public AnnotationBuilder newWeakWarningAnnotation(@NotNull PsiElement element, @Nullable @InspectionMessage String message, @NotNull IntentionAction... fixes) {
        return newAnnotation(element, HighlightSeverity.WEAK_WARNING, message, fixes);
    }

    @Nullable
    public AnnotationBuilder newAnnotation(@NotNull PsiElement element, @NotNull HighlightSeverity severity, @Nullable @InspectionMessage String message, @NotNull IntentionAction... fixes) {
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
