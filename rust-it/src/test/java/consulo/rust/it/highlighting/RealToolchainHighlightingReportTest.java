/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.highlighting;

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkTable;
import consulo.document.util.TextRange;
import consulo.it.AllowLogError;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.AnnotationSession;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.rust.bundle.RustBundleType;
import consulo.rust.module.extension.RustMutableModuleExtension;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.lang.RsLanguage;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * What the editor reports in a real Cargo project with a real toolchain: every {@code .rs} file under {@code src} of
 * {@code -Drust.report.project=<dir>} is annotated by all Rust annotators and inspected by the Rust inspections enabled
 * by default, after a sync with the toolchain at
 * {@code -Drust.report.toolchain=<toolchain root>} (for example {@code ~/.rustup/toolchains/stable-x86_64-unknown-linux-gnu}).
 * The errors and warnings go to {@code target/highlighting-report.txt} with their line and text. Skipped unless both
 * properties are set, since it needs a Rust installation.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class RealToolchainHighlightingReportTest {
    @Test
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil",
        "consulo.it.internal.HeadlessApplicationImpl",
        // the Cargo.toml inspections and reference providers need the TOML plugin, which is not loaded here
        "consulo.component.internal.NewExtensionPointImpl",
        "consulo.language.impl.internal.psi.resolve.reference.ReferenceProvidersRegistryImpl"
    })
    public void report(HeadlessProjects projects) throws Exception {
        String projectDir = System.getProperty("rust.report.project");
        String toolchain = System.getProperty("rust.report.toolchain");
        Assumptions.assumeTrue(projectDir != null && toolchain != null, "-Drust.report.project and -Drust.report.toolchain are not set");
        Path directory = Path.of(projectDir);

        Project project = projects.open(directory);
        SdkTable sdkTable = SdkTable.getInstance();
        Sdk sdk = WriteAction.compute(() -> {
            Sdk created = sdkTable.createSdk("real-rust", RustBundleType.getInstance());
            SdkModificator modificator = created.getSdkModificator();
            modificator.setHomePath(toolchain);
            modificator.commitChanges();
            sdkTable.addSdk(created);
            return created;
        });
        try {
            VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
            Module module = HeadlessModules.createModule(project, "report", root);
            WriteAction.run(() -> {
                ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
                extension.setEnabled(true);
                extension.getInheritableSdk().set(null, sdk);
                rootModel.commit();
            });
            CargoProjectsService service = CargoProjectsService.getInstance(project);
            service.attachCargoProjectAsync(directory.resolve("Cargo.toml"))
                .thenCompose(ignored -> service.refreshAllProjects())
                .get(300, TimeUnit.SECONDS);
            awaitIndexed(project);

            List<Path> sources;
            try (Stream<Path> walk = Files.walk(directory.resolve("src"))) {
                sources = walk.filter(p -> p.toString().endsWith(".rs")).sorted().toList();
            }
            List<String> report = new ArrayList<>();
            for (Path source : sources) {
                VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(source);
                report.addAll(ReadAction.compute(() -> annotate(PsiManager.getInstance(project).findFile(file), directory)));
            }
            Files.write(Path.of("target/highlighting-report.txt"), report, StandardCharsets.UTF_8);
        }
        finally {
            WriteAction.run(() -> sdkTable.removeSdk(sdk));
        }
    }

    private static List<String> annotate(PsiFile file, Path directory) {
        String text = file.getText();
        List<String> result = new ArrayList<>();
        List<Annotator> annotators = new ArrayList<>();
        for (AnnotatorFactory factory : file.getProject().getExtensionList(AnnotatorFactory.class)) {
            if (factory.getLanguage() != RsLanguage.INSTANCE) continue;
            Annotator annotator = factory.createAnnotator();
            if (annotator != null) annotators.add(annotator);
        }
        AnnotationSession session = new AnnotationSession(file);
        for (PsiElement element : PsiTreeUtil.collectElements(file, e -> true)) {
            for (Annotator annotator : annotators) {
                annotator.annotate(element, holder(session, element, annotator, text, result));
            }
        }
        result.addAll(inspect(file, text));
        result.add(0, "== " + directory.relativize(Path.of(file.getVirtualFile().getPath())));
        return result;
    }

    /** The problems of every Rust inspection that is enabled by default. */
    private static List<String> inspect(PsiFile file, String text) {
        List<String> result = new ArrayList<>();
        InspectionManager manager = InspectionManager.getInstance(file.getProject());
        for (InspectionTool tool : file.getProject().getApplication().getExtensionList(InspectionTool.class)) {
            if (!(tool instanceof LocalInspectionTool local) || tool.getLanguage() != RsLanguage.INSTANCE || !tool.isEnabledByDefault()) continue;
            ProblemsHolder holder = manager.createProblemsHolder(file, false);
            PsiElementVisitor visitor = local.buildVisitor(holder, false);
            for (PsiElement element : PsiTreeUtil.collectElements(file, e -> true)) {
                element.accept(visitor);
            }
            for (ProblemDescriptor descriptor : holder.getResults()) {
                PsiElement element = descriptor.getPsiElement();
                if (element == null) continue;
                TextRange inElement = descriptor.getTextRangeInElement();
                TextRange range = inElement != null ? inElement.shiftRight(element.getTextRange().getStartOffset()) : element.getTextRange();
                result.add(line(text, range) + " " + tool.getDefaultLevel().getName() + " [" + tool.getShortName() + "] "
                    + descriptor.getDescriptionTemplate().get()
                    + "  >>" + text.substring(range.getStartOffset(), Math.min(range.getEndOffset(), range.getStartOffset() + 60)) + "<<");
            }
        }
        return result;
    }

    /** An annotation holder that writes down every error and warning; the rest of the builder calls are accepted and ignored. */
    private static AnnotationHolder holder(AnnotationSession session, PsiElement element, Annotator annotator, String text, List<String> result) {
        return (AnnotationHolder) Proxy.newProxyInstance(AnnotationHolder.class.getClassLoader(), new Class<?>[]{AnnotationHolder.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getCurrentAnnotationSession": return session;
                case "isBatchMode": return false;
                case "newAnnotation":
                case "newOfSeverity":
                case "newSilentAnnotation": {
                    HighlightSeverity severity = args[0] instanceof HighlightSeverity s ? s : HighlightSeverity.INFORMATION;
                    String message = args.length > 1 && args[1] != null ? args[1].toString() : "";
                    return builder(severity, message, element.getTextRange(), annotator, text, result);
                }
                default:
                    if (method.getName().startsWith("create") && method.getName().endsWith("Annotation")) {
                        result.add(line(text, element.getTextRange()) + " [legacy " + method.getName() + "] " + (args.length > 1 ? args[args.length - 1] : ""));
                    }
                    return null;
            }
        });
    }

    private static Object builder(HighlightSeverity severity, String message, TextRange elementRange, Annotator annotator, String text, List<String> result) {
        TextRange[] range = {elementRange};
        Class<?> builderClass = consulo.language.editor.annotation.AnnotationBuilder.class;
        return Proxy.newProxyInstance(builderClass.getClassLoader(), new Class<?>[]{builderClass}, (proxy, method, args) -> {
            if (method.getName().equals("range") && args != null && args.length == 1) {
                if (args[0] instanceof TextRange r) range[0] = r;
                else if (args[0] instanceof PsiElement e) range[0] = e.getTextRange();
            }
            if (method.getName().equals("create")) {
                if (severity.compareTo(HighlightSeverity.WEAK_WARNING) >= 0) {
                    result.add(line(text, range[0]) + " " + severity.getName() + " [" + annotator.getClass().getSimpleName() + "] " + message
                        + "  >>" + text.substring(range[0].getStartOffset(), Math.min(range[0].getEndOffset(), range[0].getStartOffset() + 60)) + "<<");
                }
                return null;
            }
            return method.getReturnType().isInstance(proxy) ? proxy : null;
        });
    }

    private static String line(String text, TextRange range) {
        return String.valueOf(text.substring(0, range.getStartOffset()).split("\n", -1).length);
    }

    /** A real project indexes its whole dependency tree, which can outlast one idle timeout. */
    private static void awaitIndexed(Project project) throws Exception {
        for (int attempt = 1; ; attempt++) {
            try {
                HeadlessProjects.awaitIdle(project);
                return;
            }
            catch (AssertionError | IllegalStateException e) {
                if (attempt == 10) throw e;
            }
        }
    }
}
