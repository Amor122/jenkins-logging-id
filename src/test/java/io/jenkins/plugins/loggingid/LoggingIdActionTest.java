package io.jenkins.plugins.loggingid;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

public class LoggingIdActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void capturesAndRendersLoggingId() throws Exception {
        String id = UUID.randomUUID().toString();
        Logger.getLogger("io.jenkins.plugins.loggingid.test").log(
                Level.WARNING,
                "Caught unhandled exception with ID " + id,
                new RuntimeException("boom-marker", new IllegalStateException("root-marker")));

        WebClient wc = j.createWebClient();
        String page = wc.goTo("logging-id/?q=" + id).asNormalizedText();

        assertTrue(page.contains(id));
        assertTrue(page.contains("boom-marker"));
        assertTrue(page.contains("java.lang.RuntimeException"));
        assertTrue(page.contains("root-marker"));
    }

    @Test
    public void servesRawLogTextForOopsPanel() throws Exception {
        String id = UUID.randomUUID().toString();
        Logger.getLogger("io.jenkins.plugins.loggingid.test").log(
                Level.WARNING,
                "Caught unhandled exception with ID " + id,
                new RuntimeException("boom-marker"));

        WebClient wc = j.createWebClient();
        var page = wc.getPage(new URL(j.getURL(), "logging-id/log?id=" + id));

        String text = page.getWebResponse().getContentAsString();
        assertTrue(text.contains(id));
        assertTrue(text.contains("boom-marker"));
    }

    @Test
    public void rendersEmptyState() throws Exception {
        WebClient wc = j.createWebClient();
        String page = wc.goTo("logging-id/").asNormalizedText();

        assertTrue(page.contains("Logging ID"));
        assertTrue(page.contains("Keeping the most recent"));
    }
}
