/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.runAnything.wasmpack;

import org.rust.ide.actions.runAnything.RsRunAnythingItem;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class RunAnythingWasmPackItem extends RsRunAnythingItem {

    private static final Map<String, String> BUILD_OPTIONS_DESCRIPTIONS = new HashMap<>();
    private static final Map<String, String> TEST_OPTIONS_DESCRIPTIONS = new HashMap<>();
    private static final Map<String, String> PUBLISH_OPTIONS_DESCRIPTIONS = new HashMap<>();

    static {
        BUILD_OPTIONS_DESCRIPTIONS.put("--target", "Output target environment: bundler (default), nodejs, web, no-modules");
        BUILD_OPTIONS_DESCRIPTIONS.put("--dev", "Development profile: debug info, no optimizations");
        BUILD_OPTIONS_DESCRIPTIONS.put("--profiling", "Profiling profile: optimizations and debug info");
        BUILD_OPTIONS_DESCRIPTIONS.put("--release", "Release profile: optimizations, no debug info");
        BUILD_OPTIONS_DESCRIPTIONS.put("--out-dir", "Output directory");
        BUILD_OPTIONS_DESCRIPTIONS.put("--out-name", "Generated file names");
        BUILD_OPTIONS_DESCRIPTIONS.put("--scope", "The npm scope to use");

        TEST_OPTIONS_DESCRIPTIONS.put("--release", "Build with release profile");
        TEST_OPTIONS_DESCRIPTIONS.put("--headless", "Test in headless browser mode");
        TEST_OPTIONS_DESCRIPTIONS.put("--node", "Run the tests in Node.js");
        TEST_OPTIONS_DESCRIPTIONS.put("--firefox", "Run the tests in Firefox");
        TEST_OPTIONS_DESCRIPTIONS.put("--chrome", "Run the tests in Chrome");
        TEST_OPTIONS_DESCRIPTIONS.put("--safari", "Run the tests in Safari");

        PUBLISH_OPTIONS_DESCRIPTIONS.put("--tag", "NPM tag to publish with");
    }

    public RunAnythingWasmPackItem(String command, Icon icon) {
        super(command, icon);
    }

    @Override
    public String getHelpCommand() {
        return "wasm-pack";
    }

    @Override
    public Map<String, String> getCommandDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("new", "Create a new RustWasm project");
        descriptions.put("build", "Build and pack the project into pkg directory");
        descriptions.put("test", "Run tests using the wasm-bindgen test runner");
        descriptions.put("pack", "(npm) Create a tarball from pkg directory");
        descriptions.put("publish", "(npm) Create a tarball and publish to the NPM registry");
        return descriptions;
    }

    @Override
    public Map<String, String> getOptionsDescriptionsForCommand(String commandName) {
        return switch (commandName) {
            case "build" -> BUILD_OPTIONS_DESCRIPTIONS;
            case "test" -> TEST_OPTIONS_DESCRIPTIONS;
            case "publish" -> PUBLISH_OPTIONS_DESCRIPTIONS;
            default -> null;
        };
    }
}
