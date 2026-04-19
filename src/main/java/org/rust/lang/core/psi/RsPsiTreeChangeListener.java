/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

/**
 * Utility/marker class that groups the PSI tree change event handling classes.
 * <p>
 * <ul>
 *     <li>{@link RsPsiTreeChangeEvent} - sealed hierarchy of PSI tree change events</li>
 *     <li>{@link RsPsiTreeChangeAdapter} - adapter that converts {@link com.intellij.psi.PsiTreeChangeEvent}
 *         into {@link RsPsiTreeChangeEvent} instances</li>
 * </ul>
 * <p>
 * These have been converted to separate Java files:
 * <ul>
 *     <li>{@link RsPsiTreeChangeEvent}</li>
 *     <li>{@link RsPsiTreeChangeAdapter}</li>
 * </ul>
 * <p>
 * Usage: extend {@link RsPsiTreeChangeAdapter} and override
 * {@link RsPsiTreeChangeAdapter#handleEvent(RsPsiTreeChangeEvent)} to handle all PSI tree change events
 * via a single method.
 *
 * @see RsPsiTreeChangeEvent
 * @see RsPsiTreeChangeAdapter
 */
public final class RsPsiTreeChangeListener {
    private RsPsiTreeChangeListener() {
    }
}
