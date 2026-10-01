package gg.nulls.library;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

final class Repository {
    final Api api;
    final Context context;
    private final LinkedHashMap<String,JSONObject> scripts=new LinkedHashMap<>(),authors=new LinkedHashMap<>();
    private final HashMap<String,String> code=new HashMap<>();
    private int codeBytes=0;
    volatile long refreshed=0;
    volatile boolean indexing=false,cancelIndex=false;
    volatile String indexStatus="Код ещё не индексирован";
    private volatile String cacheAccount="";
    interface Progress { void update(String text); }
    Repository(Context c,Api a){context=c.getApplicationContext();api=a;readCache();}
    synchronized List<JSONObject> scripts(){return new ArrayList<>(scripts.values());}
    synchronized List<JSONObject> authors(){return new ArrayList<>(authors.values());}
    synchronized String code(String id){return code.get(id);}
    synchronized String handle(String id){JSONObject a=authors.get(id);return a==null?"":a.optString("username");}
    synchronized JSONObject author(String id){return authors.get(id);}
    void refresh(Progress progress)throws Exception{
        String account=api.authenticated()?api.identity().getString("uuid"):"";
        List<String> handles=Core.handles(Api.authorList());
        LinkedHashMap<String,JSONObject> nextScripts=new LinkedHashMap<>(),nextAuthors=new LinkedHashMap<>();int fail=0,n=0;
        ExecutorService pool=Executors.newFixedThreadPool(4);CompletionService<JSONObject> jobs=new ExecutorCompletionService<>(pool);
        try{for(String handle:handles)jobs.submit(()->{JSONObject author=Core.publicUser(api.request("GET","/users/@"+handle,null));Core.requireUuid(author.optString("uuid"));String id=author.getString("uuid");if(author.optString("username").isEmpty())author.put("username",handle);JSONArray list=api.request("GET","/users/"+id+"/scripts",null).optJSONArray("scripts");if(list==null)throw new IOException("Нет списка скриптов");return new JSONObject().put("author",author).put("scripts",list);});
            for(int job=0;job<handles.size();job++){try{JSONObject result=jobs.take().get();JSONObject author=result.getJSONObject("author");String id=author.getString("uuid");nextAuthors.put(id,author);JSONArray list=result.getJSONArray("scripts");for(int i=0;i<list.length();i++){JSONObject s=Core.publicScript(list.getJSONObject(i));Core.requireUuid(s.optString("uuid"));if(s.optString("published_at").isEmpty())continue;s.put("author_uuid",id);if(s.optString("author_name").isEmpty())s.put("author_name",author.optString("name"));nextScripts.put(s.getString("uuid"),s);}}catch(ExecutionException e){Throwable cause=e.getCause();if(cause instanceof Api.Failure&&(((Api.Failure)cause).status==401||((Api.Failure)cause).status==429))throw (Api.Failure)cause;fail++;}progress.update("Авторы "+(++n)+" / "+handles.size());}
        }finally{pool.shutdownNow();}
        if(!handles.isEmpty()&&nextAuthors.isEmpty())throw new IOException("Не удалось получить авторов. Проверьте соединение.");
        String current=api.authenticated()?api.identity().optString("uuid"):"";
        if(!account.equals(current))throw new IOException("Аккаунт изменился. Обновите библиотеку заново.");
        synchronized(this){scripts.clear();scripts.putAll(nextScripts);authors.clear();authors.putAll(nextAuthors);code.clear();codeBytes=0;indexStatus="Код ещё не индексирован";refreshed=System.currentTimeMillis();writeCache();}
        progress.update(nextScripts.size()+" скриптов · "+nextAuthors.size()+" авторов"+(fail>0?" · недоступно: "+fail:""));
    }
    List<JSONObject> own()throws Exception{JSONObject me=api.identity();if(me==null)throw new IOException("Требуется вход");JSONArray list=api.request("GET","/users/"+me.getString("uuid")+"/scripts",null).getJSONArray("scripts");List<JSONObject> result=new ArrayList<>();for(int i=0;i<list.length();i++){JSONObject s=Core.publicScript(list.getJSONObject(i));s.put("author_uuid",me.getString("uuid"));if(s.optString("author_name").isEmpty())s.put("author_name",me.optString("name"));result.add(s);}return result;}
    String loadCode(String id)throws Exception{Core.requireUuid(id);String cached=code(id);if(cached!=null)return cached;JSONObject account=api.identity();if(account==null)throw new IOException("Требуется вход");String accountId=account.getString("uuid");String value=Core.clean(api.request("GET","/scripts/"+id+"/content",null).getString("content"));synchronized(this){if(api.authenticated()&&accountId.equals(api.identity().optString("uuid"))&&codeBytes+value.length()*2<=24*1024*1024){code.put(id,value);codeBytes+=value.length()*2;}}return value;}
    void index(Progress p)throws Exception{if(indexing)return;indexing=true;cancelIndex=false;List<JSONObject> list=scripts();ExecutorService pool=Executors.newFixedThreadPool(4);CompletionService<Exception> jobs=new ExecutorCompletionService<>(pool);int submitted=0,done=0,failed=0;try{for(JSONObject s:list){if(code(s.optString("uuid"))!=null)continue;String id=s.getString("uuid");jobs.submit(()->{if(cancelIndex)return null;try{loadCode(id);return null;}catch(Exception e){return e;}});submitted++;}while(done<submitted&&!cancelIndex){Exception error=jobs.take().get();done++;if(error!=null){if(error instanceof Api.Failure&&(((Api.Failure)error).status==401||((Api.Failure)error).status==429))throw error;failed++;}if(done%4==0||done==submitted){indexStatus="Код: "+done+" / "+submitted+(failed>0?" · недоступно: "+failed:"");p.update(indexStatus);}}indexStatus="Код: "+code.size()+" / "+list.size()+(cancelIndex?" · приостановлено":"")+(failed>0?" · недоступно: "+failed:"");p.update(indexStatus);}finally{cancelIndex=true;pool.shutdownNow();indexing=false;}}
    synchronized void forgetCode(String id){String previous=code.remove(id);if(previous!=null)codeBytes-=previous.length()*2;}
    synchronized void clearAccount(){cancelIndex=true;code.clear();codeBytes=0;indexStatus="Код ещё не индексирован";cacheAccount="";scripts.clear();authors.clear();refreshed=0;File file=new File(context.getFilesDir(),"public-catalog.json");if(file.exists())file.delete();}
    synchronized void bindAccount(String id){if(!cacheAccount.isEmpty()&&!cacheAccount.equals(id))clearAccount();cacheAccount=id;}
    private synchronized void readCache(){File file=new File(context.getFilesDir(),"public-catalog.json");try(FileInputStream in=new FileInputStream(file)){JSONObject o=new JSONObject(Api.read(in,8*1024*1024));cacheAccount=o.optString("account");JSONArray a=o.getJSONArray("authors"),s=o.getJSONArray("scripts");for(int i=0;i<a.length();i++){JSONObject v=Core.publicUser(a.getJSONObject(i));authors.put(v.getString("uuid"),v);}for(int i=0;i<s.length();i++){JSONObject v=Core.publicScript(s.getJSONObject(i));scripts.put(v.getString("uuid"),v);}refreshed=o.optLong("refreshed");}catch(Exception ignored){}}
    private synchronized void writeCache(){try{JSONObject o=new JSONObject().put("account",cacheAccount).put("refreshed",refreshed).put("authors",new JSONArray(authors.values())).put("scripts",new JSONArray(scripts.values()));File tmp=new File(context.getFilesDir(),"public-catalog.tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(o.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}if(!tmp.renameTo(new File(context.getFilesDir(),"public-catalog.json")))tmp.delete();}catch(Exception ignored){}}
}
