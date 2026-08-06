/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import org.rust.openapiext.Testmark;

public final class CfgTestmarks {
    private CfgTestmarks() {
    }

    public static final class EvaluatesTrue extends Testmark {
        public static final EvaluatesTrue INSTANCE = new EvaluatesTrue();
        private EvaluatesTrue() {}
    }

    public static final class EvaluatesFalse extends Testmark {
        public static final EvaluatesFalse INSTANCE = new EvaluatesFalse();
        private EvaluatesFalse() {}
    }
}
