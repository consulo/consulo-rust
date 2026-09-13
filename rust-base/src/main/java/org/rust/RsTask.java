/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust;

public interface RsTask {
    default TaskType getTaskType() {
        return TaskType.INDEPENDENT;
    }

    default int getProgressBarShowDelay() {
        return 0;
    }

    /** If true, the task will not be run (and progress bar will not be shown) until the smart mode */
    default boolean getWaitForSmartMode() {
        return false;
    }

    default boolean getRunSyncInUnitTests() {
        return false;
    }

    /**
     * Higher position in the enum means higher priority; Newly submitted tasks with higher or equal
     * priority cancels other tasks with lower or equal priority if {@link TaskType#canBeCanceledByOther} == true.
     * E.g. {@link TaskType#CARGO_SYNC} cancels {@link TaskType#MACROS_UNPROCESSED} and subsequent but not {@link TaskType#MACROS_CLEAR} or itself.
     * {@link TaskType#MACROS_UNPROCESSED} cancels itself, {@link TaskType#MACROS_FULL} and subsequent.
     */
    enum TaskType {
        CARGO_SYNC(false),
        MACROS_CLEAR(false),
        MACROS_UNPROCESSED(true),
        MACROS_FULL(true),

        /** Can't be canceled, cancels nothing. Should be the last variant of the enum. */
        INDEPENDENT(false);

        private final boolean canBeCanceledByOther;

        TaskType(boolean canBeCanceledByOther) {
            this.canBeCanceledByOther = canBeCanceledByOther;
        }

        TaskType() {
            this(true);
        }

        public boolean canBeCanceledByOther() {
            return canBeCanceledByOther;
        }

        public boolean canCancelOther(TaskType other) {
            return other.canBeCanceledByOther && this.ordinal() <= other.ordinal();
        }
    }
}
