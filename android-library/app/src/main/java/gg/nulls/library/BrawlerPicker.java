package gg.nulls.library;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
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
        try(InputStream in=context.getAssets().open("brawlers.json")){JSONObject all=new JSONObject(Api.read(in,256*1024));String language=localizer.locale().toLanguageTag();if(!all.has(language))language=localizer.locale().getLanguage();if(language.equals("zh"))language="zh-Hans";if(language.equals("pt"))language="pt-BR";return all.optJSONObject(language)==null?all.getJSONObject("en"):all.getJSONObject(language);}
    }
    static void requireAvailable(Context context,Set<Integer> banned)throws Exception{
        try(InputStream in=context.getAssets().open("brawlers.json")){JSONObject all=new JSONObject(Api.read(in,256*1024)).getJSONObject("en");int available=0;Iterator<String> ids=all.keys();while(ids.hasNext())if(!banned.contains(Integer.parseInt(ids.next())))available++;if(available==0)throw new IllegalArgumentException("Оставьте хотя бы одного бойца");}
    }
    static void show(Activity activity,Localizer localizer,Set<Integer> selected,int bg,int card,int ink,int muted,int accent,Runnable changed){
        final JSONObject names;try{names=names(activity,localizer);}catch(Exception ignored){return;}
        float density=activity.getResources().getDisplayMetrics().density;int space=Math.round(16*density);
        List<Integer> ids=new ArrayList<>();Iterator<String> keys=names.keys();while(keys.hasNext())ids.add(Integer.parseInt(keys.next()));Collections.sort(ids);List<Integer> shown=new ArrayList<>(ids);
        Dialog dialog=new Dialog(activity);LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);root.setPadding(space,space,space,space);
        TextView heading=new TextView(activity);heading.setText(localizer.t("Заблокированные бойцы"));heading.setTextSize(22);heading.setTextColor(ink);root.addView(heading);
        TextView hint=new TextView(activity);hint.setText(localizer.t("Отмеченные бойцы недоступны в этом бою."));hint.setTextColor(muted);hint.setPadding(0,space,0,space);root.addView(hint);
        EditText search=new EditText(activity);search.setSingleLine(true);search.setTextColor(ink);search.setHintTextColor(muted);search.setHint(localizer.t("Поиск бойца"));root.addView(search);
        ListView list=new ListView(activity);list.setDivider(null);
        BaseAdapter adapter=new BaseAdapter(){public int getCount(){return shown.size();}public Object getItem(int i){return shown.get(i);}public long getItemId(int i){return shown.get(i);}public View getView(int i,View reused,android.view.ViewGroup parent){CheckBox check=reused instanceof CheckBox?(CheckBox)reused:new CheckBox(activity);int id=shown.get(i);check.setOnCheckedChangeListener(null);check.setText(names.optString(String.valueOf(id)));check.setTextColor(ink);check.setTextSize(16);check.setPadding(space,space,space,space);check.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));check.setChecked(selected.contains(id));check.setOnCheckedChangeListener((view,on)->{if(on)selected.add(id);else selected.remove(id);changed.run();});return check;}};
        list.setAdapter(adapter);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void afterTextChanged(Editable text){String query=text.toString().toLowerCase(localizer.locale());shown.clear();for(int id:ids)if(names.optString(String.valueOf(id)).toLowerCase(localizer.locale()).contains(query))shown.add(id);adapter.notifyDataSetChanged();}public void onTextChanged(CharSequence s,int a,int b,int c){}});
        LinearLayout actions=new LinearLayout(activity);actions.setGravity(Gravity.END);for(String title:new String[]{"Разблокировать всех","Готово"}){Button button=new Button(activity);button.setText(localizer.t(title));button.setAllCaps(false);button.setTextColor(ink);GradientDrawable shape=new GradientDrawable();shape.setColor(card);shape.setCornerRadius(space);button.setBackground(shape);button.setPadding(space,space,space,space);button.setOnClickListener(v->{if(title.equals("Готово"))dialog.dismiss();else{selected.clear();adapter.notifyDataSetChanged();changed.run();}});LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(0,-2,1);layout.setMargins(space/4,space/2,space/4,0);actions.addView(button,layout);}root.addView(actions);dialog.setContentView(root);dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-1);
    }
}
