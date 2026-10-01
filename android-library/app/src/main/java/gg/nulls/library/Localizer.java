package gg.nulls.library;

import android.content.Context;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.ConnectException;

final class Localizer {
    private final Context context;
    private JSONObject strings=new JSONObject();
    private JSONObject english=new JSONObject();
    private String current="";
    Localizer(Context context){this.context=context;try(InputStream in=context.getAssets().open("i18n/en.json")){english=new JSONObject(Api.read(in,256*1024));}catch(Exception ignored){}refresh();}
    Locale locale(){String code=context.getSharedPreferences("settings",0).getString("language","system");if("system".equals(code)){Locale device=context.getResources().getConfiguration().getLocales().get(0);return device==null?Locale.getDefault():device;}return Locale.forLanguageTag(code);}
    void refresh(){Locale locale=locale();String code=locale.toLanguageTag();if(code.equals(current))return;current=code;String asset=code;if(!asset.equals("pt-BR")&&!asset.equals("zh-Hans"))asset=locale.getLanguage();if(asset.equals("zh"))asset="zh-Hans";if(asset.equals("pt"))asset="pt-BR";try(InputStream in=context.getAssets().open("i18n/"+asset+".json")){strings=new JSONObject(Api.read(in,256*1024));}catch(Exception ignored){strings=english;}}
    String t(String original){String translated=strings.optString(original,"");if(!translated.isEmpty())return translated;return locale().getLanguage().equals("ru")?original:english.optString(original,original);}
    String error(Throwable cause){if(cause instanceof SocketTimeoutException)return t("Превышено время ожидания. Повторите попытку.");if(cause instanceof UnknownHostException||cause instanceof ConnectException)return t("Нет подключения к сети.");String message=Core.clean(cause.getMessage());if(message.startsWith("Сервис ответил HTTP "))return t("Сервис ответил HTTP ")+message.substring("Сервис ответил HTTP ".length());if(message.endsWith(": вне диапазона"))return t(message.substring(0,message.length()-": вне диапазона".length()))+t(": вне диапазона");if(strings.has(message)||english.has(message)||locale().getLanguage().equals("ru"))return t(message);return t("Неожиданный ответ сервиса. Попробуйте обновить данные.");}
}
