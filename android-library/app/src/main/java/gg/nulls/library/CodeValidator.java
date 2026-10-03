package gg.nulls.library;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.*;
import java.io.ByteArrayInputStream;
import java.util.function.Consumer;
import org.json.JSONObject;

/** Local compilation only: script functions are never called and network requests are denied. */
final class CodeValidator {
    private final Context context;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private WebView view;
    private Consumer<JSONObject> callback;
    private String source;
    private final Runnable timeout=()->finish(result("unavailable"));
    CodeValidator(Context context){this.context=context;}
    void check(String source,Consumer<JSONObject> callback){
        close();this.callback=callback;this.source=source;
        if(source.length()>1000000){finish(result("size"));return;}
        view=new WebView(context);view.getSettings().setJavaScriptEnabled(true);view.getSettings().setAllowFileAccess(false);view.getSettings().setAllowContentAccess(false);view.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);view.addJavascriptInterface(new Bridge(view),"AndroidValidator");
        view.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return true;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                Uri uri=request.getUrl();String path=uri.getPath();
                if("https".equals(uri.getScheme())&&"validator.local".equals(uri.getHost())&&path!=null&&path.startsWith("/validator/")&&!path.contains(".."))try{return new WebResourceResponse(path.endsWith(".js")?"text/javascript":"text/html","UTF-8",context.getAssets().open(path.substring(1)));}catch(Exception ignored){}
                return new WebResourceResponse("text/plain","UTF-8",403,"Forbidden",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
            }
        });
        ui.postDelayed(timeout,10000);view.loadUrl("https://validator.local/validator/index.html");
    }
    private final class Bridge {
        private final WebView owner;
        Bridge(WebView owner){this.owner=owner;}
        @JavascriptInterface public void ready(){ui.post(()->{if(view==owner&&callback!=null)owner.evaluateJavascript("window.validateSource("+JSONObject.quote(source)+").catch(()=>AndroidValidator.result('{\"valid\":false,\"reason\":\"unavailable\"}'))",null);});}
        @JavascriptInterface public void result(String value){ui.post(()->{if(view!=owner||callback==null)return;try{finish(new JSONObject(value));}catch(Exception error){finish(CodeValidator.result("unavailable"));}});}
    }
    private static JSONObject result(String reason){JSONObject result=new JSONObject();try{result.put("valid",false).put("reason",reason);}catch(Exception ignored){}return result;}
    private void finish(JSONObject result){Consumer<JSONObject> listener=callback;close();if(listener!=null)listener.accept(result);}
    void close(){ui.removeCallbacks(timeout);callback=null;source=null;if(view!=null){WebView old=view;view=null;old.stopLoading();old.removeJavascriptInterface("AndroidValidator");old.destroy();}}
}
