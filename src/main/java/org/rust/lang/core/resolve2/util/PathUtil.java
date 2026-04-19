/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.stubs.RsPathStub;
import org.rust.lang.core.stubs.RsUseItemStub;
import org.rust.lang.core.stubs.RsUseSpeckStub;
import org.rust.lang.core.stubs.RsVisStub;

import java.util.ArrayList;

/**
 * Utility functions for working with paths in stubs.
 */
public final class PathUtil {

    private PathUtil() {}

    @FunctionalInterface
    public interface RsLeafUseSpeckConsumer {
        void consume(@NotNull String[] usePath, @Nullable String alias, boolean isStarImport, int offsetInExpansion);
    }

    public static void forEachLeafSpeck(@NotNull RsUseItemStub useItem, @NotNull RsLeafUseSpeckConsumer consumer) {
        RsUseSpeckStub rootUseSpeck = useItem.findChildStubByType(RsUseSpeckStub.Type);
        if (rootUseSpeck == null) return;
        ArrayList<String> segments = new ArrayList<>();
        forEachLeafSpeck(rootUseSpeck, consumer, segments, true, null);
    }

    private static void forEachLeafSpeck(
        @NotNull RsUseSpeckStub useSpeck,
        @NotNull RsLeafUseSpeckConsumer consumer,
        @NotNull ArrayList<String> segments,
        boolean isRootSpeck,
        @Nullable RsPathStub basePath
    ) {
        RsPathStub path = useSpeck.getPath();
        Object useGroup = useSpeck.getUseGroup();
        boolean isStarImport = useSpeck.isStarImport();

        if (path == null && isRootSpeck) {
            if (useSpeck.isHasColonColon()) segments.add("crate");
            if (!useSpeck.isHasColonColon() && useGroup == null) return;
        }

        int numberSegments = segments.size();
        if (path != null) {
            if (!addPathSegments(path, segments)) return;
        }

        RsPathStub newBasePath = basePath != null ? basePath : path;
        if (useGroup == null) {
            if (!isStarImport && segments.size() > 1 && "self".equals(segments.get(segments.size() - 1))) {
                segments.remove(segments.size() - 1);
            }
            String alias = isStarImport ? "_" : (useSpeck.getAlias() != null ? useSpeck.getAlias().getName() : null);
            int offset = newBasePath != null ? newBasePath.getStartOffset() : -1;
            consumer.consume(segments.toArray(new String[0]), alias, isStarImport, offset);
        } else {
            for (Object childStub : useSpeck.getUseGroup().getChildrenStubs()) {
                if (childStub instanceof RsUseSpeckStub) {
                    forEachLeafSpeck((RsUseSpeckStub) childStub, consumer, segments, false, newBasePath);
                }
            }
        }

        while (segments.size() > numberSegments) {
            segments.remove(segments.size() - 1);
        }
    }

    private static boolean addPathSegments(@NotNull RsPathStub path, @NotNull ArrayList<String> segments) {
        RsPathStub subpath = path.getPath();
        if (subpath != null) {
            if (!addPathSegments(subpath, segments)) return false;
        } else if (path.getHasColonColon()) {
            segments.add("");
        }

        String segment = path.getReferenceName();
        if (segment == null) return false;
        segments.add(segment);
        return true;
    }

    @Nullable
    public static String[] getPathWithAdjustedDollarCrate(@NotNull RsPathStub path) {
        ArrayList<String> segments = new ArrayList<>();
        if (!addPathSegments(path, segments)) return null;
        return segments.toArray(new String[0]);
    }

    @Nullable
    public static String[] getRestrictedPath(@NotNull RsVisStub vis) {
        RsPathStub path = vis.getVisRestrictionPath();
        if (path == null) throw new IllegalStateException("no visibility restriction");
        ArrayList<String> segments = new ArrayList<>();
        if (!addPathSegments(path, segments)) return null;
        if (!segments.isEmpty()) {
            String first = segments.get(0);
            if (first.isEmpty() || first.equals("crate")) {
                segments.remove(0);
            }
        }
        return segments.toArray(new String[0]);
    }
}
