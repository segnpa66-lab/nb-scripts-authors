"""Generate bundled UI translations; the app never calls a translator at runtime."""
import concurrent.futures
import json
import re
import urllib.parse
import urllib.request
import requests
from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / 'app/src/main/java/gg/nulls/library'
source = (java / 'MainActivity.java').read_text()
source += (java / 'Core.java').read_text()
source += (java / 'Api.java').read_text()
source += (java / 'Repository.java').read_text()
keys = []
for literal in re.findall(r'"((?:\\.|[^"\\])*)"', source):
    if not re.search('[А-Яа-яЁё]', literal):
        continue
    try:
        key = json.loads('"' + literal + '"')
    except json.JSONDecodeError:
        continue
    if key not in keys:
        keys.append(key)

assets = root / 'app/src/main/assets/i18n'
assets.mkdir(parents=True, exist_ok=True)
(assets / 'ru.json').write_text(json.dumps({k: k for k in keys}, ensure_ascii=False), encoding='utf-8')


def libre(code):
    file=assets / (code+'.json')
    translations=json.loads(file.read_text()) if file.exists() else {}
    remaining=[key for key in keys if key not in translations]
    for offset in range(0, len(remaining), 30):
        group = remaining[offset:offset + 30]
        response = requests.post('https://translate.blueva.net/translate', json={'q': group, 'source': 'ru', 'target': code, 'format': 'text'}, timeout=90)
        response.raise_for_status()
        result = response.json()['translatedText']
        if isinstance(result, str):
            result = [result]
        if len(result) != len(group):
            raise RuntimeError(f'{code}: expected {len(group)}, got {len(result)}')
        translations.update(zip(group, result))
    return translations


def mymemory(code):
    file=assets / (code+'.json')
    translations=json.loads(file.read_text()) if file.exists() else {}
    groups=[];group=[];size=0
    for key in keys:
        if key in translations:continue
        if group and size+len(key)+1>400:
            groups.append(group);group=[];size=0
        group.append(key);size+=len(key)+1
    if group:groups.append(group)
    for group in groups:
        url = 'https://api.mymemory.translated.net/get?' + urllib.parse.urlencode({'q': '\n'.join(group), 'langpair': 'ru|' + code})
        with urllib.request.urlopen(url, timeout=30) as response:
            result = json.load(response)
        if result.get('responseStatus') != 200:
            raise RuntimeError(f'{code}: {result.get("responseDetails")}')
        lines = result['responseData']['translatedText'].split('\n')
        values=[];position=0
        for key in group:
            count=key.count('\n')+1
            values.append('\n'.join(lines[position:position+count]))
            position+=count
        if position != len(lines):
            raise RuntimeError(f'{code}: expected {position} lines, got {len(lines)}')
        translations.update(zip(group, values))
    return translations


targets = ['en', 'uk', 'pl', 'hu', 'zh-Hans', 'ja', 'pt-BR', 'es', 'it', 'de', 'nl']
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    jobs = {pool.submit(libre, code): code for code in targets}
    for job in concurrent.futures.as_completed(jobs):
        code = jobs[job]
        try:
            result = job.result()
            (assets / (code + '.json')).write_text(json.dumps(result, ensure_ascii=False), encoding='utf-8')
            print(code, len(result), flush=True)
        except Exception as exc:
            print(code, 'FAILED', exc, flush=True)

for code in ['kk', 'be', 'sr']:
    try:
        result = mymemory(code)
        (assets / (code + '.json')).write_text(json.dumps(result, ensure_ascii=False), encoding='utf-8')
        print(code, len(result), flush=True)
    except Exception as exc:
        print(code, 'FAILED', exc, flush=True)
