package io.jenkins.plugins.loggingid;

import hudson.init.InitMilestone;
import hudson.init.Initializer;

/**
 * Installs the capture handler as early as possible, including when the plugin is loaded without a restart.
 */
public final class LoggingIdInitializer {

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void init() {
        LoggingIdStore.get();
    }

    private LoggingIdInitializer() {
    }
}
