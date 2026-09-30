(() => {
  const main = document.getElementById("main");
  const state = { catalog: null, user: null, favorites: new Set(), codes: new Map(), query: "", scope: "all", sort: "random", seed: Math.random(), visible: 60, detail: null, author: null, authorScripts: null, params: [], indexing: false };
  const languages = ["system", "en", "ru", "kk", "uk", "be", "pl", "sr", "hu", "zh-Hans", "ja", "pt-BR", "es", "it", "de", "nl"];
  const bundled = location.protocol === "file:";
  const apiOrigin = bundled ? "https://script-library-nulls.tmtsttamt022.chatgpt.site" : "";
  let localSession = bundled ? sessionStorage.getItem("script-library-session") || "" : "";
  const languageNames = ["Системный", "English", "Русский", "Қазақша", "Українська", "Беларуская", "Polski", "Српски", "Magyar", "中文", "日本語", "Português (Brasil)", "Español", "Italiano", "Deutsch", "Nederlands"];
  const locale = () => { const saved = localStorage.getItem("language") || "system"; const code = saved === "system" ? navigator.language : saved; return code.startsWith("zh") ? "zh-Hans" : code.startsWith("pt-BR") ? "pt-BR" : code.split("-")[0]; };
  const tr = value => (window.APP_TRANSLATIONS[locale()] || window.APP_TRANSLATIONS.ru || {})[value] || value;
  const e = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  const js = async (path, options = {}) => {
    const headers = new Headers(options.headers || {});
    if (bundled && localSession) headers.set("x-script-library-session", localSession);
    const response = await fetch(apiOrigin + path, { credentials: bundled ? "omit" : "same-origin", ...options, headers });
    let body;
    try { body = await response.json(); } catch { body = {}; }
    if (!response.ok) throw new Error(body.error || tr("Не удалось выполнить"));
    return body;
  };
  const date = raw => raw ? new Intl.DateTimeFormat(locale(), { dateStyle: "medium", timeStyle: "short" }).format(new Date(raw)) : tr("Без даты");
  const randomRank = id => { let hash = 2166136261; for (const c of id + state.seed) hash = Math.imul(hash ^ c.charCodeAt(0), 16777619); return hash >>> 0; };
  const label = (text, value) => `<div><span>${e(tr(text))}</span>${e(value)}</div>`;
  const button = (text, action, cls = "") => `<button type="button" class="button ${cls}" data-action="${action}">${e(tr(text))}</button>`;
  const link = (path, text, cls = "") => `<a href="${e(path)}" data-route="${e(path)}" class="${cls}">${e(text)}</a>`;
  function toast(message) { const node = document.getElementById("toast"); node.textContent = message; node.classList.add("show"); clearTimeout(toast.timer); toast.timer = setTimeout(() => node.classList.remove("show"), 3200); }
  function heading(title, description = "", count = "") { return `<div class="page-head"><div><p class="eyebrow">Script Library</p><h1>${e(title)}</h1>${description ? `<p class="sub">${e(description)}</p>` : ""}</div>${count ? `<span class="count">${e(count)}</span>` : ""}</div>`; }
  function activeNav(route) {
    const section = route.startsWith("/authors") ? "authors" : route.startsWith("/favorites") ? "favorites" : route.startsWith("/account") ? "account" : "library";
    for (const node of document.querySelectorAll("[data-nav]")) node.classList.toggle("active", node.dataset.nav === section);
    for (const node of document.querySelectorAll("[data-t]")) node.textContent = tr(node.dataset.t);
    document.getElementById("refresh").title = document.getElementById("refresh").ariaLabel = tr("Обновить список авторов");
    document.getElementById("help").title = document.getElementById("help").ariaLabel = tr("Помощь");
  }
  function go(path) { if (location.pathname !== path) history.pushState({}, "", path); renderRoute(); window.scrollTo(0, 0); }
  function card(script) {
    const author = script.author_username ? "@" + script.author_username : script.author_name || "";
    return `<a class="script-card" href="/scripts/${e(script.uuid)}" data-route="/scripts/${e(script.uuid)}"><h2 class="script-title">${e(script.name || tr("Без названия"))}</h2><p class="script-author">${e(author)}</p><p class="script-desc">${e(script.description || tr("Описание не добавлено"))}</p><div class="script-bottom"><span>${e(date(script.published_at || script.created_at))}</span><span>${e(script.uuid.slice(0, 8))}</span></div></a>`;
  }
  function filtered() {
    const query = state.query.toLocaleLowerCase().trim();
    const catalog = state.catalog || { scripts: [] };
    let scripts = catalog.scripts.filter(script => {
      if (!query) return true;
      const fields = {
        title: script.name, author: [script.author_name, script.author_username].join(" "),
        description: script.description, code: state.codes.get(script.uuid), all: [script.name, script.author_name, script.author_username, script.description, script.uuid, state.codes.get(script.uuid)].join(" ")
      };
      return String(fields[state.scope] || "").toLocaleLowerCase().includes(query);
    });
    scripts = [...scripts];
    if (state.sort === "random") scripts.sort((a, b) => randomRank(a.uuid) - randomRank(b.uuid));
    if (state.sort === "new") scripts.sort((a, b) => (b.published_at || b.created_at || "").localeCompare(a.published_at || a.created_at || ""));
    if (state.sort === "old") scripts.sort((a, b) => (a.published_at || a.created_at || "").localeCompare(b.published_at || b.created_at || ""));
    if (state.sort === "az") scripts.sort((a, b) => a.name.localeCompare(b.name, navigator.language));
    if (state.sort === "za") scripts.sort((a, b) => b.name.localeCompare(a.name, navigator.language));
    if (state.sort === "updated") scripts.sort((a, b) => (b.updated_at || "").localeCompare(a.updated_at || ""));
    return scripts;
  }
  function library() {
    const items = filtered();
    const catalog = state.catalog;
    main.innerHTML = heading(tr("Библиотека"), tr("Скрипты из списка авторов"), catalog ? `${catalog.scripts.length} ${tr("скриптов")}` : "") +
      `<div class="toolbar"><label class="field"><span>${e(tr("Поиск"))}</span><input id="search" class="input" type="search" autocomplete="off" placeholder="${e(tr("Название, автор, описание, код или UUID"))}" value="${e(state.query)}"></label><label class="field"><span>${e(tr("ИСКАТЬ В"))}</span><select id="scope" class="select">${[["all","Везде"],["title","Название"],["author","Автор"],["description","Описание"],["code","Код"]].map(([v,t]) => `<option value="${v}" ${state.scope === v ? "selected" : ""}>${e(tr(t))}</option>`).join("")}</select></label><label class="field"><span>${e(tr("СОРТИРОВКА"))}</span><select id="sort" class="select">${[["random","Случайно"],["new","Сначала новые"],["old","Сначала старые"],["az","Название А–Я"],["za","Название Я–А"],["updated","Обновлённые"]].map(([v,t]) => `<option value="${v}" ${state.sort === v ? "selected" : ""}>${e(tr(t))}</option>`).join("")}</select></label></div>` +
      `<div class="toolbar-note"><span>${e(tr("Везде — название, автор, описание, UUID и загруженный код. Случайно — случайный порядок."))}</span><div class="actions">${button(state.indexing ? "Индексирование…" : "Индексировать код", "index", "small")}${state.sort === "random" ? button("Перемешать", "shuffle", "small") : ""}</div></div>` +
      (items.length ? `<div class="grid">${items.slice(0, state.visible).map(card).join("")}</div>${items.length > state.visible ? `<div style="text-align:center;margin:24px 0">${button("Показать ещё", "more")}</div>` : ""}` : `<div class="empty">${e(catalog ? tr("Ничего не найдено.") : tr("Загружаю авторов…"))}</div>`);
  }
  function authors() {
    const list = [...(state.catalog?.authors || [])].sort((a, b) => randomRank(a.uuid) - randomRank(b.uuid));
    main.innerHTML = heading(tr("Авторы скриптов"), "", list.length ? `${list.length} ${tr("авторов")}` : "") +
      (list.length ? `<div class="author-grid">${list.map(author => `<a class="author-card" href="/authors/${e(author.username)}" data-route="/authors/${e(author.username)}"><div class="author-name">${e(author.name)}</div><div class="author-handle">@${e(author.username)}</div></a>`).join("")}</div>` : `<div class="empty">${e(tr("Загружаю авторов…"))}</div>`);
  }
  function authorPage() {
    const author = state.author;
    if (!author) { main.innerHTML = `<div class="loading">${e(tr("Открываю автора…"))}</div>`; return; }
    const scripts = state.authorScripts || state.catalog?.scripts.filter(item => item.author_uuid === author.uuid) || [];
    main.innerHTML = `<button class="back" data-action="back">← ${e(tr("Авторы"))}</button>` +
      `<div class="panel"><p class="eyebrow">${e(tr("Профиль автора"))}</p><h1 class="detail-title">${e(author.name)}</h1><p class="author-handle">@${e(author.username)}</p><div class="actions">${button("Поделиться автором", "share-author")}</div><p class="detail-id">UUID ${e(author.uuid)}</p></div>` +
      `<div class="page-head" style="margin-top:28px"><h2>${e(tr("Скрипты"))}</h2><span class="count">${scripts.length}</span></div><div class="grid">${scripts.map(card).join("")}</div>`;
  }
  function detail() {
    const script = state.detail;
    if (!script) { main.innerHTML = `<div class="loading">${e(tr("Открываю скрипт…"))}</div>`; return; }
    const authorHandle = script.author_username || state.catalog?.authors.find(author => author.uuid === script.author_uuid)?.username;
    main.innerHTML = `<button class="back" data-action="back">← ${e(tr("Библиотека"))}</button>` +
      `<div class="panel"><p class="eyebrow">${e(tr("Скрипт"))}</p><h1 class="detail-title">${e(script.name)}</h1>${authorHandle ? link("/authors/" + encodeURIComponent(authorHandle), "@" + authorHandle, "author-handle") : `<span class="author-handle">${e(script.author_name)}</span>`}<p class="detail-desc">${e(script.description || tr("Описание не добавлено"))}</p><div class="meta">${label("Создан:", date(script.created_at))}${label("Обновлён:", date(script.updated_at))}${label("Опубликован:", date(script.published_at))}</div><div class="actions">${button("Настроить и запустить бой", "battle", "primary")}${button("Посмотреть код", "code")}${button("Поделиться скриптом", "share-script")}${button(state.favorites.has(script.uuid) ? "Удалить из избранного" : "В избранное", "favorite")}</div><p class="detail-id">UUID ${e(script.uuid)}</p></div>`;
  }
  function account() {
    if (state.user) {
      main.innerHTML = heading(tr("Аккаунт")) + `<div class="panel login"><h2>${e(state.user.name || state.user.username)}</h2><p class="author-handle">@${e(state.user.username)}</p><div class="actions">${button("Избранное", "favorites", "primary")}${button("Выйти", "logout")}</div></div>`;
    } else {
      main.innerHTML = heading(tr("Вход в Null’s")) + `<form id="login" class="panel login"><label class="field"><span>${e(tr("Имя пользователя"))}</span><input class="input" name="username" autocomplete="username" required></label><label class="field"><span>${e(tr("Пароль"))}</span><input class="input" type="password" name="password" autocomplete="current-password" required></label><button class="button primary" type="submit">${e(tr("Войти"))}</button><p class="sub">${e(tr("Вход нужен для избранного и просмотра кода."))}</p></form>`;
    }
  }
  function favorites() {
    if (!state.user) { main.innerHTML = heading(tr("Избранное")) + `<div class="notice">${e(tr("Войдите, чтобы увидеть избранное."))} ${link("/account", tr("Войти"))}</div>`; return; }
    const items = state.catalog?.scripts.filter(item => state.favorites.has(item.uuid)) || [];
    main.innerHTML = heading(tr("Избранное"), "", String(items.length)) + (items.length ? `<div class="grid">${items.slice(0, state.visible).map(card).join("")}</div>${items.length > state.visible ? `<div style="text-align:center;margin:24px 0">${button("Показать ещё", "more")}</div>` : ""}` : `<div class="empty">${e(tr("В избранном пока пусто"))}</div>`);
  }
  function help() {
    const question = (title, answer) => `<div class="panel"><p class="eyebrow">${e(tr(title))}</p><p>${e(tr(answer))}</p></div>`;
    main.innerHTML = heading(tr("Помощь")) +
      question("НУЖЕН ЛИ АККАУНТ?", "Нет. Смотреть скрипты и авторов можно сразу. Для избранного, просмотра кода и своих скриптов войдите в аккаунт.") +
      question("КАК НАЧАТЬ БОЙ?", "Откройте скрипт, нажмите «Настроить и запустить бой», выберите параметры и запустите. Если у автора не подключён Null’s Connect, сервис не выдаст ссылку на бой.") +
      question("КАК ИСКАТЬ ПО КОДУ?", "В библиотеке нажмите «Индексировать код», дождитесь загрузки и выберите «Код» в поле «Искать в». Недоступный аккаунту код в поиск не попадёт.") +
      `<div class="panel"><label class="field"><span>${e(tr("Язык"))}</span><select id="language" class="select">${languages.map((code, i) => `<option value="${code}" ${(localStorage.getItem("language") || "system") === code ? "selected" : ""}>${e(languageNames[i])}</option>`).join("")}</select></label></div>`;
  }
  async function renderRoute() {
    const path = decodeURIComponent(location.pathname);
    document.documentElement.lang = locale();
    activeNav(path);
    if (path === "/authors") { authors(); return; }
    if (path === "/favorites") { favorites(); return; }
    if (path === "/account") { account(); return; }
    if (path === "/help") { help(); return; }
    if (path.startsWith("/authors/")) {
      const handle = path.slice(9).replace(/^@/, "");
      state.author = state.catalog?.authors.find(author => author.username.toLowerCase() === handle.toLowerCase()) || null;
      state.authorScripts = null;
      authorPage();
      try {
        if (!state.author) state.author = await js("/api/users/@" + encodeURIComponent(handle));
        if (location.pathname !== path) return;
        authorPage();
        if (!state.catalog?.authors.some(author => author.uuid === state.author.uuid)) {
          const data = await js("/api/users/" + state.author.uuid + "/scripts");
          if (location.pathname !== path) return;
          state.authorScripts = (data.scripts || []).filter(item => item.published_at).map(item => ({ ...item, author_uuid: state.author.uuid, author_name: state.author.name, author_username: state.author.username }));
          authorPage();
        }
      } catch (error) { if (!state.author) main.innerHTML = `<div class="empty">${e(error.message)}</div>`; else toast(error.message); }
      return;
    }
    if (/^\/scripts\/[0-9a-fA-F-]{36}$/.test(path)) {
      const id = path.slice(9);
      state.detail = state.catalog?.scripts.find(item => item.uuid === id) || null;
      detail();
      try {
        const fresh = await js("/api/scripts/" + id);
        if (location.pathname !== path) return;
        state.detail = { ...state.detail, ...fresh, author_username: state.detail?.author_username };
        detail();
      } catch (error) { if (!state.detail) main.innerHTML = `<div class="empty">${e(error.message)}</div>`; }
      return;
    }
    library();
  }
  async function loadCatalog(refresh = false) {
    try {
      const data = await js("/data/catalog" + (refresh ? "?refresh=1" : ""));
      state.catalog = data;
      const node = document.getElementById("catalog-age");
      node.textContent = `${data.authors.length} ${tr("авторов")} · ${data.scripts.length} ${tr("скриптов")}`;
      renderRoute();
      if (refresh) toast(tr("Список обновлён"));
    } catch (error) { if (!state.catalog) main.innerHTML = `<div class="empty">${e(error.message)}<br><br>${button("Обновить", "refresh")}</div>`; else toast(error.message); }
  }
  async function restore() {
    try { state.user = await js("/api/users/me"); await loadFavorites(); renderRoute(); } catch { state.user = null; if (bundled) { localSession = ""; sessionStorage.removeItem("script-library-session"); } }
  }
  async function loadFavorites() {
    if (!state.user) return;
    try { const data = await js("/api/users/" + state.user.uuid + "/favorites"); state.favorites = new Set((data.scripts || []).map(item => item.uuid)); } catch { state.favorites.clear(); }
  }
  async function share(path) {
    const url = new URL(path, location.origin).href;
    try { if (navigator.share) await navigator.share({ url }); else { await navigator.clipboard.writeText(url); toast(tr("Ссылка скопирована")); } }
    catch (error) { if (error.name !== "AbortError") toast(tr("Не удалось поделиться")); }
  }
  function highlight(source) {
    return e(source).replace(/(--[^\n]*|&quot;(?:[^&]|&(?!quot;))*?&quot;|&#39;(?:[^&]|&(?!#39;))*?&#39;|\b(?:local|function|end|if|then|else|elseif|for|while|do|return|true|false|nil|and|or|not)\b|\b\d+(?:\.\d+)?\b)/g, token => {
      const kind = token.startsWith("--") ? "comment" : token.startsWith("&") ? "string" : /^\d/.test(token) ? "number" : "key";
      return `<span class="tok-${kind}">${token}</span>`;
    });
  }
  async function showCode() {
    if (!state.detail) return;
    const id = state.detail.uuid;
    main.innerHTML = `<div class="loading">${e(tr("Загружаю код…"))}</div>`;
    try {
      const data = await js("/api/scripts/" + id + "/content");
      state.codes.set(id, data.content || "");
      main.innerHTML = `<button class="back" data-action="detail">← ${e(tr("Скрипт"))}</button>` + heading(state.detail.name) + `<pre class="code"><code>${highlight(data.content || "")}</code></pre>`;
    } catch (error) { toast(error.message); detail(); }
  }
  async function indexCodes() {
    if (state.indexing) return;
    if (!state.user) { go("/account"); toast(tr("Сначала войдите в Null’s")); return; }
    const list = state.catalog?.scripts || [];
    state.indexing = true; library();
    let cursor = 0, done = 0, failed = 0;
    async function one() { while (cursor < list.length && state.indexing) { const script = list[cursor++]; if (!state.codes.has(script.uuid)) try { const data = await js("/api/scripts/" + script.uuid + "/content"); state.codes.set(script.uuid, data.content || ""); } catch { failed++; } done++; if (done % 10 === 0) toast(`${tr("Код: ")}${done} / ${list.length}`); } }
    await Promise.all(Array.from({ length: Math.min(6, list.length) }, one));
    state.indexing = false; if (location.pathname === "/") library();
    toast(`${tr("Код: ")}${state.codes.size} / ${list.length}${failed ? " · " + failed + " " + tr("недоступно") : ""}`);
  }
  const base62 = bytes => {
    const abc = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    let zeros = 0; while (zeros < bytes.length && bytes[zeros] === 0) zeros++;
    let number = BigInt("0x" + [...bytes].map(byte => byte.toString(16).padStart(2, "0")).join(""));
    let result = ""; while (number > 0n) { result = abc[Number(number % 62n)] + result; number /= 62n; }
    return "0".repeat(zeros) + result;
  };
  function roomLink(id, token, values) {
    const config = window.BATTLE_CONFIG;
    const bp = values.map((value, i) => value - (i <= 5 || i === 9 || i === 11 || i === 15 ? config.defaults[i] : 0));
    const payload = { realm: "experiment:scripts", script: `https://scripting.nulls.gg/api/scripts/${id}/content?token=${encodeURIComponent(token)}`, bp };
    return `nullsbrawl://createAndJoinRoom?roomname=params:v2:${base62(new TextEncoder().encode(JSON.stringify(payload)))}&friendly=1&side=0`;
  }
  async function battle() {
    if (!state.detail) return;
    main.innerHTML = `<div class="loading">${e(tr("Загружаю параметры…"))}</div>`;
    try {
      const data = await js("/api/scripts/" + state.detail.uuid + "/parameters");
      state.params = data.parameters || [];
      const known = new Map(state.params.map(param => [param.id, param]));
      const config = window.BATTLE_CONFIG;
      const extra = state.params.filter(param => !config.ids.includes(param.id));
      main.innerHTML = `<button class="back" data-action="detail">← ${e(tr("Скрипт"))}</button>` + heading(tr("Настройка боя"), state.detail.name) +
        (extra.length ? `<div class="notice">${e(tr("Есть дополнительные параметры, которые эта версия не передаёт."))}</div>` : "") +
        `<form id="battle-form" class="panel"><div class="battle-grid">${config.ids.map((id, i) => {
          const param = known.get(id); const value = param?.default_value ?? config.defaults[i];
          const isBool = param?.type === "bool" || [13,16,17,18,21,23,25,26,27,28,29,30].includes(i);
          const name = tr(config.names[i]);
          return `<div class="battle-option"><label for="p${i}">${e(name)}</label>${isBool ? `<input id="p${i}" name="p${i}" type="checkbox" ${value === 1 ? "checked" : ""}>` : `<input id="p${i}" name="p${i}" type="number" min="${param?.min_value ?? config.min[i]}" max="${param?.max_value ?? config.max[i]}" value="${value}" required>`}</div>`;
        }).join("")}</div><div class="actions"><button type="submit" class="button primary">${e(tr("Запустить в Null’s Brawl"))}</button></div></form>`;
    } catch (error) { toast(error.message); detail(); }
  }
  async function launch(form) {
    const config = window.BATTLE_CONFIG, values = [];
    for (let i = 0; i < 32; i++) {
      const field = form.elements["p" + i], isBool = field.type === "checkbox";
      const value = isBool ? (field.checked ? 1 : i === 23 ? -1 : 0) : Number(field.value);
      const param = state.params.find(item => item.id === config.ids[i]);
      if (!Number.isInteger(value) || value < (param?.min_value ?? config.min[i]) || value > (param?.max_value ?? config.max[i])) { field.focus(); toast(tr("Проверьте параметры боя")); return; }
      values.push(value);
    }
    try {
      const data = await js("/api/scripts/" + state.detail.uuid + "/share", { method: "POST" });
      const link = roomLink(state.detail.uuid, data.token, values);
      main.innerHTML += `<div class="panel" style="margin-top:14px"><p>${e(tr("Ссылка на бой готова."))}</p><div class="actions"><a class="button primary" href="${e(link)}">${e(tr("Открыть игру"))}</a>${button("Скопировать ссылку", "copy-room")}</div></div>`;
      state.roomLink = link;
      location.href = link;
    } catch (error) { toast(error.message); }
  }
  document.addEventListener("click", async event => {
    const route = event.target.closest("[data-route]");
    if (route) { event.preventDefault(); go(route.dataset.route); return; }
    const control = event.target.closest("[data-action]");
    if (!control) return;
    const action = control.dataset.action;
    try {
      if (action === "back") { history.length > 1 ? history.back() : go("/"); return; }
      if (action === "detail") { detail(); return; }
      if (action === "shuffle") { state.seed = Math.random(); state.visible = 60; library(); return; }
      if (action === "more") { state.visible += 60; library(); return; }
      if (action === "refresh") { await loadCatalog(true); return; }
      if (action === "index") { await indexCodes(); return; }
      if (action === "battle") { await battle(); return; }
      if (action === "code") { if (!state.user) { go("/account"); toast(tr("Сначала войдите в Null’s")); } else await showCode(); return; }
      if (action === "share-script") { await share("/scripts/" + state.detail.uuid); return; }
      if (action === "share-author") { await share("/authors/" + state.author.username); return; }
      if (action === "copy-room") { await navigator.clipboard.writeText(state.roomLink); toast(tr("Ссылка скопирована")); return; }
      if (action === "favorites") { go("/favorites"); return; }
      if (action === "favorite") {
        if (!state.user) { go("/account"); toast(tr("Сначала войдите в Null’s")); return; }
        const id = state.detail.uuid, remove = state.favorites.has(id);
        await js("/api/users/" + state.user.uuid + "/favorites", { method: remove ? "DELETE" : "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ script_id: id }) });
        if (remove) state.favorites.delete(id); else state.favorites.add(id);
        detail(); toast(tr(remove ? "Удалено из избранного" : "Добавлено в избранное")); return;
      }
      if (action === "logout") { await js("/api/auth/logout", { method: "POST" }); localSession = ""; if (bundled) sessionStorage.removeItem("script-library-session"); state.user = null; state.favorites.clear(); state.codes.clear(); go("/"); toast(tr("Вы вышли")); }
    } catch (error) { toast(error.message); }
  });
  document.addEventListener("submit", async event => {
    if (event.target.id === "login") {
      event.preventDefault();
      const form = event.target, button = form.querySelector("button");
      button.disabled = true;
      try {
        const result = await js("/api/auth/login", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ username: form.elements.namedItem("username").value.trim(), password: form.elements.namedItem("password").value }) });
        if (bundled) { if (!result.session) throw new Error(tr("Сервис не вернул сессию.")); localSession = result.session; sessionStorage.setItem("script-library-session", localSession); }
        state.user = result.user; await loadFavorites(); go("/"); toast(tr("Вход выполнен"));
      } catch (error) { toast(error.message); button.disabled = false; }
    }
    if (event.target.id === "battle-form") { event.preventDefault(); await launch(event.target); }
  });
  document.addEventListener("input", event => { if (event.target.id === "search") { state.query = event.target.value; state.visible = 60; const selection = document.activeElement; library(); const fresh = document.getElementById("search"); fresh.focus(); fresh.setSelectionRange(selection.selectionStart || state.query.length, selection.selectionEnd || state.query.length); } });
  document.addEventListener("change", event => {
    if (event.target.id === "scope") { state.scope = event.target.value; state.visible = 60; library(); }
    if (event.target.id === "sort") { state.sort = event.target.value; state.visible = 60; library(); }
    if (event.target.id === "language") { localStorage.setItem("language", event.target.value); renderRoute(); }
  });
  document.getElementById("refresh").addEventListener("click", () => loadCatalog(true));
  document.getElementById("help").addEventListener("click", () => go("/help"));
  addEventListener("popstate", renderRoute);
  renderRoute();
  loadCatalog();
  restore();
})();
