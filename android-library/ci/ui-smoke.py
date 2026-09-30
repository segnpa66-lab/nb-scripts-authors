"""Exercise the first-run screens on the Android 15 CI emulator without credentials."""
import subprocess, pathlib, xml.etree.ElementTree as ET, time
out=pathlib.Path('ui-evidence');out.mkdir(exist_ok=True)
def adb(*args):return subprocess.check_output(['adb',*args],stderr=subprocess.STDOUT)
def nodes(name):
 adb('shell','uiautomator','dump','/sdcard/window.xml')
 adb('pull','/sdcard/window.xml',str(out/(name+'.xml')))
 return list(ET.parse(out/(name+'.xml')).getroot().iter('node'))
def screenshot(name):
 (out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
def tap(label,name):
 import re
 candidates=[n for n in nodes(name) if n.get('text')==label and n.get('clickable')=='true']
 assert candidates,label+' button missing'
 a,b,c,d=map(int,re.findall(r'\d+',candidates[0].get('bounds')))
 adb('shell','input','tap',str((a+c)//2),str((b+d)//2));time.sleep(1)
 screenshot(name)
adb('install','-r','incoming/app-debug.apk')
adb('logcat','-c')
adb('shell','am','start','-W','-n','gg.nulls.library/.MainActivity');time.sleep(2)
assert any(n.get('text')=='Script Library' for n in nodes('catalog')), 'App did not render'
screenshot('catalog')
for label,name in [('Авторы','authors'),('Мои','my-scripts'),('Аккаунт','login')]:tap(label,name)
assert any(n.get('text')=='Вход в Null’s' for n in nodes('login-verified')), 'Login screen missing'
adb('shell','settings','put','system','font_scale','1.3')
time.sleep(2);screenshot('login-large-font')
log=adb('logcat','-d','-s','AndroidRuntime:E').decode('utf-8',errors='replace')
(out/'runtime.log').write_text(log)
assert 'FATAL EXCEPTION' not in log, log
print('Android 15 smoke passed: catalogue, authors, my scripts, login; no runtime crash')
