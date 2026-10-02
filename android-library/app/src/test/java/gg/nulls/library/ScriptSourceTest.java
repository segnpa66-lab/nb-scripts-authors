package gg.nulls.library;
import org.junit.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
public class ScriptSourceTest {
 private String fixture() throws Exception {try(InputStream stream=getClass().getResourceAsStream("/guard-fixture.lua")){assertNotNull(stream);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);}}
 private static final String EXPECTED="function tick()\n    log(\"_user_tick and guard stay in strings\")\nend\n";
 @Test public void restoresOriginalAndKeepsUserGuard() throws Exception {String input=fixture();assertEquals(EXPECTED,ScriptSource.original(input));assertEquals(EXPECTED,ScriptSource.original(EXPECTED));String own="local GUARD_TICKS = 100\nfunction applyGuard() end\n";assertEquals(own+EXPECTED,ScriptSource.original(own+input));}
 @Test public void unknownWrapperIsUnchanged() throws Exception {String input=fixture();for(String changed:new String[]{input.replace("elapsed < 100","elapsed < 200"),input.substring(0,input.length()-5),input+"log(\"extra\")\n",input.replace("function _user_tick()","function another()")})assertEquals(changed,ScriptSource.original(changed));}
 @Test public void ignoresQuotedHooksAndPreservesLineEndings() throws Exception {String prefix="-- function _user_tick()\nlocal text = [=[\nlocal _guard_start_tick = nil\nfunction _user_tick()\n]=]\n";assertEquals(prefix+EXPECTED,ScriptSource.original(prefix+fixture()));assertEquals(EXPECTED.replace("\n","\r\n"),ScriptSource.original(fixture().replace("\n","\r\n")));}
}
