/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class PathUtil {
    private static final Logger LOG = Logger.getInstance("#org.rust.stdext");

    private PathUtil() {
    }

    @Nonnull
    public static Path toPath(@Nonnull String str) {
        return Paths.get(str);
    }

    @Nullable
    public static Path toPathOrNull(@Nonnull String str) {
        try {
            return Paths.get(str);
        } catch (InvalidPathException e) {
            LOG.warn(e);
            return null;
        }
    }

    @Nullable
    public static Path resolveOrNull(@Nonnull Path path, @Nonnull String other) {
        try {
            return path.resolve(other);
        } catch (InvalidPathException e) {
            LOG.warn(e);
            return null;
        }
    }

    public static boolean isExecutable(@Nonnull Path path) {
        return Files.isExecutable(path);
    }

    public static boolean isDirectory(@Nonnull Path path) {
        return Files.isDirectory(path);
    }

    @Nonnull
    public static String getSystemIndependentPath(@Nonnull Path path) {
        return path.toString().replace('\\', '/');
    }

    @Nonnull
    public static Path cleanDirectory(@Nonnull Path dir) throws IOException {
        return Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    Files.delete(file);
                } catch (IOException ignored) {
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) {
                if (!d.equals(dir)) {
                    try {
                        Files.delete(d);
                    } catch (IOException ignored) {
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Nonnull
    public static DataOutputStream newDeflaterDataOutputStream(@Nonnull Path path) throws IOException {
        return new DataOutputStream(new DeflaterOutputStream(Files.newOutputStream(path)));
    }

    @Nonnull
    public static DataInputStream newInflaterDataInputStream(@Nonnull Path path) throws IOException {
        return new DataInputStream(new InflaterInputStream(Files.newInputStream(path)));
    }

    public static void delete(@Nonnull Path path, boolean recursive) throws IOException {
        if (recursive) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.delete(path);
        }
    }
}
