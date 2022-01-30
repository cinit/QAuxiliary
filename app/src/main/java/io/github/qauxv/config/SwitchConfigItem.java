package io.github.qauxv.config;

public interface SwitchConfigItem extends AbstractConfigItem {

    @Override
    boolean isValid();

    boolean isEnabled();

    /**
     * Set whether it is enabled, but NOT init or deinit it!
     *
     * @param enabled has no effect if isValid() returns false
     */
    void setEnabled(boolean enabled);
}
