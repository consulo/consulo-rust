/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceBase;
import org.rust.cargo.project.settings.RustProjectSettingsService;
import org.rust.cargo.project.settings.RustProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.wsl.RsWslToolchain;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.util.*;

@Service
public final class ProcMacroApplicationService implements Disposable {

    @NotNull
    private final Map<DistributionIdAndExpanderPath, ProcMacroServerPool> myServers = new HashMap<>();

    public ProcMacroApplicationService() {
        var connect = ApplicationManager.getApplication().getMessageBus().connect(this);

        connect.subscribe(RsProjectSettingsServiceBase.RUST_SETTINGS_TOPIC,
            new RsProjectSettingsServiceBase.RsSettingsListener() {
                @Override
                public <T extends RsProjectSettingsServiceBase.RsProjectSettingsBase<T>> void settingsChanged(
                    @NotNull RsProjectSettingsServiceBase.SettingsChangedEventBase<T> e
                ) {
                    if (e instanceof RustProjectSettingsService.SettingsChangedEvent) {
                        var event = (RustProjectSettingsService.SettingsChangedEvent) e;
                        String oldId = getDistributionId(event.getOldState().getToolchain());
                        String newId = getDistributionId(event.getNewState().getToolchain());
                        if (!Objects.equals(oldId, newId)) {
                            removeUnusableServers();
                        }
                    }
                }
            });

        connect.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
            @Override
            public void projectClosed(@NotNull Project project) {
                removeUnusableServers();
            }
        });
    }

    @Nullable
    public synchronized ProcMacroServerPool getServer(
        @NotNull RsToolchainBase toolchain,
        boolean needsVersionCheck,
        @NotNull Path procMacroExpanderPath
    ) {
        if (!isAnyEnabled()) return null;

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
            RsToolchainBase toolchain = RustProjectSettingsServiceUtil.getRustSettings(project).getToolchain();
            if (toolchain != null) {
                distributionIds.add(getDistributionId(toolchain));
            }
            for (var cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
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
    public void dispose() {}

    @NotNull
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

    public static boolean isAnyEnabled() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)
            && (OpenApiUtil.isFeatureEnabled(RsExperiments.PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.FN_LIKE_PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.DERIVE_PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.ATTR_PROC_MACROS));
    }

    public static boolean isFunctionLikeEnabled() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)
            && (OpenApiUtil.isFeatureEnabled(RsExperiments.PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.FN_LIKE_PROC_MACROS));
    }

    public static boolean isDeriveEnabled() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)
            && (OpenApiUtil.isFeatureEnabled(RsExperiments.PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.DERIVE_PROC_MACROS));
    }

    public static boolean isAttrEnabled() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)
            && (OpenApiUtil.isFeatureEnabled(RsExperiments.PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.ATTR_PROC_MACROS));
    }

    @NotNull
    private static String getDistributionId(@Nullable RsToolchainBase toolchain) {
        if (toolchain instanceof RsWslToolchain) {
            return ((RsWslToolchain) toolchain).getWslPath().getDistributionId();
        }
        return "Local";
    }

    private static final class DistributionIdAndExpanderPath {
        @NotNull
        final String distributionId;
        final boolean needsVersionCheck;
        @NotNull
        final Path procMacroExpanderPath;

        DistributionIdAndExpanderPath(@NotNull String distributionId, boolean needsVersionCheck, @NotNull Path procMacroExpanderPath) {
            this.distributionId = distributionId;
            this.needsVersionCheck = needsVersionCheck;
            this.procMacroExpanderPath = procMacroExpanderPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DistributionIdAndExpanderPath)) return false;
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
