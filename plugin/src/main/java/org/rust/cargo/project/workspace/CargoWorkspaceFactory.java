package org.rust.cargo.project.workspace;

import org.rust.cargo.api.CargoConfig;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.CargoWorkspaceData;

import jakarta.annotation.Nullable;

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

    /**
     * The data {@code workspace} would be rebuilt from, or {@code null} for a workspace this factory did
     * not build. This is what gets written into the project model so the next open needs no Cargo run.
     */
    @Nullable
    public static CargoWorkspaceData dataOf(@Nullable CargoWorkspace workspace) {
        return workspace instanceof WorkspaceImpl impl ? impl.getWorkspaceData() : null;
    }

    /** The cfg options {@code workspace} was built with, needed to rebuild it faithfully. */
    @Nullable
    public static CfgOptions cfgOptionsOf(@Nullable CargoWorkspace workspace) {
        return workspace instanceof WorkspaceImpl impl ? impl.getCfgOptions() : null;
    }
}
