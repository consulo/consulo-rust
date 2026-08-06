/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.language.editor.refactoring.ui.AbstractMemberSelectionTable;
import com.intellij.ui.RowIcon;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsItemElement;

import javax.swing.*;
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

    protected void setVisibilityIcon(@Nonnull RsMemberInfo memberInfo, @Nonnull consulo.ui.image.Image icon) {
        // we don't set visibility icon
    }

    @Nullable
    @Override
    protected consulo.ui.image.Image getOverrideIcon(@Nonnull RsMemberInfo memberInfo) {
        return null;
    }
}
