/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

public abstract class MirPlaceContext {
    private MirPlaceContext() {
    }

    public boolean isUse() {
        return !(this instanceof NonUse);
    }

    public boolean isMutatingUse() {
        return this instanceof MutatingUse;
    }

    public abstract static class NonMutatingUse extends MirPlaceContext {
        private NonMutatingUse() {
        }

        public static final class Projection extends NonMutatingUse {
            public static final Projection INSTANCE = new Projection();

            private Projection() {
            }
        }

        public static final class Inspect extends NonMutatingUse {
            public static final Inspect INSTANCE = new Inspect();

            private Inspect() {
            }
        }

        public static final class Copy extends NonMutatingUse {
            public static final Copy INSTANCE = new Copy();

            private Copy() {
            }
        }

        public static final class Move extends NonMutatingUse {
            public static final Move INSTANCE = new Move();

            private Move() {
            }
        }

        public static final class SharedBorrow extends NonMutatingUse {
            public static final SharedBorrow INSTANCE = new SharedBorrow();

            private SharedBorrow() {
            }
        }

        public static final class ShallowBorrow extends NonMutatingUse {
            public static final ShallowBorrow INSTANCE = new ShallowBorrow();

            private ShallowBorrow() {
            }
        }

        public static final class UniqueBorrow extends NonMutatingUse {
            public static final UniqueBorrow INSTANCE = new UniqueBorrow();

            private UniqueBorrow() {
            }
        }
    }

    public abstract static class MutatingUse extends MirPlaceContext {
        private MutatingUse() {
        }

        public static final class Projection extends MutatingUse {
            public static final Projection INSTANCE = new Projection();

            private Projection() {
            }
        }

        public static final class Store extends MutatingUse {
            public static final Store INSTANCE = new Store();

            private Store() {
            }
        }

        public static final class Borrow extends MutatingUse {
            public static final Borrow INSTANCE = new Borrow();

            private Borrow() {
            }
        }

        public static final class Call extends MutatingUse {
            public static final Call INSTANCE = new Call();

            private Call() {
            }
        }

        public static final class Drop extends MutatingUse {
            public static final Drop INSTANCE = new Drop();

            private Drop() {
            }
        }
    }

    public abstract static class NonUse extends MirPlaceContext {
        private NonUse() {
        }

        public static final class VarDebugInfo extends NonUse {
            public static final VarDebugInfo INSTANCE = new VarDebugInfo();

            private VarDebugInfo() {
            }
        }

        public static final class StorageLive extends NonUse {
            public static final StorageLive INSTANCE = new StorageLive();

            private StorageLive() {
            }
        }

        public static final class StorageDead extends NonUse {
            public static final StorageDead INSTANCE = new StorageDead();

            private StorageDead() {
            }
        }
    }
}
