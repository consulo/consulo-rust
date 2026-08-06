/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject.state;


import jakarta.annotation.Nonnull;

import java.util.Objects;

public class RsUserTemplate {

    
    @Nonnull
    public String name;

    @Nonnull
    public String url;

    public RsUserTemplate() {
        this("", "");
    }

    public RsUserTemplate(@Nonnull String name, @Nonnull String url) {
        this.name = name;
        this.url = url;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public String getUrl() {
        return url;
    }

    public void setUrl(@Nonnull String url) {
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
