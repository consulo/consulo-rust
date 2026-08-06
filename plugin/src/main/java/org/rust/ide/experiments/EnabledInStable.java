/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.experiments;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Experimental feature should be annotated with {@code @EnabledInStable} if it is enabled in stable releases,
 * i.e. it is included in {@code resources-stable/META-INF/experiments.xml} with {@code percentOfUsers="100"}.
 *
 * Enabled experimental features without {@code @EnabledInStable} annotation are intended to be collected in
 * {@code org.rust.ide.actions.diagnostic.CreateNewGithubIssue}
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnabledInStable {
}
