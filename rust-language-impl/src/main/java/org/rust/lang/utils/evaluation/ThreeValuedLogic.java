/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import jakarta.annotation.Nonnull;

/**
 * Three-valued logic with the following rules:
 * <ul>
 * <li>!Unknown = Unknown</li>
 * <li>True && Unknown = Unknown</li>
 * <li>False && Unknown = False</li>
 * <li>Unknown && Unknown = Unknown</li>
 * <li>True || Unknown = True</li>
 * <li>False || Unknown = Unknown</li>
 * <li>Unknown || Unknown = Unknown</li>
 * </ul>
 * For more information, see <a href="https://en.wikipedia.org/wiki/Three-valued_logic">Three-valued logic</a>
 */
public enum ThreeValuedLogic {
    True, False, Unknown;

    public boolean isTrue() {
        return this == True;
    }

    public boolean isFalse() {
        return this == False;
    }

    @Nonnull
    public static ThreeValuedLogic fromBoolean(boolean value) {
        return value ? True : False;
    }

    @Nonnull
    public ThreeValuedLogic and(@Nonnull ThreeValuedLogic other) {
        switch (this) {
            case True:
                return other;
            case False:
                return False;
            case Unknown:
                return other == False ? False : Unknown;
            default:
                return Unknown;
        }
    }

    @Nonnull
    public ThreeValuedLogic or(@Nonnull ThreeValuedLogic other) {
        switch (this) {
            case True:
                return True;
            case False:
                return other;
            case Unknown:
                return other == True ? True : Unknown;
            default:
                return Unknown;
        }
    }

    @Nonnull
    public ThreeValuedLogic not() {
        switch (this) {
            case True:
                return False;
            case False:
                return True;
            case Unknown:
                return Unknown;
            default:
                return Unknown;
        }
    }
}
