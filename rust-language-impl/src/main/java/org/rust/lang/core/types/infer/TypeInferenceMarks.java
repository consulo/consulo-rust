/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.rust.openapiext.Testmark;

public final class TypeInferenceMarks {
    private TypeInferenceMarks() {
    }

    public static final Testmark CyclicType = new Testmark();
    public static final Testmark RecursiveProjectionNormalization = new Testmark();
    public static final Testmark QuestionOperator = new Testmark();
    public static final Testmark MethodPickTraitScope = new Testmark();
    public static final Testmark MethodPickTraitsOutOfScope = new Testmark();
    public static final Testmark MethodPickCheckBounds = new Testmark();
    public static final Testmark MethodPickDerefOrder = new Testmark();
    public static final Testmark MethodPickCollapseTraits = new Testmark();
    public static final Testmark WinnowSpecialization = new Testmark();
    public static final Testmark WinnowParamCandidateWins = new Testmark();
    public static final Testmark WinnowParamCandidateLoses = new Testmark();
    public static final Testmark WinnowObjectOrProjectionCandidateWins = new Testmark();
    public static final Testmark TraitSelectionOverflow = new Testmark();
    public static final Testmark UnsizeToTraitObject = new Testmark();
    public static final Testmark UnsizeArrayToSlice = new Testmark();
    public static final Testmark UnsizeStruct = new Testmark();
    public static final Testmark UnsizeTuple = new Testmark();
}
