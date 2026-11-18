# GBO demo changes

Adds the explicit CA-based GBO demo trust profile, supported list roles, the Swift entry point and the optional hourly cache refresh. The upstream cache staging/fallback implementation is retained.

Upstream: https://github.com/eu-digital-identity-wallet/eudi-lib-kmp-etsi-1196x2

Pinned upstream base: `aa2d978205fe7c1c1c96c2fd4e7d5e5dc51ad860`.

`main` follows upstream; `gbo/main` contains these changes. The source was
previously maintained as a patch in `jeroendk/gbo-wallet-patches`. Existing
licenses and copyright notices are retained. This is a demo profile; it does
not assert EU notification or wallet certification.
