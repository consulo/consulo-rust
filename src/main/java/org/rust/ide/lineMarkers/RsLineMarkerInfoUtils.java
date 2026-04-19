/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.function.Supplier;

public final class RsLineMarkerInfoUtils {

    public static final RsLineMarkerInfoUtils INSTANCE = new RsLineMarkerInfoUtils();

    private RsLineMarkerInfoUtils() {
    }

    @NotNull
    public static LineMarkerInfo<PsiElement> create(
        @NotNull PsiElement element,
        @NotNull TextRange range,
        @NotNull Icon icon,
        @Nullable GutterIconNavigationHandler<PsiElement> navHandler,
        @NotNull GutterIconRenderer.Alignment alignment,
        @NotNull Supplier<String> messageProvider
    ) {
        return new LineMarkerInfo<>(element, range, icon, e -> messageProvider.get(), navHandler, alignment, messageProvider);
    }
}
