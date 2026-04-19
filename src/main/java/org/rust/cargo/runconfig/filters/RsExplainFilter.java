/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import com.intellij.execution.filters.BrowserHyperlinkInfo;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsConstants;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filters output for "[--explain Exxxx]" (or other similar patterns) and links
 * to the relevant documentation.
 */
public class RsExplainFilter implements Filter, DumbAware {

    private final List<Pattern> myPatterns = Arrays.asList(
        Pattern.compile("--explain E(\\d{4})"),
        Pattern.compile("(error|warning)\\[E(\\d{4})]")
    );

    @Nullable
    @Override
    public Result applyFilter(@NotNull String line, int entireLength) {
        Matcher foundMatcher = null;
        for (Pattern pattern : myPatterns) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                foundMatcher = matcher;
                break;
            }
        }
        if (foundMatcher == null) return null;

        int offset;
        int length;
        String code;
        if (foundMatcher.groupCount() == 1) {
            offset = 0;
            length = foundMatcher.group(0).length();
            code = foundMatcher.group(1);
        } else {
            offset = foundMatcher.group(1).length();
            length = foundMatcher.group(2).length() + 3;
            code = foundMatcher.group(2);
        }
        String url = RsConstants.ERROR_INDEX_URL + "#E" + code;
        BrowserHyperlinkInfo info = new BrowserHyperlinkInfo(url);

        int startOffset = entireLength - line.length() + foundMatcher.start() + offset;
        int endOffset = startOffset + length;

        return new Result(startOffset, endOffset, info);
    }
}
