# Logging ID plugin

When Jenkins hits an unhandled exception it prints a Logging ID and expects you to find the
matching stack trace in **Manage Jenkins → System Log**. That page is backed by a ring buffer of
only 256 records, so on a busy controller the record is usually rotated away before you look for
it — and you end up grepping the log file on the server instead.

This plugin captures those records on its own, keeps the most recent 200 of them, and shows the
matching stack trace directly on the error page.

## Features

- **Error page panel** — the Oops page gets a floating panel with the full captured log for the
  Logging ID it is showing, plus a Copy button and a link to the query page.
- **Query page** at `/logging-id/` — lists captured exceptions with time, level, exception type
  and message; supports free-text search (Logging ID, exception, logger, stack trace), Logging ID
  prefix matching, and level filtering.
- **Details view** — time, level, logger, exception, root cause and the complete stack trace, with
  copy and download-as-text actions.
- **Independent of the System Log ring buffer** — records are captured from the JUL root logger
  into a separate fixed-size store, so a noisy controller does not evict them.
- **Administrator only** — the query page and the log content endpoint require the `ADMINISTER`
  permission.

## Requirements

- Jenkins 2.479.3 or newer (tested against 2.528.3)
- Java 17 or newer

## Install

```bash
mvn -B -ntp package
```

Then upload `target/logging-id.hpi` via **Manage Jenkins → Plugins → Advanced settings → Deploy
plugin**, or copy it to `$JENKINS_HOME/plugins/` and restart Jenkins.

Release builds are also published to GitHub Packages. To resolve them from another Maven build,
add the repository to your `pom.xml`:

```xml
<repository>
  <id>github</id>
  <url>https://maven.pkg.github.com/Amor122/jenkins-logging-id</url>
</repository>
```

and supply credentials with `packages:read` in `~/.m2/settings.xml` under the `github` server id.

## Usage

1. Trigger any error that produces an Oops page, for example by uploading a plugin while the
   update center is misbehaving.
2. The page shows the usual `Logging ID`; the panel in the bottom-right corner loads the captured
   log for that ID.
3. To browse older records, open **Logging ID** from the side panel (or `/logging-id/`) and search
   by ID or keyword.

## Limitations

- Records are kept in memory only: they are cleared on restart, and only the latest 200 are kept.
- Only records printed by Jenkins' uncaught exception handler
  (`Caught unhandled exception with ID ...`) are captured; ordinary warnings are not.
- Log content is served to administrators only. The error page itself may be rendered for an
  anonymous session, so the panel can only show the ID in that case.

## Development

```bash
mvn -B -ntp verify   # unit + Jelly rendering tests
mvn hpi:run          # run a local Jenkins with the plugin
```

## License

[MIT](LICENSE.md)
