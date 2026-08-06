/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

/**
 * This file serves as an aggregation point for the implement members UI types
 *
 * The individual types have been converted to separate Java files:
 * <ul>
 *   <li>{@link RsTraitMemberChooserMember} - ClassMember implementation for trait members in the chooser dialog</li>
 *   <li>{@link TraitMemberChooserUi} - Utility class with showTraitMemberChooser, withMockTraitMemberChooser,
 *       and the TraitMemberChooser functional interface</li>
 * </ul>
 *
 * @see RsTraitMemberChooserMember
 * @see TraitMemberChooserUi
 */
public final class ImplementMembersUi {
    private ImplementMembersUi() {
    }
}
