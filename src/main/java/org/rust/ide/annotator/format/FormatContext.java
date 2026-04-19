/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFormatMacroArg;
import org.rust.lang.core.psi.RsMacroCall;

import java.util.*;

public class FormatContext {
    @NotNull
    private final List<FormatParameter> myParameters;
    @NotNull
    private final List<RsFormatMacroArg> myArguments;
    @NotNull
    private final RsMacroCall myMacro;
    @NotNull
    private final Set<Pair<FormatParameter, ParameterLookup.Named>> myNamedParameters;
    @NotNull
    private final Set<Pair<FormatParameter, ParameterLookup.Positional>> myPositionalParameters;
    @NotNull
    private final Map<String, RsFormatMacroArg> myNamedArguments;

    public FormatContext(@NotNull List<FormatParameter> parameters, @NotNull List<RsFormatMacroArg> arguments, @NotNull RsMacroCall macro) {
        this.myParameters = parameters;
        this.myArguments = arguments;
        this.myMacro = macro;

        Set<Pair<FormatParameter, ParameterLookup.Named>> namedParams = new LinkedHashSet<>();
        Set<Pair<FormatParameter, ParameterLookup.Positional>> positionalParams = new LinkedHashSet<>();
        for (FormatParameter param : parameters) {
            if (param.getLookup() instanceof ParameterLookup.Named) {
                namedParams.add(new Pair<>(param, (ParameterLookup.Named) param.getLookup()));
            } else if (param.getLookup() instanceof ParameterLookup.Positional) {
                positionalParams.add(new Pair<>(param, (ParameterLookup.Positional) param.getLookup()));
            }
        }
        this.myNamedParameters = namedParams;
        this.myPositionalParameters = positionalParams;

        Map<String, RsFormatMacroArg> namedArgs = new LinkedHashMap<>();
        for (RsFormatMacroArg arg : arguments) {
            String name = FormatImpl.getArgName(arg);
            if (name != null) {
                namedArgs.put(name, arg);
            }
        }
        this.myNamedArguments = namedArgs;
    }

    @NotNull
    public List<FormatParameter> getParameters() {
        return myParameters;
    }

    @NotNull
    public List<RsFormatMacroArg> getArguments() {
        return myArguments;
    }

    @NotNull
    public RsMacroCall getMacro() {
        return myMacro;
    }

    @NotNull
    public Set<Pair<FormatParameter, ParameterLookup.Named>> getNamedParameters() {
        return myNamedParameters;
    }

    @NotNull
    public Set<Pair<FormatParameter, ParameterLookup.Positional>> getPositionalParameters() {
        return myPositionalParameters;
    }

    @NotNull
    public Map<String, RsFormatMacroArg> getNamedArguments() {
        return myNamedArguments;
    }
}
