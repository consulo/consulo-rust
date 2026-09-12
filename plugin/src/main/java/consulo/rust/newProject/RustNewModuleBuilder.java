/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.newProject;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.content.bundle.Sdk;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.creation.scratch.NewModuleBuilder;
import consulo.module.creation.scratch.NewModuleBuilderProcessor;
import consulo.module.creation.scratch.NewModuleContext;
import consulo.module.creation.scratch.NewModuleContextGroup;
import consulo.rust.icon.RustIconGroup;
import consulo.rust.module.extension.RustMutableModuleExtension;
import consulo.ui.ex.wizard.WizardStep;
import jakarta.annotation.Nonnull;
import org.rust.cargo.CargoConstants;

import java.util.function.Consumer;

/**
 * Adds a Rust entry to the new project / new module wizard. The created module carries the Rust
 * module extension bound to the toolchain chosen in the wizard, with the standard Cargo layout
 * folders registered.
 */
@ExtensionImpl(id = "rust")
public class RustNewModuleBuilder implements NewModuleBuilder {

    @Override
    public void setupContext(@Nonnull NewModuleContext context) {
        NewModuleContextGroup group = context.addGroup("rust", LocalizeValue.localizeTODO("Rust"));

        group.add(LocalizeValue.localizeTODO("Empty"), RustIconGroup.rust(), new NewModuleBuilderProcessor<RustNewModuleWizardContext>() {
            @Nonnull
            @Override
            public RustNewModuleWizardContext createContext(boolean isNewProject) {
                return new RustNewModuleWizardContext(isNewProject);
            }

            @Override
            public void buildSteps(@Nonnull Consumer<WizardStep<RustNewModuleWizardContext>> consumer,
                                   @Nonnull RustNewModuleWizardContext context) {
                consumer.accept(new RustNewModuleSetupStep<>(context));
            }

            @RequiredReadAction
            @Override
            public void process(@Nonnull RustNewModuleWizardContext context,
                                @Nonnull ContentEntry contentEntry,
                                @Nonnull ModifiableRootModel modifiableRootModel) {
                setupModule(context, contentEntry, modifiableRootModel);
            }
        });
    }

    @RequiredReadAction
    private static void setupModule(@Nonnull RustNewModuleWizardContext context,
                                    @Nonnull ContentEntry contentEntry,
                                    @Nonnull ModifiableRootModel modifiableRootModel) {
        RustMutableModuleExtension extension = modifiableRootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
        if (extension != null) {
            extension.setEnabled(true);

            Sdk toolchainBundle = context.getToolchainBundle();
            if (toolchainBundle != null) {
                extension.getInheritableSdk().set(null, toolchainBundle);
                modifiableRootModel.addModuleExtensionSdkEntry(extension);
            }
        }

        String contentUrl = contentEntry.getUrl();
        contentEntry.addFolder(contentUrl + "/src", ProductionContentFolderTypeProvider.getInstance());
        contentEntry.addFolder(contentUrl + "/" + CargoConstants.ProjectLayout.target, ExcludedContentFolderTypeProvider.getInstance());
    }
}
