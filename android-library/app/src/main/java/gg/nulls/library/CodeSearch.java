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
    private boolean painting=false;
    private final Runnable refreshTask=()->refresh(false,false);
    private final Runnable queryTask=()->refresh(true,true);
    private static final class Hit extends BackgroundColorSpan {Hit(int color){super(color);}}

    CodeSearch(Context context,TextView code,Function<String,String> translate,int ink,int muted,int surface,int line,int accent,int soft){
        super(context);this.code=code;this.translate=translate;this.accent=accent;this.soft=soft;setOrientation(VERTICAL);setPadding(0,dp(10),0,dp(12));
        stats=new TextView(context);stats.setTextColor(muted);stats.setTextSize(12);stats.setPadding(0,0,0,dp(8));addView(stats);
        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);addView(row,new LayoutParams(-1,dp(44)));
        query=new EditText(context);query.setSingleLine(true);query.setTextSize(13);query.setTextColor(ink);query.setHintTextColor(muted);query.setHint(translate.apply("Поиск в коде"));query.setContentDescription(translate.apply("Поиск в коде"));query.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);query.setPadding(dp(12),0,dp(12),0);query.setBackground(background(surface,line));row.addView(query,new LayoutParams(0,-1,1));
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
        String term=query.getText().toString();if(!term.isEmpty()){Matcher matcher=Pattern.compile(Pattern.quote(term),Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(text);while(matcher.find()){if(size==starts.length){starts=Arrays.copyOf(starts,size*2);ends=Arrays.copyOf(ends,size*2);}starts[size]=matcher.start();ends[size]=matcher.end();size++;}}
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
    private void reveal(){if(current<0)return;final int offset=starts[current],end=ends[current];code.post(()->{
        android.text.Layout layout=code.getLayout();if(layout==null)return;
        if(code instanceof EditText)((EditText)code).setSelection(offset,end);
        int line=layout.getLineForOffset(Math.min(offset,code.length()));Rect rect=new Rect(code.getTotalPaddingLeft(),layout.getLineTop(line)+code.getTotalPaddingTop(),code.getWidth()-code.getTotalPaddingRight(),layout.getLineBottom(line)+code.getTotalPaddingTop());
        ViewParent parent=code.getParent();while(parent!=null){if(parent instanceof ScrollView){ScrollView scroll=(ScrollView)parent;scroll.offsetDescendantRectToMyCoords(code,rect);scroll.smoothScrollTo(0,Math.max(0,rect.top-scroll.getHeight()/3));break;}parent=parent.getParent();}
    });}
    @Override protected void onDetachedFromWindow(){handler.removeCallbacks(refreshTask);handler.removeCallbacks(queryTask);super.onDetachedFromWindow();}
}
