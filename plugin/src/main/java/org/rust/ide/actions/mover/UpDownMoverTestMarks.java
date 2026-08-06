/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.mover;

import org.rust.openapiext.Testmark;

public final class UpDownMoverTestMarks {
    public static final Testmark MoveOutOfImpl = new Testmark() {};
    public static final Testmark MoveOutOfMatch = new Testmark() {};
    public static final Testmark MoveOutOfBody = new Testmark() {};
    public static final Testmark MoveOutOfBlock = new Testmark() {};

    private UpDownMoverTestMarks() {}
}
