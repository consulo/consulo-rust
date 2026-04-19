/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import com.intellij.execution.target.LanguageRuntimeType.VolumeDescriptor;
import com.intellij.execution.target.TargetEnvironment;
import com.intellij.execution.target.TargetEnvironment.TargetPath.Temporary;
import com.intellij.execution.target.TargetEnvironmentRequest;
import com.intellij.execution.target.TargetProgressIndicator;
import com.intellij.execution.target.local.LocalTargetEnvironment;
import com.intellij.execution.target.value.DeferredTargetValue;
import com.intellij.execution.target.value.TargetValue;
import com.intellij.execution.target.value.TargetEnvironmentFunctions;
import com.intellij.lang.LangCoreBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.rust.lang.core.psi.ext.RsPathUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class RsCommandLineSetup {

    private static final Logger LOG = Logger.getInstance(RsCommandLineSetup.class);

    private final TargetEnvironmentRequest myRequest;
    private final RsLanguageRuntimeConfiguration myLanguageRuntime;
    private final AsyncPromise<Pair<TargetEnvironment, TargetProgressIndicator>> myEnvironmentPromise = new AsyncPromise<>();
    private final List<Promise<Void>> myDependingOnEnvironmentPromise = new ArrayList<>();
    private final List<Upload> myUploads = new ArrayList<>();
    private final VolumeDescriptor myProjectHomeOnTarget;

    public RsCommandLineSetup(@NotNull TargetEnvironmentRequest request) {
        myRequest = request;
        myLanguageRuntime = request.getConfiguration() != null
            ? TargetUtil.getLanguageRuntime(request.getConfiguration())
            : null;
        myProjectHomeOnTarget = new VolumeDescriptor(
            RsCommandLineSetup.class.getSimpleName() + ":projectHomeOnTarget",
            "",
            "",
            "",
            request.getProjectPathOnTarget()
        );
    }

    @NotNull
    public TargetEnvironmentRequest getRequest() {
        return myRequest;
    }

    @NotNull
    public TargetValue<String> requestUploadIntoTarget(@NotNull String uploadPathString) {
        Path uploadPath = Paths.get(FileUtil.toSystemDependentName(uploadPathString));
        boolean isDir = java.nio.file.Files.isDirectory(uploadPath);
        Path localRootPath = isDir ? uploadPath : (uploadPath.getParent() != null ? uploadPath.getParent() : Paths.get("."));

        kotlin.Pair<TargetEnvironment.UploadRoot, String> uploadInfo = TargetEnvironmentFunctions.getUploadRootForLocalPath(myRequest, localRootPath);
        TargetEnvironment.UploadRoot uploadRoot;
        String pathToRoot;
        if (uploadInfo != null) {
            uploadRoot = uploadInfo.getFirst();
            pathToRoot = uploadInfo.getSecond();
        } else {
            uploadRoot = createUploadRoot(myProjectHomeOnTarget, localRootPath);
            myRequest.getUploadVolumes().add(uploadRoot);
            pathToRoot = ".";
        }

        DeferredTargetValue<String> result = new DeferredTargetValue<>(uploadPathString);
        String finalPathToRoot = pathToRoot;
        TargetEnvironment.UploadRoot finalUploadRoot = uploadRoot;
        Promise<Void> dependentPromise = myEnvironmentPromise.then(pair -> {
            TargetEnvironment environment = pair.getFirst();
            TargetProgressIndicator targetProgressIndicator = pair.getSecond();

            if (targetProgressIndicator.isCanceled() || targetProgressIndicator.isStopped()) {
                result.stopProceeding();
                return null;
            }
            TargetEnvironment.UploadableVolume volume = environment.getUploadVolumes().get(finalUploadRoot);
            try {
                String relativePath;
                if (isDir) {
                    relativePath = finalPathToRoot;
                } else {
                    String fileName = uploadPath.getFileName().toString();
                    relativePath = ".".equals(finalPathToRoot) ? fileName : joinPath(finalPathToRoot, fileName);
                }
                String resolvedTargetPath = volume.resolveTargetPath(relativePath);
                myUploads.add(new Upload(volume, relativePath));
                result.resolve(resolvedTargetPath);
            } catch (Throwable t) {
                LOG.warn(t);
                targetProgressIndicator.stopWithErrorMessage(
                    LangCoreBundle.message(
                        "progress.message.failed.to.resolve.0.1",
                        volume.getLocalRoot(), t.getLocalizedMessage()
                    )
                );
                result.resolveFailure(t);
            }
            return null;
        });
        myDependingOnEnvironmentPromise.add(dependentPromise);
        return result;
    }

    @NotNull
    private TargetEnvironment.UploadRoot createUploadRoot(
        @NotNull VolumeDescriptor volumeDescriptor,
        @NotNull Path localRootPath
    ) {
        if (myLanguageRuntime != null) {
            TargetEnvironment.UploadRoot runtimeRoot = myLanguageRuntime.createUploadRoot(volumeDescriptor, localRootPath);
            if (runtimeRoot != null) return runtimeRoot;
        }
        return new TargetEnvironment.UploadRoot(localRootPath, new Temporary());
    }

    @NotNull
    private String joinPath(@NotNull String... segments) {
        String separator = String.valueOf(myRequest.getTargetPlatform().getPlatform().fileSeparator);
        return String.join(separator, segments);
    }

    public void provideEnvironment(@NotNull TargetEnvironment environment,
                                   @NotNull TargetProgressIndicator targetProgressIndicator) throws java.io.IOException, java.util.concurrent.TimeoutException, java.util.concurrent.ExecutionException {
        com.intellij.openapi.application.Application application = ApplicationManager.getApplication();
        LOG.assertTrue(
            environment instanceof LocalTargetEnvironment ||
                myUploads.isEmpty() ||
                !application.isDispatchThread() ||
                application.isUnitTestMode(),
            "Preparation of environment shouldn't be performed on EDT."
        );
        myEnvironmentPromise.setResult(new Pair<>(environment, targetProgressIndicator));

        // Sort uploads by relative path length and group by volume
        Map<TargetEnvironment.UploadableVolume, List<String>> groupedUploads = new LinkedHashMap<>();
        myUploads.stream()
            .sorted((a, b) -> Integer.compare(a.myRelativePath.length(), b.myRelativePath.length()))
            .forEach(upload -> groupedUploads
                .computeIfAbsent(upload.myVolume, k -> new ArrayList<>())
                .add(upload.myRelativePath));

        for (Map.Entry<TargetEnvironment.UploadableVolume, List<String>> entry : groupedUploads.entrySet()) {
            entry.getKey().upload(entry.getValue().get(0), targetProgressIndicator);
        }

        for (Promise<Void> promise : myDependingOnEnvironmentPromise) {
            promise.blockingGet(0); // Just rethrows errors
        }
    }

    private static class Upload {
        private final TargetEnvironment.UploadableVolume myVolume;
        private final String myRelativePath;

        Upload(@NotNull TargetEnvironment.UploadableVolume volume, @NotNull String relativePath) {
            myVolume = volume;
            myRelativePath = relativePath;
        }
    }
}
