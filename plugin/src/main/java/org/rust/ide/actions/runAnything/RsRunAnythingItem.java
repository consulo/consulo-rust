/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.runAnything;

import consulo.ide.runAnything.RunAnythingItemBase;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.process.cmd.ParametersListUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public abstract class RsRunAnythingItem extends RunAnythingItemBase {

    public RsRunAnythingItem(String command, consulo.ui.image.Image icon) {
        super(command, icon);
    }

    public abstract String getHelpCommand();

    public abstract Map<String, String> getCommandDescriptions();

    public abstract Map<String, String> getOptionsDescriptionsForCommand(String commandName);

    @Override
    public Component createComponent(String pattern, boolean isSelected, boolean hasFocus) {
        Component component = super.createComponent(pattern, isSelected, hasFocus);
        customizeComponent(component);
        return component;
    }

    private void customizeComponent(Component component) {
        if (!(component instanceof JPanel panel)) return;

        List<String> params = ParametersListUtil.parse(StringUtil.trimStart(getCommand(), getHelpCommand()));
        String description;
        if (params.isEmpty()) {
            description = null;
        } else if (params.size() == 1) {
            description = getCommandDescriptions().get(params.get(params.size() - 1));
        } else {
            Map<String, String> optionsDescriptions = getOptionsDescriptionsForCommand(params.get(0));
            description = optionsDescriptions != null ? optionsDescriptions.get(params.get(params.size() - 1)) : null;
        }
        if (description == null) return;

        SimpleColoredComponent descriptionComponent = new SimpleColoredComponent();
        descriptionComponent.append(
            StringUtil.shortenTextWithEllipsis(" " + description + ".", 200, 0),
            SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES
        );
        panel.add(descriptionComponent, BorderLayout.EAST);
    }
}
