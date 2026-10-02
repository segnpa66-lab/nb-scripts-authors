package gg.nulls.library;

import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.*;
import java.util.regex.*;
import org.json.*;

/** Protocol and public-data allowlists, shared by the UI and tests. */
public final class Core {
    private Core() {}
    public static final String ORIGIN="https://scripting.nulls.gg", API=ORIGIN+"/api";
    public static final String AUTHORS="https://raw.githubusercontent.com/segnpa66-lab/nb-scripts-authors/android-script-library/list.txt";
    public static final String[] BP_IDS={"08d59fd7-b5ba-41c0-ac91-0ec36ad2fda7","27debbd9-1800-453b-98cb-778faebf03ec","850fa2e7-b6a8-4f65-a4d7-15e730772380","0118efa9-4916-4258-896d-08d8fa6cb447","67308acc-a26a-423d-9bd4-0a483aaddc16","ef7f2daf-ecb6-4b45-b54a-05be2abf3028","27df50dd-9b42-480b-ba29-252cb5b7d390","d406daa9-299a-4c1f-8e38-7c0b7447f0e3","588e2a5d-b1da-4ab1-af06-90fbf82ad845","06e2388f-caee-4434-a2b5-729b837d0c63","4a66586c-2d0e-48b8-b7a8-82dbd31f60ba","9984be89-1a06-469a-b6e4-4fb9cf6fdcc6","90c21bbe-dfd4-4bf0-b0a8-26f5a9c193e5","19c252fe-337b-4830-b42b-831ccb1fd019","7425f7c2-3a70-4655-8a55-aa6e40b55cac","a3526238-44ab-4465-b338-a26a0bac9009","0f1e4985-d6f2-4acc-8629-bc052fae5df9","3b448328-99e4-4c63-ac08-1a6144299386","2793b21b-a1b2-47e6-a777-7dde43388332","6c60b2c1-90df-4ed6-9d65-f72cb3df959c","5e9dfbe2-dc36-4492-b2f7-163b2b61b507","3e4e283d-0950-4d29-9694-0b788c6f2151","a709ece0-ff74-4271-acae-9fdb0b6edb86","6dfa1632-da82-4c48-9028-83b11729396a","afc1c68c-34f3-4808-ad44-4d0019fa2a7c","9f654a06-ff46-4150-8313-eb602e245103","5ba9cf79-aa86-4979-9c60-c3fff5f5503f","13d8a58d-aa94-483a-8423-20da88dac86a","b464565d-f0c7-4f0d-a178-3cfeaf0bb6dd","5a35d6df-3180-4ea0-bb9d-554eae8071cb","e8798ee0-3ced-4863-ae18-9c64d49ea7d2","e42bf28e-fb71-473e-a90c-e8181a1caf2c"};
    public static final String[] BP_NAMES={"Уровень здоровья","Уровень атаки","Уровень супера","Боевой уровень","Скорость движения, %","Скорость перезарядки, %","Множитель заряда супера, %","Автозаряд супера, %","Автозаряд гипера, %","Откат гаджета, %","Дополнительные питомцы","Урон яда, %","Регенерация, %","Выключить яд","Сложность ботов","Щит после возрождения, с","Разрешить гаджеты","Разрешить звёздные силы","Разрешить гиперзаряды","Количество снаряжения","Кубики из ящиков","Выключить усиление яда","Смещение появления","Ограничить вход","Кубики из бойцов, %","Показать выбор бойца","Разрешить ранний выход","Чат боя","Усиления гаджетов","Усиления звёздных сил","Усиления гиперзаряда","Тики автозаряда"};
    public static final int[] DEFAULT={11,11,11,11,100,100,100,0,0,100,0,20,13,0,4,3,1,1,1,2,1,0,0,-1,100,0,0,0,1,1,1,20};
    public static final int[] MIN={-8,-8,-8,-8,30,10,0,0,0,0,0,0,0,0,-1,0,0,0,0,0,0,0,0,-1,0,0,0,0,0,0,0,1};
    public static final int[] MAX={100,100,100,100,220,500,500,100,100,200,20,100,100,1,4,20,1,1,1,2,10,1,10,1,500,1,1,1,1,1,1,100};
    public static boolean delta(int i){return i<=5||i==9||i==11||i==15;}
    public static boolean flag(int i){return i==13||(i>=16&&i<=18)||i==21||i==23||(i>=25&&i<=30);}
    public static JSONArray battle(int[] values)throws JSONException{
        if(values.length!=32)throw new IllegalArgumentException("32 параметра боя");
        JSONArray a=new JSONArray();for(int i=0;i<32;i++){if(values[i]<MIN[i]||values[i]>MAX[i])throw new IllegalArgumentException(BP_NAMES[i]+": вне диапазона");a.put(values[i]-(delta(i)?DEFAULT[i]:0));}return a;
    }
    public static String base62(byte[] bytes){String abc="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";BigInteger n=new BigInteger(1,bytes),b=BigInteger.valueOf(62);StringBuilder out=new StringBuilder();while(n.signum()>0){BigInteger[] d=n.divideAndRemainder(b);out.append(abc.charAt(d[1].intValue()));n=d[0];}for(byte v:bytes){if(v!=0)break;out.append('0');}return out.reverse().toString();}
    public static String link(String uuid,String token,int[] values)throws Exception{
        requireUuid(uuid);if(token==null||token.trim().isEmpty())throw new IllegalArgumentException("Нет токена запуска");
        JSONObject o=new JSONObject().put("realm","experiment:scripts").put("script",API+"/scripts/"+uuid+"/content?token="+java.net.URLEncoder.encode(token,"UTF-8")).put("bp",battle(values));
        return "nullsbrawl://createAndJoinRoom?roomname=params:v2:"+base62(o.toString().getBytes(StandardCharsets.UTF_8))+"&friendly=1&side=0";
    }
    public static void requireUuid(String id){if(id==null||!id.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IllegalArgumentException("Некорректный UUID");}
    public static String scriptId(String input){
        if(input==null)return null;
        String value=input.trim();
        try{requireUuid(value);return value.toLowerCase(Locale.ROOT);}catch(IllegalArgumentException ignored){}
        try{URI uri=URI.create(value);if(!"https".equalsIgnoreCase(uri.getScheme())||!"scripting.nulls.gg".equalsIgnoreCase(uri.getHost())||uri.getUserInfo()!=null||uri.getPort()!=-1)return null;
            Matcher match=Pattern.compile("^/scripts/([0-9a-fA-F-]{36})/?$").matcher(uri.getPath());if(!match.matches())return null;String id=match.group(1);requireUuid(id);return id.toLowerCase(Locale.ROOT);
        }catch(Exception ignored){return null;}
    }
    public static boolean hidden(JSONObject script,String handle,Set<String> blockedAuthors,Set<String> blockedScripts,boolean hideD2Random,boolean randomSort){
        return blockedScripts.contains(script.optString("uuid"))||blockedAuthors.contains(script.optString("author_uuid"))||randomSort&&hideD2Random&&"d2rkmean".equalsIgnoreCase(handle);
    }
    public static String clean(String s){return s==null?"":s.replaceAll("(?i)[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9-]+(?:\\.[a-z0-9-]+)+","[email удалён]");}
    public static JSONObject publicUser(JSONObject raw)throws JSONException{JSONObject o=new JSONObject();for(String k:new String[]{"uuid","name","username"})o.put(k,clean(raw.optString(k,"")));return o;}
    public static JSONObject publicScript(JSONObject raw)throws JSONException{JSONObject o=new JSONObject();for(String k:new String[]{"uuid","author_uuid","author_name","name","description","created_at","updated_at","published_at"})if(!raw.isNull(k))o.put(k,clean(raw.optString(k,"")));o.put("is_favorite",raw.optBoolean("is_favorite"));return o;}
    public static List<String> handles(String text){Set<String> out=new LinkedHashSet<>();Matcher m=Pattern.compile("https://scripting\\.(?:donutquine\\.dev|nulls\\.gg)/(?:users/)?@([A-Za-z0-9_.-]{1,64})").matcher(text);while(m.find())out.add(m.group(1).toLowerCase(Locale.ROOT));return new ArrayList<>(out);}
    public static boolean matches(JSONObject s,String handle,String code,String query,int mode){String q=query.toLowerCase(Locale.ROOT).trim();if(q.isEmpty())return true;String[] fields={s.optString("name"),s.optString("author_name")+" "+handle,s.optString("description"),code==null?"":code,s.optString("uuid")};if(mode==0){for(String f:fields)if(f.toLowerCase(Locale.ROOT).contains(q))return true;return false;}return fields[mode-1].toLowerCase(Locale.ROOT).contains(q);}
    public static Comparator<JSONObject> comparator(int sort,long seed){Collator col=Collator.getInstance(new Locale("ru"));col.setStrength(Collator.PRIMARY);return (a,b)->{int c;switch(sort){case 1:c=date(b).compareTo(date(a));break;case 2:c=date(a).compareTo(date(b));break;case 3:c=col.compare(a.optString("name"),b.optString("name"));break;case 4:c=col.compare(b.optString("name"),a.optString("name"));break;case 5:c=b.optString("updated_at").compareTo(a.optString("updated_at"));break;default:c=Long.compare(rank(a.optString("uuid"),seed),rank(b.optString("uuid"),seed));}return c!=0?c:a.optString("uuid").compareTo(b.optString("uuid"));};}
    private static long rank(String id,long seed){long x=seed^id.hashCode();x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;return x^(x>>>31);}
    private static String date(JSONObject s){return s.optString("published_at",s.optString("created_at"));}
    public static List<JSONObject> releases(JSONArray array)throws JSONException{
        List<JSONObject> result=new ArrayList<>();
        for(int i=0;i<array.length();i++){JSONObject item=array.getJSONObject(i);if(!item.has("id")||item.optLong("id")<=0)continue;result.add(new JSONObject().put("id",item.getLong("id")).put("created_at",item.optString("created_at")).put("description",clean(item.isNull("description")?"":item.optString("description"))));}
        result.sort((a,b)->b.optString("created_at").compareTo(a.optString("created_at")));return result;
    }
    public static String fileName(String name,String uuid){String n=clean(name).replaceAll("[^\\p{L}\\p{N}._-]+","_");if(n.length()>64)n=n.substring(0,64);if(n.isEmpty())n="script";return n+"_"+uuid.substring(0,8)+".lua";}
}
