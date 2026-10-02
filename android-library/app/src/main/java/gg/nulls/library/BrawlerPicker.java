package gg.nulls.library;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.*;

final class BrawlerPicker {
    static Set<Integer> load(Context context,String script){
        Set<Integer> result=new TreeSet<>();
        for(String value:context.getSharedPreferences("brawlers",0).getStringSet(script,Collections.emptySet()))try{result.add(Integer.parseInt(value));}catch(NumberFormatException ignored){}
        return result;
    }
    static void save(Context context,String script,Set<Integer> ids){Set<String> stored=new HashSet<>();for(int id:ids)stored.add(String.valueOf(id));context.getSharedPreferences("brawlers",0).edit().putStringSet(script,stored).apply();}
    private static JSONObject names(Context context,Localizer localizer)throws Exception{
        try(InputStream in=context.getAssets().open("brawlers.json")){JSONObject all=new JSONObject(Api.read(in,256*1024));String language=localizer.locale().toLanguageTag();if(!all.has(language))language=localizer.locale().getLanguage();if(language.equals("zh"))language="zh-Hans";if(language.equals("pt"))language="pt-BR";JSONObject fallback=all.getJSONObject("en"),localized=all.optJSONObject(language);if(localized!=null){Iterator<String> keys=localized.keys();while(keys.hasNext()){String key=keys.next();fallback.put(key,localized.get(key));}}return fallback;}
    }
    static void requireAvailable(Context context,Set<Integer> banned)throws Exception{
        try(InputStream in=context.getAssets().open("brawlers.json")){JSONObject all=new JSONObject(Api.read(in,256*1024)).getJSONObject("en");Iterator<String> ids=all.keys();while(ids.hasNext())if(!banned.contains(Integer.parseInt(ids.next())))return;throw new IllegalArgumentException("Оставьте хотя бы одного бойца");}
    }
    private static GradientDrawable shape(int color,int stroke,int radius){GradientDrawable result=new GradientDrawable();result.setColor(color);result.setCornerRadius(radius);if(stroke!=0)result.setStroke(2,stroke);return result;}
    static void show(Activity activity,Localizer localizer,Set<Integer> selected,int bg,int card,int ink,int muted,int accent,Runnable changed){
        final JSONObject names;try{names=names(activity,localizer);}catch(Exception ignored){Toast.makeText(activity,localizer.t("Не удалось выполнить"),Toast.LENGTH_SHORT).show();return;}
        float density=activity.getResources().getDisplayMetrics().density;int space=Math.round(16*density),portrait=Math.round(64*density);
        List<Integer> ids=new ArrayList<>();Iterator<String> keys=names.keys();while(keys.hasNext())ids.add(Integer.parseInt(keys.next()));Collections.sort(ids);List<Integer> shown=new ArrayList<>(ids);Map<Integer,Bitmap> images=new HashMap<>();
        Dialog dialog=new Dialog(activity);LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);root.setPadding(space,space,space,space);
        TextView heading=new TextView(activity);heading.setText(localizer.t("Заблокированные бойцы"));heading.setTextSize(22);heading.setTextColor(ink);root.addView(heading);
        TextView count=new TextView(activity);count.setTextColor(muted);count.setTextSize(14);count.setPadding(0,space/2,0,space);root.addView(count);
        Runnable update=()->{int blocked=0;for(int id:ids)if(selected.contains(id))blocked++;count.setText(localizer.t("Заблокировано")+": "+blocked+" / "+ids.size());changed.run();};update.run();
        EditText search=new EditText(activity);search.setSingleLine(true);search.setTextColor(ink);search.setHintTextColor(muted);search.setHint(localizer.t("Поиск бойца"));search.setPadding(space,space/2,space,space/2);search.setBackground(shape(card,0,space));root.addView(search,new LinearLayout.LayoutParams(-1,Math.round(50*density)));
        LinearLayout bulk=new LinearLayout(activity);root.addView(bulk);
        GridView grid=new GridView(activity);grid.setNumColumns(activity.getResources().getConfiguration().screenWidthDp>=480?4:3);grid.setHorizontalSpacing(space/2);grid.setVerticalSpacing(space/2);grid.setPadding(0,space,0,space);grid.setClipToPadding(false);
        BaseAdapter adapter=new BaseAdapter(){
            public int getCount(){return shown.size();}public Object getItem(int i){return shown.get(i);}public long getItemId(int i){return shown.get(i);}
            public View getView(int i,View reused,ViewGroup parent){
                int id=shown.get(i);boolean blocked=selected.contains(id);LinearLayout cell=reused instanceof LinearLayout?(LinearLayout)reused:new LinearLayout(activity);cell.removeAllViews();cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);cell.setPadding(space/2,space/2,space/2,space/2);cell.setBackground(shape(card,blocked?accent:0,space));
                FrameLayout art=new FrameLayout(activity);ImageView image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setAlpha(blocked?.55f:1f);
                {Bitmap bitmap=images.get(id);if(bitmap==null)try(InputStream in=activity.getAssets().open("brawler-icons/"+id+(id>=109?".png":".webp"))){bitmap=BitmapFactory.decodeStream(in);images.put(id,bitmap);}catch(Exception ignored){}image.setImageBitmap(bitmap);}
                FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(portrait,portrait,Gravity.CENTER);art.addView(image,ip);TextView badge=new TextView(activity);badge.setText(blocked?"−":"");badge.setTextColor(0xffffffff);badge.setTextSize(18);badge.setGravity(Gravity.CENTER);badge.setBackground(shape(blocked?accent:0x00000000,0,space));art.addView(badge,new FrameLayout.LayoutParams(space+space/2,space+space/2,Gravity.RIGHT|Gravity.TOP));cell.addView(art,new LinearLayout.LayoutParams(-1,portrait));
                TextView title=new TextView(activity);title.setText(names.optString(String.valueOf(id)));title.setTextColor(ink);title.setTextSize(12);title.setGravity(Gravity.CENTER);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);title.setPadding(0,space/2,0,0);cell.addView(title,new LinearLayout.LayoutParams(-1,Math.round(40*density)));
                cell.setContentDescription(title.getText()+", "+localizer.t(blocked?"Заблокирован":"Доступен"));cell.setFocusable(true);cell.setOnClickListener(v->{if(selected.contains(id))selected.remove(id);else selected.add(id);notifyDataSetChanged();update.run();});return cell;
            }
        };grid.setAdapter(adapter);
        for(String title:new String[]{"Заблокировать всех","Разблокировать всех"}){Button button=new Button(activity);button.setText(localizer.t(title));button.setTextSize(12);button.setAllCaps(false);button.setTextColor(ink);android.graphics.drawable.Drawable mark=activity.getDrawable(title.equals("Заблокировать всех")?R.drawable.nav_block:R.drawable.nav_check).mutate();mark.setTint(accent);mark.setBounds(0,0,Math.round(18*density),Math.round(18*density));button.setCompoundDrawables(mark,null,null,null);button.setCompoundDrawablePadding(space/2);button.setPadding(space/2,0,space/2,0);button.setBackground(shape(card,0,space));button.setOnClickListener(v->{if(title.equals("Заблокировать всех"))selected.addAll(ids);else selected.clear();adapter.notifyDataSetChanged();update.run();});LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(0,Math.round(52*density),1);layout.setMargins(space/4,space,space/4,0);bulk.addView(button,layout);}
        root.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void afterTextChanged(Editable text){String query=text.toString().toLowerCase(localizer.locale());shown.clear();for(int id:ids)if(names.optString(String.valueOf(id)).toLowerCase(localizer.locale()).contains(query))shown.add(id);adapter.notifyDataSetChanged();}public void onTextChanged(CharSequence s,int a,int b,int c){}});
        Button done=new Button(activity);done.setText(localizer.t("Готово"));done.setAllCaps(false);done.setTextColor(0xffffffff);done.setBackground(shape(accent,0,space));done.setOnClickListener(v->dialog.dismiss());root.addView(done,new LinearLayout.LayoutParams(-1,Math.round(50*density)));dialog.setContentView(root);dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-1);
    }
}
