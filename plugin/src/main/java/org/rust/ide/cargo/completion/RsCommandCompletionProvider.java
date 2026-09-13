/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.cargo.completion;

import consulo.ide.impl.idea.codeInsight.completion.CodeCompletionHandlerBase;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.CharFilter;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.process.cmd.ParametersListUtil;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.workspace.CargoWorkspace;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.rust.cargo.util.Context;
import org.rust.cargo.util.ParametersListLexer;

public abstract class RsCommandCompletionProvider extends TextFieldCompletionProvider {

    private final CargoProjectsService projects;
    private final String implicitTextPrefix;
    private final Supplier<CargoWorkspace> workspaceGetter;

    public RsCommandCompletionProvider(
        CargoProjectsService projects,
        String implicitTextPrefix,
        Supplier<CargoWorkspace> workspaceGetter
    ) {
        this.projects = projects;
        this.implicitTextPrefix = implicitTextPrefix;
        this.workspaceGetter = workspaceGetter;
    }

    protected abstract List<CmdBase> getCommonCommands();

    public CargoProjectsService getProjects() {
        return projects;
    }

    public Supplier<CargoWorkspace> getWorkspaceGetter() {
        return workspaceGetter;
    }

    @Override
    public String getPrefix(String currentTextPrefix) {
        String[] split = splitContextPrefix(currentTextPrefix);
        return split[1];
    }

    public CharFilter.Result acceptChar(char c) {
        if (c == '-') return CharFilter.Result.ADD_TO_PREFIX;
        return null;
    }

    @Override
    public void addCompletionVariants(String text, int offset, String prefix, CompletionResultSet result) {
        String[] split = splitContextPrefix(text);
        result.addAllElements(complete(split[0]));
    }

    // public for testing
    public String[] splitContextPrefix(String text) {
        ParametersListLexer lexer = new ParametersListLexer(text);
        int contextEnd = 0;
        while (lexer.nextToken()) {
            if (lexer.getTokenEnd() == text.length()) {
                return new String[]{text.substring(0, contextEnd), lexer.getCurrentToken()};
            }
            contextEnd = lexer.getTokenEnd();
        }

        return new String[]{text.substring(0, contextEnd), ""};
    }

    // public for testing
    public List<LookupElement> complete(String context) {
        List<String> args = ParametersListUtil.parse(implicitTextPrefix + context);
        if (args.contains("--")) return Collections.emptyList();
        if (args.isEmpty()) {
            return getCommonCommands().stream()
                .map(CmdBase::getLookupElement)
                .collect(Collectors.toList());
        }

        String firstName = args.get(0);
        CmdBase cmd = null;
        for (CmdBase c : getCommonCommands()) {
            if (c.getName().equals(firstName)) {
                cmd = c;
                break;
            }
        }
        if (cmd == null) return Collections.emptyList();

        String lastArg = args.get(args.size() - 1);
        for (Opt opt : cmd.getOptions()) {
            if (opt.getLong().equals(lastArg) && opt.getArgCompleter() != null) {
                return opt.getArgCompleter().apply(new Context(projects.getAllProjects(), workspaceGetter.get(), args));
            }
        }

        return cmd.getOptions().stream()
            .filter(opt -> !args.contains(opt.getLong()))
            .map(Opt::getLookupElement)
            .collect(Collectors.toList());
    }
}
