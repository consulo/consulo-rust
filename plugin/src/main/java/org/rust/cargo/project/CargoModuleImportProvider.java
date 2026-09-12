/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.localize.LocalizeValue;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.creation.importing.ModuleImportContext;
import consulo.module.creation.importing.ModuleImportProvider;
import consulo.project.Project;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.project.startup.StartupManager;
import consulo.rust.bundle.RustBundleType;
import consulo.rust.icon.RustIconGroup;
import consulo.rust.module.extension.RustMutableModuleExtension;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

import java.io.File;
import java.util.function.Consumer;

/**
 * Turns a directory holding a {@code Cargo.toml} into a module: registers the directory as the
 * content root, marks the Cargo layout folders as sources, tests and excluded output, and requests
 * toolchain discovery plus a workspace refresh once the project has been initialized.
 */
@ExtensionImpl
public class CargoModuleImportProvider implements ModuleImportProvider<ModuleImportContext> {

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Cargo");
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return RustIconGroup.rust();
    }

    @Override
    public boolean canImport(@Nonnull File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            return new File(fileOrDirectory, CargoConstants.MANIFEST_FILE).isFile();
        }
        return CargoConstants.MANIFEST_FILE.equalsIgnoreCase(fileOrDirectory.getName());
    }

    @Override
    public Coroutine<Object, Object> process(@Nonnull ModuleImportContext context,
                                             @Nonnull Project project,
                                             @Nonnull ModifiableModuleModel model,
                                             @Nonnull Consumer<Module> newModuleConsumer) {
        return ReadLock.<Object, ModifiableRootModel>apply(input -> {
                String path = context.getPath();
                VirtualFile contentRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);

                Module module = model.newModule(moduleName(context, contentRoot, project), path);
                newModuleConsumer.accept(module);

                ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                if (contentRoot == null) {
                    rootModel.addContentEntry(VirtualFileUtil.pathToUrl(path));
                }
                else {
                    ContentEntry contentEntry = rootModel.addContentEntry(contentRoot);
                    CargoProjectServiceUtil.setup(contentEntry, contentRoot);
                }
                enableRustExtension(rootModel);
                return rootModel;
            })
            .toCoroutine()
            .then(WriteLock.<ModifiableRootModel, Object>apply((rootModel, continuation) -> {
                rootModel.commit();

                StartupManager.getInstance(project)
                    .runWhenProjectIsInitialized(() -> CargoProjectServiceUtil.guessAndSetupRustProject(project));
                return null;
            }));
    }

    /**
     * Marks the freshly created module as a Rust module and binds it to a known Rust toolchain
     * bundle when one is available.
     * <p>
     * Selecting a bundle is not by itself enough to put the toolchain on the module - the order entry
     * that carries the bundle roots, and with them the standard library sources, has to be added as
     * well.
     */
    private static void enableRustExtension(@Nonnull ModifiableRootModel rootModel) {
        RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
        if (extension == null) {
            return;
        }

        extension.setEnabled(true);

        Sdk toolchainBundle = SdkTable.getInstance().findMostRecentSdk(sdk -> sdk.getSdkType() instanceof RustBundleType);
        if (toolchainBundle != null) {
            extension.getInheritableSdk().set(null, toolchainBundle);
            rootModel.addModuleExtensionSdkEntry(extension);
        }
    }

    @Nonnull
    private static String moduleName(@Nonnull ModuleImportContext context,
                                     @Nullable VirtualFile contentRoot,
                                     @Nonnull Project project) {
        String name = context.getName();
        if (!StringUtil.isEmptyOrSpaces(name)) {
            return name;
        }
        return contentRoot == null ? project.getName() : contentRoot.getName();
    }
}
