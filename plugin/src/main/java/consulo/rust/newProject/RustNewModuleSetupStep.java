/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.newProject;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.module.creation.ui.UnifiedProjectOrModuleNameStep;
import consulo.module.ui.BundleBox;
import consulo.module.ui.BundleBoxBuilder;
import consulo.rust.bundle.RustBundleType;
import consulo.ui.ComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import jakarta.annotation.Nonnull;

/**
 * Name and location step of the new module wizard, extended with a Rust toolchain chooser.
 */
public class RustNewModuleSetupStep<C extends RustNewModuleWizardContext> extends UnifiedProjectOrModuleNameStep<C> {
    private BundleBox myBundleBox;
    private consulo.ui.RadioGroup<Boolean> myCrateTypeGroup;

    public RustNewModuleSetupStep(@Nonnull C context) {
        super(context);
    }

    @RequiredUIAccess
    @Override
    protected void extend(@Nonnull FormBuilder builder, @Nonnull Disposable uiDisposable) {
        super.extend(builder, uiDisposable);

        BundleBoxBuilder boxBuilder = BundleBoxBuilder.create(uiDisposable);
        boxBuilder.withSdkTypeFilterByClass(RustBundleType.class);

        myBundleBox = boxBuilder.build();

        ComboBox<BundleBox.BundleBoxItem> component = myBundleBox.getComponent();
        builder.addLabeled(LocalizeValue.localizeTODO("Toolchain:"), component);
        component.selectFirst();

        // cargo init offers exactly two crate kinds; the choice drives --bin vs --lib
        myCrateTypeGroup = consulo.ui.RadioGroup.create();
        consulo.ui.layout.HorizontalLayout crateTypeLayout = consulo.ui.layout.HorizontalLayout.create();
        crateTypeLayout.add(myCrateTypeGroup.newButton(LocalizeValue.localizeTODO("Binary"), Boolean.TRUE));
        crateTypeLayout.add(myCrateTypeGroup.newButton(LocalizeValue.localizeTODO("Library"), Boolean.FALSE));
        myCrateTypeGroup.setValue(Boolean.TRUE);
        builder.addLabeled(LocalizeValue.localizeTODO("Crate type:"), crateTypeLayout);
    }

    @Override
    public void onStepLeave(@Nonnull C context) {
        super.onStepLeave(context);

        BundleBox.BundleBoxItem selectedItem = myBundleBox.getComponent().getValue();
        context.setToolchainBundle(selectedItem == null ? null : selectedItem.getBundle());
        Boolean binary = myCrateTypeGroup == null ? null : myCrateTypeGroup.getValue();
        context.setBinary(binary == null || binary);
    }
}
