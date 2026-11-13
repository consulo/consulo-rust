/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.colors;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.AttributesFlyweightBuilder;
import consulo.colorScheme.EditorColorSchemeExtender;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EffectType;
import consulo.ui.color.RGBColor;
import org.rust.cargo.runconfig.filters.RsBacktraceItemFilter;
import org.rust.colors.RsColor;

/**
 * The Rust highlighting colors of the dark (Darcula) color scheme.
 */
@ExtensionImpl
public class RsDarculaColorSchemeExtender implements EditorColorSchemeExtender {
    @Override
    public String getColorSchemeId() {
        return EditorColorsScheme.DARCULA_SCHEME_NAME;
    }

    @Override
    public void extend(Builder builder) {
        builder.add(RsColor.LIFETIME.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0x20, 0x99, 0x9D))
                .withItalicFont()
                .build());
        builder.add(RsColor.MACRO.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0x4E, 0xAD, 0xE5))
                .build());
        builder.add(RsColor.TYPE_PARAMETER.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0x20, 0x99, 0x9D))
                .build());
        builder.add(RsColor.MUT_BINDING.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withEffect(EffectType.LINE_UNDERSCORE, new RGBColor(0xBC, 0xA5, 0xC4))
                .build());
        builder.add(RsColor.MUT_PARAMETER.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withEffect(EffectType.LINE_UNDERSCORE, new RGBColor(0xBC, 0xA5, 0xC4))
                .build());
        builder.add(RsColor.DOC_EMPHASIS.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withBoldFont()
                .withItalicFont()
                .build());
        builder.add(RsColor.DOC_STRONG.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withBoldFont()
                .build());
        builder.add(RsBacktraceItemFilter.DIMMED_TEXT_KEY,
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0x60, 0x60, 0x60))
                .build());
        builder.add(RsColor.UNSAFE_CODE.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withBackground(new RGBColor(0x4D, 0x3E, 0x3E))
                .build());
        builder.add(RsColor.FUNCTION_CALL.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0xFF, 0xC6, 0x6D))
                .build());
        builder.add(RsColor.METHOD_CALL.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0xFF, 0xC6, 0x6D))
                .build());
        builder.add(RsColor.CFG_DISABLED_CODE.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0x68, 0x6A, 0x4E))
                .build());
        builder.add(RsColor.GENERATED_ITEM.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0x84, 0x85, 0x04))
                .build());
    }
}
