/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import java.io.IOException;

public class ProcessAbortedException extends IOException {
    private final int myExitCode;

    public ProcessAbortedException(Throwable cause, int exitCode) {
        super("`intellij-rust-helper` is aborted; exit code: " + exitCode, cause);
        myExitCode = exitCode;
    }

    public int getExitCode() {
        return myExitCode;
    }
}
