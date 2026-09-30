# Script Library — web

The site reads the current `main/list.txt` from the author registry, resolves public profiles and scripts through a same-origin Worker, and caches the resulting catalogue for five minutes. It does not use a commit-pinned list URL.

The Worker also forwards a small allowlist of Null’s scripting API requests. Login credentials are sent to the service and are not saved in the site source or server storage. The upstream session is kept in a first-party, HttpOnly, Secure cookie. Script editing and file downloads are intentionally absent.

`npm test && npm run build` validates and packages the Cloudflare Worker at `dist/server/index.js`. It also writes `dist/files/index.html` for the game’s in-app news WebView and GitHub Pages. The static page requests catalogue and API data through the same service; the API hostname is not shown in the game. The unsigned mod source is in `mod/`; the game’s ordinary client requires the community signing process before installation.
