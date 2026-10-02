package gg.nulls.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Named battle configurations shared across scripts and account clients. */
final class BattlePresets {
    interface Reader { int[] read() throws Exception; }
    interface Apply { void apply(int[] values,Set<Integer> banned); }
    private final Activity activity;private final Localizer text;private final SharedPreferences prefs;private final int card,ink,accent;
    private final Reader reader;private final Set<Integer> banned;private final Apply apply;
    private Spinner chooser;private String selected="";private final List<String> ids=new ArrayList<>();
    BattlePresets(Activity activity,Localizer text,int card,int ink,int accent,Reader reader,Set<Integer> banned,Apply apply){this.activity=activity;this.text=text;this.card=card;this.ink=ink;this.accent=accent;this.reader=reader;this.banned=banned;this.apply=apply;prefs=activity.getSharedPreferences("presets",0);}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private JSONObject get(String id){try{return new JSONObject(prefs.getString(id,"{}"));}catch(Exception ignored){return new JSONObject();}}
    private void toast(String title){Toast.makeText(activity,text.t(title),Toast.LENGTH_SHORT).show();}
    private Button button(String title,Runnable clicked){Button b=new Button(activity);b.setText(text.t(title));b.setAllCaps(false);b.setTextColor(ink);b.setTextSize(13);GradientDrawable shape=new GradientDrawable();shape.setColor(card);shape.setCornerRadius(dp(16));b.setBackground(shape);b.setOnClickListener(v->clicked.run());return b;}
    View controls(){
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(0,dp(16),0,dp(16));TextView title=new TextView(activity);title.setText(text.t("Конфигурации боя"));title.setTextColor(ink);title.setTextSize(18);root.addView(title);
        chooser=new Spinner(activity);root.addView(chooser,new LinearLayout.LayoutParams(-1,dp(56)));reload();
        chooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int position,long id){if(position<0||position>=ids.size())return;String next=ids.get(position);if(next.equals(selected))return;selected=next;if(next.isEmpty())return;try{JSONObject config=get(next);JSONArray values=config.getJSONArray("values"),blocked=config.getJSONArray("brawlers");int[] settings=new int[32];if(values.length()!=32)throw new IllegalArgumentException();for(int i=0;i<32;i++)settings[i]=values.getInt(i);Core.battle(settings);Set<Integer> bans=new TreeSet<>();for(int i=0;i<blocked.length();i++)bans.add(blocked.getInt(i));apply.apply(settings,bans);}catch(Exception e){toast("Не удалось сохранить параметры");}}});
        String[] labels={"Сохранить конфигурацию","Создать конфигурацию","Переименовать","Удалить"};Runnable[] handlers={this::saveCurrent,()->nameDialog(false),()->nameDialog(true),this::delete};
        for(int row=0;row<2;row++){LinearLayout line=new LinearLayout(activity);for(int j=0;j<2;j++){int i=row*2+j;Button b=button(labels[i],handlers[i]);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(56),1);lp.setMargins(dp(3),dp(6),dp(3),0);line.addView(b,lp);}root.addView(line);}return root;
    }
    private void reload(){
        List<String> sorted=new ArrayList<>(prefs.getAll().keySet());sorted.removeIf(id->get(id).optString("name").isEmpty());sorted.sort((a,b)->Long.compare(get(b).optLong("updatedAt"),get(a).optLong("updatedAt")));ids.clear();ids.add("");ids.addAll(sorted);
        List<String> names=new ArrayList<>();names.add(text.t("Текущие параметры"));for(String id:sorted)names.add(get(id).optString("name"));if(!ids.contains(selected))selected="";
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_item,names){@Override public View getView(int position,View reused,android.view.ViewGroup parent){View v=super.getView(position,reused,parent);((TextView)v).setTextColor(ink);return v;}@Override public View getDropDownView(int position,View reused,android.view.ViewGroup parent){View v=super.getDropDownView(position,reused,parent);((TextView)v).setTextColor(ink);v.setBackgroundColor(card);return v;}};adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);chooser.setAdapter(adapter);chooser.setSelection(ids.indexOf(selected));
    }
    private boolean persist(String id,String name,boolean rename){try{JSONObject value=rename?get(id):new JSONObject();if(!rename){value.put("values",new JSONArray(reader.read()));value.put("brawlers",new JSONArray(new TreeSet<>(banned)));}value.put("name",name);value.put("updatedAt",System.currentTimeMillis());prefs.edit().putString(id,value.toString()).apply();selected=id;reload();toast("Конфигурация сохранена");return true;}catch(Exception e){toast("Не удалось сохранить параметры");return false;}}
    private void saveCurrent(){if(selected.isEmpty())nameDialog(false);else persist(selected,get(selected).optString("name"),false);}
    private void nameDialog(boolean rename){if(rename&&selected.isEmpty()){toast("Выберите конфигурацию");return;}String id=rename?selected:UUID.randomUUID().toString();EditText input=new EditText(activity);input.setSingleLine(true);input.setTextColor(ink);input.setText(rename?get(id).optString("name"):text.t("Конфигурация")+" "+(prefs.getAll().size()+1));input.selectAll();LinearLayout container=new LinearLayout(activity);container.setPadding(dp(24),dp(8),dp(24),0);container.addView(input,new LinearLayout.LayoutParams(-1,-2));AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(text.t(rename?"Переименовать":"Создать конфигурацию")).setView(container).setPositiveButton(text.t("Сохранить"),null).setNegativeButton(text.t("Отмена"),null).create();dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{String name=input.getText().toString().trim();if(name.isEmpty()||name.length()>80){input.setError(text.t("Название: от 1 до 80 символов"));return;}if(persist(id,name,rename))dialog.dismiss();}));dialog.show();}
    private void delete(){if(selected.isEmpty()){toast("Выберите конфигурацию");return;}String id=selected;new AlertDialog.Builder(activity).setTitle(text.t("Удалить конфигурацию?")).setMessage(get(id).optString("name")).setPositiveButton(text.t("Удалить"),(d,w)->{selected="";prefs.edit().remove(id).apply();reload();}).setNegativeButton(text.t("Отмена"),null).show();}
}
