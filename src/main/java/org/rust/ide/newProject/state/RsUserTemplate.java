/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject.state;

import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RsUserTemplate {

    @NlsContexts.ListItem
    @NotNull
    public String name;

    @NotNull
    public String url;

    public RsUserTemplate() {
        this("", "");
    }

    public RsUserTemplate(@NotNull String name, @NotNull String url) {
        this.name = name;
        this.url = url;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getUrl() {
        return url;
    }

    public void setUrl(@NotNull String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RsUserTemplate that = (RsUserTemplate) o;
        return Objects.equals(name, that.name) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }

    @Override
    public String toString() {
        return "RsUserTemplate(name=" + name + ", url=" + url + ")";
    }
}
