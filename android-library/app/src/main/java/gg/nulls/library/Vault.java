package gg.nulls.library;
import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Only explicitly remembered credentials are persisted, encrypted with the device key. */
final class Vault {
    private final Context context;
    Vault(Context c){context=c.getApplicationContext();}
    private javax.crypto.SecretKey key()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(!ks.containsAlias("script-library-login")){KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("script-library-login",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}return (javax.crypto.SecretKey)ks.getKey("script-library-login",null);}
    void save(String user,String password)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());String plain=new JSONObject().put("user",user).put("password",password).toString();String value=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP);context.getSharedPreferences("vault",0).edit().putString("login",value).apply();}
    JSONObject read(){try{String value=context.getSharedPreferences("vault",0).getString("login","");if(value.isEmpty())return null;String[] p=value.split(":");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],Base64.NO_WRAP)));return new JSONObject(new String(c.doFinal(Base64.decode(p[1],Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){clear();return null;}}
    void clear(){context.getSharedPreferences("vault",0).edit().clear().apply();}
}
