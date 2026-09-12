/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.colors;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.util.io.StreamUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.highlight.RsHighlighter;
import org.rust.ide.icons.RsIcons;

import javax.swing.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class RsColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] ATTRS;
    private static final Map<String, TextAttributesKey> ANNOTATOR_TAGS;
    private static volatile String DEMO_TEXT;

    static {
        RsColor[] values = RsColor.values();
        ATTRS = new AttributesDescriptor[values.length];
        ANNOTATOR_TAGS = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            ATTRS[i] = values[i].getAttributesDescriptor();
            ANNOTATOR_TAGS.put(values[i].name(), values[i].getTextAttributesKey());
        }
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("settings.rust.color.scheme.title"));
    }

    @Nonnull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return ATTRS;
    }

    @Nonnull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Nonnull
    @Override
    public consulo.language.editor.highlight.SyntaxHighlighter getHighlighter() {
        return new RsHighlighter();
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return ANNOTATOR_TAGS;
    }

    @Nonnull
    @Override
    public String getDemoText() {
        if (DEMO_TEXT == null) {
            synchronized (RsColorSettingsPage.class) {
                if (DEMO_TEXT == null) {
                    InputStream stream = RsColorSettingsPage.class.getClassLoader()
                        .getResourceAsStream("org/rust/ide/colors/highlighterDemoText.rs");
                    if (stream == null) {
                        throw new IllegalStateException("Cannot load resource `org/rust/ide/colors/highlighterDemoText.rs`");
                    }
                    try {
                        InputStreamReader reader = new InputStreamReader(stream);
                        String text = new String(StreamUtil.readTextAndConvertSeparators(reader));
                        DEMO_TEXT = text;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            stream.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        return DEMO_TEXT;
    }
}
