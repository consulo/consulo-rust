/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.google.gson.Gson;
import consulo.process.io.ProcessIOExecutorService;
import consulo.component.PropertiesComponent;
import consulo.project.ui.notification.NotificationAction;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper.DoNotAskOption;
import consulo.ui.ex.awt.MessageDialogBuilder;
import consulo.ui.ex.awt.Messages;
import consulo.http.HttpRequests;
import com.intellij.util.net.ssl.CertificateManager;
import consulo.http.impl.internal.proxy.CommonProxy;
import jakarta.annotation.Nonnull;

import org.rust.RsBundle;
import org.rust.ide.notifications.NotificationUtils;
import org.rust.ide.utils.IoUtil;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.JsonUtils;

import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ShareInPlaygroundAction extends DumbAwareAction {

    private static final String SHOW_SHARE_IN_PLAYGROUND_CONFIRMATION = "rs.show.share.in.playground.confirmation";
    private static String MOCK = null;


    @Override
    public void update(AnActionEvent e) {
        var file = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabledAndVisible(file instanceof RsFile);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;
        var psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (!(psiFile instanceof RsFile file)) return;
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        var result = ReadAction.compute(() -> {
            String selectedText = editor != null ? editor.getSelectionModel().getSelectedText() : null;
            if (selectedText != null) {
                return new String[]{selectedText, "true"};
            } else {
                return new String[]{file.getText(), "false"};
            }
        });
        String text = result[0];
        boolean hasSelection = Boolean.parseBoolean(result[1]);

        Context context = new Context(file, text, hasSelection);
        performAction(project, context);
    }

    public static void performAction(Project project, Context context) {
        RsFile file = context.file;
        String text = context.text;
        boolean hasSelection = context.hasSelection;
        if (!confirmShare(file, hasSelection)) return;

        var cargoProject = file.getCargoProject();
        String channel = "stable";
        if (cargoProject != null && cargoProject.getRustcInfo() != null &&
            cargoProject.getRustcInfo().getVersion() != null &&
            cargoProject.getRustcInfo().getVersion().getChannel() != null) {
            channel = cargoProject.getRustcInfo().getVersion().getChannel().getChannel();
        }
        String edition = file.getCrate().getEdition().getPresentation();
        String finalChannel = channel;

        new Task.Backgroundable(project, RsBundle.message("action.Rust.ShareInPlayground.progress.title")) {
            private volatile String gistId = null;

            @Override
            public boolean shouldStartInBackground() {
                return true;
            }

            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                String json = new Gson().toJson(new PlaygroundCode(text));
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getPlaygroundHost() + "/meta/gist/"))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", IoUtil.USER_AGENT)
                    .timeout(Duration.ofMillis(60000))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

                HttpClient client = createHttpClient();
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    gistId = JsonUtils.parseJsonObject(response.body()).getAsJsonPrimitive("id").getAsString();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void onSuccess() {
                String url = "https://play.rust-lang.org/?version=" + finalChannel + "&edition=" + edition + "&gist=" + gistId;
                var copyUrlAction = NotificationAction.create(consulo.localize.LocalizeValue.of(RsBundle.message("action.Rust.ShareInPlayground.notification.copy.url.text")), (e, n) -> CopyPasteManager.getInstance().setContents(new StringSelection(url)));
                NotificationUtils.showBalloon(
                    project,
                    RsBundle.message("action.Rust.ShareInPlayground.notification.title"),
                    RsBundle.message("action.Rust.ShareInPlayground.notification.text", url),
                    NotificationType.INFORMATION,
                    copyUrlAction,
                    NotificationListener.URL_OPENING_LISTENER
                );
            }

            @Override
            public void onThrowable(@Nonnull Throwable error) {
                if (!org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
                    super.onThrowable(error);
                }
                NotificationUtils.showBalloon(
                    project,
                    RsBundle.message("action.Rust.ShareInPlayground.notification.title"),
                    RsBundle.message("action.Rust.ShareInPlayground.notification.error"),
                    NotificationType.ERROR
                );
            }
        }.queue();
    }

    private static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(10000))
            .executor(ProcessIOExecutorService.INSTANCE)
            .sslContext(CertificateManager.getInstance().getSslContext())
            .proxy(CommonProxy.getInstance())
            .build();
    }

    private static boolean confirmShare(RsFile file, boolean hasSelection) {
        boolean showConfirmation = consulo.application.Application.get().getInstance(consulo.component.PropertiesComponent.class).getBoolean(SHOW_SHARE_IN_PLAYGROUND_CONFIRMATION, true);
        if (!showConfirmation) {
            return true;
        }
        DoNotAskOption doNotAskOption = new DoNotAskOption.Adapter() {
            @Override
            public void rememberChoice(boolean isSelected, int exitCode) {
                if (isSelected && exitCode == Messages.OK) {
                    consulo.application.Application.get().getInstance(consulo.component.PropertiesComponent.class).setValue(SHOW_SHARE_IN_PLAYGROUND_CONFIRMATION, false, true);
                }
            }
        };

        String message = hasSelection
            ? RsBundle.message("action.Rust.ShareInPlayground.confirmation.selected.text")
            : RsBundle.message("action.Rust.ShareInPlayground.confirmation", file.getName());

        return MessageDialogBuilder.okCancel(RsBundle.message("action.Rust.ShareInPlayground.text"), message)
            .yesText(Messages.OK_BUTTON)
            .noText(Messages.CANCEL_BUTTON)
            .icon(Messages.getQuestionIcon())
            .doNotAsk(doNotAskOption)
            .isOk();
    }

    private static String getPlaygroundHost() {
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            if (MOCK == null) throw new IllegalStateException("Use `withMockPlaygroundHost`");
            return MOCK;
        } else {
            return "https://play.rust-lang.org";
        }
    }

    
    public static void withMockPlaygroundHost(String host, Runnable action) {
        MOCK = host;
        try {
            action.run();
        } finally {
            MOCK = null;
        }
    }

    public record Context(RsFile file, String text, boolean hasSelection) {}

    
    public record PlaygroundCode(String code) {}
}
