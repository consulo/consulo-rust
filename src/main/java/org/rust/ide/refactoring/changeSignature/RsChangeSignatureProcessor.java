/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessorBase;
import com.intellij.usageView.BaseUsageViewDescriptor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

public class RsChangeSignatureProcessor extends ChangeSignatureProcessorBase {

    public RsChangeSignatureProcessor(@NotNull Project project, @NotNull ChangeInfo changeInfo) {
        super(project, changeInfo);
    }

    @NotNull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
        return new BaseUsageViewDescriptor(getChangeInfo().getMethod());
    }

    @Override
    protected boolean preprocessUsages(@NotNull Ref<UsageInfo[]> refUsages) {
        MultiMap<PsiElement, String> conflicts = new MultiMap<>();
        collectConflictsFromExtensions(refUsages, conflicts, myChangeInfo);
        return showConflicts(conflicts, refUsages.get());
    }

    public static void runChangeSignatureRefactoring(@NotNull RsChangeFunctionSignatureConfig config) {
        new RsChangeSignatureProcessor(config.getFunction().getProject(), config.createChangeInfo()).run();
    }
}
