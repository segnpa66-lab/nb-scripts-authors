package gg.nulls.library;

import android.content.Context;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class Localizer {
    private final Context context;
    private JSONObject strings=new JSONObject();
    private String current="";
    Localizer(Context context){this.context=context;refresh();}
    Locale locale(){String code=context.getSharedPreferences("settings",0).getString("language","system");if("system".equals(code)){Locale device=context.getResources().getConfiguration().getLocales().get(0);return device==null?Locale.getDefault():device;}return Locale.forLanguageTag(code);}
    void refresh(){Locale locale=locale();String code=locale.toLanguageTag();if(code.equals(current))return;current=code;String asset=code;if(!asset.equals("pt-BR")&&!asset.equals("zh-Hans"))asset=locale.getLanguage();if(asset.equals("zh"))asset="zh-Hans";try(InputStream in=context.getAssets().open("i18n/"+asset+".json")){byte[] bytes=new byte[in.available()];int n=0;while(n<bytes.length){int count=in.read(bytes,n,bytes.length-n);if(count<0)break;n+=count;}strings=new JSONObject(new String(bytes,0,n,StandardCharsets.UTF_8));}catch(Exception ignored){strings=new JSONObject();}}
    String t(String original){return strings.optString(original,original);}
}
