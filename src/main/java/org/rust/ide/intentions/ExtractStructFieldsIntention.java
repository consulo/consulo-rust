/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.rust.RsBundle;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;
import org.rust.ide.refactoring.extractStructFields.RsExtractStructFieldsAction;

public class ExtractStructFieldsIntention extends RsRefactoringAdaptorIntention {

    @Override
    public String getText() {
        return RsBundle.message("action.Rust.RsExtractStructFields.intention.text");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public RsBaseEditorRefactoringAction getRefactoringAction() {
        return new RsExtractStructFieldsAction();
    }
}
