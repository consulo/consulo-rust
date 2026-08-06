/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

public class TypeInferenceOptions {
    public static final TypeInferenceOptions DEFAULT = new TypeInferenceOptions(false);

    private final boolean myTraceObligations;

    public TypeInferenceOptions() {
        this(false);
    }

    public TypeInferenceOptions(boolean traceObligations) {
        myTraceObligations = traceObligations;
    }

    public boolean isTraceObligations() {
        return myTraceObligations;
    }
}
