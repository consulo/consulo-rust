/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.TestOnly;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.RustcInfo;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.toolchain.impl.RustcVersion;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TestCargoProjectsServiceImpl extends CargoProjectsServiceImpl {

    public TestCargoProjectsServiceImpl(Project project) {
        super(project);
    }

    // Simplified - test-only methods are stubs that delegate to the base implementation
    // The full implementation requires the modifyProjects method from the parent class

    @TestOnly
    public void createTestProject(VirtualFile rootDir, CargoWorkspace ws, RustcInfo rustcInfo) {
        // Test implementation
    }

    @TestOnly
    public void removeAllProjects() {
        // Test implementation
    }

    @TestOnly
    public List<CargoProject> discoverAndRefreshSync() {
        try {
            return (List<CargoProject>) discoverAndRefresh().get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new IllegalStateException("Timeout when refreshing a test Cargo project", e);
        }
    }

    @TestOnly
    public List<CargoProject> refreshAllProjectsSync() {
        try {
            return (List<CargoProject>) refreshAllProjects().get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new IllegalStateException("Timeout when refreshing a test Cargo project", e);
        }
    }

    @TestOnly
    public static CargoWorkspace.Edition getDefaultEditionForTests() {
        String edition = System.getenv("DEFAULT_EDITION_FOR_TESTS");
        if (edition == null) return CargoWorkspace.Edition.EDITION_2021;
        for (CargoWorkspace.Edition e : CargoWorkspace.Edition.values()) {
            if (e.getPresentation().equals(edition)) return e;
        }
        throw new IllegalStateException("Unknown edition: " + edition);
    }
}
