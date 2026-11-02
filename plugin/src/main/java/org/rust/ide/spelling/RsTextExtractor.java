/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rust.ide.spelling;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.spellcheker.text.TextContent;
import consulo.language.spellcheker.text.TextContent.Exclusion;
import consulo.language.spellcheker.text.TextContent.ExclusionKind;
import consulo.language.spellcheker.text.TextContentBuilder;
import consulo.language.spellcheker.text.TextExtractor;
import consulo.language.spellcheker.text.util.EscapeUtils;
import consulo.language.spellcheker.text.util.PsiUtils;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.doc.psi.RsDocAutoLink;
import org.rust.lang.doc.psi.RsDocCodeBlock;
import org.rust.lang.doc.psi.RsDocCodeFence;
import org.rust.lang.doc.psi.RsDocCodeSpan;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.doc.psi.RsDocElementTypes;
import org.rust.lang.doc.psi.RsDocEmphasis;
import org.rust.lang.doc.psi.RsDocHtmlBlock;
import org.rust.lang.doc.psi.RsDocInlineLink;
import org.rust.lang.doc.psi.RsDocLinkDefinition;
import org.rust.lang.doc.psi.RsDocLinkDestination;
import org.rust.lang.doc.psi.RsDocLinkLabel;
import org.rust.lang.doc.psi.RsDocLinkReferenceFull;
import org.rust.lang.doc.psi.RsDocLinkReferenceShort;
import org.rust.lang.doc.psi.RsDocLinkText;
import org.rust.lang.doc.psi.RsDocStrong;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static consulo.language.spellcheker.text.TextContent.TextDomain.COMMENTS;
import static consulo.language.spellcheker.text.TextContent.TextDomain.DOCUMENTATION;
import static consulo.language.spellcheker.text.TextContent.TextDomain.LITERALS;

/**
 * Extracts natural language text from Rust comments, Markdown doc comments and string literals.
 */
@ExtensionImpl
public class RsTextExtractor extends TextExtractor {
    private static final String EMPHASIS_MARKERS = "*_";

    private static final Pattern ESCAPE = Pattern.compile("\\\\(?:x[0-9a-fA-F]{2}|u\\{[0-9a-fA-F_]*}|.)", Pattern.DOTALL);
    private static final Pattern FORMAT_PLACEHOLDER = Pattern.compile("\\{\\{|}}|\\{[^{}\\n]*}");

    private static final TextContentBuilder DOC_BUILDER = TextContentBuilder.FromPsi
        .excluding(e -> PsiUtilCore.getElementType(e) == RsDocElementTypes.DOC_GAP
            || e instanceof RsDocCodeFence
            || e instanceof RsDocCodeBlock
            || e instanceof RsDocHtmlBlock
            || e instanceof RsDocLinkDefinition
            || e instanceof RsDocLinkDestination
            || e instanceof RsDocLinkLabel)
        .withUnknown(e -> e instanceof RsDocCodeSpan || e instanceof RsDocAutoLink || e instanceof RsDocLinkReferenceShort)
        .withMarkup(e -> PsiUtilCore.getElementType(e) == RsDocElementTypes.DOC_DATA
            && (e.getParent() instanceof RsDocInlineLink || e.getParent() instanceof RsDocLinkReferenceFull))
        .removingIndents(" \t")
        .removingLineSuffixes(" \t");

    private static final TextContentBuilder LINE_COMMENT_BUILDER =
        TextContentBuilder.FromPsi.removingIndents(" \t/").removingLineSuffixes(" \t");

    private static final TextContentBuilder BLOCK_COMMENT_BUILDER =
        TextContentBuilder.FromPsi.removingIndents(" \t*/").removingLineSuffixes(" \t*/");

    @Override
    @RequiredReadAction
    protected List<TextContent> buildTextContents(PsiElement element, Set<TextContent.TextDomain> allowedDomains) {
        if (element instanceof RsDocComment) {
            if (!allowedDomains.contains(DOCUMENTATION)) {
                return List.of();
            }
            return ContainerUtil.createMaybeSingletonList(buildDocContent(element));
        }

        IElementType type = PsiUtilCore.getElementType(element);
        if (type == RsTokenType.EOL_COMMENT) {
            if (!allowedDomains.contains(COMMENTS)) {
                return List.of();
            }
            List<PsiElement> roots = PsiUtils.getNotSoDistantSimilarSiblings(element, e -> PsiUtilCore.getElementType(e) == RsTokenType.EOL_COMMENT);
            List<TextContent> fragments = ContainerUtil.mapNotNull(roots, root -> LINE_COMMENT_BUILDER.build(root, COMMENTS));
            return ContainerUtil.createMaybeSingletonList(TextContent.joinWithWhitespace('\n', fragments));
        }

        if (type == RsTokenType.BLOCK_COMMENT) {
            if (!allowedDomains.contains(COMMENTS)) {
                return List.of();
            }
            return ContainerUtil.createMaybeSingletonList(BLOCK_COMMENT_BUILDER.build(element, COMMENTS));
        }

        if ((type == RsElementTypes.STRING_LITERAL || type == RsElementTypes.RAW_STRING_LITERAL) && allowedDomains.contains(LITERALS)) {
            return ContainerUtil.createMaybeSingletonList(buildStringContent(element, type == RsElementTypes.RAW_STRING_LITERAL));
        }

        return List.of();
    }

    @RequiredReadAction
    private static @Nullable TextContent buildDocContent(PsiElement docComment) {
        TextContent content = DOC_BUILDER.build(docComment, DOCUMENTATION);
        if (content == null) {
            return null;
        }

        List<Exclusion> exclusions = new ArrayList<>();
        for (PsiElement markupOwner : PsiTreeUtil.findChildrenOfAnyType(docComment, RsDocLinkText.class, RsDocEmphasis.class, RsDocStrong.class)) {
            TextRange range = markupOwner.getTextRange();
            Integer start = content.fileOffsetToText(range.getStartOffset());
            Integer end = content.fileOffsetToText(range.getEndOffset() - 1);
            if (start == null || end == null || start > end) {
                continue;
            }
            int markupEnd = start;
            int markupStart = end + 1;
            if (markupOwner instanceof RsDocLinkText) {
                if (content.charAt(start) == '[') {
                    markupEnd = start + 1;
                }
                if (content.charAt(end) == ']' && end >= markupEnd) {
                    markupStart = end;
                }
            }
            else {
                while (markupEnd <= end && EMPHASIS_MARKERS.indexOf(content.charAt(markupEnd)) >= 0) {
                    markupEnd++;
                }
                while (markupStart > markupEnd && EMPHASIS_MARKERS.indexOf(content.charAt(markupStart - 1)) >= 0) {
                    markupStart--;
                }
            }
            if (markupEnd > start) {
                addIfFree(exclusions, new Exclusion(start, markupEnd, ExclusionKind.markup));
            }
            if (markupStart <= end) {
                addIfFree(exclusions, new Exclusion(markupStart, end + 1, ExclusionKind.markup));
            }
        }

        exclusions.sort(Comparator.comparingInt(e -> e.start));
        return content.excludeRanges(exclusions).trimWhitespace();
    }

    @RequiredReadAction
    private static @Nullable TextContent buildStringContent(PsiElement literal, boolean raw) {
        TextRange valueRange = raw ? rawValueRange(literal.getText()) : quotedValueRange(literal.getText());
        if (valueRange == null || valueRange.isEmpty()) {
            return null;
        }

        TextContent content = TextContentBuilder.FromPsi.build(literal, LITERALS, valueRange);
        if (content == null) {
            return null;
        }

        if (!raw) {
            content = content.excludeRanges(findLineContinuations(content));
            content = EscapeUtils.replaceBackslashEscapedWhitespace(content);
            content = content.excludeRanges(findAll(ESCAPE, content, List.of()));
        }
        content = content.excludeRanges(findAll(FORMAT_PLACEHOLDER, content, List.of("{{", "}}")));
        return content.trimWhitespace();
    }

    private static @Nullable TextRange quotedValueRange(String text) {
        int start = text.indexOf('"');
        int end = text.lastIndexOf('"');
        if (start < 0 || end <= start) {
            return null;
        }
        return new TextRange(start + 1, end);
    }

    private static @Nullable TextRange rawValueRange(String text) {
        int quote = text.indexOf('"');
        if (quote < 0) {
            return null;
        }
        int hashes = 0;
        for (int i = quote - 1; i >= 0 && text.charAt(i) == '#'; i--) {
            hashes++;
        }
        String closer = "\"" + "#".repeat(hashes);
        int end = text.lastIndexOf(closer);
        if (end <= quote) {
            return null;
        }
        return new TextRange(quote + 1, end);
    }

    private static List<Exclusion> findLineContinuations(CharSequence text) {
        List<Exclusion> result = new ArrayList<>();
        int i = 0;
        while (i < text.length() - 1) {
            if (text.charAt(i) != '\\') {
                i++;
                continue;
            }
            char next = text.charAt(i + 1);
            if (next == '\n' || next == '\r') {
                int end = i + 1;
                while (end < text.length() && Character.isWhitespace(text.charAt(end))) {
                    end++;
                }
                result.add(Exclusion.exclude(new TextRange(i, end)));
                i = end;
            }
            else {
                i += 2;
            }
        }
        return result;
    }

    private static List<Exclusion> findAll(Pattern pattern, CharSequence text, List<String> partialMatches) {
        List<Exclusion> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            if (partialMatches.contains(matcher.group())) {
                result.add(Exclusion.exclude(new TextRange(matcher.start(), matcher.start() + 1)));
            }
            else {
                result.add(Exclusion.markUnknown(new TextRange(matcher.start(), matcher.end())));
            }
        }
        return result;
    }

    private static void addIfFree(List<Exclusion> exclusions, Exclusion exclusion) {
        for (Exclusion existing : exclusions) {
            if (existing.start < exclusion.end && exclusion.start < existing.end) {
                return;
            }
        }
        exclusions.add(exclusion);
    }

    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }
}
