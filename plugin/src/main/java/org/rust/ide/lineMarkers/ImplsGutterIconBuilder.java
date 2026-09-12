/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import consulo.ui.image.Image;

/**
 * Thin wrapper over {@link NavigationGutterIconBuilder} — previously extended it to override
 * renderer / navigation, but those fields/methods are not accessible in Consulo. For the migration
 * we just delegate to the standard builder.
 */
public class ImplsGutterIconBuilder {
    private ImplsGutterIconBuilder() {
    }

    @Nonnull
    public static NavigationGutterIconBuilder<PsiElement> create(@Nonnull String elementName,
                                                                 @Nonnull consulo.ui.image.Image icon) {
        return NavigationGutterIconBuilder.create(icon);
    }
}
