/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.codeEditor.Editor;
import consulo.project.Project;

import consulo.language.editor.Pass;
import consulo.language.editor.refactoring.introduce.IntroduceTargetChooser;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.introduce.inplace.OccurrencesChooser;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsFunction;
import org.rust.openapiext.OpenApiUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ExtractExpressionUi {
    RsExpr chooseTarget(List<RsExpr> exprs);

    List<RsExpr> chooseOccurrences(RsExpr expr, List<RsExpr> occurrences);

    default RsFunction chooseMethod(List<RsFunction> methods) {
        return methods.get(0);
    }
}
