package com.fakkerni.reminder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Animated, procedural silver wire sculpture inspired by the supplied assistant reference. */
final class AssistantOrb extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final boolean light;
    private Shader aura;
    private float auraX,auraY,auraRadius;
    private ValueAnimator motion;
    private float phase,level;
    private boolean listening,processing;
    AssistantOrb(Context c){super(c);light=Appearance.light(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setLayerType(View.LAYER_TYPE_HARDWARE,null);}
    void state(boolean recording,boolean busy){listening=recording;processing=busy;invalidate();}
    void amplitude(float value){level=level*.6f+value*.4f;invalidate();}
    private void animateIfVisible(){
        if(motion!=null){motion.cancel();motion=null;}
        if(!isAttachedToWindow()||!isShown()||getWindowVisibility()!=VISIBLE||!ValueAnimator.areAnimatorsEnabled())return;
        motion=ValueAnimator.ofFloat(0,(float)(Math.PI*2));motion.setDuration(11000);
        motion.setRepeatCount(ValueAnimator.INFINITE);motion.setInterpolator(new LinearInterpolator());
        motion.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});motion.start();
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();animateIfVisible();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);animateIfVisible();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);animateIfVisible();}
    @Override protected void onDetachedFromWindow(){if(motion!=null){motion.cancel();motion=null;}super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas){
        float side=Math.min(getWidth(),getHeight());if(side<=0)return;
        float cx=getWidth()/2f,cy=getHeight()/2f,r=side*(listening?.375f+level*.018f:.365f);
        paint.setStyle(Paint.Style.FILL);
        if(aura==null||auraX!=cx||auraY!=cy||Math.abs(auraRadius-r)>2){
            auraX=cx;auraY=cy;auraRadius=r;
            aura=new RadialGradient(cx,cy,r*1.25f,
                    light?new int[]{0x2064766D,0x0864766D,0x0064766D}:new int[]{0x2FC9DEDA,0x0AC9DEDA,0x00C9DEDA},null,Shader.TileMode.CLAMP);
        }
        paint.setShader(aura);
        canvas.drawCircle(cx,cy,r*1.25f,paint);paint.setShader(null);
        canvas.save();canvas.translate(cx,cy);
        float spin=phase*(processing?2.3f:1f);
        for(int j=0;j<44;j++){
            double band=j*Math.PI*2/44;
            path.reset();
            for(int i=0;i<=112;i++){
                double a=i*Math.PI*2/112;
                double ripple=Math.sin(4*a+band*3+spin)*.075+Math.cos(7*a-band*2-spin*.7)*.028;
                double radius=r*(.68+.19*Math.cos(band+3*a+spin*.45)+ripple);
                float x=(float)(radius*Math.cos(a)+r*.10*Math.sin(3*a+band+spin));
                float y=(float)(radius*Math.sin(a)*.88+r*.12*Math.cos(2*a-band-spin*.8));
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            int alpha=(int)(light?34+66*Math.pow(.5+.5*Math.cos(band+spin*.3),2):30+104*Math.pow(.5+.5*Math.cos(band+spin*.3),2));
            paint.setColor((Math.min(255,alpha)<<24)|(light?0x253430:0xE6F2EE));
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(j%9==0?1.25f:.72f);
            canvas.drawPath(path,paint);
        }
        for(int j=0;j<13;j++){
            double offset=j*Math.PI*2/13;
            path.reset();
            for(int i=0;i<=96;i++){
                double a=i*Math.PI*2/96;
                double rad=r*(.48+.19*Math.sin(3*a+offset+spin*.6));
                float x=(float)(rad*Math.cos(a)+r*.08*Math.cos(5*a+offset));
                float y=(float)(rad*Math.sin(a)*.9+r*.08*Math.sin(4*a-offset));
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            paint.setColor(light?0x412D3D37:0x6BEAF7F1);paint.setStrokeWidth(1.2f);canvas.drawPath(path,paint);
        }
        canvas.restore();paint.setStyle(Paint.Style.FILL);
    }
}
