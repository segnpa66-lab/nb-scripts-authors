package gg.nulls.library;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/** Restores only the complete known service wrapper; unknown code is preserved. */
public final class ScriptSource {
 private ScriptSource() {}
 private static final String SIGNATURE = "local_guard_start_tick=nillocal_guard_welcome_shown=falselocal_guard_server=serverlocal_GUARD_WELCOME_TEXT=functiontick()ifnot_guard_start_tickthen_guard_start_tick=_guard_server.tickendlocalelapsed=_guard_server.tick-_guard_start_tickifnot_guard_welcome_shownthenlog(_GUARD_WELCOME_TEXT)_guard_welcome_shown=trueendifelapsed<100thenreturnendiftype(_user_tick)==then_user_tick()endend";
 public static String original(String source) {
  String mask=mask(source);
  Matcher marker=Pattern.compile("(?m)^local _guard_start_tick\\s*=\\s*nil\\s*$").matcher(mask);
  if(!marker.find())return source;
  int start=marker.start();
  if(!Pattern.compile("type\\s*\\(_user_tick\\)\\s*==\\s*([\"'])function\\1").matcher(source.substring(start)).find())return source;
  if(!mask.substring(start).replaceAll("\\s+", "").equals(SIGNATURE))return source;
  Matcher hook=Pattern.compile("(?m)^(\\s*function\\s+)(_user_tick)(\\s*\\(\\s*\\))").matcher(mask.substring(0,start));
  if(!hook.find())return source;
  int from=hook.start(2),to=hook.end(2);if(hook.find())return source;
  String body=source.substring(0,from)+"tick"+source.substring(to,start);
  return body.replaceFirst("[\\r\\n]+$", "")+(source.contains("\r\n")?"\r\n":"\n");
 }
 private static int longEnd(String source,int start){
  if(start>=source.length()||source.charAt(start)!='[')return -1;
  int i=start+1;while(i<source.length()&&source.charAt(i)=='=')i++;
  if(i>=source.length()||source.charAt(i)!='[')return -1;
  String end="]"+source.substring(start+1,i)+"]";int close=source.indexOf(end,i+1);return close<0?source.length():close+end.length();
 }
 private static String mask(String source){
  char[] out=source.toCharArray();
  for(int i=0;i<source.length();){
   int end=-1;char ch=source.charAt(i);
   if(source.startsWith("--",i)){end=longEnd(source,i+2);if(end<0){end=source.indexOf('\n',i);if(end<0)end=source.length();}}
   else if(ch=='\''||ch=='"'){end=i+1;while(end<source.length()){char value=source.charAt(end++);if(value=='\\')end=Math.min(source.length(),end+1);else if(value==ch)break;}}
   else if(ch=='[')end=longEnd(source,i);
   if(end<0){i++;continue;}for(int j=i;j<end;j++)if(out[j]!='\n'&&out[j]!='\r')out[j]=' ';i=end;
  }
  return new String(out);
 }
}
