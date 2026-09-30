import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { join } from "node:path";

const root = new URL("../", import.meta.url).pathname;
const read = path => readFileSync(join(root, path), "utf8");
const locales = {};
for (const code of ["ru", "en", "kk", "uk", "be", "pl", "sr", "hu", "zh-Hans", "ja", "pt-BR", "es", "it", "de", "nl"]) {
  locales[code] = JSON.parse(read("i18n/" + code + ".json"));
}
const battle = JSON.parse(read("web/battle-config.json"));
let page = read("web/index.html")
  .replace("__STYLE__", read("web/style.css").replace(/<\/style/gi, "<\\/style"))
  .replace("__SCRIPT__", read("web/app.js").replace(/<\/script/gi, "<\\/script"))
  .replace("__I18N__", JSON.stringify(locales))
  .replace("__BATTLE__", JSON.stringify(battle));
if (page.includes("__STYLE__") || page.includes("__SCRIPT__") || page.includes("__I18N__") || page.includes("__BATTLE__")) throw Error("Unresolved asset");
const worker = read("worker/index.js").replace('"__PAGE__"', JSON.stringify(page));
if (worker.includes('"__PAGE__"')) throw Error("Unresolved HTML");
mkdirSync(join(root, "dist/server"), { recursive: true });
mkdirSync(join(root, "dist/.openai"), { recursive: true });
writeFileSync(join(root, "dist/server/index.js"), worker);
writeFileSync(join(root, "dist/.openai/hosting.json"), read("web/../.openai/hosting.json"));
console.log("Built single-file Worker:", Buffer.byteLength(worker), "bytes");
