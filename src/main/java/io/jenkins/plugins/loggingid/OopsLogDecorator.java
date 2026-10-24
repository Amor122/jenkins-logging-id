package io.jenkins.plugins.loggingid;

import hudson.Extension;
import hudson.model.PageDecorator;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Exposes the Logging ID of the failed request to the footer of the core "Oops" page.
 *
 * <p>Jenkins serves that page through the container's error dispatch, which runs without the
 * security filters, so the request is anonymous there and {@code hasPermission} cannot be used to
 * decide whether to show the log. The footer therefore only renders the ID and lets the browser
 * fetch the text from {@code /logging-id/log}, where the permission check happens with the
 * viewer's own session.
 */
@Extension
public class OopsLogDecorator extends PageDecorator {

    private static final String ID_ATTRIBUTE = "jenkins.exception.id";

    public String getCurrentLoggingId() {
        StaplerRequest2 request = Stapler.getCurrentRequest2();
        if (request == null) {
            return null;
        }
        Object id = request.getAttribute(ID_ATTRIBUTE);
        return id instanceof String ? (String) id : null;
    }
}
