/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.support;

import consulo.annotation.component.ServiceImpl;
import consulo.process.DefaultCharsetProvider;
import jakarta.inject.Singleton;

import java.nio.charset.Charset;

@Singleton
@ServiceImpl
public class TestDefaultCharsetProvider implements DefaultCharsetProvider {

    @Override
    public Charset getDefaultCharset() {
        return Charset.defaultCharset();
    }
}
