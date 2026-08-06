/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

/**
 *
 * In Java, interfaces should ideally be in their own files. However, since the original
 * interoperable with Java), this file serves as a reference point.
 *
 * <ul>
 *   <li>{@link RsDocElement} - Base interface for doc elements</li>
 *   <li>{@link RsDocGap} - Skipped comment decorations</li>
 *   <li>{@link RsDocHeading} - Base for heading types</li>
 *   <li>{@link RsDocAtxHeading} - ATX-style headings (# Header)</li>
 *   <li>{@link RsDocSetextHeading} - Setext-style headings (underlined)</li>
 *   <li>{@link RsDocEmphasis} - Emphasis spans (*text* or _text_)</li>
 *   <li>{@link RsDocStrong} - Strong spans (**text** or __text__)</li>
 *   <li>{@link RsDocCodeSpan} - Code spans (`code`)</li>
 *   <li>{@link RsDocAutoLink} - Auto links (&lt;url&gt;)</li>
 *   <li>{@link RsDocLink} - Base for link types</li>
 *   <li>{@link RsDocInlineLink} - Inline links [text](url)</li>
 *   <li>{@link RsDocPathLinkParent} - Sealed interface for path link parents</li>
 *   <li>{@link RsDocLinkReferenceShort} - Short reference links [label]</li>
 *   <li>{@link RsDocLinkReferenceFull} - Full reference links [text][label]</li>
 *   <li>{@link RsDocLinkDefinition} - Link definitions [label]: url</li>
 *   <li>{@link RsDocLinkText} - Link text portion</li>
 *   <li>{@link RsDocLinkLabel} - Link label portion</li>
 *   <li>{@link RsDocLinkTitle} - Link title portion</li>
 *   <li>{@link RsDocLinkDestination} - Link destination portion</li>
 *   <li>{@link RsDocCodeFence} - Fenced code blocks</li>
 *   <li>{@link RsDocCodeBlock} - Indented code blocks</li>
 *   <li>{@link RsDocHtmlBlock} - HTML blocks</li>
 *   <li>{@link RsDocCodeFenceStartEnd} - Code fence delimiters</li>
 *   <li>{@link RsDocCodeFenceLang} - Code fence language specifier</li>
 * </ul>
 */
public final class Psi {
    private Psi() {
    }
}
