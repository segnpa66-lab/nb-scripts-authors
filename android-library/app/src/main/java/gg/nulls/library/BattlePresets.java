package gg.nulls.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Gravity;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
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
    private android.graphics.drawable.Drawable surface(){GradientDrawable shape=new GradientDrawable();shape.setColor(card);shape.setCornerRadius(dp(16));shape.setStroke(dp(1),(ink&0x00ffffff)|0x18000000);GradientDrawable mask=new GradientDrawable();mask.setColor(0xffffffff);mask.setCornerRadius(dp(16));return new RippleDrawable(ColorStateList.valueOf((accent&0x00ffffff)|0x26000000),shape,mask);}
    private Drawable icon(int resource){Drawable d=activity.getDrawable(resource).mutate();d.setTint(accent);d.setBounds(0,0,dp(22),dp(22));return d;}
    private ImageButton action(String title,int resource,Runnable click){ImageButton button=new ImageButton(activity);button.setImageDrawable(icon(resource));button.setScaleType(ImageView.ScaleType.CENTER);button.setBackground(surface());button.setPadding(0,0,0,0);button.setContentDescription(text.t(title));button.setTooltipText(text.t(title));button.setOnClickListener(v->click.run());return button;}
    View controls(){
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(0,dp(8),0,dp(16));
        Button save=new Button(activity);save.setText(text.t("Сохранить конфигурацию"));save.setAllCaps(false);save.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));save.setTextSize(14);save.setTextColor(ink);save.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);save.setCompoundDrawables(icon(R.drawable.nav_save),null,null,null);save.setCompoundDrawablePadding(dp(12));save.setPadding(dp(16),0,dp(16),0);save.setBackground(surface());save.setStateListAnimator(null);save.setOnClickListener(v->saveCurrent());root.addView(save,new LinearLayout.LayoutParams(-1,dp(50)));
        TextView title=new TextView(activity);title.setText(text.t("Конфигурации боя"));title.setTextColor(ink);title.setTextSize(14);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setPadding(0,dp(16),0,dp(8));root.addView(title);
        LinearLayout line=new LinearLayout(activity);line.setGravity(Gravity.CENTER_VERTICAL);chooser=new Spinner(activity);chooser.setBackground(surface());chooser.setPadding(dp(8),0,dp(4),0);line.addView(chooser,new LinearLayout.LayoutParams(0,dp(50),1));
        String[] labels={"Создать конфигурацию","Переименовать","Удалить"};int[] icons={R.drawable.nav_plus,R.drawable.nav_edit,R.drawable.nav_trash};Runnable[] handlers={()->nameDialog(false),()->nameDialog(true),this::delete};
        for(int i=0;i<3;i++){LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(dp(44),dp(50));layout.leftMargin=dp(6);line.addView(action(labels[i],icons[i],handlers[i]),layout);}root.addView(line);
        initialize();reload();applySelected();
        chooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int position,long id){if(position<0||position>=ids.size())return;String next=ids.get(position);if(next.equals(selected))return;selected=next;applySelected();}});
        return root;
    }
    private void initialize(){List<String> stored=new ArrayList<>(prefs.getAll().keySet());stored.removeIf(id->get(id).optString("name").isEmpty());stored.sort((a,b)->Long.compare(get(b).optLong("updatedAt"),get(a).optLong("updatedAt")));if(!stored.isEmpty()){selected=stored.get(0);return;}try{JSONObject config=new JSONObject();config.put("name",text.t("Конфигурация")+" 1");config.put("values",new JSONArray(reader.read()));config.put("brawlers",new JSONArray(new TreeSet<>(banned)));config.put("updatedAt",System.currentTimeMillis());selected=UUID.randomUUID().toString();prefs.edit().putString(selected,config.toString()).apply();}catch(Exception ignored){selected="";}}
    private void applySelected(){if(selected.isEmpty())return;try{JSONObject config=get(selected);JSONArray values=config.getJSONArray("values"),blocked=config.getJSONArray("brawlers");int[] settings=new int[32];if(values.length()!=32)throw new IllegalArgumentException();for(int i=0;i<32;i++)settings[i]=values.getInt(i);Core.battle(settings);Set<Integer> bans=new TreeSet<>();for(int i=0;i<blocked.length();i++)bans.add(blocked.getInt(i));apply.apply(settings,bans);}catch(Exception e){toast("Не удалось сохранить параметры");}}
    private void reload(){
        List<String> sorted=new ArrayList<>(prefs.getAll().keySet());sorted.removeIf(id->get(id).optString("name").isEmpty());sorted.sort((a,b)->Long.compare(get(b).optLong("updatedAt"),get(a).optLong("updatedAt")));ids.clear();ids.addAll(sorted);if(ids.isEmpty())ids.add("");
        List<String> names=new ArrayList<>();if(sorted.isEmpty())names.add(text.t("Конфигурация")+" 1");for(String id:sorted)names.add(get(id).optString("name"));if(!ids.contains(selected))selected=ids.get(0);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_item,names){@Override public View getView(int position,View reused,android.view.ViewGroup parent){View v=super.getView(position,reused,parent);((TextView)v).setTextColor(ink);((TextView)v).setSingleLine(true);((TextView)v).setEllipsize(android.text.TextUtils.TruncateAt.END);((TextView)v).setCompoundDrawables(null,null,icon(R.drawable.nav_chevron),null);return v;}@Override public View getDropDownView(int position,View reused,android.view.ViewGroup parent){View v=super.getDropDownView(position,reused,parent);((TextView)v).setTextColor(ink);v.setBackgroundColor(card);return v;}};adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);chooser.setAdapter(adapter);chooser.setSelection(ids.indexOf(selected));
    }
    private boolean persist(String id,String name,boolean rename){try{JSONObject value=rename?get(id):new JSONObject();if(!rename){value.put("values",new JSONArray(reader.read()));value.put("brawlers",new JSONArray(new TreeSet<>(banned)));}value.put("name",name);value.put("updatedAt",System.currentTimeMillis());prefs.edit().putString(id,value.toString()).apply();selected=id;reload();toast("Конфигурация сохранена");return true;}catch(Exception e){toast("Не удалось сохранить параметры");return false;}}
    private void saveCurrent(){if(selected.isEmpty())persist(UUID.randomUUID().toString(),text.t("Конфигурация")+" 1",false);else persist(selected,get(selected).optString("name"),false);}
    private void nameDialog(boolean rename){if(rename&&selected.isEmpty()){toast("Выберите конфигурацию");return;}String id=rename?selected:UUID.randomUUID().toString();EditText input=new EditText(activity);input.setSingleLine(true);input.setTextColor(ink);input.setText(rename?get(id).optString("name"):text.t("Конфигурация")+" "+(prefs.getAll().size()+1));input.selectAll();LinearLayout container=new LinearLayout(activity);container.setPadding(dp(24),dp(8),dp(24),0);container.addView(input,new LinearLayout.LayoutParams(-1,-2));AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(text.t(rename?"Переименовать":"Создать конфигурацию")).setView(container).setPositiveButton(text.t("Сохранить"),null).setNegativeButton(text.t("Отмена"),null).create();dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{String name=input.getText().toString().trim();if(name.isEmpty()||name.length()>80){input.setError(text.t("Название: от 1 до 80 символов"));return;}if(persist(id,name,rename))dialog.dismiss();}));dialog.show();}
    private void delete(){if(selected.isEmpty()){toast("Выберите конфигурацию");return;}String id=selected;new AlertDialog.Builder(activity).setTitle(text.t("Удалить конфигурацию?")).setMessage(get(id).optString("name")).setPositiveButton(text.t("Удалить"),(d,w)->{selected="";prefs.edit().remove(id).apply();reload();applySelected();}).setNegativeButton(text.t("Отмена"),null).show();}
}
