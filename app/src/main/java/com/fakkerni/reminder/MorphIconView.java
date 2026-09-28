package com.fakkerni.reminder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import org.json.*;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/** Native rendering of actual Morphicons-generated animation frames. */
public class MorphIconView extends View {
    private static JSONObject catalog;
    private static final Map<String, Path> shapes = new HashMap<>();
    private static final Map<String, Path[]> sequences = new HashMap<>();
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path current = new Path();
    private String name = "";
    private ValueAnimator animator, spin;
    public MorphIconView(Context c) { this(c,null); }
    public MorphIconView(Context c, AttributeSet attrs) {
        super(c,attrs);
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeWidth(1.9f); ink.setStrokeCap(Paint.Cap.ROUND); ink.setStrokeJoin(Paint.Join.ROUND);
        ink.setColor(c.getColor(R.color.ink));
        setFocusable(true); setClickable(true);
        load(c);
        Object tag = getTag();
        setIcon(tag instanceof String ? (String) tag : "mic", false);
    }
    private static synchronized void load(Context c) {
        if (catalog != null) return;
        try (InputStream in=c.getAssets().open("morphicons.json"); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] b=new byte[4096]; int n;
            while((n=in.read(b))!=-1) out.write(b,0,n);
            catalog=new JSONObject(out.toString("UTF-8"));
        } catch(Exception e) { throw new IllegalStateException("Missing Morphicons assets",e); }
    }
    private static Path path(JSONArray parts) throws JSONException {
        Path result=new Path();
        for(int k=0;k<parts.length();k++) {
            JSONObject sub=parts.getJSONObject(k); JSONArray p=sub.getJSONArray("p");
            result.moveTo((float)p.getDouble(0),(float)p.getDouble(1));
            for(int i=2;i<p.length();i+=2) result.lineTo((float)p.getDouble(i),(float)p.getDouble(i+1));
            if(sub.getBoolean("closed")) result.close();
        }
        return result;
    }
    public void setInk(int color) { ink.setColor(color); invalidate(); }
    public void setIcon(String next, boolean animate) {
        if (next.equals(name)) return;
        if(animator!=null) animator.cancel();
        try {
            if(!shapes.containsKey(next)) shapes.put(next,path(catalog.getJSONObject("shapes").getJSONArray(next)));
            String pair=name+"_"+next;
            name=next;
            JSONArray data=catalog.getJSONObject("transitions").optJSONArray(pair);
            if(animate && isAttachedToWindow() && ValueAnimator.areAnimatorsEnabled() && data!=null) {
                if(!sequences.containsKey(pair)) {
                    Path[] frames=new Path[data.length()];
                    for(int i=0;i<frames.length;i++) frames[i]=path(data.getJSONArray(i));
                    sequences.put(pair,frames);
                }
                Path[] frames=sequences.get(pair);
                animator=ValueAnimator.ofInt(0,frames.length-1);
                animator.setDuration(frames.length*1000L/60);
                animator.setInterpolator(new android.view.animation.LinearInterpolator());
                animator.addUpdateListener(a->{ current=frames[(int)a.getAnimatedValue()]; invalidate(); });
                animator.start();
            } else { current=shapes.get(next); invalidate(); }
        } catch(JSONException e) { throw new IllegalArgumentException("Unknown icon "+next,e); }
    }
    public void spinning(boolean enabled) {
        if(spin!=null) { spin.cancel(); spin=null; }
        setRotation(0);
        if(enabled && ValueAnimator.areAnimatorsEnabled()) {
            spin=ValueAnimator.ofFloat(0,360); spin.setDuration(1000); spin.setRepeatCount(ValueAnimator.INFINITE);
            spin.setInterpolator(new android.view.animation.LinearInterpolator());
            spin.addUpdateListener(a->setRotation((float)a.getAnimatedValue())); spin.start();
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float side=Math.min(getWidth()-getPaddingLeft()-getPaddingRight(),getHeight()-getPaddingTop()-getPaddingBottom());
        canvas.save(); canvas.translate((getWidth()-side)/2,(getHeight()-side)/2);
        canvas.scale(side/24f,side/24f); canvas.drawPath(current,ink); canvas.restore();
    }
    @Override public CharSequence getAccessibilityClassName() { return "android.widget.Button"; }
    @Override protected void onDetachedFromWindow() {
        if(animator!=null) animator.cancel(); spinning(false);
        super.onDetachedFromWindow();
    }
}
