/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.stubs.common.RsMetaItemArgsPsiOrStub;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration predicate
 */
abstract class CfgPredicate {

    static class NameOption extends CfgPredicate {
        @NotNull
        final String myName;

        NameOption(@NotNull String name) {
            myName = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NameOption that = (NameOption) o;
            return Objects.equals(myName, that.myName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myName);
        }
    }

    static class NameValueOption extends CfgPredicate {
        @NotNull
        final String myName;
        @NotNull
        final String myValue;

        NameValueOption(@NotNull String name, @NotNull String value) {
            myName = name;
            myValue = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NameValueOption that = (NameValueOption) o;
            return Objects.equals(myName, that.myName) && Objects.equals(myValue, that.myValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myName, myValue);
        }
    }

    static class All extends CfgPredicate {
        @NotNull
        final List<CfgPredicate> myList;

        All(@NotNull List<CfgPredicate> list) {
            myList = list;
        }
    }

    static class Any extends CfgPredicate {
        @NotNull
        final List<CfgPredicate> myList;

        Any(@NotNull List<CfgPredicate> list) {
            myList = list;
        }
    }

    static class Not extends CfgPredicate {
        @NotNull
        final CfgPredicate mySingle;

        Not(@NotNull CfgPredicate single) {
            mySingle = single;
        }
    }

    static final CfgPredicate ERROR = new CfgPredicate() {};

    @NotNull
    static CfgPredicate fromCfgAttributes(@NotNull Iterable<RsMetaItemPsiOrStub> cfgAttributes) {
        List<CfgPredicate> cfgPredicates = new ArrayList<>();
        for (RsMetaItemPsiOrStub attr : cfgAttributes) {
            List<? extends RsMetaItemPsiOrStub> argsList = attr.getMetaItemArgsList();
            if (!argsList.isEmpty()) {
                cfgPredicates.add(fromMetaItem(argsList.get(0)));
            }
        }

        if (cfgPredicates.size() == 1) {
            return cfgPredicates.get(0);
        }
        return new All(cfgPredicates);
    }

    @NotNull
    static CfgPredicate fromMetaItem(@NotNull RsMetaItemPsiOrStub metaItem) {
        RsMetaItemArgsPsiOrStub args = metaItem.getMetaItemArgs();
        String name = metaItem.getName();
        String value = metaItem.getValue();

        if (args != null) {
            List<CfgPredicate> predicates = new ArrayList<>();
            for (RsMetaItemPsiOrStub item : args.getMetaItemList()) {
                predicates.add(fromMetaItem(item));
            }
            if ("all".equals(name)) return new All(predicates);
            if ("any".equals(name)) return new Any(predicates);
            if ("not".equals(name)) {
                CfgPredicate single = predicates.size() == 1 ? predicates.get(0) : ERROR;
                return new Not(single);
            }
            return ERROR;
        }

        if (name != null && value != null) {
            return new NameValueOption(name, value);
        }

        if (name != null) {
            return new NameOption(name);
        }

        return ERROR;
    }
}
