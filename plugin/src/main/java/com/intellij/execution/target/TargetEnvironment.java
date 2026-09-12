package com.intellij.execution.target;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/** Environment a command runs in, with upload volumes mapping local roots onto target paths. */
public interface TargetEnvironment {
    default String getTypeId() { return ""; }
    default String getProjectRootOnTarget() { return ""; }
    default Map<UploadRoot, UploadableVolume> getUploadVolumes() { return Collections.emptyMap(); }
    default Process createProcess(TargetedCommandLine commandLine, Object progressIndicator) throws Exception {
        throw new UnsupportedOperationException("target execution not implemented in consulo stub");
    }
    default void shutdown() {}

    interface TargetPath {
        class Temporary implements TargetPath {
            public Temporary() {}
            public Temporary(String hint) {}
        }
        class Persistent implements TargetPath {
            public Persistent(String absolutePath) {}
        }
    }

    class UploadRoot {
        public final Path localRootPath;
        public final TargetPath targetRootPath;
        public UploadRoot(Path localRootPath, TargetPath targetRootPath) {
            this.localRootPath = localRootPath;
            this.targetRootPath = targetRootPath;
        }
    }

    interface UploadableVolume {
        default String getTargetRoot() { return ""; }
        default String resolveTargetPath(String localRelativePath) { return localRelativePath; }
        default void upload(String relativePath, Object ignoredProgress) throws Exception {}
    }
}
