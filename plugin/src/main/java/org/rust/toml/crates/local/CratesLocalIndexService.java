/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import io.github.milkdrinkers.javasemver.Version;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.stdext.RsResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ServiceAPI(ComponentScope.APPLICATION)
public interface CratesLocalIndexService {

    @Nonnull
    RsResult<CargoRegistryCrate, Error> getCrate(@Nonnull String crateName);

    @Nonnull
    RsResult<List<String>, Error> getAllCrateNames();

    @Nonnull
    static CratesLocalIndexService getInstance() {
        return ApplicationManager.getApplication().getService(CratesLocalIndexService.class);
    }

    @Nullable
    static CratesLocalIndexService getInstanceIfCreated() {
        return ApplicationManager.getApplication().getInstanceIfCreated(CratesLocalIndexService.class);
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

                public NoCargoIndex(@Nonnull Path path) {
                    myPath = path;
                }

                @Nonnull
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

                public RepoReadError(@Nonnull Path path, @Nonnull String message) {
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

                public PersistentHashMapInitError(@Nonnull Path path, @Nonnull String message) {
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

                public PersistentHashMapWriteError(@Nonnull String message) {
                    myMessage = message;
                }

                @Override
                public String toString() {
                    return "PersistentHashMapWriteError(" + myMessage + ")";
                }
            }

            public static class PersistentHashMapReadError extends InternalError {
                private final String myMessage;

                public PersistentHashMapReadError(@Nonnull String message) {
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

        public CargoRegistryCrate(@Nonnull List<CargoRegistryCrateVersion> versions) {
            myVersions = versions;
        }

        @Nonnull
        public List<CargoRegistryCrateVersion> getVersions() {
            return myVersions;
        }

        @Nonnull
        public List<CargoRegistryCrateVersion> getSortedVersions() {
            List<CargoRegistryCrateVersion> sorted = new ArrayList<>(myVersions);
            sorted.sort(Comparator.comparing(CargoRegistryCrateVersion::getSemanticVersion,
                Comparator.nullsLast(Comparator.naturalOrder())));
            return sorted;
        }

        
        @Nonnull
        public static CargoRegistryCrate of(@Nonnull String... versions) {
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

        public CargoRegistryCrateVersion(@Nonnull String version, boolean isYanked, @Nonnull List<String> features) {
            myVersion = version;
            myIsYanked = isYanked;
            myFeatures = features;
        }

        @Nonnull
        public String getVersion() {
            return myVersion;
        }

        public boolean isYanked() {
            return myIsYanked;
        }

        @Nonnull
        public List<String> getFeatures() {
            return myFeatures;
        }

        @Nullable
        public Version getSemanticVersion() {
            return Version.coerce(myVersion).orElse(null);
        }
    }
}
