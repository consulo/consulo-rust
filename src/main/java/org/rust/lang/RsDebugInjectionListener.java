/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang;

import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.util.messages.Topic;
import org.rust.lang.core.psi.ext.RsElement;

public interface RsDebugInjectionListener {

    Topic<RsDebugInjectionListener> INJECTION_TOPIC = Topic.create("Rust Language Injected", RsDebugInjectionListener.class);

    void evalDebugContext(PsiLanguageInjectionHost host, DebugContext context);

    void didInject(PsiLanguageInjectionHost host);

    class DebugContext {
        private RsElement element;

        public DebugContext() {
            this.element = null;
        }

        public DebugContext(RsElement element) {
            this.element = element;
        }

        public RsElement getElement() {
            return element;
        }

        public void setElement(RsElement element) {
            this.element = element;
        }
    }
}
