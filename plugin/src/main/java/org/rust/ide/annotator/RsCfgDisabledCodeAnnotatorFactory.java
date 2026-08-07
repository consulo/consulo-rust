/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;

/**
 * Registers {@link RsCfgDisabledCodeAnnotator}.
 * <p>
 * Consulo binds annotators to a language through an {@link AnnotatorFactory} extension rather than
 * registering the {@link Annotator} itself, so each highlighting pass gets a fresh instance.
 */
@ExtensionImpl
public class RsCfgDisabledCodeAnnotatorFactory implements AnnotatorFactory {

    @Nullable
    @Override
    public Annotator createAnnotator() {
        return new RsCfgDisabledCodeAnnotator();
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }
}
