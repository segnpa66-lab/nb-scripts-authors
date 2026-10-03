package gg.nulls.library;

import android.net.Uri;
import org.json.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Room links are data to preview, never commands to execute. Tokens are discarded. */
final class BattleImport {
    final int[] values;final Set<Integer> banned;final String script;
    private BattleImport(int[] values,Set<Integer> banned,String script){this.values=values;this.banned=banned;this.script=script;}
    static BattleImport parse(String text)throws Exception{
        if(text==null||text.length()>65536)throw new IllegalArgumentException("Некорректная конфигурация боя");
        Uri uri=Uri.parse(text.trim());if(!"nullsbrawl".equalsIgnoreCase(uri.getScheme())||!"createAndJoinRoom".equalsIgnoreCase(uri.getHost()))throw new IllegalArgumentException("Некорректная конфигурация боя");
        String room=uri.getQueryParameter("roomname");if(room==null||!room.startsWith("params:v2:"))throw new IllegalArgumentException("Некорректная конфигурация боя");
        String encoded=room.substring(10);if(encoded.isEmpty()||encoded.length()>16384||!encoded.matches("[0-9A-Za-z]+"))throw new IllegalArgumentException("Некорректная конфигурация боя");
        String alphabet="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";BigInteger value=BigInteger.ZERO,base=BigInteger.valueOf(62);for(int i=0;i<encoded.length();i++)value=value.multiply(base).add(BigInteger.valueOf(alphabet.indexOf(encoded.charAt(i))));byte[] bytes=value.toByteArray();int offset=bytes.length>1&&bytes[0]==0?1:0;
        JSONObject data=new JSONObject(new String(bytes,offset,bytes.length-offset,StandardCharsets.UTF_8));int[] settings=Core.DEFAULT.clone();JSONArray bp=data.optJSONArray("bp");if(data.has("bp")&&bp==null)throw new IllegalArgumentException("Некорректная конфигурация боя");if(bp!=null){if(bp.length()!=32)throw new IllegalArgumentException("32 параметра боя");for(int i=0;i<32;i++){Object raw=bp.get(i);if(!(raw instanceof Number))throw new IllegalArgumentException("Некорректная конфигурация боя");double n=((Number)raw).doubleValue()+(Core.delta(i)?Core.DEFAULT[i]:0);if(!Double.isFinite(n)||n!=Math.rint(n)||n<Core.MIN[i]||n>Core.MAX[i])throw new IllegalArgumentException("Некорректная конфигурация боя");settings[i]=(int)n;}}Core.battle(settings);
        TreeSet<Integer> banned=new TreeSet<>();JSONArray bc=data.optJSONArray("bc");if(data.has("bc")&&bc==null)throw new IllegalArgumentException("Некорректная конфигурация боя");if(bc!=null){if(bc.length()>256)throw new IllegalArgumentException("Некорректная конфигурация боя");for(int i=0;i<bc.length();i++){Object raw=bc.get(i);if(!(raw instanceof Number))throw new IllegalArgumentException("Некорректная конфигурация боя");double n=((Number)raw).doubleValue();if(n!=Math.rint(n)||n<0||n>1024)throw new IllegalArgumentException("Некорректная конфигурация боя");banned.add((int)n);}}
        String script="";String source=data.optString("script","");if(!source.isEmpty()){Uri url=Uri.parse(source);String host=url.getHost();if("https".equalsIgnoreCase(url.getScheme())&&Core.scriptingHost(host)){java.util.regex.Matcher match=java.util.regex.Pattern.compile("^/(?:api/)?scripts/([0-9a-fA-F-]{36})(?:/content)?/?$").matcher(url.getPath());if(match.matches()){script=match.group(1).toLowerCase(Locale.ROOT);Core.requireUuid(script);}}if(script.isEmpty())throw new IllegalArgumentException("Некорректная ссылка на скрипт");}
        return new BattleImport(settings,banned,script);
    }
}
