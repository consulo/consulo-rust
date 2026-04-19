/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.completion.LookupElementsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CargoCommandCompletionProvider extends RsCommandCompletionProvider {

    private final List<CmdBase> commonCommandsList;

    public CargoCommandCompletionProvider(
        CargoProjectsService projects,
        String implicitTextPrefix,
        Supplier<CargoWorkspace> workspaceGetter
    ) {
        super(projects, implicitTextPrefix, workspaceGetter);
        this.commonCommandsList = buildCommonCommands();
    }

    public CargoCommandCompletionProvider(CargoProjectsService projects, Supplier<CargoWorkspace> workspaceGetter) {
        this(projects, "", workspaceGetter);
    }

    public CargoCommandCompletionProvider(CargoProjectsService projects, CargoWorkspace workspace) {
        this(projects, "", () -> workspace);
    }

    @Override
    protected List<CmdBase> getCommonCommands() {
        return commonCommandsList;
    }

    private List<CmdBase> buildCommonCommands() {
        List<CmdBase> commands = new ArrayList<>();
        for (CargoCommands command : CargoCommands.values()) {
            Cmd cmd = new Cmd(command.getPresentableName(), command);
            commands.add(cmd);
        }
        return commands;
    }

    private static ArgCompleter getCompleterForOption(String name) {
        return switch (name) {
            case "bin" -> targetCompleter(CargoWorkspace.TargetKind.Bin.INSTANCE);
            case "example" -> targetCompleter(CargoWorkspace.TargetKind.ExampleBin.INSTANCE);
            case "test" -> targetCompleter(CargoWorkspace.TargetKind.Test.INSTANCE);
            case "bench" -> targetCompleter(CargoWorkspace.TargetKind.Bench.INSTANCE);
            case "package" -> (ctx) -> {
                CargoWorkspace ws = ctx.currentWorkspace();
                if (ws == null) return Collections.emptyList();
                return ws.getPackages().stream()
                    .map(CargoCommandCompletionProvider::packageLookupElement)
                    .collect(Collectors.toList());
            };
            case "manifest-path" -> (ctx) -> ctx.projects().stream()
                .map(CargoCommandCompletionProvider::projectLookupElement)
                .collect(Collectors.toList());
            case "target" -> CargoCommandCompletionProvider::getTargetTripleCompletions;
            default -> null;
        };
    }

    private static ArgCompleter targetCompleter(CargoWorkspace.TargetKind kind) {
        return (ctx) -> {
            CargoWorkspace ws = ctx.currentWorkspace();
            if (ws == null) return Collections.emptyList();
            return ws.getPackages().stream()
                .filter(pkg -> pkg.getOrigin() == PackageOrigin.WORKSPACE)
                .flatMap(pkg -> pkg.getTargets().stream().filter(t -> t.getKind() == kind))
                .map(target -> LookupElementBuilder.create(target.getName()))
                .collect(Collectors.toList());
        };
    }

    private static List<LookupElement> getTargetTripleCompletions(Context ctx) {
        CargoProject cargoProject = ctx.projects().stream().findFirst().orElse(null);
        if (cargoProject == null) return Collections.emptyList();
        var rustcInfo = cargoProject.getRustcInfo();
        if (rustcInfo == null) return Collections.emptyList();
        var targets = rustcInfo.getTargets();
        if (targets == null) return Collections.emptyList();
        return targets.stream()
            .map(LookupElementBuilder::create)
            .collect(Collectors.toList());
    }

    private static LookupElement projectLookupElement(CargoProject project) {
        return LookupElementBuilder.create(project.getManifest().toString()).withIcon(CargoIcons.ICON);
    }

    private static LookupElement packageLookupElement(CargoWorkspace.Package pkg) {
        double priority = pkg.getOrigin() == PackageOrigin.WORKSPACE ? 1.0 : 0.0;
        return LookupElementsUtil.withPriority(LookupElementBuilder.create(pkg.getName()), priority);
    }

    private static class Cmd extends CmdBase {
        private final List<Opt> optionsList;

        Cmd(String name, CargoCommands command) {
            super(name);
            List<Opt> opts = new ArrayList<>();
            for (CargoOption option : command.getOptions()) {
                ArgCompleter completer = getCompleterForOption(option.name());
                if (completer != null) {
                    opts.add(new Opt(option.name(), completer));
                } else {
                    opts.add(new Opt(option.name()));
                }
            }
            this.optionsList = opts;
        }

        @Override
        public List<Opt> getOptions() {
            return optionsList;
        }
    }
}
