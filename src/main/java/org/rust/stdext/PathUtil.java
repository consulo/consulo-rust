/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
    public static Path toPath(@NotNull String str) {
        return Paths.get(str);
    }

    @Nullable
    public static Path toPathOrNull(@NotNull String str) {
        try {
            return Paths.get(str);
        } catch (InvalidPathException e) {
            LOG.warn(e);
            return null;
        }
    }

    @Nullable
    public static Path resolveOrNull(@NotNull Path path, @NotNull String other) {
        try {
            return path.resolve(other);
        } catch (InvalidPathException e) {
            LOG.warn(e);
            return null;
        }
    }

    public static boolean isExecutable(@NotNull Path path) {
        return Files.isExecutable(path);
    }

    public static boolean isDirectory(@NotNull Path path) {
        return Files.isDirectory(path);
    }

    @NotNull
    public static String getSystemIndependentPath(@NotNull Path path) {
        return path.toString().replace('\\', '/');
    }

    @NotNull
    public static Path cleanDirectory(@NotNull Path dir) throws IOException {
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

    @NotNull
    public static DataOutputStream newDeflaterDataOutputStream(@NotNull Path path) throws IOException {
        return new DataOutputStream(new DeflaterOutputStream(Files.newOutputStream(path)));
    }

    @NotNull
    public static DataInputStream newInflaterDataInputStream(@NotNull Path path) throws IOException {
        return new DataInputStream(new InflaterInputStream(Files.newInputStream(path)));
    }

    public static void delete(@NotNull Path path, boolean recursive) throws IOException {
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
