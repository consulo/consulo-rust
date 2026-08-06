/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractStructFields;

/**
 * This file serves as an aggregation point for the extract struct fields UI types
 *
 * The individual types have been converted to separate Java files:
 * <ul>
 *   <li>{@link ExtractFieldsUi} - Interface for selecting a struct name</li>
 *   <li>{@link ExtractFieldsDialog} - Dialog implementation of {@link ExtractFieldsUi}</li>
 *   <li>{@link ExtractFieldsUiUtils} - Utility methods: showExtractStructFieldsDialog, withMockExtractFieldsUi</li>
 * </ul>
 *
 * @see ExtractFieldsUi
 * @see ExtractFieldsDialog
 * @see ExtractFieldsUiUtils
 */
public final class ExtractStructFieldsUi {
    private ExtractStructFieldsUi() {
    }
}
