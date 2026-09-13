/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.format;

import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFormatMacroArg;
import org.rust.lang.core.psi.RsMacroCall;

import java.util.*;

public class FormatContext {
    @Nonnull
    private final List<FormatParameter> myParameters;
    @Nonnull
    private final List<RsFormatMacroArg> myArguments;
    @Nonnull
    private final RsMacroCall myMacro;
    @Nonnull
    private final Set<Pair<FormatParameter, ParameterLookup.Named>> myNamedParameters;
    @Nonnull
    private final Set<Pair<FormatParameter, ParameterLookup.Positional>> myPositionalParameters;
    @Nonnull
    private final Map<String, RsFormatMacroArg> myNamedArguments;

    public FormatContext(@Nonnull List<FormatParameter> parameters, @Nonnull List<RsFormatMacroArg> arguments, @Nonnull RsMacroCall macro) {
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

    @Nonnull
    public List<FormatParameter> getParameters() {
        return myParameters;
    }

    @Nonnull
    public List<RsFormatMacroArg> getArguments() {
        return myArguments;
    }

    @Nonnull
    public RsMacroCall getMacro() {
        return myMacro;
    }

    @Nonnull
    public Set<Pair<FormatParameter, ParameterLookup.Named>> getNamedParameters() {
        return myNamedParameters;
    }

    @Nonnull
    public Set<Pair<FormatParameter, ParameterLookup.Positional>> getPositionalParameters() {
        return myPositionalParameters;
    }

    @Nonnull
    public Map<String, RsFormatMacroArg> getNamedArguments() {
        return myNamedArguments;
    }
}
