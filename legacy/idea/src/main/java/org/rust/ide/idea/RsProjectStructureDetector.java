/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.idea;

import com.intellij.ide.util.importProject.ModuleDescriptor;
import com.intellij.ide.util.importProject.ProjectDescriptor;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.DetectedSourceRoot;
import com.intellij.ide.util.projectWizard.importSources.ProjectFromSourcesBuilder;
import com.intellij.ide.util.projectWizard.importSources.ProjectStructureDetector;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.ide.module.CargoConfigurationWizardStep;
import org.rust.ide.module.RsModuleType;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RsProjectStructureDetector extends ProjectStructureDetector {

    @NotNull
    @Override
    public DirectoryProcessingResult detectRoots(
        @NotNull File dir,
        @NotNull File @NotNull [] children,
        @NotNull File base,
        @NotNull List<DetectedProjectRoot> result
    ) {
        boolean hasManifest = Arrays.stream(children)
            .anyMatch(f -> f.getName().equals(CargoConstants.MANIFEST_FILE));
        if (hasManifest) {
            result.add(new DetectedProjectRoot(dir) {
                @NotNull
                @Override
                public String getRootTypeName() {
                    return RsBundle.message("rust");
                }
            });
        }

        return DirectoryProcessingResult.SKIP_CHILDREN;
    }

    @Override
    public void setupProjectStructure(
        @NotNull Collection<DetectedProjectRoot> roots,
        @NotNull ProjectDescriptor projectDescriptor,
        @NotNull ProjectFromSourcesBuilder builder
    ) {
        if (roots.size() != 1) return;
        DetectedProjectRoot root = roots.iterator().next();
        if (builder.hasRootsFromOtherDetectors(this) || !projectDescriptor.getModules().isEmpty()) {
            return;
        }

        ModuleDescriptor moduleDescriptor = new ModuleDescriptor(
            root.getDirectory(),
            RsModuleType.INSTANCE,
            Collections.<DetectedSourceRoot>emptyList()
        );
        projectDescriptor.setModules(List.of(moduleDescriptor));
    }

    @NotNull
    @Override
    public List<ModuleWizardStep> createWizardSteps(
        @NotNull ProjectFromSourcesBuilder builder,
        @NotNull ProjectDescriptor projectDescriptor,
        Icon stepIcon
    ) {
        return List.of(new CargoConfigurationWizardStep(builder.getContext(), updater -> {
            List<ModuleDescriptor> modules = projectDescriptor.getModules();
            if (!modules.isEmpty()) {
                modules.get(0).addConfigurationUpdater(updater);
            }
        }));
    }
}
