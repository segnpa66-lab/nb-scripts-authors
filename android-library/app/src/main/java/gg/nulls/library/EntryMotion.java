package gg.nulls.library;

import android.animation.ValueAnimator;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.PathInterpolator;

/** Resumes the entrance when cached or restored data redraws the screen. */
final class EntryMotion {
    private static final long DURATION=400;
    private final PathInterpolator easing=new PathInterpolator(.2f,0f,0f,1f);
    private long started=-1;

    void begin(){started=ValueAnimator.areAnimatorsEnabled()?SystemClock.uptimeMillis():-1;}

    boolean apply(View header,View logo,View content,View navigation){
        if(started<0||!ValueAnimator.areAnimatorsEnabled())return false;
        long elapsed=SystemClock.uptimeMillis()-started;
        if(elapsed>=DURATION+100){started=-1;return false;}
        reveal(header,elapsed,0,10);
        reveal(content,elapsed,45,16);
        reveal(navigation,elapsed,100,10);
        float fraction=easing.getInterpolation(Math.min(1f,Math.max(0f,(float)elapsed/DURATION)));
        logo.animate().cancel();logo.setScaleX(.82f+.18f*fraction);logo.setScaleY(.82f+.18f*fraction);logo.setRotation(-10f*(1f-fraction));
        if(elapsed<DURATION)logo.animate().scaleX(1).scaleY(1).rotation(0).setDuration(DURATION-elapsed).setInterpolator(easing).start();
        return true;
    }

    private void reveal(View view,long elapsed,long delay,float distance){
        view.animate().cancel();float fraction=easing.getInterpolation(Math.min(1f,Math.max(0f,(float)(elapsed-delay)/DURATION)));
        float density=view.getResources().getDisplayMetrics().density;
        view.setAlpha(fraction);view.setTranslationY(distance*density*(1f-fraction));
        long remaining=DURATION+delay-elapsed;
        if(remaining>0)view.animate().alpha(1).translationY(0).setStartDelay(Math.max(0,delay-elapsed)).setDuration(Math.min(DURATION,remaining)).setInterpolator(easing).start();
    }
}
