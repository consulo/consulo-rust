/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

public record CargoBuildResult(
    boolean succeeded,
    boolean canceled,
    long started,
    long duration,
    int errors,
    int warnings,
    String message
) {
    public CargoBuildResult(boolean succeeded, boolean canceled, long started) {
        this(succeeded, canceled, started, 0, 0, 0, "");
    }

    public CargoBuildResult(boolean succeeded, boolean canceled, long started, long duration) {
        this(succeeded, canceled, started, duration, 0, 0, "");
    }
}
