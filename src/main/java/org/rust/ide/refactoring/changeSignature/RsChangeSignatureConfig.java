/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

/**
 * This file serves as an aggregation point for the change signature configuration types
 *
 * The individual types have been converted to separate Java files:
 * <ul>
 *   <li>{@link RsSignatureChangeInfo} - Holds config and implements {@link com.intellij.refactoring.changeSignature.ChangeInfo}</li>
 *   <li>{@link ParameterProperty} - Sealed hierarchy for empty, invalid, and valid type references/expressions</li>
 *   <li>{@link Parameter} - Represents a function parameter with pattern, type, index, and default value</li>
 *   <li>{@link RsChangeFunctionSignatureConfig} - Mutable configuration for the Change Signature dialog</li>
 * </ul>
 *
 * @see RsSignatureChangeInfo
 * @see ParameterProperty
 * @see Parameter
 * @see RsChangeFunctionSignatureConfig
 */
public final class RsChangeSignatureConfig {
    private RsChangeSignatureConfig() {
    }
}
