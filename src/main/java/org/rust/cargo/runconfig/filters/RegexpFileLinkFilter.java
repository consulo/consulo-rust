/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;

import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for regexp-based output filters that extract
 * source code location from the output and add corresponding hyperlinks.
 * <p>
 * Can't use {@link com.intellij.execution.filters.RegexpFilter} directly because it doesn't handle
 * relative paths in 2017.1
 */
public class RegexpFileLinkFilter implements Filter, DumbAware {

    // TODO: named groups when supported
    @Language("RegExp")
    public static final String FILE_POSITION_RE = "((?:\\p{Alpha}:)?[0-9 a-z_A-Z\\-\\\\./]+):([0-9]+)(?::([0-9]+))?";

    private static final Pattern RUSTC_ABSOLUTE_PATH_RE = Pattern.compile("/rustc/\\w+/(.*)");

    private final Project myProject;
    private final VirtualFile myCargoProjectDirectory;
    private final Pattern myLinePattern;

    public RegexpFileLinkFilter(@NotNull Project project,
                                @NotNull VirtualFile cargoProjectDirectory,
                                @NotNull String lineRegExp) {
        if (!lineRegExp.contains(FILE_POSITION_RE)) {
            throw new IllegalArgumentException("lineRegExp must contain FILE_POSITION_RE");
        }
        if (lineRegExp.indexOf('^') >= 0 || lineRegExp.indexOf('$') >= 0) {
            throw new IllegalArgumentException("lineRegExp must not contain ^ or $");
        }
        myProject = project;
        myCargoProjectDirectory = cargoProjectDirectory;
        myLinePattern = Pattern.compile("^" + lineRegExp + "\\R?$");
    }

    @Nullable
    @Override
    public Result applyFilter(@NotNull String line, int entireLength) {
        Matcher match = matchLine(line);
        if (match == null) return null;

        String fileGroup = match.group(1);
        int fileGroupStart = match.start(1);
        int fileGroupEnd = match.end(1);
        int lineNumber = match.group(2) != null ? zeroBasedNumber(match.group(2)) : 0;
        int columnNumber = match.group(3) != null ? zeroBasedNumber(match.group(3)) : 0;

        int lineStart = entireLength - line.length();

        ResolvedPath file = resolveFilePath(fileGroup);
        OpenFileHyperlinkInfo link = file != null
            ? new OpenFileHyperlinkInfo(myProject, file.getFile(), lineNumber, columnNumber)
            : null;

        boolean grayedOut;
        if (file == null) {
            grayedOut = false;
        } else {
            grayedOut = !(file instanceof ResolvedPath.Workspace);
        }

        int end;
        if (match.group(3) != null) {
            end = match.end(3);
        } else if (match.group(2) != null) {
            end = match.end(2);
        } else {
            end = fileGroupEnd;
        }

        return new Result(
            lineStart + fileGroupStart,
            lineStart + end,
            link,
            grayedOut
        );
    }

    @Nullable
    public Matcher matchLine(@NotNull String line) {
        Matcher matcher = myLinePattern.matcher(line);
        return matcher.matches() ? matcher : null;
    }

    private int zeroBasedNumber(@NotNull String number) {
        try {
            return Math.max(0, Integer.parseInt(number) - 1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Nullable
    private ResolvedPath resolveFilePath(@NotNull String fileName) {
        String path = FileUtil.toSystemIndependentName(fileName);
        VirtualFile file = myCargoProjectDirectory.findFileByRelativePath(path);
        if (file != null) return new ResolvedPath.Workspace(file);

        ResolvedPath externalPath = resolveStdlibPath(fileName);
        if (externalPath == null) {
            externalPath = resolveCargoPath(fileName);
        }
        if (externalPath != null) return externalPath;

        // try to resolve absolute path
        VirtualFile absFile = myCargoProjectDirectory.getFileSystem().findFileByPath(path);
        return absFile != null ? new ResolvedPath.Unknown(absFile) : null;
    }

    @Nullable
    private ResolvedPath resolveCargoPath(@NotNull String path) {
        if (!path.startsWith("/cargo")) return null;
        String fullPath = Paths.get(getCargoRoot(), path.substring("/cargo".length())).toString();
        VirtualFile file = myCargoProjectDirectory.getFileSystem().findFileByPath(fullPath);
        return file != null ? new ResolvedPath.CargoDependency(file) : null;
    }

    @Nullable
    private ResolvedPath resolveStdlibPath(@NotNull String path) {
        String sysroot = getSysroot();
        if (sysroot == null) return null;
        String normalizedPath = normalizeStdLibPath(path);
        String fullPath = sysroot + "/lib/rustlib/src/rust/" + normalizedPath;
        VirtualFile file = myCargoProjectDirectory.getFileSystem().findFileByPath(fullPath);
        return file != null ? new ResolvedPath.Stdlib(file) : null;
    }

    // /rustc/<commit hash>/src/libstd/... -> src/libstd/...
    @NotNull
    private String normalizeStdLibPath(@NotNull String path) {
        Matcher match = RUSTC_ABSOLUTE_PATH_RE.matcher(path);
        if (!match.matches()) return path;
        return match.group(1);
    }

    @Nullable
    private String getSysroot() {
        return CargoProjectServiceUtil.getCargoProjects(myProject).getAllProjects().stream()
            .filter(p -> p.getRustcInfo() != null)
            .map(p -> p.getRustcInfo().getSysroot())
            .findFirst()
            .orElse(null);
    }

    @NotNull
    private String getCargoRoot() {
        Object location = RsProjectSettingsServiceUtil.getRustSettings(myProject).getToolchain() != null
            ? RsProjectSettingsServiceUtil.getRustSettings(myProject).getToolchain().getLocation()
            : null;
        return location != null ? location.toString() : "";
    }

    public static abstract class ResolvedPath {
        private final VirtualFile myFile;

        protected ResolvedPath(@NotNull VirtualFile file) {
            myFile = file;
        }

        @NotNull
        public VirtualFile getFile() {
            return myFile;
        }

        public static class Workspace extends ResolvedPath {
            public Workspace(@NotNull VirtualFile file) {
                super(file);
            }
        }

        public static class Stdlib extends ResolvedPath {
            public Stdlib(@NotNull VirtualFile file) {
                super(file);
            }
        }

        public static class CargoDependency extends ResolvedPath {
            public CargoDependency(@NotNull VirtualFile file) {
                super(file);
            }
        }

        public static class Unknown extends ResolvedPath {
            public Unknown(@NotNull VirtualFile file) {
                super(file);
            }
        }
    }
}
