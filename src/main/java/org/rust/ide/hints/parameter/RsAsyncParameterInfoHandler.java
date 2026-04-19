/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import com.intellij.lang.parameterInfo.*;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.openapiext.OpenApiUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * This is a hack to make {@link ParameterInfoHandler} asynchronous. Usual {@link ParameterInfoHandler} is called from the EDT
 * and so complex computations inside it (e.g. name resolution) can freeze the UI.
 */
public abstract class RsAsyncParameterInfoHandler<ParameterOwner extends PsiElement, ParameterType>
    implements ParameterInfoHandler<ParameterOwner, ParameterType> {

    private static final Executor EXECUTOR =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("Rust async parameter info handler", 1);

    @Nullable
    public abstract ParameterOwner findTargetElement(@NotNull PsiFile file, int offset);

    /**
     * Ran in background thread
     */
    @Nullable
    public abstract ParameterType[] calculateParameterInfo(@NotNull ParameterOwner element);

    @Nullable
    @Override
    public final ParameterOwner findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        ParameterOwner element = findTargetElement(context.getFile(), context.getOffset());
        if (element == null) return null;

        if (OpenApiUtil.isUnitTestMode()) {
            ParameterType[] paramInfo = calculateParameterInfo(element);
            if (paramInfo == null) return null;
            context.setItemsToShow(paramInfo);
            return element;
        } else {
            ParameterOwner elementRef = element;
            ReadAction.nonBlocking((Callable<ParameterType[]>) () -> calculateParameterInfo(elementRef))
                .finishOnUiThread(ModalityState.defaultModalityState(), paramInfo -> {
                    if (paramInfo != null) {
                        context.setItemsToShow(paramInfo);
                        showParameterInfo(elementRef, context);
                    }
                })
                .expireWhen(() -> !elementRef.isValid())
                .submit(EXECUTOR);
            return null;
        }
    }

    @Nullable
    @Override
    public final ParameterOwner findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        return findTargetElement(context.getFile(), context.getOffset());
    }

    @Override
    public void showParameterInfo(@NotNull ParameterOwner element, @NotNull CreateParameterInfoContext context) {
        context.showHint(element, element.getTextRange().getStartOffset(), this);
    }

    public static void updateUI(@NotNull String text, @NotNull TextRange range, @NotNull ParameterInfoUIContext context) {
        context.setupUIComponentPresentation(
            text,
            range.getStartOffset(),
            range.getEndOffset(),
            !context.isUIComponentEnabled(),
            false,
            false,
            context.getDefaultParameterColor()
        );
    }

    @NotNull
    public static TextRange getArgumentRange(@NotNull String[] arguments, int index) {
        if (index < 0 || index >= arguments.length) return TextRange.EMPTY_RANGE;
        int start = 0;
        for (int i = 0; i < index; i++) {
            start += arguments[i].length() + 2;
        }
        return new TextRange(start, start + arguments[index].length());
    }
}
