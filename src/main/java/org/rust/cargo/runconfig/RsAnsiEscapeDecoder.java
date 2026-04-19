/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.process.AnsiEscapeDecoder;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.openapiext.OpenApiUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Currently IntelliJ Platform supports only 16 ANSI colors (standard colors and high intensity colors). The base
 * {@link AnsiEscapeDecoder} class simply ignores 8-bit and 24-bit ANSI color escapes. This class converts (quantizes) such
 * escapes to supported 3/4-bit ANSI color escapes. Note that the user can configure color mapping in editor settings
 * (Preferences &gt; Editor &gt; Console Scheme &gt; Console Colors &gt; ANSI Colors). In addition, the themes also set the colors.
 * So, this solution gives us interoperability with existing themes.
 */
public class RsAnsiEscapeDecoder extends AnsiEscapeDecoder {

    public static final String CSI = "\u001B["; // "Control Sequence Initiator"

    public static final Pattern ANSI_SGR_RE = Pattern.compile(
        StringUtil.escapeToRegexp(CSI) + "(\\d+(;\\d+)*)m"
    );

    private static final int ANSI_SET_FOREGROUND_ATTR = 38;
    private static final int ANSI_SET_BACKGROUND_ATTR = 48;

    public static final int ANSI_24_BIT_COLOR_FORMAT = 2;
    public static final int ANSI_8_BIT_COLOR_FORMAT = 5;

    @Override
    public void escapeText(@NotNull String text, @NotNull Key outputType, @NotNull ColoredTextAcceptor textAcceptor) {
        super.escapeText(quantizeAnsiColors(text), outputType, textAcceptor);
    }

    /**
     * Parses ANSI-value codes from text and replaces 8-bit and 24-bit colors with nearest (in Euclidean space)
     * 4-bit value.
     *
     * @param text a string with ANSI escape sequences
     */
    @NotNull
    public static String quantizeAnsiColors(@NotNull String text) {
        java.util.regex.Matcher matcher = ANSI_SGR_RE.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String group1 = matcher.group(1);
            String[] rawParts = group1.split(";");
            List<String> rawList = new ArrayList<>();
            for (String part : rawParts) {
                rawList.add(part);
            }
            Iterator<String> rawAttributes = rawList.iterator();
            List<Integer> result = new ArrayList<>();
            while (rawAttributes.hasNext()) {
                Integer attribute = parseAttribute(rawAttributes);
                if (attribute == null) continue;
                if (attribute != ANSI_SET_FOREGROUND_ATTR && attribute != ANSI_SET_BACKGROUND_ATTR) {
                    result.add(attribute);
                    continue;
                }
                Color color = parseColor(rawAttributes);
                if (color == null) continue;
                Ansi4BitColor ansiColor = getNearestAnsiColor(color);
                if (ansiColor == null) continue;
                int colorAttribute = getColorAttribute(ansiColor, attribute == ANSI_SET_FOREGROUND_ATTR);
                result.add(colorAttribute);
            }
            StringBuilder replacement = new StringBuilder();
            replacement.append(CSI);
            for (int i = 0; i < result.size(); i++) {
                if (i > 0) replacement.append(';');
                replacement.append(result.get(i));
            }
            replacement.append('m');
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Nullable
    private static Integer parseAttribute(@NotNull Iterator<String> iterator) {
        if (!iterator.hasNext()) return null;
        String next = iterator.next();
        try {
            return Integer.parseInt(next);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static Color parseColor(@NotNull Iterator<String> rawAttributes) {
        Integer format = parseAttribute(rawAttributes);
        if (format == null) return null;
        if (format == ANSI_24_BIT_COLOR_FORMAT) {
            return parse24BitColor(rawAttributes);
        } else if (format == ANSI_8_BIT_COLOR_FORMAT) {
            return parse8BitColor(rawAttributes);
        }
        return null;
    }

    @Nullable
    private static Color parse24BitColor(@NotNull Iterator<String> rawAttributes) {
        Integer red = parseAttribute(rawAttributes);
        if (red == null) return null;
        Integer green = parseAttribute(rawAttributes);
        if (green == null) return null;
        Integer blue = parseAttribute(rawAttributes);
        if (blue == null) return null;
        return new Color(red, green, blue);
    }

    @Nullable
    private static Color parse8BitColor(@NotNull Iterator<String> rawAttributes) {
        Integer attribute = parseAttribute(rawAttributes);
        if (attribute == null) return null;
        if (attribute >= 0 && attribute <= 15) {
            // Standard colors or high intensity colors
            Ansi4BitColor[] values = Ansi4BitColor.values();
            if (attribute >= 0 && attribute < values.length) {
                return values[attribute].getValue();
            }
            return null;
        } else if (attribute >= 16 && attribute <= 231) {
            // 6 x 6 x 6 cube (216 colors): 16 + 36 x r + 6 x g + b (0 <= r, g, b <= 5)
            int red = (attribute - 16) / 36 * 51;
            int green = (attribute - 16) % 36 / 6 * 51;
            int blue = (attribute - 16) % 6 * 51;
            return new Color(red, green, blue);
        } else if (attribute >= 232 && attribute <= 255) {
            // Grayscale from black to white in 24 steps
            int value = (attribute - 232) * 10 + 8;
            return new Color(value, value, value);
        }
        return null;
    }

    @Nullable
    private static Ansi4BitColor getNearestAnsiColor(@NotNull Color color) {
        Ansi4BitColor[] values = Ansi4BitColor.values();
        Ansi4BitColor nearest = null;
        int minDistance = Integer.MAX_VALUE;
        for (Ansi4BitColor ansiColor : values) {
            int distance = calcEuclideanDistance(ansiColor.getValue(), color);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = ansiColor;
            }
        }
        return nearest;
    }

    private static int calcEuclideanDistance(@NotNull Color from, @NotNull Color to) {
        double redDiff = (double) from.getRed() - to.getRed();
        double greenDiff = (double) from.getGreen() - to.getGreen();
        double blueDiff = (double) from.getBlue() - to.getBlue();
        return (int) Math.round(Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff));
    }

    private static int getColorAttribute(@NotNull Ansi4BitColor realAnsiColor, boolean isForeground) {
        // Rude hack for Windows: map the bright white foreground color to black.
        // See https://github.com/intellij-rust/intellij-rust/pull/3312#issue-249111003
        boolean isForcedWhiteFontUnderLightTheme = realAnsiColor == Ansi4BitColor.BRIGHT_WHITE &&
            isForeground && SystemInfo.isWindows && !OpenApiUtil.isUnderDarkTheme();
        Ansi4BitColor ansiColor;
        if (isForcedWhiteFontUnderLightTheme) {
            ansiColor = Ansi4BitColor.BLACK;
        } else {
            ansiColor = realAnsiColor;
        }
        int colorIndex = ansiColor.getIndex();
        if (colorIndex >= 0 && colorIndex <= 7 && isForeground) {
            return colorIndex + 30;
        } else if (colorIndex >= 0 && colorIndex <= 7) {
            return colorIndex + 40;
        } else if (colorIndex >= 8 && colorIndex <= 15 && isForeground) {
            return colorIndex + 82;
        } else if (colorIndex >= 8 && colorIndex <= 15) {
            return colorIndex + 92;
        } else {
            throw new IllegalStateException("impossible");
        }
    }

    /**
     * Removes ANSI escape sequences from text using the given decoder for state management.
     */
    @NotNull
    public static String removeEscapeSequences(@NotNull AnsiEscapeDecoder decoder, @NotNull String text) {
        return ANSI_SGR_RE.matcher(text).replaceAll("");
    }

    private enum Ansi4BitColor {
        BLACK(new Color(0, 0, 0)),
        RED(new Color(128, 0, 0)),
        GREEN(new Color(0, 128, 0)),
        YELLOW(new Color(128, 128, 0)),
        BLUE(new Color(0, 0, 128)),
        MAGENTA(new Color(128, 0, 128)),
        CYAN(new Color(0, 128, 128)),
        WHITE(new Color(192, 192, 192)),
        BRIGHT_BLACK(new Color(128, 128, 128)),
        BRIGHT_RED(new Color(255, 0, 0)),
        BRIGHT_GREEN(new Color(0, 255, 0)),
        BRIGHT_YELLOW(new Color(255, 255, 0)),
        BRIGHT_BLUE(new Color(0, 0, 255)),
        BRIGHT_MAGENTA(new Color(255, 0, 255)),
        BRIGHT_CYAN(new Color(0, 255, 255)),
        BRIGHT_WHITE(new Color(255, 255, 255));

        private final Color myValue;

        Ansi4BitColor(@NotNull Color value) {
            myValue = value;
        }

        @NotNull
        public Color getValue() {
            return myValue;
        }

        public int getIndex() {
            Ansi4BitColor[] values = values();
            for (int i = 0; i < values.length; i++) {
                if (values[i] == this) return i;
            }
            return -1;
        }

        @Nullable
        public static Ansi4BitColor get(int index) {
            Ansi4BitColor[] values = values();
            if (index >= 0 && index < values.length) {
                return values[index];
            }
            return null;
        }
    }
}
