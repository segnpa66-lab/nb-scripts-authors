package gg.nulls.library;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Per-account preferences with a durable local outbox; no credentials are synchronized. */
final class AccountSync implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Api api;
    private final SharedPreferences settings,blocks,battle,brawlers,meta;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService network=Executors.newSingleThreadExecutor();
    private final Consumer<Boolean> notify;
    private String owner,status="";
    private JSONObject pending=new JSONObject(),last=new JSONObject(),bootstrap=new JSONObject();
    private boolean bound,busy,applying,closed;
    private final Runnable delayed=this::pull;
    private final Runnable poll=new Runnable(){public void run(){if(closed)return;if(api.authenticated())pull();ui.postDelayed(this,45000);}};

    AccountSync(Context context,Api api,Consumer<Boolean> notify){
        this.api=api;this.notify=notify;settings=context.getSharedPreferences("settings",0);blocks=context.getSharedPreferences("blocklist",0);battle=context.getSharedPreferences("battle",0);brawlers=context.getSharedPreferences("brawlers",0);meta=context.getSharedPreferences("account_sync",0);
        owner=meta.getString("current","");pending=read("outbox:"+owner);last=snapshot();
        for(SharedPreferences prefs:Arrays.asList(settings,blocks,battle,brawlers))prefs.registerOnSharedPreferenceChangeListener(this);
        ui.postDelayed(poll,45000);
    }
    String status(){return status;}
    private JSONObject read(String key){try{return new JSONObject(meta.getString(key,"{}"));}catch(Exception ignored){return new JSONObject();}}
    private void save(String key,JSONObject value){meta.edit().putString(key,value.toString()).apply();}
    private static void put(JSONObject object,String key,Object value){try{object.put(key,value);}catch(Exception ignored){}}
    JSONObject snapshot(){
        JSONObject result=new JSONObject();String language=settings.getString("language","system");if(language.equals("zh"))language="zh-Hans";
        put(result,"language",language);put(result,"stripWrapper",settings.getBoolean("strip_service_wrapper",false));put(result,"hideD2Random",blocks.getBoolean("hide_d2_random",false));
        for(String id:blocks.getStringSet("authors",Collections.emptySet()))put(result,"author:"+id,true);
        for(String id:blocks.getStringSet("scripts",Collections.emptySet()))put(result,"script:"+id,true);
        for(Map.Entry<String,?> entry:battle.getAll().entrySet())try{Core.requireUuid(entry.getKey());put(result,"battle:"+entry.getKey(),new JSONArray(String.valueOf(entry.getValue())));}catch(Exception ignored){}
        for(Map.Entry<String,?> entry:brawlers.getAll().entrySet())if(entry.getValue() instanceof Set){TreeSet<Integer> ids=new TreeSet<>();for(Object value:(Set<?>)entry.getValue())try{ids.add(Integer.parseInt(String.valueOf(value)));}catch(Exception ignored){}put(result,"brawlers:"+entry.getKey(),new JSONArray(ids));}
        return result;
    }
    private static boolean equal(Object a,Object b){return Objects.equals(a==null?null:a.toString(),b==null?null:b.toString());}
    private static boolean same(JSONObject a,JSONObject b){if(a.length()!=b.length())return false;Iterator<String> keys=a.keys();while(keys.hasNext()){String key=keys.next();if(!equal(a.opt(key),b.opt(key)))return false;}return true;}
    void bind(){
        JSONObject identity=api.identity();String id=identity==null?"":identity.optString("uuid");
        if(id.equals(owner)){if(!bound){bound=true;bootstrap=snapshot();if(!id.isEmpty())pull();}return;}
        bound=true;JSONObject current=snapshot();String old=owner;
        if(!old.isEmpty())save("backup:"+old,current);else if(!id.isEmpty())save("guest",current);
        owner=id;meta.edit().putString("current",id).apply();pending=read("outbox:"+id);
        JSONObject next=id.isEmpty()?read("guest"):read("backup:"+id);
        bootstrap=next.length()>0?next:old.isEmpty()?current:new JSONObject();
        apply(next);status="";notify.accept(true);if(!id.isEmpty())pull();
    }
    private void apply(JSONObject values){
        applying=true;
        Set<String> authors=new HashSet<>(),scripts=new HashSet<>();SharedPreferences.Editor battleEdit=battle.edit().clear(),brawlerEdit=brawlers.edit().clear();
        Iterator<String> keys=values.keys();while(keys.hasNext()){String key=keys.next();String[] parts=key.split(":",2);if(parts.length!=2)continue;String kind=parts[0],id=parts[1];if(kind.equals("author")&&values.optBoolean(key))authors.add(id);if(kind.equals("script")&&values.optBoolean(key))scripts.add(id);if(kind.equals("battle")&&values.optJSONArray(key)!=null)battleEdit.putString(id,values.optJSONArray(key).toString());if(kind.equals("brawlers")&&values.optJSONArray(key)!=null){Set<String> selected=new HashSet<>();JSONArray array=values.optJSONArray(key);for(int i=0;i<array.length();i++)selected.add(String.valueOf(array.optInt(i)));brawlerEdit.putStringSet(id,selected);}}
        settings.edit().putString("language",values.optString("language","system")).putBoolean("strip_service_wrapper",values.optBoolean("stripWrapper",false)).apply();
        blocks.edit().putStringSet("authors",authors).putStringSet("scripts",scripts).putBoolean("hide_d2_random",values.optBoolean("hideD2Random",false)).apply();battleEdit.apply();brawlerEdit.apply();last=snapshot();applying=false;
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences prefs,String key){
        if(applying||closed)return;JSONObject current=snapshot();
        if(!owner.isEmpty()){
            Set<String> keys=new HashSet<>();Iterator<String> first=last.keys(),second=current.keys();while(first.hasNext())keys.add(first.next());while(second.hasNext())keys.add(second.next());
            for(String changed:keys){Object value=current.opt(changed);if(value==null)value=changed.startsWith("author:")||changed.startsWith("script:")?false:new JSONArray();if(!equal(last.opt(changed),current.opt(changed)))put(pending,changed,value);}
            save("outbox:"+owner,pending);ui.removeCallbacks(delayed);ui.postDelayed(delayed,500);
        }
        last=current;
    }
    void pull(){
        if(closed||busy||owner.isEmpty()||!api.authenticated())return;
        busy=true;String id=owner;boolean initialize=!meta.getBoolean("known:"+id,false);JSONObject sent;try{sent=new JSONObject(pending.toString());}catch(Exception ignored){sent=new JSONObject();}final JSONObject outgoing=sent;
        JSONObject changes=new JSONObject();if(initialize){Iterator<String> keys=bootstrap.keys();while(keys.hasNext()){String key=keys.next();put(changes,key,bootstrap.opt(key));}}Iterator<String> keys=sent.keys();while(keys.hasNext()){String key=keys.next();put(changes,key,sent.opt(key));}
        JSONObject request=new JSONObject();put(request,"initialize",initialize);put(request,"changes",changes);status="Синхронизация…";notify.accept(false);
        network.execute(()->{try{JSONObject data=api.sync(request,id);ui.post(()->{
            busy=false;if(closed)return;if(!owner.equals(id)){pull();return;}if(!id.equals(data.optString("user_uuid"))){status="Синхронизация недоступна. Настройки сохранены на устройстве.";notify.accept(false);return;}
            JSONObject values=data.optJSONObject("values");if(values==null)values=new JSONObject();Iterator<String> sentKeys=outgoing.keys();while(sentKeys.hasNext()){String key=sentKeys.next();if(equal(pending.opt(key),outgoing.opt(key))&&(!initialize||data.optBoolean("initialized")||equal(values.opt(key),outgoing.opt(key))))pending.remove(key);}
            meta.edit().putBoolean("known:"+id,true).apply();save("outbox:"+id,pending);Iterator<String> localKeys=pending.keys();while(localKeys.hasNext()){String key=localKeys.next();put(values,key,pending.opt(key));}
            boolean changed=!same(snapshot(),positiveSnapshot(values));apply(values);save("backup:"+id,snapshot());status="Синхронизировано";notify.accept(changed);if(pending.length()>0)ui.postDelayed(delayed,500);
        });}catch(Exception ignored){ui.post(()->{busy=false;if(closed)return;if(owner.equals(id)){status="Синхронизация недоступна. Настройки сохранены на устройстве.";notify.accept(false);}else pull();});}});
    }
    private static JSONObject positiveSnapshot(JSONObject values){JSONObject clean=new JSONObject();Iterator<String> keys=values.keys();while(keys.hasNext()){String key=keys.next();if((key.startsWith("author:")||key.startsWith("script:"))&&!values.optBoolean(key))continue;put(clean,key,values.opt(key));}if(!clean.has("language"))put(clean,"language","system");if(!clean.has("stripWrapper"))put(clean,"stripWrapper",false);if(!clean.has("hideD2Random"))put(clean,"hideD2Random",false);return clean;}
    void close(){closed=true;for(SharedPreferences prefs:Arrays.asList(settings,blocks,battle,brawlers))prefs.unregisterOnSharedPreferenceChangeListener(this);ui.removeCallbacksAndMessages(null);network.shutdownNow();}
}
