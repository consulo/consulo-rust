/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.common.graph;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class Node<N, E> {
    @Nonnull
    private final N myData;
    private final int myIndex;
    @Nullable
    private Edge<N, E> myFirstOutEdge;
    @Nullable
    private Edge<N, E> myFirstInEdge;

    public Node(@Nonnull N data, int index) {
        this(data, index, null, null);
    }

    public Node(@Nonnull N data, int index, @Nullable Edge<N, E> firstOutEdge, @Nullable Edge<N, E> firstInEdge) {
        myData = data;
        myIndex = index;
        myFirstOutEdge = firstOutEdge;
        myFirstInEdge = firstInEdge;
    }

    @Nonnull
    public N getData() {
        return myData;
    }

    public int getIndex() {
        return myIndex;
    }

    @Nullable
    public Edge<N, E> getFirstOutEdge() {
        return myFirstOutEdge;
    }

    public void setFirstOutEdge(@Nullable Edge<N, E> firstOutEdge) {
        myFirstOutEdge = firstOutEdge;
    }

    @Nullable
    public Edge<N, E> getFirstInEdge() {
        return myFirstInEdge;
    }

    public void setFirstInEdge(@Nullable Edge<N, E> firstInEdge) {
        myFirstInEdge = firstInEdge;
    }
}
