const PAGE = "__PAGE__";
const API = "https://scripting.nulls.gg/api";
const AUTHORS = "https://raw.githubusercontent.com/segnpa66-lab/nb-scripts-authors/main/list.txt";
const COOKIE = "sl_session";
const UUID = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
const HANDLE = "[A-Za-z0-9_.-]{1,64}";
let catalogSnapshot = null;
let catalogExpires = 0;
const json = (value, status = 200, extra = {}) => new Response(JSON.stringify(value), {
  status, headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store", ...extra }
});

function session(request) {
  const cookie = request.headers.get("cookie") || "";
  const match = cookie.match(new RegExp("(?:^|;\\s*)" + COOKIE + "=([^;]+)"));
  if (!match) return { cookies: {}, bearer: "" };
  try {
    const raw = atob(match[1].replace(/-/g, "+").replace(/_/g, "/"));
    const value = JSON.parse(raw);
    if (!value || typeof value !== "object") return { cookies: {}, bearer: "" };
    return { cookies: value.cookies || {}, bearer: value.bearer || "" };
  } catch { return { cookies: {}, bearer: "" }; }
}
function sessionHeader(value) {
  const raw = btoa(JSON.stringify(value)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  return `${COOKIE}=${raw}; Path=/; Max-Age=604800; Secure; HttpOnly; SameSite=Lax`;
}
function clearSession() { return `${COOKIE}=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=Lax`; }
function updatedSession(old, response) {
  const next = { cookies: { ...old.cookies }, bearer: old.bearer };
  for (const line of response.headers.getSetCookie?.() || (response.headers.get("set-cookie") ? [response.headers.get("set-cookie")] : [])) {
    const pair = line.split(";", 1)[0];
    const equal = pair.indexOf("=");
    if (equal < 1) continue;
    const name = pair.slice(0, equal), value = pair.slice(equal + 1);
    if (!/^[A-Za-z0-9_!#$%&'*+.^`|~-]+$/.test(name)) continue;
    if (value) next.cookies[name] = value; else delete next.cookies[name];
  }
  return next;
}
function requestHeaders(sessionValue) {
  const headers = { accept: "application/json" };
  const pairs = Object.entries(sessionValue.cookies).filter(([key, value]) => /^[A-Za-z0-9_!#$%&'*+.^`|~-]+$/.test(key) && typeof value === "string");
  if (pairs.length) headers.cookie = pairs.map(([key, value]) => `${key}=${value}`).join("; ");
  if (sessionValue.bearer) headers.authorization = `Bearer ${sessionValue.bearer}`;
  return headers;
}
async function upstream(path, method = "GET", value = null, auth = { cookies: {}, bearer: "" }) {
  const headers = requestHeaders(auth);
  if (value !== null) headers["content-type"] = "application/json";
  return fetch(API + path, { method, headers, body: value === null ? undefined : JSON.stringify(value), redirect: "manual", signal: AbortSignal.timeout(20000) });
}
async function responseJson(response) {
  const body = await response.text();
  try { return body ? JSON.parse(body) : {}; } catch { return { error: "Некорректный ответ сервиса" }; }
}
function safeError(status, body) {
  const message = status === 401 ? "Сессия завершена. Войдите снова."
    : status === 403 ? "Недостаточно прав."
    : status === 404 ? "Не найдено."
    : status === 429 ? "Слишком много запросов. Повторите позже."
    : status === 400 && String(body.error || "").includes("author does not have connect") ? "У автора нет Null’s Connect."
    : "Сервис временно недоступен.";
  return json({ error: message }, status >= 400 && status < 600 ? status : 502);
}
function handles(text) {
  return [...new Set([...text.matchAll(/@([A-Za-z0-9_.-]{1,64})/g)].map(match => match[1].toLowerCase()))];
}
async function catalog() {
  const source = await fetch(AUTHORS + "?refresh=" + Math.floor(Date.now() / 300000), { signal: AbortSignal.timeout(15000) });
  if (!source.ok) throw new Error("author list");
  const names = handles(await source.text());
  if (!names.length || names.length > 500) throw new Error("author list");
  const authors = [], scripts = [];
  let cursor = 0;
  async function one() {
    while (cursor < names.length) {
      const handle = names[cursor++];
      try {
        const userResponse = await upstream("/users/@" + encodeURIComponent(handle));
        if (!userResponse.ok) continue;
        const user = await responseJson(userResponse);
        if (!new RegExp("^" + UUID + "$").test(user.uuid || "")) continue;
        const scriptsResponse = await upstream("/users/" + user.uuid + "/scripts");
        if (!scriptsResponse.ok) continue;
        const data = await responseJson(scriptsResponse);
        authors.push({ uuid: user.uuid, name: user.name || handle, username: user.username || handle });
        for (const item of data.scripts || []) if (item.published_at && new RegExp("^" + UUID + "$").test(item.uuid || "")) {
          scripts.push({ uuid: item.uuid, name: item.name || "", description: item.description || "", created_at: item.created_at || "", updated_at: item.updated_at || "", published_at: item.published_at || "", author_uuid: user.uuid, author_name: item.author_name || user.name || handle, author_username: user.username || handle });
        }
      } catch { /* Continue with the other authors. */ }
    }
  }
  await Promise.all(Array.from({ length: Math.min(4, names.length) }, one));
  if (!authors.length) throw new Error("catalog");
  return { authors, scripts, listed: names.length, refreshed_at: new Date().toISOString() };
}
async function catalogResponse(request) {
  if (catalogSnapshot && Date.now() < catalogExpires && !new URL(request.url).searchParams.has("refresh"))
    return json(catalogSnapshot, 200, { "cache-control": "public, max-age=300" });
  try {
    catalogSnapshot = await catalog();
    catalogExpires = Date.now() + 300000;
    return json(catalogSnapshot, 200, { "cache-control": "public, max-age=300" });
  } catch (error) {
    console.error("catalog load failed", error);
    return json({ error: "Не удалось загрузить список авторов." }, 503);
  }
}
function allowed(method, path) {
  const script = new RegExp("^/scripts/" + UUID + "$");
  const scriptPart = new RegExp("^/scripts/" + UUID + "/(parameters|content|share)$");
  const user = new RegExp("^/users/(?:@" + HANDLE + "|" + UUID + ")$");
  const userScripts = new RegExp("^/users/" + UUID + "/scripts$");
  const favorites = new RegExp("^/users/" + UUID + "/favorites$");
  if (method === "GET") return script.test(path) || scriptPart.test(path) && !path.endsWith("/share") || user.test(path) || userScripts.test(path) || favorites.test(path) || path === "/users/me";
  if (method === "POST") return scriptPart.test(path) && path.endsWith("/share") || favorites.test(path);
  if (method === "DELETE") return favorites.test(path);
  return false;
}
async function apiResponse(request, path) {
  if (path === "/auth/login" && request.method === "POST") {
    let credentials;
    try { credentials = await request.json(); } catch { return json({ error: "Введите имя и пароль." }, 400); }
    if (typeof credentials.username !== "string" || typeof credentials.password !== "string" || !credentials.username || !credentials.password || credentials.username.length > 128 || credentials.password.length > 512) return json({ error: "Введите имя и пароль." }, 400);
    const login = await upstream(path, "POST", { username: credentials.username, password: credentials.password });
    const body = await responseJson(login);
    if (!login.ok) return safeError(login.status, body);
    const auth = updatedSession({ cookies: {}, bearer: "" }, login);
    auth.bearer = body.token || body.access_token || "";
    if (!Object.keys(auth.cookies).length && !auth.bearer) return json({ error: "Сервис не вернул сессию." }, 502);
    const meResponse = await upstream("/users/me", "GET", null, auth);
    if (!meResponse.ok) return safeError(meResponse.status, await responseJson(meResponse));
    return json({ user: await responseJson(meResponse) }, 200, { "set-cookie": sessionHeader(updatedSession(auth, meResponse)) });
  }
  const auth = session(request);
  if (path === "/auth/logout" && request.method === "POST") {
    if (Object.keys(auth.cookies).length || auth.bearer) await upstream(path, "POST", null, auth).catch(() => null);
    return json({ ok: true }, 200, { "set-cookie": clearSession() });
  }
  if (!allowed(request.method, path)) return json({ error: "Недоступный запрос." }, 404);
  const needsAuth = path === "/users/me" || path.endsWith("/favorites") || path.endsWith("/content");
  if (needsAuth && !Object.keys(auth.cookies).length && !auth.bearer) return json({ error: "Требуется вход." }, 401);
  let body = null;
  if (request.method !== "GET") {
    const raw = await request.text();
    if (raw.length > 4096) return json({ error: "Слишком большой запрос." }, 413);
    if (raw) try { body = JSON.parse(raw); } catch { return json({ error: "Некорректный запрос." }, 400); }
  }
  const response = await upstream(path, request.method, body, auth);
  const value = await responseJson(response);
  if (!response.ok) return safeError(response.status, value);
  const refreshed = updatedSession(auth, response);
  const headers = (Object.keys(refreshed.cookies).length || refreshed.bearer) && JSON.stringify(refreshed) !== JSON.stringify(auth) ? { "set-cookie": sessionHeader(refreshed) } : {};
  return json(value, response.status, headers);
}
export default {
  async fetch(request) {
    const url = new URL(request.url);
    if (url.pathname === "/data/catalog" && request.method === "GET") return catalogResponse(request);
    if (url.pathname.startsWith("/api/")) {
      try { return await apiResponse(request, url.pathname.slice(4)); }
      catch { return json({ error: "Нет связи с сервисом." }, 502); }
    }
    if (request.method !== "GET" && request.method !== "HEAD") return new Response("Method not allowed", { status: 405 });
    if (["/", "/authors", "/favorites", "/account", "/help"].includes(url.pathname) || /^\/scripts\/[0-9a-fA-F-]{36}$/.test(url.pathname) || /^\/authors\/[A-Za-z0-9_.-]{1,64}$/.test(url.pathname)) {
      return new Response(request.method === "HEAD" ? null : PAGE, { status: 200, headers: { "content-type": "text/html; charset=utf-8", "cache-control": "public, max-age=300", "x-content-type-options": "nosniff", "referrer-policy": "strict-origin-when-cross-origin" } });
    }
    return new Response("Not found", { status: 404 });
  }
};
