package gg.nulls.library;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.*;

/** Literal, case-insensitive search shared by the viewer and editor. */
final class CodeSearch extends LinearLayout {
    private final TextView code,stats,count;
    private final EditText query;
    private final ImageButton up,down;
    private final Function<String,String> translate;
    private final int accent,soft;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private int[] starts=new int[64],ends=new int[64];
    private int size=0,current=-1;
    private boolean painting=false,caseSensitive=false;
    private android.app.Dialog lineDialog;
    private final Runnable refreshTask=()->refresh(false,false);
    private final Runnable queryTask=()->refresh(true,true);
    private static final class Hit extends BackgroundColorSpan {Hit(int color){super(color);}}

    CodeSearch(Context context,TextView code,Function<String,String> translate,int ink,int muted,int surface,int line,int accent,int soft){
        super(context);this.code=code;this.translate=translate;this.accent=accent;this.soft=soft;setOrientation(VERTICAL);setPadding(0,dp(10),0,dp(12));
        stats=new TextView(context);stats.setTextColor(muted);stats.setTextSize(12);stats.setPadding(0,0,0,dp(8));LinearLayout metrics=new LinearLayout(context);metrics.setGravity(Gravity.CENTER_VERTICAL);metrics.addView(stats,new LayoutParams(0,-2,1));ImageButton go=arrow(context,R.drawable.nav_code,"Перейти к строке",ink,surface,line);metrics.addView(go,new LayoutParams(dp(34),dp(34)));addView(metrics);go.setOnClickListener(v->goToLine(context,ink,muted,surface,line));
        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);addView(row,new LayoutParams(-1,dp(44)));
        query=new EditText(context);query.setSingleLine(true);query.setTextSize(13);query.setTextColor(ink);query.setHintTextColor(muted);query.setHint(translate.apply("Поиск в коде"));query.setContentDescription(translate.apply("Поиск в коде"));query.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);query.setPadding(dp(12),0,dp(12),0);query.setBackground(background(surface,line));row.addView(query,new LayoutParams(0,-1,1));
        ImageButton casing=arrow(context,R.drawable.nav_match_case,"Учитывать регистр",ink,surface,line);casing.setTooltipText(translate.apply("Учитывать регистр"));LayoutParams caseSpace=new LayoutParams(dp(34),dp(38));caseSpace.leftMargin=dp(6);row.addView(casing,caseSpace);casing.setOnClickListener(v->{caseSensitive=!caseSensitive;casing.setSelected(caseSensitive);casing.setBackground(background(caseSensitive?soft:surface,caseSensitive?accent:line));refresh(true,true);});
        count=new TextView(context);count.setTextColor(muted);count.setTextSize(12);count.setGravity(Gravity.CENTER);row.addView(count,new LayoutParams(dp(64),-1));
        up=arrow(context,R.drawable.nav_up,"Предыдущее совпадение",ink,surface,line);down=arrow(context,R.drawable.nav_down,"Следующее совпадение",ink,surface,line);row.addView(up,new LayoutParams(dp(38),dp(38)));LayoutParams gap=new LayoutParams(dp(38),dp(38));gap.leftMargin=dp(6);row.addView(down,gap);
        up.setOnClickListener(v->step(-1));down.setOnClickListener(v->step(1));
        query.addTextChangedListener(watcher(()->{handler.removeCallbacks(queryTask);handler.postDelayed(queryTask,120);}));
        query.setOnEditorActionListener((v,action,event)->{refresh(false,false);step(1);return true;});
        if(code instanceof EditText)((EditText)code).addTextChangedListener(watcher(()->{if(!painting){handler.removeCallbacks(refreshTask);handler.postDelayed(refreshTask,120);}}));
        refresh(true,false);
    }
    private TextWatcher watcher(Runnable changed){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){changed.run();}public void afterTextChanged(Editable value){}};}
    private ImageButton arrow(Context context,int icon,String label,int ink,int surface,int line){ImageButton button=new ImageButton(context);button.setImageResource(icon);button.setColorFilter(ink);button.setPadding(dp(9),dp(9),dp(9),dp(9));button.setBackground(background(surface,line));button.setContentDescription(translate.apply(label));return button;}
    private GradientDrawable background(int surface,int line){GradientDrawable value=new GradientDrawable();value.setColor(surface);value.setCornerRadius(dp(12));value.setStroke(dp(1),line);return value;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void refresh(boolean reset,boolean reveal){
        CharSequence text=code.getText();int prior=current>=0&&current<size?starts[current]:0;size=0;
        int lines=1;for(int i=0;i<text.length();i++)if(text.charAt(i)=='\n')lines++;
        stats.setText(translate.apply("Символов")+": "+Character.codePointCount(text,0,text.length())+" · "+translate.apply("Строк")+": "+lines);
        String term=query.getText().toString();if(!term.isEmpty()){Matcher matcher=Pattern.compile(Pattern.quote(term),caseSensitive?0:Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(text);while(matcher.find()){if(size==starts.length){starts=Arrays.copyOf(starts,size*2);ends=Arrays.copyOf(ends,size*2);}starts[size]=matcher.start();ends[size]=matcher.end();size++;}}
        current=size==0?-1:0;if(!reset&&size>0)while(current<size-1&&starts[current]<prior)current++;
        paint();if(reveal)reveal();
    }
    private void paint(){
        painting=true;CharSequence raw=code.getText();Spannable text=raw instanceof Spannable?(Spannable)raw:new SpannableString(raw);
        for(Hit hit:text.getSpans(0,text.length(),Hit.class))text.removeSpan(hit);
        // Keep span work bounded for very large files; every occurrence remains navigable.
        int from=current>=2000?Math.max(0,current-1000):0,to=Math.min(size,from+2000);
        for(int i=from;i<to;i++)text.setSpan(new Hit(i==current?accent:soft),starts[i],ends[i],Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if(!(raw instanceof Spannable))code.setText(text,TextView.BufferType.SPANNABLE);
        count.setText((current<0?0:current+1)+" / "+size);count.setContentDescription(translate.apply("Совпадения")+": "+count.getText());up.setEnabled(size>0);down.setEnabled(size>0);up.setAlpha(size>0?1f:.35f);down.setAlpha(size>0?1f:.35f);painting=false;
    }
    private void step(int direction){handler.removeCallbacks(queryTask);handler.removeCallbacks(refreshTask);refresh(false,false);if(size==0)return;current=(current+direction+size)%size;paint();reveal();}
    private void reveal(){if(current<0)return;revealOffset(starts[current],ends[current]);}
    private void revealOffset(final int offset,final int end){code.post(()->{
        android.text.Layout layout=code.getLayout();if(layout==null)return;
        if(code instanceof EditText)((EditText)code).setSelection(offset,end);
        int line=layout.getLineForOffset(Math.min(offset,code.length()));Rect rect=new Rect(code.getTotalPaddingLeft(),layout.getLineTop(line)+code.getTotalPaddingTop(),code.getWidth()-code.getTotalPaddingRight(),layout.getLineBottom(line)+code.getTotalPaddingTop());
        ViewParent parent=code.getParent();while(parent!=null){if(parent instanceof ScrollView){ScrollView scroll=(ScrollView)parent;scroll.offsetDescendantRectToMyCoords(code,rect);scroll.smoothScrollTo(0,Math.max(0,rect.top-scroll.getHeight()/3));break;}parent=parent.getParent();}
    });}
    private void goToLine(Context context,int ink,int muted,int surface,int line){String text=code.getText().toString();int lines=1;for(int i=0;i<text.length();i++)if(text.charAt(i)==10)lines++;final int max=lines;android.app.Dialog dialog=new android.app.Dialog(context);lineDialog=dialog;LinearLayout panel=new LinearLayout(context);panel.setOrientation(VERTICAL);panel.setPadding(dp(20),dp(20),dp(20),dp(20));GradientDrawable shape=background(surface,line);shape.setCornerRadius(dp(24));panel.setBackground(shape);TextView title=new TextView(context);title.setText(translate.apply("Перейти к строке"));title.setTextSize(21);title.setTextColor(ink);panel.addView(title);TextView hint=new TextView(context);hint.setText(translate.apply("Номер строки")+" · 1–"+max);hint.setTextColor(muted);hint.setPadding(0,dp(12),0,dp(8));panel.addView(hint);EditText number=new EditText(context);number.setTextColor(ink);number.setText("1");number.setSingleLine(true);number.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);number.setPadding(dp(12),dp(8),dp(12),dp(8));number.setBackground(background(surface,line));panel.addView(number,new LayoutParams(-1,dp(48)));LinearLayout buttons=new LinearLayout(context);buttons.setGravity(Gravity.END);for(boolean open:new boolean[]{false,true}){Button button=new Button(context);button.setText(translate.apply(open?"Открыть":"Отмена"));button.setTextColor(ink);button.setBackground(background(surface,line));LayoutParams space=new LayoutParams(0,dp(44),1);space.topMargin=dp(14);space.leftMargin=open?dp(8):0;buttons.addView(button,space);button.setOnClickListener(v->{if(!open){dialog.dismiss();return;}try{int target=Integer.parseInt(number.getText().toString());if(target<1||target>max)throw new NumberFormatException();String value=code.getText().toString();int offset=0;for(int n=1;n<target;n++){int next=value.indexOf((char)10,offset);if(next<0)throw new NumberFormatException();offset=next+1;}dialog.dismiss();if(code instanceof EditText)code.requestFocus();revealOffset(offset,offset);}catch(NumberFormatException error){number.setError("1–"+max);}});}panel.addView(buttons);dialog.setContentView(panel);dialog.show();Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(420)),-2);window.setDimAmount(.6f);}dialog.setOnDismissListener(d->{if(lineDialog==dialog)lineDialog=null;});number.selectAll();}
    @Override protected void onDetachedFromWindow(){if(lineDialog!=null)lineDialog.dismiss();handler.removeCallbacks(refreshTask);handler.removeCallbacks(queryTask);super.onDetachedFromWindow();}
}
