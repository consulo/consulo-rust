package com.intellij;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** IntelliJ-compat stub — message bundle base. */
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
