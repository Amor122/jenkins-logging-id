package io.jenkins.plugins.loggingid;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.util.HttpResponses;
import java.util.List;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Searchable view of the captured unhandled exceptions, available at {@code /logging-id/}.
 */
@Extension
public class LoggingIdAction implements RootAction {

    private final LoggingIdStore store = LoggingIdStore.get();

    @Override
    public String getIconFileName() {
        return isPermitted() ? "document.png" : null;
    }

    @Override
    public String getDisplayName() {
        return "Logging ID";
    }

    @Override
    public String getUrlName() {
        return "logging-id";
    }

    public boolean isPermitted() {
        return Jenkins.get().hasPermission(Jenkins.ADMINISTER);
    }

    /**
     * Serves the raw log text for one ID so the Oops page can load it with the viewer's own session.
     * The Oops page itself is rendered anonymously, so the permission check has to happen here.
     */
    public HttpResponse doLog(@QueryParameter String id) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        LoggingIdEntry entry = store.resolve(id);
        if (entry == null) {
            return HttpResponses.notFound();
        }
        return HttpResponses.plainText(entry.getText());
    }

    public String getQuery() {
        StaplerRequest2 request = Stapler.getCurrentRequest2();
        if (request == null) {
            return "";
        }
        String query = request.getParameter("q");
        return query == null ? "" : query.trim();
    }

    public String getLevelFilter() {
        StaplerRequest2 request = Stapler.getCurrentRequest2();
        if (request == null) {
            return "";
        }
        String level = request.getParameter("level");
        return level == null ? "" : level.trim();
    }

    public List<LoggingIdEntry> getEntries() {
        if (!isPermitted()) {
            return List.of();
        }
        List<LoggingIdEntry> entries = store.search(getQuery());
        String level = getLevelFilter();
        if (level.isEmpty()) {
            return entries;
        }
        return entries.stream().filter(entry -> level.equalsIgnoreCase(entry.getLevel())).toList();
    }

    public LoggingIdEntry getSelected() {
        return isPermitted() ? store.resolve(getQuery()) : null;
    }

    public int getMaxEntries() {
        return store.getMaxEntries();
    }

    public int getStoredCount() {
        return store.size();
    }
}
