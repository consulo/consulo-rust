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
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("action.Rust.RsExtractStructFields.intention.text"));
        }

    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Override
    public RsBaseEditorRefactoringAction getRefactoringAction() {
        return new RsExtractStructFieldsAction();
    }
}
