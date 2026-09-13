/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;



import java.util.function.Supplier;

public interface TestmarkPred {
    
    <T> T checkHit(Supplier<T> f);

    
    <T> T checkNotHit(Supplier<T> f);
}
