package com.fakkerni.reminder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** A lightweight, continuously morphing blue orb. No video decoder or bitmap assets. */
final class AssistantOrb extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape=new Path();
    private ValueAnimator motion;
    private float phase,level;
    private boolean listening,processing;
    AssistantOrb(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void state(boolean recording,boolean busy){listening=recording;processing=busy;invalidate();}
    void amplitude(float value){level=level*.55f+value*.45f;invalidate();}
    private void animateIfVisible(){
        if(motion!=null){motion.cancel();motion=null;}
        if(!isAttachedToWindow()||!isShown()||getWindowVisibility()!=VISIBLE||!ValueAnimator.areAnimatorsEnabled())return;
        motion=ValueAnimator.ofFloat(0,6.283185f);motion.setDuration(8000);motion.setRepeatCount(ValueAnimator.INFINITE);
        motion.setInterpolator(new android.view.animation.LinearInterpolator());
        motion.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});motion.start();
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();animateIfVisible();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);animateIfVisible();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);animateIfVisible();}
    @Override protected void onDetachedFromWindow(){if(motion!=null){motion.cancel();motion=null;}super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas){
        float side=Math.min(getWidth(),getHeight());if(side<=0)return;
        float cx=getWidth()/2f,cy=getHeight()/2f;
        float radius=side*(.33f+(listening?.018f*level:0));
        paint.setShader(new RadialGradient(cx,cy,radius*1.45f,new int[]{0x445777FF,0x125777FF,0x005777FF},null,Shader.TileMode.CLAMP));
        canvas.drawCircle(cx,cy,radius*1.45f,paint);
        shape.reset();
        for(int i=0;i<=180;i++){
            double angle=i*Math.PI/90;
            float r=radius*(1+.043f*(float)Math.sin(angle*3+phase)+.024f*(float)Math.cos(angle*5-phase*2));
            float x=cx+r*(float)Math.cos(angle),y=cy+r*(float)Math.sin(angle);
            if(i==0)shape.moveTo(x,y);else shape.lineTo(x,y);
        }
        shape.close();
        canvas.save();canvas.clipPath(shape);
        paint.setShader(new RadialGradient(cx-radius*.38f,cy-radius*.56f,radius*1.9f,
                new int[]{0xFFDAF5FF,0xFF8BABFF,0xFF344BF1,0xFF111885},new float[]{0,.25f,.63f,1},Shader.TileMode.CLAMP));
        canvas.drawPath(shape,paint);
        canvas.rotate((float)Math.sin(phase)*12+(processing?phase*57:0),cx,cy);
        // Lit, curved ribbons reproduce the reference's sculpted surface.
        for(int i=0;i<4;i++){
            float offset=(i-1.5f)*radius*.49f+(float)Math.sin(phase+i)*radius*.06f;
            paint.setShader(new LinearGradient(cx+offset-radius*.15f,cy,cx+offset+radius*.22f,cy,
                    new int[]{0x003C57EF,0x667C9EFF,0xAAD5EFFF,0x003C57EF},new float[]{0,.42f,.65f,1},Shader.TileMode.CLAMP));
            Path ribbon=new Path();ribbon.moveTo(cx+offset-radius*.3f,cy-radius*1.15f);
            ribbon.cubicTo(cx+offset+radius*.65f,cy-radius*.4f,cx+offset-radius*.7f,cy+radius*.45f,cx+offset+radius*.2f,cy+radius*1.2f);
            ribbon.lineTo(cx+offset+radius*.47f,cy+radius*1.2f);
            ribbon.cubicTo(cx+offset-radius*.43f,cy+radius*.45f,cx+offset+radius*.92f,cy-radius*.4f,cx+offset-radius*.03f,cy-radius*1.15f);
            ribbon.close();canvas.drawPath(ribbon,paint);
        }
        canvas.restore();
        canvas.save();canvas.clipPath(shape);
        paint.setShader(new RadialGradient(cx-radius*.32f,cy-radius*.5f,radius*1.75f,
                new int[]{0x00CCDFFF,0x003134D0,0x551017A4,0xD809106B},new float[]{0,.48f,.76f,1},Shader.TileMode.CLAMP));
        canvas.drawPath(shape,paint);
        paint.setShader(new RadialGradient(cx,cy,radius*1.04f,
                new int[]{0x00C3E9FF,0x00C3E9FF,0x55C3E9FF},new float[]{0,.88f,1},Shader.TileMode.CLAMP));
        canvas.drawPath(shape,paint);canvas.restore();paint.setShader(null);
    }
}
