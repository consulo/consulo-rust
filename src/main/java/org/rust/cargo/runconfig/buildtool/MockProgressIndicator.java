/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MockProgressIndicator extends EmptyProgressIndicator {
    private final List<String> textHistory = new ArrayList<>();

    public List<String> getTextHistory() {
        return Collections.unmodifiableList(textHistory);
    }

    @Override
    public void setText(@Nullable String text) {
        super.setText(text);
        textHistory.add(text);
    }

    @Override
    public void setText2(@Nullable String text) {
        super.setText2(text);
        textHistory.add(text);
    }
}
