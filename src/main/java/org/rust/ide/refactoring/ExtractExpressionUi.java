/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.Pass;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
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
