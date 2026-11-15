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
import consulo.it.AllowLogError;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.language.psi.stub.StubTreeLoader;
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
import org.rust.cargo.api.model.CargoProjectsUtil;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve.ResolveUtil;
import org.rust.lang.core.resolve2.DefMapService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * How long the first answers take in a real Cargo project, cold and then warm: the crate of a file, the names visible in
 * a function body (what completion lists) and the resolution of every path of the file (what highlighting does), with
 * the number of crates whose def map got built on the way.
 * <p>
 * {@code -Drust.bench.project=<cargo dir> -Drust.bench.toolchain=<toolchain root> -Drust.bench.file=<file relative to the
 * project>}; the timings go to {@code target/cold-start-benchmark.txt}. Skipped unless the properties are set, since it needs
 * a Rust installation and the project's dependencies in the Cargo registry.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class ColdStartBenchmarkTest {
    @Test
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil",
        "consulo.it.internal.HeadlessApplicationImpl",
        // the Cargo.toml inspections and reference providers need the TOML plugin, which is not loaded here
        "consulo.component.internal.NewExtensionPointImpl",
        "consulo.language.impl.internal.psi.resolve.reference.ReferenceProvidersRegistryImpl",
        // run configurations saved in the project may need execution plugins the headless test does not load
        "consulo.execution.impl.internal.configuration.RunManagerImpl"
    })
    public void benchmark(HeadlessProjects projects) throws Exception {
        String projectDir = System.getProperty("rust.bench.project");
        String toolchain = System.getProperty("rust.bench.toolchain");
        String fileName = System.getProperty("rust.bench.file");
        Assumptions.assumeTrue(projectDir != null && toolchain != null && fileName != null,
            "-Drust.bench.project, -Drust.bench.toolchain and -Drust.bench.file are not set");
        Path directory = Path.of(projectDir);
        List<String> report = new ArrayList<>();

        Project project = projects.open(directory);
        SdkTable sdkTable = SdkTable.getInstance();
        Sdk sdk = WriteAction.compute(() -> {
            Sdk created = sdkTable.createSdk("bench-rust", RustBundleType.getInstance());
            SdkModificator modificator = created.getSdkModificator();
            modificator.setHomePath(toolchain);
            modificator.commitChanges();
            sdkTable.addSdk(created);
            return created;
        });
        try {
            VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
            Module module = HeadlessModules.createModule(project, "bench", root);
            WriteAction.run(() -> {
                ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
                extension.setEnabled(true);
                extension.getInheritableSdk().set(null, sdk);
                rootModel.commit();
            });
            long syncStart = System.nanoTime();
            CargoProjectsService service = CargoProjectsService.getInstance(project);
            service.attachCargoProjectAsync(directory.resolve("Cargo.toml"))
                .thenCompose(ignored -> service.refreshAllProjects())
                .get(600, TimeUnit.SECONDS);
            report.add("cargo sync: " + millis(syncStart) + " ms");
            long indexStart = System.nanoTime();
            awaitIndexed(project);
            report.add("indexing: " + millis(indexStart) + " ms");

            VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory.resolve(fileName));
            Supplier<RsFile> rsFile = () -> (RsFile) PsiManager.getInstance(project).findFile(file);
            report.add("package of the file: " + ReadAction.compute(() -> {
                CargoWorkspace.Package pkg = CargoProjectsUtil.findPackageForFile(project, file);
                return pkg == null ? "none - the first lookup builds the def maps of every crate" : pkg.getName();
            }));
            report.add("crates the file's package depends on: " + ReadAction.compute(() -> {
                CargoWorkspace.Package pkg = CargoProjectsUtil.findPackageForFile(project, file);
                if (pkg == null) return "-";
                Set<Crate> closure = new HashSet<>();
                StringBuilder perTarget = new StringBuilder();
                for (CargoWorkspace.Target target : pkg.getTargets()) {
                    VirtualFile crateRoot = target.getCrateRoot();
                    Crate crate = crateRoot != null ? CrateGraphService.crateGraph(project).findCrateByRootMod(crateRoot) : null;
                    if (crate == null) continue;
                    closure.add(crate);
                    closure.addAll(crate.getFlatDependencies());
                    perTarget.append(" ").append(target.getName()).append("(").append(target.getKind()).append(")=")
                        .append(crate.getFlatDependencies().size());
                }
                return closure.size() + " -" + perTarget;
            }));
            report.add("crate roots with stubs in the index, by origin: " + ReadAction.compute(() -> {
                Map<String, int[]> counts = new java.util.TreeMap<>();
                for (Crate crate : CrateGraphService.crateGraph(project).getTopSortedCrates()) {
                    VirtualFile crateRoot = crate.getRootModFile();
                    if (crateRoot == null) continue;
                    int[] count = counts.computeIfAbsent(crate.getOrigin().name(), k -> new int[2]);
                    count[1]++;
                    if (StubTreeLoader.getInstance().readFromVFile(project, crateRoot) != null) count[0]++;
                }
                StringBuilder result = new StringBuilder();
                counts.forEach((origin, count) -> result.append(" ").append(origin).append("=").append(count[0]).append("/").append(count[1]));
                return result.toString();
            }));
            report.add("crates in the graph: " + ReadAction.compute(() -> CrateGraphService.crateGraph(project).getTopSortedCrates().size()));

            time(report, project, "nothing (def maps built before the first step)", () -> "");
            for (String round : List.of("cold", "warm")) {
                report.add("== " + round + " " + fileName);
                time(report, project, "crate of the file", () -> {
                    Crate crate = rsFile.get().getCrate();
                    return crate.getPresentableName();
                });
                time(report, project, "names visible in a function body", () -> {
                    RsBlock block = null;
                    for (RsFunction fn : PsiTreeUtil.findChildrenOfType(rsFile.get(), RsFunction.class)) {
                        block = RsFunctionUtil.getBlock(fn);
                        if (block != null) break;
                    }
                    if (block == null) return "no function with a body";
                    Set<String> names = new HashSet<>();
                    NameResolution.processNestedScopesUpwards(block, Namespace.TYPES_N_VALUES_N_MACROS,
                        ResolveUtil.createProcessor(entry -> names.add(entry.getName())));
                    return names.size() + " names";
                });
                time(report, project, "resolve every path of the file", () -> {
                    int resolved = 0;
                    int total = 0;
                    List<String> unresolved = new ArrayList<>();
                    for (RsPath path : PsiTreeUtil.findChildrenOfType(rsFile.get(), RsPath.class)) {
                        PsiReference reference = path.getReference();
                        if (reference == null) continue;
                        total++;
                        if (reference.resolve() != null) resolved++;
                        else unresolved.add(path.getText());
                    }
                    if (round.equals("cold")) report.add("unresolved: " + unresolved);
                    return resolved + "/" + total + " resolved";
                });
            }
        }
        finally {
            Files.write(Path.of("target/cold-start-benchmark.txt"), report, StandardCharsets.UTF_8);
            WriteAction.run(() -> sdkTable.removeSdk(sdk));
        }
    }

    /** Runs {@code step} in a read action and writes down its time, its result and how many crates have a def map since. */
    private static void time(List<String> report, Project project, String name, Supplier<String> step) {
        Thread worker = Thread.currentThread();
        Map<String, Integer> inclusive = new HashMap<>();
        Map<String, Integer> self = new HashMap<>();
        int[] samples = {0};
        AtomicBoolean running = new AtomicBoolean(true);
        Thread sampler = new Thread(() -> {
            while (running.get()) {
                // every thread running Rust code: def maps are built on the resolve pool, not on the test thread
                for (Map.Entry<Thread, StackTraceElement[]> thread : Thread.getAllStackTraces().entrySet()) {
                    if (thread.getKey() != worker && !thread.getKey().getName().startsWith("Rust-")) continue;
                    StackTraceElement[] frames = thread.getValue();
                    boolean runsRust = false;
                    for (StackTraceElement frame : frames) {
                        if (frame.getClassName().startsWith("org.rust.")) {
                            runsRust = true;
                            break;
                        }
                    }
                    // where the time is spent: the top of the stack, any code, and the first Rust method under it
                    if (runsRust && frames.length > 0 && !isWaiting(frames[0])) {
                        String leaf = frames[0].getClassName().substring(frames[0].getClassName().lastIndexOf('.') + 1) + "." + frames[0].getMethodName();
                        String rustCaller = "";
                        for (StackTraceElement frame : frames) {
                            if (frame.getClassName().startsWith("org.rust.")) {
                                rustCaller = frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1) + "." + frame.getMethodName();
                                break;
                            }
                        }
                        self.merge(leaf + "  <- " + rustCaller, 1, Integer::sum);
                    }
                    Set<String> seen = new HashSet<>();
                    for (StackTraceElement frame : frames) {
                        if (!frame.getClassName().startsWith("org.rust.")) continue;
                        String method = frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1) + "." + frame.getMethodName();
                        if (seen.add(method)) inclusive.merge(method, 1, Integer::sum);
                    }
                }
                samples[0]++;
                try {
                    Thread.sleep(20);
                }
                catch (InterruptedException e) {
                    return;
                }
            }
        }, "cold-start-sampler");
        sampler.setDaemon(true);
        sampler.start();
        long start = System.nanoTime();
        String result;
        try {
            result = ReadAction.compute(step::get);
        }
        finally {
            running.set(false);
        }
        long elapsed = millis(start);
        try {
            sampler.join();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int built = ReadAction.compute(() -> {
            DefMapService defMapService = project.getService(DefMapService.class);
            int count = 0;
            for (Crate crate : CrateGraphService.crateGraph(project).getTopSortedCrates()) {
                Integer id = crate.getId();
                if (id != null && defMapService.hasDefMapFor(id) && defMapService.getDefMapHolder(id).getDefMap() != null) count++;
            }
            return count;
        });
        report.add(String.format("%-45s %7d ms  %-24s def maps built: %d", name, elapsed, result, built));
        if (elapsed > 1000) {
            inclusive.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(150)
                .forEach(e -> report.add(String.format("      %3d%%  %s", e.getValue() * 100 / Math.max(1, samples[0]), e.getKey())));
            report.add("    self time (top of stack <- first Rust method):");
            self.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(40)
                .forEach(e -> report.add(String.format("      %4d  %s", e.getValue(), e.getKey())));
        }
    }

    /** A frame of a thread that waits rather than works. */
    private static boolean isWaiting(StackTraceElement frame) {
        String name = frame.getClassName() + "." + frame.getMethodName();
        return name.startsWith("jdk.internal.misc.Unsafe.park") || name.startsWith("java.lang.Object.wait")
            || name.startsWith("java.lang.Thread.sleep") || name.contains("LockSupport.park");
    }

    private static long millis(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
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
