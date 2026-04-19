/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.RsBundle;
import org.rust.ide.notifications.NotificationUtils;
import org.rust.ide.utils.IoUtil;
import com.intellij.openapi.application.ex.ApplicationUtil;
import org.rust.openapiext.OpenApiUtil;
import org.toml.lang.psi.TomlKeySegment;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class CratesIoApi {
    private CratesIoApi() {}

    private static List<CrateDescription> MOCK = null;

    @NotNull
    public static Collection<CrateDescription> searchCrate(@NotNull TomlKeySegment key) {
        if (OpenApiUtil.isUnitTestMode()) {
            if (MOCK != null) return MOCK;
            return Collections.emptyList();
        }

        PsiElement original = CompletionUtil.getOriginalElement(key);
        String name = original != null ? original.getText() : "";
        if (name.isEmpty()) return Collections.emptyList();

        SearchResult response = requestCratesIo(key, "crates?page=1&per_page=100&q=" + name, SearchResult.class);
        if (response == null) return Collections.emptyList();
        return response.crates;
    }

    @Nullable
    public static String getCrateLastVersion(@NotNull TomlKeySegment key) {
        if (OpenApiUtil.isUnitTestMode()) {
            if (MOCK != null && !MOCK.isEmpty()) return MOCK.get(0).maxVersion;
            return null;
        }

        PsiElement original = CompletionUtil.getOriginalElement(key);
        String name = original != null ? original.getText() : "";
        if (name.isEmpty()) return null;

        CrateInfoResult response = requestCratesIo(key, "crates/" + name, CrateInfoResult.class);
        if (response == null) return null;
        return response.crate.maxVersion;
    }

    @Nullable
    private static <T> T requestCratesIo(@NotNull PsiElement context, @NotNull String path, @NotNull Class<T> cls) {
        try {
            return ApplicationUtil.runWithCheckCanceled(() -> {
                String response = HttpRequests.request("https://crates.io/api/v1/" + path)
                    .userAgent(IoUtil.USER_AGENT)
                    .readString(ProgressManager.getInstance().getProgressIndicator());
                return new Gson().fromJson(response, cls);
            }, ProgressManager.getInstance().getProgressIndicator());
        } catch (IOException e) {
            NotificationUtils.showBalloon(context.getProject(), RsBundle.message("notification.content.could.not.reach.crates.io"), NotificationType.WARNING);
            return null;
        } catch (JsonSyntaxException e) {
            NotificationUtils.showBalloon(context.getProject(), RsBundle.message("notification.content.bad.answer.from.crates.io"), NotificationType.WARNING);
            return null;
        } catch (Exception e) {
            if (e instanceof com.intellij.openapi.progress.ProcessCanceledException) {
                throw (com.intellij.openapi.progress.ProcessCanceledException) e;
            }
            NotificationUtils.showBalloon(context.getProject(), RsBundle.message("notification.content.could.not.reach.crates.io"), NotificationType.WARNING);
            return null;
        }
    }

    @TestOnly
    public static void withMockedCrateSearch(@NotNull List<CrateDescription> mock, @NotNull Runnable action) {
        MOCK = mock;
        try {
            action.run();
        } finally {
            MOCK = null;
        }
    }

    public static class SearchResult {
        public List<CrateDescription> crates;
    }

    public static class CrateInfoResult {
        public CrateDescription crate;
    }

    public static class CrateDescription {
        public String name;

        @SerializedName("max_version")
        public String maxVersion;

        @NotNull
        public String getDependencyLine() {
            return name + " = \"" + maxVersion + "\"";
        }
    }
}
