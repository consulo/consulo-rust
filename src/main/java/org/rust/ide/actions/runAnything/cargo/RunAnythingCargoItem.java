/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.runAnything.cargo;

import org.rust.cargo.util.CargoCommands;
import org.rust.cargo.util.CargoOption;
import org.rust.ide.actions.runAnything.RsRunAnythingItem;

import javax.swing.*;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class RunAnythingCargoItem extends RsRunAnythingItem {

    public RunAnythingCargoItem(String command, Icon icon) {
        super(command, icon);
    }

    @Override
    public String getHelpCommand() {
        return "cargo";
    }

    @Override
    public Map<String, String> getCommandDescriptions() {
        return Arrays.stream(CargoCommands.values())
            .collect(Collectors.toMap(CargoCommands::getPresentableName, CargoCommands::getDescription));
    }

    @Override
    public Map<String, String> getOptionsDescriptionsForCommand(String commandName) {
        CargoCommands command = Arrays.stream(CargoCommands.values())
            .filter(c -> c.getPresentableName().equals(commandName))
            .findFirst()
            .orElse(null);
        if (command == null) return null;
        return command.getOptions().stream()
            .collect(Collectors.toMap(CargoOption::getLongName, CargoOption::getDescription));
    }
}
