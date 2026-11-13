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
 * The Rust highlighting colors of the light (Default) color scheme.
 */
@ExtensionImpl
public class RsDefaultColorSchemeExtender implements EditorColorSchemeExtender {
    @Override
    public String getColorSchemeId() {
        return EditorColorsScheme.DEFAULT_SCHEME_NAME;
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
                .withForeground(new RGBColor(0xDD, 0x67, 0x18))
                .build());
        builder.add(RsColor.TYPE_PARAMETER.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0x20, 0x99, 0x9D))
                .build());
        builder.add(RsColor.MUT_BINDING.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withEffect(EffectType.LINE_UNDERSCORE, new RGBColor(0x00, 0x00, 0x00))
                .build());
        builder.add(RsColor.MUT_PARAMETER.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withEffect(EffectType.LINE_UNDERSCORE, new RGBColor(0x00, 0x00, 0x00))
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
        builder.add(RsColor.DOC_CODE.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0x40, 0x40, 0x40))
                .build());
        builder.add(RsBacktraceItemFilter.DIMMED_TEXT_KEY,
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0xCB, 0xCB, 0xCB))
                .build());
        builder.add(RsColor.UNSAFE_CODE.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withBackground(new RGBColor(0xFF, 0xE8, 0xC6))
                .build());
        builder.add(RsColor.CFG_DISABLED_CODE.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0x52, 0x65, 0x44))
                .build());
        builder.add(RsColor.GENERATED_ITEM.getTextAttributesKey(),
            AttributesFlyweightBuilder.create()
                .withForeground(new RGBColor(0x72, 0x72, 0x38))
                .build());
    }
}
