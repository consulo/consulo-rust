/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.macros;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.settings.RsProjectSettingsServiceBase;
import org.rust.cargo.api.settings.RsSettingsListener;
import org.rust.cargo.api.settings.RustProjectSettingsService;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainLocator;
import org.rust.experiments.RsExperiments;
import org.rust.lang.core.macros.proc.ProcMacroFlags;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.util.*;

@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public final class ProcMacroApplicationService implements Disposable {

    @Nonnull
    private final Map<DistributionIdAndExpanderPath, ProcMacroServerPool> myServers = new HashMap<>();

    public ProcMacroApplicationService() {
        var connect = ApplicationManager.getApplication().getMessageBus().connect(this);

        connect.subscribe(RsProjectSettingsServiceBase.RUST_SETTINGS_TOPIC,
            new RsSettingsListener() {
                @Override
                public void settingsChanged(
                    @Nonnull RsProjectSettingsServiceBase.SettingsChangedEventBase<?> e
                ) {
                    if (e instanceof RustProjectSettingsService.SettingsChangedEvent) {
                        var event = (RustProjectSettingsService.SettingsChangedEvent) e;
                        String oldId = getDistributionId(RsToolchainLocator.load(event.getOldState()));
                        String newId = getDistributionId(RsToolchainLocator.load(event.getNewState()));
                        if (!Objects.equals(oldId, newId)) {
                            removeUnusableServers();
                        }
                    }
                }
            });

        connect.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
            @Override
            public void projectClosed(@Nonnull Project project) {
                removeUnusableServers();
            }
        });
    }

    @Nullable
    public synchronized ProcMacroServerPool getServer(
        @Nonnull RsToolchainBase toolchain,
        boolean needsVersionCheck,
        @Nonnull Path procMacroExpanderPath
    ) {
        if (!ProcMacroFlags.isAnyEnabled()) {
            return null;
        }

        String id = getDistributionId(toolchain);
        DistributionIdAndExpanderPath key = new DistributionIdAndExpanderPath(id, needsVersionCheck, procMacroExpanderPath);
        ProcMacroServerPool server = myServers.get(key);
        if (server == null) {
            server = ProcMacroServerPool.create(toolchain, needsVersionCheck, procMacroExpanderPath, this);
            myServers.put(key, server);
        }
        return server;
    }

    private synchronized void removeUnusableServers() {
        Set<String> distributionIds = new HashSet<>();
        Set<Path> procMacroExpanderPaths = new HashSet<>();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            RsToolchainBase toolchain = RsToolchainLocator.fromSettings(project);
            if (toolchain != null) {
                distributionIds.add(getDistributionId(toolchain));
            }
            for (var cargoProject : CargoProjectsService.getInstance(project).getAllProjects()) {
                Path expanderPath = cargoProject.getProcMacroExpanderPath();
                if (expanderPath != null) {
                    procMacroExpanderPaths.add(expanderPath);
                }
            }
        }
        List<ProcMacroServerPool> toDispose = new ArrayList<>();
        Iterator<Map.Entry<DistributionIdAndExpanderPath, ProcMacroServerPool>> it = myServers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DistributionIdAndExpanderPath, ProcMacroServerPool> entry = it.next();
            DistributionIdAndExpanderPath key = entry.getKey();
            if (!distributionIds.contains(key.distributionId) || !procMacroExpanderPaths.contains(key.procMacroExpanderPath)) {
                toDispose.add(entry.getValue());
                it.remove();
            }
        }
        for (ProcMacroServerPool pool : toDispose) {
            Disposer.dispose(pool);
        }
    }

    @Override
    public void dispose() {
    }

    @Nonnull
    public static ProcMacroApplicationService getInstance() {
        return ApplicationManager.getApplication().getService(ProcMacroApplicationService.class);
    }

    public static boolean isFullyEnabled() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)
            && (OpenApiUtil.isFeatureEnabled(RsExperiments.PROC_MACROS)
            || (OpenApiUtil.isFeatureEnabled(RsExperiments.FN_LIKE_PROC_MACROS)
            && OpenApiUtil.isFeatureEnabled(RsExperiments.DERIVE_PROC_MACROS)
            && OpenApiUtil.isFeatureEnabled(RsExperiments.ATTR_PROC_MACROS)));
    }


    @Nonnull
    private static String getDistributionId(@Nullable RsToolchainBase toolchain) {
        return "Local";
    }

    private static final class DistributionIdAndExpanderPath {
        @Nonnull
        final String distributionId;
        final boolean needsVersionCheck;
        @Nonnull
        final Path procMacroExpanderPath;

        DistributionIdAndExpanderPath(@Nonnull String distributionId, boolean needsVersionCheck, @Nonnull Path procMacroExpanderPath) {
            this.distributionId = distributionId;
            this.needsVersionCheck = needsVersionCheck;
            this.procMacroExpanderPath = procMacroExpanderPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DistributionIdAndExpanderPath)) {
                return false;
            }
            DistributionIdAndExpanderPath that = (DistributionIdAndExpanderPath) o;
            return needsVersionCheck == that.needsVersionCheck
                && Objects.equals(distributionId, that.distributionId)
                && Objects.equals(procMacroExpanderPath, that.procMacroExpanderPath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(distributionId, needsVersionCheck, procMacroExpanderPath);
        }
    }
}
