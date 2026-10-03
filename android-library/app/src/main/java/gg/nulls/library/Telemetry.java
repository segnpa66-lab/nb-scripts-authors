package gg.nulls.library;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;
import java.util.concurrent.*;
/** Installation and daily activity counts only. Credentials and script contents are never included. */
final class Telemetry {
    private static final ExecutorService network=Executors.newSingleThreadExecutor();private static boolean sending;
    static synchronized void record(Context context,Api api){
        if(!context.getSharedPreferences("settings",0).getBoolean("usage_enabled",true))return;SharedPreferences prefs=context.getSharedPreferences("usage",0);String client=prefs.getString("client","");if(client.isEmpty()){client=UUID.randomUUID().toString();prefs.edit().putString("client",client).apply();}
        String account=api.identity()==null?"guest":api.identity().optString("uuid","guest");String stamp=account+":"+BuildConfig.VERSION_NAME;
        if(sending||(stamp.equals(prefs.getString("stamp",""))&&System.currentTimeMillis()-prefs.getLong("sent",0)<15*60*1000))return;
        sending=true;final String id=client;network.execute(()->{try{api.telemetry(id);prefs.edit().putString("stamp",stamp).putLong("sent",System.currentTimeMillis()).apply();}catch(Exception ignored){}finally{synchronized(Telemetry.class){sending=false;}}});
    }
}
