/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.debugger;

import consulo.language.psi.PsiLanguageInjectionHost;
import org.rust.lang.core.psi.ext.RsElement;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

@TopicAPI(value = ComponentScope.PROJECT)
public interface RsDebugInjectionListener {

    Class<RsDebugInjectionListener> INJECTION_TOPIC = RsDebugInjectionListener.class;

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
