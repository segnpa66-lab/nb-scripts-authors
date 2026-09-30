import test from "node:test";
import assert from "node:assert/strict";
import worker from "../worker/index.js";

const origin = "https://script-library-nulls.example";
const data = value => new Response(JSON.stringify(value), { status: 200, headers: { "content-type": "application/json" } });

test("catalog reads the mutable main list and includes newly added authors", async () => {
  const original = globalThis.fetch;
  const calls = [];
  globalThis.fetch = async input => {
    const url = String(input);
    calls.push(url);
    if (url.startsWith("https://raw.githubusercontent.com/")) return new Response("1. https://scripting.nulls.gg/@first\n2. https://scripting.nulls.gg/@second\n3. https://scripting.nulls.gg/@first");
    if (url.endsWith("/users/@first")) return data({ uuid: "00000000-0000-0000-0000-000000000001", name: "First", username: "first" });
    if (url.endsWith("/users/@second")) return data({ uuid: "00000000-0000-0000-0000-000000000002", name: "Second", username: "second" });
    if (url.endsWith("/users/00000000-0000-0000-0000-000000000001/scripts")) return data({ scripts: [{ uuid: "10000000-0000-0000-0000-000000000001", name: "One", published_at: "2026-09-30T00:00:00Z" }] });
    if (url.endsWith("/users/00000000-0000-0000-0000-000000000002/scripts")) return data({ scripts: [{ uuid: "10000000-0000-0000-0000-000000000002", name: "Two", published_at: "2026-09-30T00:00:00Z" }] });
    throw Error("Unexpected request: " + url);
  };
  try {
    const response = await worker.fetch(new Request(origin + "/data/catalog?refresh=1"));
    const catalog = await response.json();
    assert.equal(response.status, 200);
    assert.equal(catalog.authors.length, 2);
    assert.equal(catalog.scripts.length, 2);
    assert(calls[0].includes("/main/list.txt"));
    assert(!calls[0].includes("40a087499e066c5dfb95b8e90b315590427fb282"));
  } finally { globalThis.fetch = original; }
});

test("login stores upstream session in a first-party HttpOnly cookie", async () => {
  const original = globalThis.fetch;
  globalThis.fetch = async (input, options) => {
    const url = String(input);
    if (url.endsWith("/auth/login")) return new Response("{}", { status: 200, headers: { "content-type": "application/json", "set-cookie": "session=abc; HttpOnly; Secure; Path=/" } });
    if (url.endsWith("/users/me")) {
      assert.equal(options.headers.cookie, "session=abc");
      return data({ uuid: "00000000-0000-0000-0000-000000000001", username: "first" });
    }
    throw Error("Unexpected request");
  };
  try {
    const login = await worker.fetch(new Request(origin + "/api/auth/login", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ username: "first", password: "secret" }) }));
    assert.equal(login.status, 200);
    assert.equal((await login.json()).user.username, "first");
    const cookie = login.headers.get("set-cookie");
    assert(cookie.includes("HttpOnly"));
    assert(cookie.includes("Secure"));
    const me = await worker.fetch(new Request(origin + "/api/users/me", { headers: { cookie: cookie.split(";")[0] } }));
    assert.equal((await me.json()).username, "first");
  } finally { globalThis.fetch = original; }
});

test("API proxy rejects script editing and page routes load inside the site", async () => {
  const blocked = await worker.fetch(new Request(origin + "/api/scripts/00000000-0000-0000-0000-000000000001", { method: "DELETE" }));
  assert.equal(blocked.status, 404);
  for (const route of ["/", "/authors", "/favorites", "/account", "/help", "/scripts/00000000-0000-0000-0000-000000000001", "/authors/first"]) {
    const response = await worker.fetch(new Request(origin + route));
    assert.equal(response.status, 200, route);
    assert(response.headers.get("content-type").includes("text/html"));
  }
});
