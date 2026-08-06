/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import org.rust.openapiext.OpenApiUtil;

import java.util.Set;

public abstract class AnnotatorBase implements Annotator {

    @Override
    public final void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (isEnabled(getClass())) {
            annotateInternal(element, holder);
        }
    }

    protected abstract void annotateInternal(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder);

    private static final Set<Class<? extends AnnotatorBase>> myEnabledAnnotators = ContainerUtil.newConcurrentSet();

    
    public static void enableAnnotator(@Nonnull Class<? extends AnnotatorBase> annotatorClass, @Nonnull Disposable parentDisposable) {
        myEnabledAnnotators.add(annotatorClass);
        Disposer.register(parentDisposable, () -> myEnabledAnnotators.remove(annotatorClass));
    }

    public static boolean isEnabled(@Nonnull Class<? extends AnnotatorBase> annotatorClass) {
        return !OpenApiUtil.isUnitTestMode() || myEnabledAnnotators.contains(annotatorClass);
    }
}
