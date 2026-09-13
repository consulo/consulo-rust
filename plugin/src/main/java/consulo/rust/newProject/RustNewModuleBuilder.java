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

        group.add(LocalizeValue.localizeTODO("From Cargo"), RustIconGroup.cargo(), new NewModuleBuilderProcessor<RustNewModuleWizardContext>() {
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

        // process() runs on the UI thread inside NewOrImportModuleUtil.doCreate and the root model is
        // committed right after it returns, so cargo cannot run here.
        consulo.project.Project project = modifiableRootModel.getProject();
        consulo.virtualFileSystem.VirtualFile contentRoot = contentEntry.getFile();
        Sdk bundle = context.getToolchainBundle();
        boolean binary = context.isBinary();
        String crateName = context.getName();
        if (contentRoot != null && bundle != null) {
            consulo.application.Application.get().invokeLater(
                () -> generateCrate(project, bundle, contentRoot, crateName, binary));
        }
    }

    /**
     * Runs {@code cargo init} in the freshly created module and opens the entry file once the project
     * is registered.
     * <p>
     * Generation runs on a background task: {@code cargo init} blocks, and the attach that follows is
     * asynchronous, so the source file may only be opened after that attach has completed - otherwise
     * the editor asks about a project that does not exist yet and reports the file as belonging to none.
     */
    private static void generateCrate(@Nonnull consulo.project.Project project,
                                      @Nonnull Sdk bundle,
                                      @Nonnull consulo.virtualFileSystem.VirtualFile contentRoot,
                                      @Nonnull String crateName,
                                      boolean binary) {
        org.rust.cargo.toolchain.RsToolchainBase toolchain = consulo.rust.bundle.RustBundleType.toToolchain(bundle);
        if (toolchain == null) {
            return;
        }

        consulo.application.progress.Task.Backgroundable.queue(
            project,
            LocalizeValue.localizeTODO("Creating Cargo project..."),
            indicator -> {
                org.rust.cargo.toolchain.tools.Cargo cargo =
                    org.rust.cargo.toolchain.tools.Cargo.cargoOrWrapper(toolchain, null);

                org.rust.stdext.RsResult<org.rust.cargo.toolchain.tools.Cargo.GeneratedFilesHolder, ?> result =
                    cargo.init(project, project, contentRoot, crateName, binary, "none");
                if (!(result instanceof org.rust.stdext.RsResult.Ok)) {
                    return;
                }
                org.rust.cargo.toolchain.tools.Cargo.GeneratedFilesHolder generated =
                    ((org.rust.stdext.RsResult.Ok<org.rust.cargo.toolchain.tools.Cargo.GeneratedFilesHolder, ?>) result).ok();

                indicator.setText(LocalizeValue.localizeTODO("Attaching Cargo project...").get());
                org.rust.cargo.api.model.CargoProjectsService cargoProjects =
                    org.rust.cargo.project.model.CargoProjectServiceUtil.getCargoProjects(project);

                cargoProjects
                    .attachCargoProjectAsync(org.rust.openapiext.OpenApiUtil.getPathAsPath(generated.getManifest()))
                    .whenComplete((ignored, throwable) -> openEntryFile(project, generated));
            });
    }

    private static void openEntryFile(@Nonnull consulo.project.Project project,
                                      @Nonnull org.rust.cargo.toolchain.tools.Cargo.GeneratedFilesHolder generated) {
        java.util.List<consulo.virtualFileSystem.VirtualFile> sourceFiles = generated.getSourceFiles();
        if (sourceFiles.isEmpty()) {
            return;
        }
        consulo.virtualFileSystem.VirtualFile entry = sourceFiles.get(0);
        consulo.application.Application.get().invokeLater(() -> {
            if (!project.isDisposed() && entry.isValid()) {
                consulo.fileEditor.FileEditorManager.getInstance(project).openFile(entry, true);
            }
        });
    }
}
