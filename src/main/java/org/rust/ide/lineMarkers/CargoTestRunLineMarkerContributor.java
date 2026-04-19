/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import com.intellij.execution.TestStateStorage;
import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.execution.testframework.TestIconMapper;
import com.intellij.execution.testframework.sm.runner.states.TestStateInfo;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.runconfig.command.CargoExecutableRunConfigurationProducer;
import org.rust.cargo.runconfig.test.CargoBenchRunConfigurationProducer;
import org.rust.cargo.runconfig.test.CargoTestLocator;
import org.rust.cargo.runconfig.test.CargoTestRunConfigurationProducer;
import org.rust.cargo.runconfig.test.DocTestContext;
import org.rust.cargo.runconfig.test.DoctestCtxUtil;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.doc.psi.RsDocCodeFence;
import org.rust.lang.doc.psi.RsDocCodeFenceStartEnd;
import org.rust.lang.doc.psi.RsDocElementTypes;

import javax.swing.Icon;
import java.util.Collections;

public class CargoTestRunLineMarkerContributor extends RunLineMarkerContributor {

    @Nullable
    @Override
    public Info getInfo(@NotNull PsiElement element) {
        PsiElement target;
        if (RsElementUtil.getElementType(element) == RsDocElementTypes.DOC_DATA) {
            if (!(element.getParent() instanceof RsDocCodeFenceStartEnd)) return null;
            RsDocCodeFenceStartEnd parent = (RsDocCodeFenceStartEnd) element.getParent();
            if (!(parent.getParent() instanceof RsDocCodeFence)) return null;
            RsDocCodeFence fence = (RsDocCodeFence) parent.getParent();
            if (fence.getStart() != parent) return null;
            target = fence;
        } else if (RsElementUtil.getElementType(element) == RsElementTypes.IDENTIFIER) {
            PsiElement parent = element.getParent();
            if (!(parent instanceof RsNameIdentifierOwner)) return null;
            if (element != ((RsNameIdentifierOwner) parent).getNameIdentifier()) return null;
            if (parent instanceof RsFunction && CargoExecutableRunConfigurationProducer.isMainFunction((RsFunction) parent)) return null;
            target = parent;
        } else {
            return null;
        }

        Object state = new CargoTestRunConfigurationProducer().findTestConfig(Collections.singletonList(target), false);
        if (state == null) {
            state = new CargoBenchRunConfigurationProducer().findTestConfig(Collections.singletonList(target), false);
        }
        if (state == null) return null;

        // Can't directly access state fields from Object, so cast to the appropriate type
        // This is a simplification - the actual type depends on the producer
        Icon icon = getTestStateIcon(target);
        if (icon == null) {
            icon = AllIcons.RunConfigurations.TestState.Run;
        }

        Icon finalIcon = icon;
        return new Info(
            finalIcon,
            psiElement -> "",
            ExecutorAction.getActions(1)
        );
    }

    @Nullable
    public static Icon getTestStateIcon(@NotNull PsiElement sourceElement) {
        String url;
        if (sourceElement instanceof RsQualifiedNamedElement) {
            url = CargoTestLocator.getTestUrl((RsQualifiedNamedElement) sourceElement);
        } else if (sourceElement instanceof RsDocCodeFence) {
            DocTestContext ctx = DoctestCtxUtil.getDoctestCtx((RsDocCodeFence) sourceElement);
            if (ctx == null) return null;
            url = CargoTestLocator.getTestUrl(ctx);
        } else {
            return null;
        }

        TestStateStorage.Record record = TestStateStorage.getInstance(sourceElement.getProject()).getState(url);
        if (record == null) return CargoIcons.TEST;
        TestStateInfo.Magnitude magnitude = TestIconMapper.getMagnitude(record.magnitude);
        if (magnitude == TestStateInfo.Magnitude.PASSED_INDEX || magnitude == TestStateInfo.Magnitude.COMPLETE_INDEX) {
            return CargoIcons.TEST_GREEN;
        }
        if (magnitude == TestStateInfo.Magnitude.ERROR_INDEX || magnitude == TestStateInfo.Magnitude.FAILED_INDEX) {
            return CargoIcons.TEST_RED;
        }
        return CargoIcons.TEST;
    }
}
