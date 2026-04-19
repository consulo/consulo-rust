/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import com.intellij.openapi.application.ApplicationManager;
import io.github.z4kn4fein.semver.Version;
import io.github.z4kn4fein.semver.StringExtensionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.stdext.RsResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public interface CratesLocalIndexService {

    @NotNull
    RsResult<CargoRegistryCrate, Error> getCrate(@NotNull String crateName);

    @NotNull
    RsResult<List<String>, Error> getAllCrateNames();

    @NotNull
    static CratesLocalIndexService getInstance() {
        return ApplicationManager.getApplication().getService(CratesLocalIndexService.class);
    }

    @Nullable
    static CratesLocalIndexService getInstanceIfCreated() {
        return ApplicationManager.getApplication().getServiceIfCreated(CratesLocalIndexService.class);
    }

    abstract class Error {
        public static final Error UPDATING = new Error() {
            @Override
            public String toString() {
                return "CratesLocalIndexService.Error.Updating(Index is being updated)";
            }
        };

        public static final Error NOT_YET_LOADED = new Error() {
            @Override
            public String toString() {
                return "CratesLocalIndexService.Error.NotYetLoaded(The index is not yet loaded)";
            }
        };

        public static final Error DISPOSED = new Error() {
            @Override
            public String toString() {
                return "CratesLocalIndexService.Error.Disposed(The service has been disposed)";
            }
        };

        public static abstract class InternalError extends Error {
            public static class NoCargoIndex extends InternalError {
                private final Path myPath;

                public NoCargoIndex(@NotNull Path path) {
                    myPath = path;
                }

                @NotNull
                public Path getPath() {
                    return myPath;
                }

                @Override
                public String toString() {
                    return "NoCargoIndex(" + myPath + ")";
                }
            }

            public static class RepoReadError extends InternalError {
                private final Path myPath;
                private final String myMessage;

                public RepoReadError(@NotNull Path path, @NotNull String message) {
                    myPath = path;
                    myMessage = message;
                }

                @Override
                public String toString() {
                    return "RepoReadError(" + myPath + ", " + myMessage + ")";
                }
            }

            public static class PersistentHashMapInitError extends InternalError {
                private final Path myPath;
                private final String myMessage;

                public PersistentHashMapInitError(@NotNull Path path, @NotNull String message) {
                    myPath = path;
                    myMessage = message;
                }

                @Override
                public String toString() {
                    return "PersistentHashMapInitError(" + myPath + ", " + myMessage + ")";
                }
            }

            public static class PersistentHashMapWriteError extends InternalError {
                private final String myMessage;

                public PersistentHashMapWriteError(@NotNull String message) {
                    myMessage = message;
                }

                @Override
                public String toString() {
                    return "PersistentHashMapWriteError(" + myMessage + ")";
                }
            }

            public static class PersistentHashMapReadError extends InternalError {
                private final String myMessage;

                public PersistentHashMapReadError(@NotNull String message) {
                    myMessage = message;
                }

                @Override
                public String toString() {
                    return "PersistentHashMapReadError(" + myMessage + ")";
                }
            }
        }
    }

    class CargoRegistryCrate {
        private final List<CargoRegistryCrateVersion> myVersions;

        public CargoRegistryCrate(@NotNull List<CargoRegistryCrateVersion> versions) {
            myVersions = versions;
        }

        @NotNull
        public List<CargoRegistryCrateVersion> getVersions() {
            return myVersions;
        }

        @NotNull
        public List<CargoRegistryCrateVersion> getSortedVersions() {
            List<CargoRegistryCrateVersion> sorted = new ArrayList<>(myVersions);
            sorted.sort(Comparator.comparing(CargoRegistryCrateVersion::getSemanticVersion,
                Comparator.nullsLast(Comparator.naturalOrder())));
            return sorted;
        }

        @TestOnly
        @NotNull
        public static CargoRegistryCrate of(@NotNull String... versions) {
            List<CargoRegistryCrateVersion> versionList = new ArrayList<>();
            for (String v : versions) {
                versionList.add(new CargoRegistryCrateVersion(v, false, Collections.emptyList()));
            }
            return new CargoRegistryCrate(versionList);
        }
    }

    class CargoRegistryCrateVersion {
        private final String myVersion;
        private final boolean myIsYanked;
        private final List<String> myFeatures;

        public CargoRegistryCrateVersion(@NotNull String version, boolean isYanked, @NotNull List<String> features) {
            myVersion = version;
            myIsYanked = isYanked;
            myFeatures = features;
        }

        @NotNull
        public String getVersion() {
            return myVersion;
        }

        public boolean isYanked() {
            return myIsYanked;
        }

        @NotNull
        public List<String> getFeatures() {
            return myFeatures;
        }

        @Nullable
        public Version getSemanticVersion() {
            return StringExtensionsKt.toVersionOrNull(myVersion, false);
        }
    }
}
