package gg.nulls.library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Arrays;

/** Draws logical line numbers without changing code, clipboard text or search offsets. */
final class LineNumbers {
    private final TextView view;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect clip=new Rect();
    private final int color,background,baseLeft;
    private int[] starts=new int[64];
    private int size,width;
    private LineNumbers(TextView view,int color,int background){this.view=view;this.color=color;this.background=background;this.baseLeft=view.getPaddingLeft();changed();}
    private int dp(int n){return Math.round(n*view.getResources().getDisplayMetrics().density);}
    private void changed(){
        CharSequence text=view.getText();size=1;starts[0]=0;
        for(int i=0;i<text.length();i++)if(text.charAt(i)=='\n'){if(size==starts.length)starts=Arrays.copyOf(starts,size*2);starts[size++]=i+1;}
        paint.setTypeface(Typeface.MONOSPACE);paint.setTextSize(view.getTextSize()*.9f);paint.setTextAlign(Paint.Align.RIGHT);
        width=(int)Math.ceil(paint.measureText(String.valueOf(Math.max(999,size))))+dp(18);
        view.setPadding(baseLeft+width,view.getPaddingTop(),view.getPaddingRight(),view.getPaddingBottom());view.invalidate();
    }
    private void draw(Canvas canvas){
        Layout layout=view.getLayout();if(layout==null||!canvas.getClipBounds(clip))return;
        int layoutLine=layout.getLineForVertical(Math.max(0,clip.top-view.getTotalPaddingTop()));int offset=layout.getLineStart(layoutLine),first=Arrays.binarySearch(starts,0,size,offset);if(first<0)first=Math.max(0,-first-2);
        float edge=view.getScrollX()+baseLeft+width-dp(4);paint.setColor(background);canvas.drawRect(view.getScrollX(),clip.top,edge,clip.bottom,paint);paint.setColor(color);
        for(int i=first;i<size;i++){int line=layout.getLineForOffset(starts[i]);float top=layout.getLineTop(line)+view.getTotalPaddingTop();if(top>clip.bottom)break;float baseline=layout.getLineBaseline(line)+view.getTotalPaddingTop();if(baseline>=clip.top)canvas.drawText(String.valueOf(i+1),edge-dp(8),baseline,paint);}
        paint.setAlpha(60);canvas.drawLine(edge,clip.top,edge,clip.bottom,paint);paint.setAlpha(255);
    }
    static final class Viewer extends TextView {
        private LineNumbers numbers;
        Viewer(Context context){super(context);setTextDirection(TEXT_DIRECTION_LTR);}
        void configureNumbers(int color,int background){numbers=new LineNumbers(this,color,background);}
        @Override protected void onTextChanged(CharSequence text,int start,int before,int count){super.onTextChanged(text,start,before,count);if(numbers!=null)numbers.changed();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(numbers!=null)numbers.draw(canvas);}
    }
    static final class Editor extends EditText {
        private LineNumbers numbers;
        Editor(Context context){super(context);setTextDirection(TEXT_DIRECTION_LTR);}
        void configureNumbers(int color,int background){numbers=new LineNumbers(this,color,background);}
        @Override protected void onTextChanged(CharSequence text,int start,int before,int count){super.onTextChanged(text,start,before,count);if(numbers!=null)numbers.changed();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(numbers!=null)numbers.draw(canvas);}
    }
}
