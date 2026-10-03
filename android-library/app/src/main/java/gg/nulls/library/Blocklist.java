package gg.nulls.library;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

final class Blocklist {
    private static final String AUTHORS="authors", SCRIPTS="scripts", D2_RANDOM="hide_d2_random";
    private final SharedPreferences prefs;
    private final Set<String> authors, scripts,randomAuthors;

    Blocklist(Context context){prefs=context.getSharedPreferences("blocklist",Context.MODE_PRIVATE);authors=new HashSet<>(prefs.getStringSet(AUTHORS,new HashSet<>()));scripts=new HashSet<>(prefs.getStringSet(SCRIPTS,new HashSet<>()));randomAuthors=new HashSet<>(prefs.getStringSet("random_authors",new HashSet<>()));}
    void reload(){authors.clear();authors.addAll(prefs.getStringSet(AUTHORS,new HashSet<>()));scripts.clear();scripts.addAll(prefs.getStringSet(SCRIPTS,new HashSet<>()));randomAuthors.clear();randomAuthors.addAll(prefs.getStringSet("random_authors",new HashSet<>()));}
    boolean author(String id){return authors.contains(id);}
    boolean script(String id){return scripts.contains(id);}
    Set<String> authors(){return new HashSet<>(authors);}
    Set<String> scripts(){return new HashSet<>(scripts);}
    boolean hideD2Random(){return prefs.getBoolean(D2_RANDOM,false);}
    boolean hidden(JSONObject item,String handle,int sort){return sort==0&&randomAuthor(item.optString("author_uuid"),handle)||Core.hidden(item,handle,authors,scripts,hideD2Random(),sort==0);}
    boolean randomAuthor(String id,String handle){return randomAuthors.contains(id)||"d2rkmean".equalsIgnoreCase(handle)&&hideD2Random();}
    void toggleRandomAuthor(String id,String handle){Core.requireUuid(id);boolean hidden=randomAuthor(id,handle);if(hidden)randomAuthors.remove(id);else randomAuthors.add(id);SharedPreferences.Editor edit=prefs.edit().putStringSet("random_authors",new HashSet<>(randomAuthors));if("d2rkmean".equalsIgnoreCase(handle))edit.putBoolean(D2_RANDOM,false);edit.apply();}
    void toggleAuthor(String id){Core.requireUuid(id);if(!authors.remove(id))authors.add(id);prefs.edit().putStringSet(AUTHORS,new HashSet<>(authors)).apply();}
    void toggleScript(String id){Core.requireUuid(id);if(!scripts.remove(id))scripts.add(id);prefs.edit().putStringSet(SCRIPTS,new HashSet<>(scripts)).apply();}
    void unblockAuthor(String id){if(authors.remove(id))prefs.edit().putStringSet(AUTHORS,new HashSet<>(authors)).apply();}
    void unblockScript(String id){if(scripts.remove(id))prefs.edit().putStringSet(SCRIPTS,new HashSet<>(scripts)).apply();}
    void toggleD2Random(){prefs.edit().putBoolean(D2_RANDOM,!hideD2Random()).apply();}
}
