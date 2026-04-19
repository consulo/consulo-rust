/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import org.rust.lang.utils.Node;
import org.rust.lang.utils.PresentableGraph;

/**
 *   typealias MacroGraph = PresentableGraph<MGNodeData, Void>
 *   typealias MacroGraphNode = Node<MGNodeData, Void>
 *
 * In Java, use the types directly:
 *   PresentableGraph<MGNodeData, Void> for MacroGraph
 *   Node<MGNodeData, Void> for MacroGraphNode
 *
 * This class provides factory methods and type documentation.
 */
public final class MacroGraph {
    private MacroGraph() {
    }

    /**
     * Creates a new empty macro graph.
     */
    public static PresentableGraph<MGNodeData, Void> create() {
        return new PresentableGraph<>();
    }
}
