package com.fakkerni.reminder;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class Ui {
    static final int BG=0xFF20232B, INK=0xFF22252E, SURFACE=0xFF30343E,
            PAPER=0xFFDEE0E7, YELLOW=0xFFB39AE4,
            WHITE=0xFFF4F3F8, MUTED=0xFF666A76, SECONDARY=0xFFADB0BE;
    static String t(Context c,String ar,String en) {
        return c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar")?ar:en;
    }
    static int dp(Context c,float n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    static String date(String pattern,long time) {
        return Digits.latin(new SimpleDateFormat(pattern,Locale.getDefault()).format(new Date(time)));
    }
    static GradientDrawable rounded(int color,float radius,Context c) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c,radius)); return d;
    }
    static TextView text(Context c,String value,float size,int color) {
        TextView t=new TextView(c); t.setText(Digits.latin(value)); t.setTextSize(size);
        t.setTypeface(c.getResources().getFont(R.font.cairo)); t.setTextColor(color);
        t.setGravity(Gravity.START|Gravity.CENTER_VERTICAL); t.setIncludeFontPadding(false);
        return t;
    }
    static LinearLayout column(Context c) { LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    static LinearLayout row(Context c) { LinearLayout l=new LinearLayout(c); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    static void add(LinearLayout parent,View child,int top) {
        int height=child.getLayoutParams()==null?-2:child.getLayoutParams().height;
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height);
        p.topMargin=dp(parent.getContext(),top); parent.addView(child,p);
    }
    static void space(LinearLayout row) { row.addView(new Space(row.getContext()),new LinearLayout.LayoutParams(0,1,1)); }
    static Button button(Context c,String title,int color) {
        Button b=new Button(c); b.setText(title); b.setAllCaps(false); b.setTextColor(INK); b.setTextSize(15);
        b.setTypeface(c.getResources().getFont(R.font.cairo),Typeface.BOLD);
        b.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x22555555),
                rounded(color,28,c),null)); b.setMinHeight(dp(c,54)); return b;
    }
    static MorphIconView icon(Context c,String name,int background,String label,int size) {
        MorphIconView v=new MorphIconView(c); v.setIcon(name,false); v.setContentDescription(label);
        v.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x22667178),
                rounded(background,50,c),null));
        if(background==SURFACE||background==INK||background==Color.TRANSPARENT)v.setInk(WHITE);
        int pad=dp(c,size<=48?12:16); v.setPadding(pad,pad,pad,pad);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c,size),dp(c,size))); return v;
    }
    static LinearLayout card(Context c,int color) {
        LinearLayout l=column(c); l.setBackground(rounded(color,30,c));
        int p=dp(c,20); l.setPadding(p,p,p,p); return l;
    }
    static void setup(Activity a,View root) {
        a.setContentView(root); root.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        int l=root.getPaddingLeft(),t=root.getPaddingTop(),r=root.getPaddingRight(),b=root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int top=insets.getSystemWindowInsetTop(),bottom=insets.getSystemWindowInsetBottom();
            if(android.os.Build.VERSION.SDK_INT>=30) {
                Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                top=bars.top; bottom=Math.max(bars.bottom,insets.getInsets(WindowInsets.Type.ime()).bottom);
            }
            v.setPadding(l,t+top,r,b+bottom); return insets;
        });
        root.requestApplyInsets();
    }
    static View divider(Context c) {
        View v=new View(c);
        v.setBackground(new Pattern(0,0x5585949E,2,c));
        v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(c,10))); return v;
    }
    static final class HeroRing extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        HeroRing(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){
            float pad=dp(getContext(),7);RectF r=new RectF(pad,pad,getWidth()-pad,getHeight()-pad);
            p.setStyle(Paint.Style.FILL);p.setColor(0xFF41404E);canvas.drawOval(r,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(getContext(),5));p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(YELLOW);canvas.drawArc(r,-85,290,false,p);
        }
    }
    static final class EmptyClock extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        EmptyClock(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float scale=Math.min(getWidth()/200f,getHeight()/160f);
            c.save();c.translate((getWidth()-200*scale)/2,(getHeight()-160*scale)/2);c.scale(scale,scale);
            p.setStyle(Paint.Style.FILL);p.setColor(0xFF3A454F);c.drawCircle(100,80,68,p);
            p.setColor(0xFF829097);for(int x=26;x<182;x+=9)for(int y=17;y<148;y+=9)
                if(Math.hypot(x-100,y-80)>70)c.drawCircle(x,y,.7f,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(14);p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(PAPER);c.drawArc(48,28,152,132,12,238,false,p);
            p.setColor(YELLOW);c.drawArc(48,28,152,132,267,62,false,p);
            p.setStrokeWidth(3);p.setColor(WHITE);c.drawLine(100,80,100,48,p);c.drawLine(100,80,122,92,p);
            p.setStyle(Paint.Style.FILL);p.setColor(YELLOW);c.drawCircle(100,80,4,p);
            c.restore();
        }
    }
    /** Soft orbital arcs and spaced pin-dots. Native vector drawing keeps the APK light. */
    static final class Pattern extends Drawable {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final int bg,fg,mode; final float density;
        Pattern(int bg,int fg,int mode,Context c) {
            this.bg=bg;this.fg=fg;this.mode=mode;density=c.getResources().getDisplayMetrics().density;
        }
        @Override public void draw(Canvas canvas) {
            Rect bounds=getBounds(); canvas.save();
            if(mode==0) {
                paint.setShader(new LinearGradient(0,0,bounds.width(),bounds.height(),
                        new int[]{0xFF363A43,0xFF242730,0xFF1E2128},null,Shader.TileMode.CLAMP));
                canvas.drawRect(bounds,paint); paint.setShader(null);
                paint.setColor(0x08C9BAE8);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);
                float cx=bounds.right-24*density,cy=bounds.top+90*density;
                for(int ring=0;ring<3;ring++)canvas.drawCircle(cx,cy,(68+ring*26)*density,paint);
                canvas.drawCircle(bounds.left+12*density,bounds.bottom-130*density,100*density,paint);
                paint.setStyle(Paint.Style.FILL);paint.setColor(0x0CC9BAE8);
                for(float x=22*density;x<bounds.width();x+=42*density)
                    for(float y=24*density;y<bounds.height();y+=42*density)canvas.drawCircle(x,y,.8f*density,paint);
            } else {
                Path clip=new Path();clip.addRoundRect(new RectF(bounds),24*density,24*density,Path.Direction.CW);
                canvas.clipPath(clip); paint.setColor(bg); canvas.drawRect(bounds,paint);
                paint.setColor(fg); paint.setStrokeWidth(density);
                if(mode==1){
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawCircle(bounds.right-12*density,bounds.centerY(),36*density,paint);
                    canvas.drawCircle(bounds.right-12*density,bounds.centerY(),52*density,paint);
                    paint.setStyle(Paint.Style.FILL);
                    for(float x=14*density;x<bounds.width();x+=32*density)
                        for(float y=12*density;y<bounds.height();y+=28*density)canvas.drawCircle(x,y,.8f*density,paint);
                }
                else for(float x=4*density;x<bounds.width();x+=8*density)
                    canvas.drawCircle(x,bounds.exactCenterY(),density*.8f,paint);
            }
            canvas.restore();
        }
        @Override public void setAlpha(int a) { paint.setAlpha(a); }
        @Override public void setColorFilter(ColorFilter c) { paint.setColorFilter(c); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
