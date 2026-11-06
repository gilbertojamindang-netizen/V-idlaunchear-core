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

The request batch and concurrency limits were reduced in commit
`47205ec71b81dabd9dfb8c7f3521c9f6d839e83d` to avoid large request bursts,
which can be especially troublesome on mobile connections and may trigger
provider rate limits. This is a conservative request-pressure adjustment;
it is not a retry policy and does not guarantee availability during provider
outages or rate limiting.

## Remaining verification

- Exercise projects with fewer than 50 files, exactly 50 files, and multiple
  full pages followed by a partial page.
- Verify cancellation stops outstanding page work.
- Verify provider rate-limit and server-error feedback is visible to the UI.
- Test with both official API access and any configured mirror.
- Run unit tests and an Android build before claiming runtime reliability.

No APK build or device test is implied by this document.

## Transient HTTP GET recovery

Commit `34e17ad92c9bfbec1991adb774a8678b3f2cdd9c` routes JSON GET requests
through the shared bounded retry helper. It retries network I/O/DNS failures,
HTTP 5xx responses, and HTTP 429 rate-limit responses. Cancellation is never
retried. Commit `d5aa9ef3d2364225a29322d51478163a352ea937` corrected the retry
loop so `maxRetries` means retries after the first attempt, validates delay
settings, and does not sleep after the final failed attempt.

The retry schedule starts at 1 second and doubles up to 10 seconds, with three
retries by default. POST requests are intentionally not automatically retried
because their side effects may not be idempotent. Provider failover remains
available where the platform search layer already uses configured mirrors;
this change does not promise that every provider supports mirror fallback.

Unit coverage for retry success, non-retryable errors, and cancellation was
added in commit `7efdba33ba8f326e1a1d0a0b240761bf491b1247`. Tests have not yet
been executed.
