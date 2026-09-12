package com.intellij;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** Loads messages from a {@link ResourceBundle}, formatting parameters and falling back to the key when it is missing. */
public abstract class DynamicBundle {
    private final String bundleName;
    private ResourceBundle bundle;

    protected DynamicBundle(String bundleName) { this.bundleName = bundleName; }

    public String getMessage(String key, Object... params) {
        try {
            if (bundle == null) bundle = ResourceBundle.getBundle(bundleName);
            String pattern = bundle.getString(key);
            return params.length == 0 ? pattern : MessageFormat.format(pattern, params);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
