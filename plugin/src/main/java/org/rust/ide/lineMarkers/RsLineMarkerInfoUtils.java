/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import consulo.language.editor.gutter.GutterIconNavigationHandler;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.ui.image.Image;
import java.util.function.Supplier;

public final class RsLineMarkerInfoUtils {

    public static final RsLineMarkerInfoUtils INSTANCE = new RsLineMarkerInfoUtils();

    private RsLineMarkerInfoUtils() {
    }

    @Nonnull
    public static LineMarkerInfo<PsiElement> create(
        @Nonnull PsiElement element,
        @Nonnull TextRange range,
        @Nonnull Image icon,
        @Nullable GutterIconNavigationHandler<PsiElement> navHandler,
        @Nonnull GutterIconRenderer.Alignment alignment,
        @Nonnull Supplier<String> messageProvider
    ) {
        return new LineMarkerInfo<>(element, range, icon, consulo.language.editor.Pass.LINE_MARKERS, e -> messageProvider.get(), navHandler, alignment);
    }
}
