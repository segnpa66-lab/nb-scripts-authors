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
        Localizer language=new Localizer(context);
        String account=api.authenticated()?api.identity().getString("uuid"):"";
        List<String> handles=Core.handles(Api.authorList());
        LinkedHashMap<String,JSONObject> nextScripts=new LinkedHashMap<>(),nextAuthors=new LinkedHashMap<>();int fail=0,n=0;
        ExecutorService pool=Executors.newFixedThreadPool(Math.min(10,Math.max(1,handles.size())));CompletionService<JSONObject> jobs=new ExecutorCompletionService<>(pool);
        try{for(String handle:handles)jobs.submit(()->{JSONObject author=Core.publicUser(api.request("GET","/users/@"+handle,null));Core.requireUuid(author.optString("uuid"));String id=author.getString("uuid");if(author.optString("username").isEmpty())author.put("username",handle);JSONArray list=api.request("GET","/users/"+id+"/scripts",null).optJSONArray("scripts");if(list==null)throw new IOException("Нет списка скриптов");return new JSONObject().put("author",author).put("scripts",list);});
            for(int job=0;job<handles.size();job++){try{JSONObject result=jobs.take().get();JSONObject author=result.getJSONObject("author");String id=author.getString("uuid");nextAuthors.put(id,author);JSONArray list=result.getJSONArray("scripts");for(int i=0;i<list.length();i++){JSONObject s=Core.publicScript(list.getJSONObject(i));Core.requireUuid(s.optString("uuid"));if(s.optString("published_at").isEmpty())continue;s.put("author_uuid",id);if(s.optString("author_name").isEmpty())s.put("author_name",author.optString("name"));nextScripts.put(s.getString("uuid"),s);}}catch(ExecutionException e){Throwable cause=e.getCause();if(cause instanceof Api.Failure&&(((Api.Failure)cause).status==401||((Api.Failure)cause).status==429))throw (Api.Failure)cause;fail++;}progress.update(language.t("Авторы ")+(++n)+" / "+handles.size());}
        }finally{pool.shutdownNow();}
        if(!handles.isEmpty()&&nextAuthors.isEmpty())throw new IOException("Не удалось получить авторов. Проверьте соединение.");
        String current=api.authenticated()?api.identity().optString("uuid"):"";
        if(!account.equals(current))throw new IOException("Аккаунт изменился. Обновите библиотеку заново.");
        synchronized(this){for(String id:new ArrayList<>(code.keySet())){JSONObject old=scripts.get(id),next=nextScripts.get(id);if(next==null||old==null||!old.optString("updated_at").equals(next.optString("updated_at")))forgetCode(id);}scripts.clear();scripts.putAll(nextScripts);authors.clear();authors.putAll(nextAuthors);code.keySet().retainAll(nextScripts.keySet());codeBytes=code.values().stream().mapToInt(value->value.length()*2).sum();indexStatus="Код ещё не индексирован";refreshed=System.currentTimeMillis();writeCache();}
        progress.update(nextScripts.size()+language.t(" скриптов · ")+nextAuthors.size()+language.t(" авторов")+(fail>0?language.t(" · недоступно: ")+fail:""));
    }
    List<JSONObject> own()throws Exception{JSONObject me=api.identity();if(me==null)throw new IOException("Требуется вход");JSONArray list=api.request("GET","/users/"+me.getString("uuid")+"/scripts",null).getJSONArray("scripts");List<JSONObject> result=new ArrayList<>();for(int i=0;i<list.length();i++){JSONObject s=Core.publicScript(list.getJSONObject(i));s.put("author_uuid",me.getString("uuid"));if(s.optString("author_name").isEmpty())s.put("author_name",me.optString("name"));result.add(s);}return result;}
    String loadCode(String id)throws Exception{Core.requireUuid(id);String cached=code(id);if(cached!=null)return cached;JSONObject account=api.identity();if(account==null)throw new IOException("Требуется вход");String accountId=account.getString("uuid");String value=Core.clean(api.request("GET","/scripts/"+id+"/content",null).getString("content"));synchronized(this){if(api.authenticated()&&accountId.equals(api.identity().optString("uuid"))&&codeBytes+value.length()*2<=24*1024*1024){code.put(id,value);codeBytes+=value.length()*2;}}return value;}
    void index(Progress p)throws Exception{
        if(indexing)return;Localizer language=new Localizer(context);indexing=true;cancelIndex=false;List<JSONObject> list=scripts();
        ExecutorService pool=Executors.newFixedThreadPool(12);CompletionService<Exception> jobs=new ExecutorCompletionService<>(pool);
        java.util.concurrent.atomic.AtomicLong cooldown=new java.util.concurrent.atomic.AtomicLong();int submitted=0,done=0,failed=0;
        try{
            for(JSONObject item:list){String id=item.getString("uuid");if(code(id)!=null)continue;jobs.submit(()->{
                for(int attempt=0;attempt<3&&!cancelIndex;attempt++){
                    long pause=cooldown.get()-System.currentTimeMillis();if(pause>0)Thread.sleep(pause);
                    try{loadCode(id);return null;}catch(Api.Failure error){
                        if(error.status==429||error.status==502||error.status==503){cooldown.accumulateAndGet(System.currentTimeMillis()+1000L*(attempt+1),Math::max);if(attempt<2)continue;}
                        return error;
                    }catch(Exception error){return error;}
                }return null;
            });submitted++;}
            while(done<submitted&&!cancelIndex){Exception error=jobs.take().get();done++;if(error!=null){if(error instanceof Api.Failure&&(((Api.Failure)error).status==401||((Api.Failure)error).status==429))throw error;failed++;}
                if(done%12==0||done==submitted){indexStatus=language.t("Код: ")+(list.size()-submitted+done)+" / "+list.size()+(failed>0?language.t(" · недоступно: ")+failed:"");p.update(indexStatus);}}
            indexStatus=language.t("Код: ")+code.size()+" / "+list.size()+(cancelIndex?language.t(" · приостановлено"):"")+(failed>0?language.t(" · недоступно: ")+failed:"");p.update(indexStatus);
        }finally{cancelIndex=true;pool.shutdownNow();indexing=false;writeCodeCache();}
    }
    private synchronized void readCodeCache(){
        try(FileInputStream input=new FileInputStream(new File(context.getFilesDir(),"indexed-code.json"))){JSONObject saved=new JSONObject(Api.read(input,32*1024*1024));if(!cacheAccount.equals(saved.optString("account"))||cacheAccount.isEmpty())return;JSONArray rows=saved.getJSONArray("rows");for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i),script=scripts.get(row.optString("uuid"));String content=row.optString("content");if(script!=null&&script.optString("updated_at").equals(row.optString("updated_at"))&&codeBytes+content.length()*2<=24*1024*1024){code.put(row.getString("uuid"),content);codeBytes+=content.length()*2;}}}catch(Exception ignored){}
    }
    private synchronized void writeCodeCache(){
        if(cacheAccount.isEmpty())return;try{JSONArray rows=new JSONArray();for(Map.Entry<String,String> entry:code.entrySet()){JSONObject script=scripts.get(entry.getKey());if(script!=null)rows.put(new JSONObject().put("uuid",entry.getKey()).put("updated_at",script.optString("updated_at")).put("content",entry.getValue()));}JSONObject saved=new JSONObject().put("account",cacheAccount).put("rows",rows);File temporary=new File(context.getFilesDir(),"indexed-code.tmp");try(FileOutputStream output=new FileOutputStream(temporary)){output.write(saved.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}if(!temporary.renameTo(new File(context.getFilesDir(),"indexed-code.json")))temporary.delete();}catch(Exception ignored){}
    }
    void flushCodeCache(){writeCodeCache();}
    synchronized void forgetCode(String id){String previous=code.remove(id);if(previous!=null)codeBytes-=previous.length()*2;}
    synchronized void clearAccount(){cancelIndex=true;code.clear();codeBytes=0;indexStatus="Код ещё не индексирован";cacheAccount="";scripts.clear();authors.clear();refreshed=0;File file=new File(context.getFilesDir(),"public-catalog.json");if(file.exists())file.delete();new File(context.getFilesDir(),"indexed-code.json").delete();}
    synchronized void bindAccount(String id){if(!cacheAccount.isEmpty()&&!cacheAccount.equals(id))clearAccount();cacheAccount=id;if(code.isEmpty())readCodeCache();}
    private synchronized void readCache(){File file=new File(context.getFilesDir(),"public-catalog.json");try(FileInputStream in=new FileInputStream(file)){JSONObject o=new JSONObject(Api.read(in,8*1024*1024));cacheAccount=o.optString("account");JSONArray a=o.getJSONArray("authors"),s=o.getJSONArray("scripts");for(int i=0;i<a.length();i++){JSONObject v=Core.publicUser(a.getJSONObject(i));authors.put(v.getString("uuid"),v);}for(int i=0;i<s.length();i++){JSONObject v=Core.publicScript(s.getJSONObject(i));scripts.put(v.getString("uuid"),v);}refreshed=o.optLong("refreshed");}catch(Exception ignored){}}
    private synchronized void writeCache(){try{JSONObject o=new JSONObject().put("account",cacheAccount).put("refreshed",refreshed).put("authors",new JSONArray(authors.values())).put("scripts",new JSONArray(scripts.values()));File tmp=new File(context.getFilesDir(),"public-catalog.tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(o.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}if(!tmp.renameTo(new File(context.getFilesDir(),"public-catalog.json")))tmp.delete();}catch(Exception ignored){}}
}
