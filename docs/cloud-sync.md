# Live company sync

Mobile uses the company API for live authorized project pages. Open
**Practice > Live company sync**, connect to the HTTPS API origin, and sign in.
Projects update automatically; online edits require current permissions and
expected versions.

Local backup/restore, offline project snapshots, saved drafts and the legacy
manual-sync entry point have been removed at the owner's request. Only the
encrypted login credential is retained. Company records clear on connection
loss or leaving the screen.

See [live company sync](live-company-sync.md) for behavior, implementation,
verification and remaining coverage. The old direct Neon transport remains
unreachable compatibility code; it is not the current Mobile flow.
