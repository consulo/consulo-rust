/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.impl;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.toolchain.RustChannel;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RustcVersion {

    private final SemVer semver;
    private final String host;
    private final RustChannel channel;
    @Nullable
    private final String commitHash;
    @Nullable
    private final LocalDate commitDate;

    public RustcVersion(SemVer semver, String host, RustChannel channel, @Nullable String commitHash, @Nullable LocalDate commitDate) {
        this.semver = semver;
        this.host = host;
        this.channel = channel;
        this.commitHash = commitHash;
        this.commitDate = commitDate;
    }

    public RustcVersion(SemVer semver, String host, RustChannel channel) {
        this(semver, host, channel, null, null);
    }

    public SemVer getSemver() {
        return semver;
    }

    public String getHost() {
        return host;
    }

    public RustChannel getChannel() {
        return channel;
    }

    @Nullable
    public String getCommitHash() {
        return commitHash;
    }

    @Nullable
    public LocalDate getCommitDate() {
        return commitDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RustcVersion)) return false;
        RustcVersion that = (RustcVersion) o;
        return Objects.equals(semver, that.semver) &&
            Objects.equals(host, that.host) &&
            channel == that.channel &&
            Objects.equals(commitHash, that.commitHash) &&
            Objects.equals(commitDate, that.commitDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(semver, host, channel, commitHash, commitDate);
    }

    @Override
    public String toString() {
        return "RustcVersion(" +
            "semver=" + semver +
            ", host=" + host +
            ", channel=" + channel +
            ", commitHash=" + commitHash +
            ", commitDate=" + commitDate +
            ")";
    }

    private static final Pattern RELEASE_RE = Pattern.compile("release: (\\d+\\.\\d+\\.\\d+.*)");
    private static final Pattern HOST_RE = Pattern.compile("host: (.*)");
    private static final Pattern COMMIT_HASH_RE = Pattern.compile("commit-hash: ([A-Fa-f0-9]{40})");
    private static final Pattern COMMIT_DATE_RE = Pattern.compile("commit-date: (\\d{4}-\\d{2}-\\d{2})");

    @VisibleForTesting
    @Nullable
    public static RustcVersion parseRustcVersion(List<String> lines) {
        String releaseText = null;
        String hostText = null;
        String commitHash = null;
        String commitDateStr = null;

        for (String line : lines) {
            Matcher releaseMatcher = RELEASE_RE.matcher(line);
            if (releaseMatcher.matches()) {
                releaseText = releaseMatcher.group(1);
            }
            Matcher hostMatcher = HOST_RE.matcher(line);
            if (hostMatcher.matches()) {
                hostText = hostMatcher.group(1);
            }
            Matcher commitHashMatcher = COMMIT_HASH_RE.matcher(line);
            if (commitHashMatcher.matches()) {
                commitHash = commitHashMatcher.group(1);
            }
            Matcher commitDateMatcher = COMMIT_DATE_RE.matcher(line);
            if (commitDateMatcher.matches()) {
                commitDateStr = commitDateMatcher.group(1);
            }
        }

        if (releaseText == null || hostText == null) return null;

        SemVer semVer = SemVer.parseFromText(releaseText);
        if (semVer == null) return null;

        String preRelease = semVer.getPreRelease() != null ? semVer.getPreRelease() : "";
        RustChannel channel = RustChannel.fromPreRelease(preRelease);

        LocalDate commitDate = null;
        if (commitDateStr != null) {
            try {
                commitDate = LocalDate.parse(commitDateStr);
            } catch (DateTimeParseException e) {
                // ignore
            }
        }

        return new RustcVersion(semVer, hostText, channel, commitHash, commitDate);
    }
}
