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
