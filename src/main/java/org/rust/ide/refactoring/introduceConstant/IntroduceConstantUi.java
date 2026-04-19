/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceConstant;

/**
 * This file serves as an aggregation point for the introduce constant UI types
 *
 * The individual types have been converted to separate Java files:
 * <ul>
 *   <li>{@link ExtractConstantUi} - Interface for choosing an insertion point</li>
 *   <li>{@link InsertionCandidate} - Data class representing a candidate insertion point</li>
 *   <li>{@link IntroduceConstantUiUtils} - Utility class with showInsertionChooser, findInsertionCandidates,
 *       withMockExtractConstantChooser, MOCK holder, and the Highlighter inner class</li>
 * </ul>
 *
 * @see ExtractConstantUi
 * @see InsertionCandidate
 * @see IntroduceConstantUiUtils
 */
public final class IntroduceConstantUi {
    private IntroduceConstantUi() {
    }
}
