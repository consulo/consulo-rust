/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import consulo.project.Project;
import consulo.util.lang.ref.Ref;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.changeSignature.ChangeInfo;
import consulo.language.editor.refactoring.changeSignature.ChangeSignatureProcessorBase;
import consulo.usage.BaseUsageViewDescriptor;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewDescriptor;
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nonnull;
import consulo.localize.LocalizeValue;
import consulo.util.lang.ref.SimpleReference;

public class RsChangeSignatureProcessor extends ChangeSignatureProcessorBase {

    public RsChangeSignatureProcessor(@Nonnull Project project, @Nonnull ChangeInfo changeInfo) {
        super(project, changeInfo);
    }

    @Nonnull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
        return new BaseUsageViewDescriptor(getChangeInfo().getMethod());
    }

    protected boolean preprocessUsages(@Nonnull consulo.util.lang.ref.SimpleReference<UsageInfo[]> refUsages) {
        MultiMap<PsiElement, consulo.localize.LocalizeValue> conflicts = new MultiMap<>();
        // collectConflictsFromExtensions requires a String-keyed MultiMap; skip for now
        return showConflicts(conflicts, refUsages.get());
    }

    public static void runChangeSignatureRefactoring(@Nonnull RsChangeFunctionSignatureConfig config) {
        new RsChangeSignatureProcessor(config.getFunction().getProject(), config.createChangeInfo()).run();
    }
}
