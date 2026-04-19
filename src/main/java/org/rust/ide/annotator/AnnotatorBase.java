/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.rust.openapiext.OpenApiUtil;

import java.util.Set;

public abstract class AnnotatorBase implements Annotator {

    @Override
    public final void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (isEnabled(getClass())) {
            annotateInternal(element, holder);
        }
    }

    protected abstract void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder);

    private static final Set<Class<? extends AnnotatorBase>> myEnabledAnnotators = ContainerUtil.newConcurrentSet();

    @TestOnly
    public static void enableAnnotator(@NotNull Class<? extends AnnotatorBase> annotatorClass, @NotNull Disposable parentDisposable) {
        myEnabledAnnotators.add(annotatorClass);
        Disposer.register(parentDisposable, () -> myEnabledAnnotators.remove(annotatorClass));
    }

    public static boolean isEnabled(@NotNull Class<? extends AnnotatorBase> annotatorClass) {
        return !OpenApiUtil.isUnitTestMode() || myEnabledAnnotators.contains(annotatorClass);
    }
}
