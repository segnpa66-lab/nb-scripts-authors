package gg.nulls.library;
import org.junit.Test;
import org.json.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class CoreTest {
    @Test public void battleMatchesOfficialProtocol()throws Exception{JSONArray a=Core.battle(Core.DEFAULT.clone());assertEquals(32,a.length());for(int i=0;i<32;i++)assertEquals(Core.delta(i)?0:Core.DEFAULT[i],a.getInt(i));int[] v=Core.DEFAULT.clone();v[4]=150;v[13]=1;v[23]=1;JSONArray b=Core.battle(v);assertEquals(50,b.getInt(4));assertEquals(1,b.getInt(13));assertEquals(1,b.getInt(23));}
    @Test(expected=IllegalArgumentException.class) public void invalidBattleRejected()throws Exception{int[] v=Core.DEFAULT.clone();v[4]=999;Core.battle(v);}
    @Test public void realDeeplinkRoundTripsUtf8AndToken()throws Exception{String id="33a91f5c-fca7-4e6f-957a-ea2f424b1179",link=Core.link(id,"a+b/?=",Core.DEFAULT);String encoded=link.substring(link.indexOf("params:v2:")+10,link.indexOf("&friendly"));BigInteger value=BigInteger.ZERO;String alphabet="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";for(char c:encoded.toCharArray())value=value.multiply(BigInteger.valueOf(62)).add(BigInteger.valueOf(alphabet.indexOf(c)));JSONObject room=new JSONObject(new String(value.toByteArray(),StandardCharsets.UTF_8));assertEquals("experiment:scripts",room.getString("realm"));assertTrue(room.getString("script").endsWith("?token=a%2Bb%2F%3F%3D"));assertEquals(32,room.getJSONArray("bp").length());}
    @Test public void publicFieldsNeverCarryEmail()throws Exception{JSONObject raw=new JSONObject().put("uuid","x").put("name","name a@example.org").put("email","secret@example.org").put("password","secret").put("username","author");JSONObject clean=Core.publicUser(raw);assertFalse(clean.has("email"));assertFalse(clean.has("password"));assertFalse(clean.toString().contains("example.org"));assertEquals("local@var",Core.clean("local@var"));assertEquals("-- [email удалён]",Core.clean("-- user@example.com"));}
    @Test public void partialSearchIncludesAllRequestedFields()throws Exception{JSONObject s=new JSONObject().put("name","Супер бой").put("description","Ускоренная перезарядка").put("author_name","Банан").put("uuid","123-abcd");assertTrue(Core.matches(s,"banaanae","function start()","перезар",0));assertTrue(Core.matches(s,"banaanae",null,"ANA",2));assertTrue(Core.matches(s,"", "function start()","START",4));assertTrue(Core.matches(s,"",null,"ABCD",5));assertFalse(Core.matches(s,"",null,"start",4));}
    @Test public void handlesUsePermanentListAndDedupe(){List<String> h=Core.handles("https://scripting.donutquine.dev/@ban\nhttps://scripting.donutquine.dev/users/@daily1337\nhttps://scripting.nulls.gg/@ban\nhttps://evil.test/@bad");assertEquals(Arrays.asList("ban","daily1337"),h);assertTrue(Core.AUTHORS.contains("/main/"));}
    @Test public void randomSortIsStableAndNameSortIsRussian()throws Exception{JSONObject a=new JSONObject().put("uuid","a").put("name","Яд"),b=new JSONObject().put("uuid","b").put("name","Атака");assertTrue(Core.comparator(3,0).compare(a,b)>0);assertEquals(Core.comparator(0,44).compare(a,b),Core.comparator(0,44).compare(a,b));}
    @Test public void fileNamesCannotEscapeDownloadDirectory(){assertFalse(Core.fileName("../../bad\\path","33a91f5c-fca7-4e6f-957a-ea2f424b1179").contains("/"));assertTrue(Core.fileName("Скрипт","33a91f5c-fca7-4e6f-957a-ea2f424b1179").endsWith(".lua"));}
}
