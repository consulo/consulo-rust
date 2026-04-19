/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

/**
 * This file serves as an aggregation point for the generate UI types
 *
 * The individual types have been converted to separate Java files:
 * <ul>
 *   <li>{@link StructMemberChooserUi} - Interface with showStructMemberChooserDialog and withMockStructMemberChooserUi</li>
 *   <li>{@link RsStructMemberChooserObject} - MemberChooserObjectBase/ClassMember for struct fields</li>
 *   <li>{@link DialogStructMemberChooserUi} - Dialog implementation of StructMemberChooserUi</li>
 *   <li>{@link StructMemberChooserUiHolder} - Holder for the MOCK instance</li>
 * </ul>
 *
 * @see StructMemberChooserUi
 * @see RsStructMemberChooserObject
 * @see DialogStructMemberChooserUi
 * @see StructMemberChooserUiHolder
 */
public final class GenerateUi {
    private GenerateUi() {
    }
}
