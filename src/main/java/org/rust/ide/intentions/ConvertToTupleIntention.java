/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.rust.RsBundle;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;
import org.rust.ide.refactoring.convertStruct.RsConvertToTupleAction;

public class ConvertToTupleIntention extends RsRefactoringAdaptorIntention {
    @Override
    public RsBaseEditorRefactoringAction getRefactoringAction() {
        return new RsConvertToTupleAction();
    }

    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.to.tuple");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }
}
