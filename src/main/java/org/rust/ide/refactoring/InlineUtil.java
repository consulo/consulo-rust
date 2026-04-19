/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

/**
 * This file serves as an aggregation point for the inline refactoring utility types
 *
 * The individual types have been converted to separate Java files:
 * <ul>
 *   <li>{@link RsInlineDialog} - Abstract dialog extending InlineOptionsDialog for Rust inline refactorings</li>
 *   <li>{@link RsInlineUsageViewDescriptor} - UsageViewDescriptor implementation for inline refactorings</li>
 * </ul>
 *
 * @see RsInlineDialog
 * @see RsInlineUsageViewDescriptor
 */
public final class InlineUtil {
    private InlineUtil() {
    }
}
