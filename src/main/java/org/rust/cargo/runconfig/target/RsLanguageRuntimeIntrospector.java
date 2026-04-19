/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import com.intellij.execution.target.LanguageRuntimeType;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class RsLanguageRuntimeIntrospector implements LanguageRuntimeType.Introspector<RsLanguageRuntimeConfiguration> {

    private final RsLanguageRuntimeConfiguration myConfig;

    public RsLanguageRuntimeIntrospector(@NotNull RsLanguageRuntimeConfiguration config) {
        myConfig = config;
    }

    @NotNull
    public RsLanguageRuntimeConfiguration getConfig() {
        return myConfig;
    }

    @NotNull
    @Override
    public CompletableFuture<RsLanguageRuntimeConfiguration> introspect(
        @NotNull LanguageRuntimeType.Introspectable subject
    ) {
        CompletableFuture<String> rustcPathPromise;
        if (myConfig.getRustcPath().isBlank()) {
            rustcPathPromise = promiseOneLineScript(subject, "/usr/bin/which rustc").thenApply(output -> {
                if (output != null) {
                    String trimmed = output.trim();
                    if (!trimmed.isEmpty()) {
                        myConfig.setRustcPath(trimmed);
                        return trimmed;
                    }
                }
                return null;
            });
        } else {
            rustcPathPromise = CompletableFuture.completedFuture(myConfig.getRustcPath());
        }

        CompletableFuture<?> rustcVersionPromise = rustcPathPromise.thenCompose(rustcPath -> {
            if (rustcPath == null) return CompletableFuture.completedFuture(null);
            return promiseOneLineScript(subject, rustcPath + " --version").thenApply(output -> {
                if (output != null) {
                    String version = output.replaceFirst("^rustc", "").trim();
                    if (!version.isEmpty()) {
                        myConfig.setRustcVersion(version);
                    }
                }
                return null;
            });
        });

        CompletableFuture<String> cargoPathPromise;
        if (myConfig.getCargoPath().isBlank()) {
            cargoPathPromise = promiseOneLineScript(subject, "/usr/bin/which cargo").thenApply(output -> {
                if (output != null) {
                    String trimmed = output.trim();
                    if (!trimmed.isEmpty()) {
                        myConfig.setCargoPath(trimmed);
                        return trimmed;
                    }
                }
                return null;
            });
        } else {
            cargoPathPromise = CompletableFuture.completedFuture(myConfig.getCargoPath());
        }

        CompletableFuture<?> cargoVersionPromise = cargoPathPromise.thenCompose(cargoPath -> {
            if (cargoPath == null) return CompletableFuture.completedFuture(null);
            return promiseOneLineScript(subject, cargoPath + " --version").thenApply(output -> {
                if (output != null) {
                    String version = output.replaceFirst("^cargo", "").trim();
                    if (!version.isEmpty()) {
                        myConfig.setCargoVersion(version);
                    }
                }
                return null;
            });
        });

        return CompletableFuture.allOf(rustcVersionPromise, cargoVersionPromise).thenApply(v -> myConfig);
    }

    @NotNull
    private static CompletableFuture<String> promiseOneLineScript(
        @NotNull LanguageRuntimeType.Introspectable subject,
        @NotNull String script
    ) {
        return subject.promiseExecuteScript(script)
            .thenApply(output -> {
                if (output == null) return null;
                String[] lines = StringUtil.splitByLines(output, true);
                return lines.length > 0 ? lines[0] : null;
            });
    }
}
