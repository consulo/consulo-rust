/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack.util;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.util.CmdBase;
import org.rust.cargo.util.Opt;
import org.rust.cargo.util.OptBuilder;
import org.rust.cargo.util.RsCommandCompletionProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WasmPackCommandCompletionProvider extends RsCommandCompletionProvider {

    public WasmPackCommandCompletionProvider(
        @NotNull CargoProjectsService projects,
        @NotNull String implicitTextPrefix,
        @NotNull Supplier<CargoWorkspace> workspaceGetter
    ) {
        super(projects, implicitTextPrefix, workspaceGetter);
    }

    public WasmPackCommandCompletionProvider(
        @NotNull CargoProjectsService projects,
        @NotNull Supplier<CargoWorkspace> workspaceGetter
    ) {
        this(projects, "", workspaceGetter);
    }

    @NotNull
    @Override
    protected List<CmdBase> getCommonCommands() {
        return Arrays.asList(
            new WasmPackCmd("new"),

            new WasmPackCmd("build") {
                @Override
                protected void initOptions(@NotNull WasmPackOptBuilder builder) {
                    builder.opt("target", ctx ->
                        Arrays.asList("bundler", "nodejs", "web", "no-modules").stream()
                            .map(LookupElementBuilder::create)
                            .collect(Collectors.toList())
                    );

                    // Profile
                    builder.flag("dev");
                    builder.flag("profiling");
                    builder.flag("release");

                    builder.opt("out-dir", ctx ->
                        List.of(LookupElementBuilder.create("pkg"))
                    );

                    builder.opt("out-name", ctx ->
                        ctx.projects().stream()
                            .map(project -> LookupElementBuilder.create(project.getPresentableName()))
                            .collect(Collectors.toList())
                    );

                    builder.opt("scope", ctx ->
                        List.of(LookupElementBuilder.create("example"))
                    );
                }
            },

            new WasmPackCmd("test") {
                @Override
                protected void initOptions(@NotNull WasmPackOptBuilder builder) {
                    // Profile
                    builder.flag("release");

                    // Test environment
                    builder.flag("headless");
                    builder.flag("node");
                    builder.flag("firefox");
                    builder.flag("chrome");
                    builder.flag("safari");
                }
            },

            new WasmPackCmd("pack"),

            new WasmPackCmd("publish") {
                @Override
                protected void initOptions(@NotNull WasmPackOptBuilder builder) {
                    builder.opt("tag", ctx ->
                        List.of(LookupElementBuilder.create("latest"))
                    );
                }
            }
        );
    }

    private static class WasmPackCmd extends CmdBase {
        private final List<Opt> options;

        WasmPackCmd(@NotNull String name) {
            super(name);
            WasmPackOptBuilder builder = new WasmPackOptBuilder();
            initOptions(builder);
            this.options = builder.getResult();
        }

        protected void initOptions(@NotNull WasmPackOptBuilder builder) {
            // Default: no options
        }

        @NotNull
        @Override
        public List<Opt> getOptions() {
            return options;
        }
    }

    private static class WasmPackOptBuilder implements OptBuilder {
        private final List<Opt> result = new ArrayList<>();

        @NotNull
        @Override
        public List<Opt> getResult() {
            return result;
        }
    }
}
