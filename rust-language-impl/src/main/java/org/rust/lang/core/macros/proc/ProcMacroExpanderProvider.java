package org.rust.lang.core.macros.proc;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.crate.Crate;

/**
 * Supplies the expander for a crate. Implemented where the toolchain is known, so that macro
 * expansion itself does not need to reach for it.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ProcMacroExpanderProvider {
    static ProcMacroExpanderProvider getInstance() {
        return Application.get().getInstance(ProcMacroExpanderProvider.class);
    }

    @Nonnull
    ProcMacroExpander forCrate(@Nonnull Crate crate);
}
