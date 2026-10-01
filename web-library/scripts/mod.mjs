import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { join } from "node:path";
import { deflateRawSync } from "node:zlib";

const root = new URL("../", import.meta.url).pathname;
const uuid = "bda43cca-1004-447d-90e6-508c41470255";
const entries = [
  ["content.json", readFileSync(join(root, `mod/${uuid}/content.json`))],
  ["files/index.html", readFileSync(join(root, "dist/files/index.html"))]
];
const manifest = JSON.parse(entries[0][1].toString("utf8"));
if (manifest["@gv"] !== 68 || manifest.locales?.["*"]?.SinglePageAppCommunityLaserboxUrl !== "https://segnpa66-lab.github.io/" || !manifest["@features"]?.local_page?.locales?.["*"]?.SinglePageAppCommunityLaserboxUrl?.endsWith(`/${uuid}/files/index.html`)) throw Error("Invalid mod");
const table = Array.from({ length: 256 }, (_, n) => { let value = n; for (let bit = 0; bit < 8; bit++) value = value & 1 ? 0xedb88320 ^ value >>> 1 : value >>> 1; return value >>> 0; });
const chunks = [], directory = [];
let offset = 0;
for (const [path, data] of entries) {
  const name = Buffer.from(path);
  const compressed = deflateRawSync(data);
  let crc = 0xffffffff;
  for (const byte of data) crc = table[(crc ^ byte) & 255] ^ crc >>> 8;
  crc = (crc ^ 0xffffffff) >>> 0;
  const local = Buffer.alloc(30);
  local.writeUInt32LE(0x04034b50, 0); local.writeUInt16LE(20, 4); local.writeUInt16LE(8, 8);
  local.writeUInt32LE(crc, 14); local.writeUInt32LE(compressed.length, 18); local.writeUInt32LE(data.length, 22); local.writeUInt16LE(name.length, 26);
  chunks.push(local, name, compressed);
  const central = Buffer.alloc(46);
  central.writeUInt32LE(0x02014b50, 0); central.writeUInt16LE(20, 4); central.writeUInt16LE(20, 6); central.writeUInt16LE(8, 10);
  central.writeUInt32LE(crc, 16); central.writeUInt32LE(compressed.length, 20); central.writeUInt32LE(data.length, 24); central.writeUInt16LE(name.length, 28); central.writeUInt32LE(offset, 42);
  directory.push(central, name);
  offset += local.length + name.length + compressed.length;
}
const directoryBytes = Buffer.concat(directory);
const end = Buffer.alloc(22);
end.writeUInt32LE(0x06054b50, 0); end.writeUInt16LE(entries.length, 8); end.writeUInt16LE(entries.length, 10);
end.writeUInt32LE(directoryBytes.length, 12); end.writeUInt32LE(offset, 16);
mkdirSync(join(root, "dist"), { recursive: true });
writeFileSync(join(root, "dist/Script-Library-unsigned.zip"), Buffer.concat([...chunks, directoryBytes, end]));
console.log("Built unsigned local-page mod ZIP");
