# Mod download reliability notes

## CurseForge project version pagination

The CurseForge project-version fetcher uses bounded pagination in
`CurseForgeSearcher.getVersions(projectID, pageCallback)`.

Current request policy:
- 50 files per API page.
- 5 pages per request batch.
- At most 4 requests in flight.
- File IDs are de-duplicated while preserving the first-seen API order.
- End-of-list detection uses the raw API page size, not the de-duplicated count.
- Pagination stops with a clear error after 1,000 pages (50,000 raw file slots) rather than looping forever if a provider never signals the end.

The request batch and concurrency limits were reduced in commit
`47205ec71b81dabd9dfb8c7f3521c9f6d839e83d` to avoid large request bursts,
which can be especially troublesome on mobile connections and may trigger
provider rate limits. This is a conservative request-pressure adjustment;
it is separate from the shared transient-GET retry policy and does not guarantee availability during
provider outages or sustained rate limiting.

## Modrinth project version pagination

`ModrinthSearcher.getVersions(projectID, pageCallback)` requests
100 versions per page, de-duplicates by version ID, advances by the raw page
length, and stops if a full page adds no new IDs. It also has a 100-full-page
(10,000 raw version slot) safety cap and permits one terminal probe to
distinguish an exactly-full final page from additional data. A provider that
keeps returning full, unique pages beyond the cap produces a clear error
instead of an unbounded loop or silent truncation.

## Remaining verification

- Exercise projects with fewer than 50 files, exactly 50 files, and multiple
  full pages followed by a partial page.
- Verify cancellation stops outstanding page work.
- Verify provider rate-limit and server-error feedback is visible to the UI.
- Test with both official API access and any configured mirror.
- Run unit tests and an Android build before claiming runtime reliability.

No APK build or device test is implied by this document.

## Transient HTTP GET recovery

Commit `5c7c1bba9626d73e18bfb6d7c11e0a468951ec82` routes JSON GET requests
through the shared bounded retry helper. It retries network I/O/DNS failures,
HTTP 5xx responses, Ktor request timeouts, HTTP 408 timeouts, and HTTP 429
rate-limit responses. Cancellation is never retried. Commit `d5aa9ef3d2364225a29322d51478163a352ea937` corrected the retry
loop so `maxRetries` means retries after the first attempt, validates delay
settings, and does not sleep after the final failed attempt.

The retry schedule starts at 1 second and doubles up to 10 seconds, with three
retries by default. POST requests are intentionally not automatically retried
because their side effects may not be idempotent. Provider failover remains
available where the platform search layer already uses configured mirrors;
this change does not promise that every provider supports mirror fallback.

Unit coverage for retry success, non-retryable errors, cancellation,
retry-count semantics, and overflow-safe exponential delay calculation was
added/extended in commits
`7efdba33ba8f326e1a1d0a0b240761bf491b1247`,
`69e59ecc3cf58b4d2e316628594fb2ceee2e6882`,
`f6f4fe88db025bb045ceee0a66c73228258f8f20`, and
`af33132fe485fbdc4fb5589b02f9f00956ad9d2b`. Tests have not yet been
executed.
