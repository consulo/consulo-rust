/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.language.editor.refactoring.ui.AbstractMemberSelectionTable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsItemElement;

import java.util.List;

public class RsMemberSelectionTable extends AbstractMemberSelectionTable<RsItemElement, RsMemberInfo> {

    public RsMemberSelectionTable(@Nonnull List<RsMemberInfo> memberInfo) {
        super(memberInfo, null, null);
        setTableHeader(null);
    }

    @Nullable
    @Override
    protected Object getAbstractColumnValue(@Nullable RsMemberInfo memberInfo) {
        return null;
    }

    @Override
    protected boolean isAbstractColumnEditable(int rowIndex) {
        return false;
    }

    @Nullable
    @Override
    protected consulo.ui.image.Image getOverrideIcon(@Nonnull RsMemberInfo memberInfo) {
        return null;
    }
}
