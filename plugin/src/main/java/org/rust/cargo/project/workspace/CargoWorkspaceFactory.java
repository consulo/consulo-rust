package org.rust.cargo.project.workspace;

import org.rust.cargo.api.CargoConfig;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.CargoWorkspaceData;

import java.nio.file.Path;

/**
 * Builds {@link CargoWorkspace} instances from the data cargo reports.
 */
public final class CargoWorkspaceFactory {
    private CargoWorkspaceFactory() {
    }

    public static CargoWorkspace deserialize(Path manifestPath,
                                             CargoWorkspaceData data,
                                             CfgOptions cfgOptions,
                                             CargoConfig cargoConfig) {
        return WorkspaceImpl.deserialize(manifestPath, data, cfgOptions, cargoConfig);
    }

    public static CargoWorkspace deserialize(Path manifestPath, CargoWorkspaceData data) {
        return deserialize(manifestPath, data, CfgOptions.DEFAULT, CargoConfig.DEFAULT);
    }
}
