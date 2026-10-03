package gg.nulls.library;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class Api {
    static class Failure extends IOException { final int status; Failure(int s,String text){super(text);status=s;} }
    private final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    private volatile String username="",password="",bearer="";
    private volatile JSONObject me;private volatile long sessionRevision;
    private final Vault vault;
    Api(Vault v){vault=v;}
    synchronized JSONObject identity(){return me;}
    synchronized boolean authenticated(){return me!=null;}
    synchronized JSONObject login(String u,String p,boolean remember)throws Exception{
        sessionRevision++;cookies.getCookieStore().removeAll();bearer="";me=null;username="";password="";
        JSONObject r;
        try{r=raw("POST","/auth/login",new JSONObject().put("username",u).put("password",p));}
        catch(Failure e){if(e.status==401)throw new Failure(401,"Неверное имя пользователя или пароль.");throw e;}
        bearer=r.optString("token",r.optString("access_token",""));
        try{me=Core.publicUser(raw("GET","/users/me",null));Core.requireUuid(me.optString("uuid"));username=u;password=p;if(remember)vault.save(u,p);else vault.clear();return me;}catch(Exception e){clear();throw e;}
    }
    synchronized boolean restore()throws Exception{JSONObject o=vault.read();if(o==null)return false;login(o.getString("user"),o.getString("password"),true);return true;}
    synchronized void clear(){me=null;username="";password="";bearer="";cookies.getCookieStore().removeAll();vault.clear();}
    synchronized void logout(){try{raw("POST","/auth/logout",null);}catch(Exception ignored){}clear();}
    synchronized List<HttpCookie> webCookies(){return new ArrayList<>(cookies.getCookieStore().getCookies());}
    JSONObject request(String method,String path,JSONObject body)throws Exception{
        long revision=sessionRevision;try{return raw(method,path,body);}catch(Failure e){
            if(e.status!=401)throw e;
            synchronized(this){
                if(username.isEmpty()){clear();throw e;}
                String u=username,p=password;boolean remembered=vault.read()!=null;
                try{if(revision==sessionRevision)login(u,p,remembered);}catch(Exception loginError){clear();throw loginError;}
            }
            try{return raw(method,path,body);}catch(Failure retryError){if(retryError.status==401&&path.endsWith("/content"))throw new Failure(403,"Код этого скрипта недоступен вашему аккаунту.");throw retryError;}
        }
    }
    JSONObject sync(JSONObject body,String expectedAccount)throws Exception{
        try{return syncRaw(body,expectedAccount);}catch(Failure error){if(error.status!=401)throw error;request("GET","/users/me",null);return syncRaw(body,expectedAccount);}
    }
    private JSONObject syncRaw(JSONObject body,String expectedAccount)throws Exception{
        String cookieHeader="",token;
        synchronized(this){if(me==null)throw new Failure(401,"Требуется вход.");if(!expectedAccount.equals(me.optString("uuid")))throw new Failure(409,"Аккаунт изменён.");Map<String,List<String>> values=cookies.get(URI.create(Core.API+"/users/me"),Collections.emptyMap());for(Map.Entry<String,List<String>> value:values.entrySet())if(value.getKey().equalsIgnoreCase("Cookie"))cookieHeader=String.join("; ",value.getValue());token=bearer;}
        HttpURLConnection connection=(HttpURLConnection)new URL("https://esmqzozbwufdixcxgtts.supabase.co/functions/v1/settings-sync").openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(25000);connection.setInstanceFollowRedirects(false);connection.setRequestMethod("POST");connection.setRequestProperty("Content-Type","application/json");connection.setRequestProperty("x-nulls-cookie",cookieHeader);connection.setRequestProperty("x-nulls-bearer",token);connection.setDoOutput(true);
        try{byte[] data=body.toString().getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(data.length);try(OutputStream out=connection.getOutputStream()){out.write(data);}int status=connection.getResponseCode();if(status<200||status>=300)throw new Failure(status,"Синхронизация недоступна. Настройки сохранены на устройстве.");return new JSONObject(read(connection.getInputStream(),1024*1024));}finally{connection.disconnect();}
    }
    void telemetry(String client)throws Exception{
        String cookieHeader="",token;synchronized(this){Map<String,List<String>> values=cookies.get(URI.create(Core.API+"/users/me"),Collections.emptyMap());for(Map.Entry<String,List<String>> value:values.entrySet())if(value.getKey().equalsIgnoreCase("Cookie"))cookieHeader=String.join("; ",value.getValue());token=bearer;}
        HttpURLConnection connection=(HttpURLConnection)new URL("https://esmqzozbwufdixcxgtts.supabase.co/functions/v1/settings-sync").openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(10000);connection.setInstanceFollowRedirects(false);connection.setRequestMethod("POST");connection.setRequestProperty("Content-Type","application/json");connection.setRequestProperty("x-nulls-cookie",cookieHeader);connection.setRequestProperty("x-nulls-bearer",token);connection.setDoOutput(true);
        try{byte[] data=new JSONObject().put("op","telemetry").put("platform","android").put("client_id",client).put("version",BuildConfig.VERSION_NAME).toString().getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(data.length);try(OutputStream out=connection.getOutputStream()){out.write(data);}if(connection.getResponseCode()!=200)throw new IOException("usage");try(InputStream in=connection.getInputStream()){read(in,4096);}}finally{connection.disconnect();}
    }
    private JSONObject raw(String method,String path,JSONObject body)throws Exception{
        if(!path.startsWith("/")||path.contains("..")||path.contains("?token"))throw new IllegalArgumentException("Некорректный путь API");
        URI uri=URI.create(Core.API+path);HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);c.setRequestProperty("Accept","application/json");
        for(Map.Entry<String,List<String>> entry:cookies.get(uri,Collections.emptyMap()).entrySet())c.setRequestProperty(entry.getKey(),String.join("; ",entry.getValue()));
        if(!bearer.isEmpty())c.setRequestProperty("Authorization","Bearer "+bearer);
        try{if(body!=null||method.equals("POST")||method.equals("PUT")||method.equals("DELETE")){c.setRequestProperty("Content-Type","application/json");if(body!=null){byte[] data=body.toString().getBytes(StandardCharsets.UTF_8);c.setDoOutput(true);c.setFixedLengthStreamingMode(data.length);try(OutputStream out=c.getOutputStream()){out.write(data);}}}
            int status=c.getResponseCode();cookies.put(uri,c.getHeaderFields());
            if(status<200||status>=300){String message=status==401?"Сессия завершена. Войдите снова.":status==403?"Аккаунту недоступно это действие.":status==404?"Скрипт или автор не найден.":status==429?"Слишком много запросов. Попробуйте позже.":"Сервис ответил HTTP "+status;
                if(status==400){String error=read(c.getErrorStream(),65536);if(error.contains("author does not have connect"))message="У автора нет Null’s Connect — запуск недоступен.";else message="Сервис отклонил запрос. Проверьте параметры и права аккаунта.";}
                throw new Failure(status,message);
            }
            String text=read(c.getInputStream(),8*1024*1024);if(text.trim().isEmpty())return new JSONObject();return new JSONObject(text);
        }finally{c.disconnect();}
    }
    static String read(InputStream stream,int limit)throws IOException{if(stream==null)return "";try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Файл превышает допустимый размер");out.write(b,0,n);}return out.toString(StandardCharsets.UTF_8.name());}}
    static String authorList()throws IOException{HttpURLConnection c=(HttpURLConnection)new URL(Core.AUTHORS+"?refresh="+System.currentTimeMillis()).openConnection();c.setUseCaches(false);c.setRequestProperty("Cache-Control","no-cache");c.setRequestProperty("Pragma","no-cache");c.setConnectTimeout(15000);c.setReadTimeout(15000);try{if(c.getResponseCode()!=200)throw new IOException("Список авторов временно недоступен");return read(c.getInputStream(),256*1024);}finally{c.disconnect();}}
}
