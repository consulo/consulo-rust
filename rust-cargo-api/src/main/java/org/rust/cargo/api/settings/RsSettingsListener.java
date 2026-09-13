/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.settings;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;
import jakarta.annotation.Nonnull;

@TopicAPI(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.TO_PARENT)
public interface RsSettingsListener {
    void settingsChanged(@Nonnull RsProjectSettingsServiceBase.SettingsChangedEventBase<?> e);
}
