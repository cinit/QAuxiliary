package io.github.qauxv.config;

/**
 * Base interface for a config item
 */
public interface AbstractConfigItem {

    boolean isValid();

    boolean sync();
}
