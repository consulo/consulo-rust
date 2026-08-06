/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import org.rust.lang.utils.Node;
import org.rust.lang.utils.PresentableGraph;

/**
 * <pre>
 *   typealias CFGNode = Node&lt;CFGNodeData, CFGEdgeData&gt;
 *   typealias CFGGraph = PresentableGraph&lt;CFGNodeData, CFGEdgeData&gt;
 * </pre>
 * In Java, use {@code Node<CFGNodeData, CFGEdgeData>} and {@code PresentableGraph<CFGNodeData, CFGEdgeData>} directly,
 * or use the type aliases defined here.
 */
public final class CFGTypes {
    private CFGTypes() {
    }
}

// Java cannot create true typealiases. Use the following in your code:
//   Node<CFGNodeData, CFGEdgeData> for CFGNode
//   PresentableGraph<CFGNodeData, CFGEdgeData> for CFGGraph
