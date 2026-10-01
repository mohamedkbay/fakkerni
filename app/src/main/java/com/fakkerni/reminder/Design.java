package com.fakkerni.reminder;

import android.app.Activity;
import android.view.*;
import android.widget.*;

final class Design {
    static LinearLayout root(Activity a) {
        Ui.apply(a);
        LinearLayout root=Ui.column(a); root.setBackground(new Ui.Pattern(0,0,0,a));
        int p=Ui.dp(a,18);root.setPadding(p,Ui.dp(a,12),p,Ui.dp(a,12)); return root;
    }
    static void main(Activity a) {
        LinearLayout root=root(a);
        ScrollView scroll=new ScrollView(a); scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout page=Ui.column(a);scroll.addView(page);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=Ui.row(a);
        ImageView logo=new ImageView(a);logo.setImageResource(R.drawable.brand_mark);
        logo.setContentDescription(a.getString(R.string.logo_description));
        nav.addView(logo,new LinearLayout.LayoutParams(Ui.dp(a,48),Ui.dp(a,48)));Ui.space(nav);
        TextView language=Ui.text(a,a.getString(R.string.language_button),14,Ui.WHITE);
        language.setId(R.id.languageButton);language.setGravity(Gravity.CENTER);
        language.setBackground(Ui.rounded(Ui.SURFACE,30,a));
        nav.addView(language,new LinearLayout.LayoutParams(Ui.dp(a,58),Ui.dp(a,48)));
        MorphIconView settings=Ui.icon(a,"settings",Ui.SURFACE,Ui.t(a,"الإعدادات","Settings"),48);
        settings.setId(R.id.aiSettingsButton);nav.addView(settings);
        ((LinearLayout.LayoutParams)settings.getLayoutParams()).setMarginStart(Ui.dp(a,6));
        MorphIconView add=Ui.icon(a,"plus",Ui.SURFACE,a.getString(R.string.add_reminder),48);
        add.setId(R.id.topAddButton);nav.addView(add);
        ((LinearLayout.LayoutParams)add.getLayoutParams()).setMarginStart(Ui.dp(a,6));
        Ui.add(page,nav,0);
        FrameLayout hero=new FrameLayout(a);
        hero.addView(new Ui.HeroRing(a),new FrameLayout.LayoutParams(Ui.dp(a,80),Ui.dp(a,80),Gravity.CENTER));
        MorphIconView heroBell=Ui.icon(a,"bell",android.graphics.Color.TRANSPARENT,a.getString(R.string.my_reminders),48);
        heroBell.setClickable(false);heroBell.setFocusable(false);hero.addView(heroBell,new FrameLayout.LayoutParams(Ui.dp(a,48),Ui.dp(a,48),Gravity.CENTER));
        hero.setLayoutParams(new LinearLayout.LayoutParams(-1,Ui.dp(a,88)));Ui.add(page,hero,20);
        TextView title=Ui.text(a,a.getString(R.string.my_reminders),29,Ui.WHITE);title.setGravity(Gravity.CENTER);Ui.add(page,title,8);
        TextView date=Ui.text(a,"",13,Ui.SECONDARY);date.setGravity(Gravity.CENTER);date.setId(R.id.todayDate);Ui.add(page,date,3);

        LinearLayout filters=Ui.row(a);
        String[] labels={a.getString(R.string.today),a.getString(R.string.this_week),a.getString(R.string.all)};
        int[] ids={R.id.filterToday,R.id.filterWeek,R.id.filterAll};
        for(int i=0;i<3;i++) {
            TextView chip=Ui.text(a,labels[i],13,Ui.WHITE);chip.setId(ids[i]);chip.setGravity(Gravity.CENTER);
            chip.setBackground(Ui.rounded(Ui.INK,28,a));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(a,48),1);
            if(i>0)p.setMarginStart(Ui.dp(a,6));filters.addView(chip,p);
        }
        Ui.add(page,filters,22);
        LinearLayout banner=Ui.card(a,Ui.YELLOW);banner.setId(R.id.permissionBanner);
        TextView message=Ui.text(a,"",12,Ui.BG);message.setId(R.id.permissionMessage);banner.addView(message);
        Button enable=Ui.button(a,a.getString(R.string.enable),Ui.WHITE);enable.setId(R.id.permissionButton);banner.addView(enable);
        Ui.add(page,banner,14);banner.setVisibility(View.GONE);
        LinearLayout heading=Ui.row(a);
        heading.addView(Ui.text(a,Ui.t(a,"جدولك","Your schedule"),15,Ui.WHITE));Ui.space(heading);
        TextView count=Ui.text(a,"",13,Ui.SECONDARY);count.setId(R.id.summaryCount);heading.addView(count);
        Ui.add(page,heading,20);
        LinearLayout list=Ui.column(a);list.setId(R.id.reminderList);Ui.add(page,list,8);
        Button addButton=Ui.button(a,a.getString(R.string.add_reminder),Ui.YELLOW);addButton.setId(R.id.addButton);
        Ui.add(root,addButton,12);
        Ui.setup(a,root);
    }
    static void editor(Activity a) {
        LinearLayout root=root(a);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout page=Ui.column(a);scroll.addView(page);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=Ui.row(a);
        MorphIconView back=Ui.icon(a,"back",Ui.SURFACE,Ui.t(a,"رجوع","Back"),48);back.setId(R.id.backButton);
        if(a.getResources().getConfiguration().getLayoutDirection()==View.LAYOUT_DIRECTION_RTL)back.setScaleX(-1);
        nav.addView(back);Ui.space(nav);
        TextView badge=Ui.text(a,a.getString(R.string.app_name),12,Ui.WHITE);badge.setPadding(Ui.dp(a,20),0,Ui.dp(a,20),0);
        nav.addView(badge,new LinearLayout.LayoutParams(-2,Ui.dp(a,44)));
        MorphIconView settings=Ui.icon(a,"settings",Ui.SURFACE,Ui.t(a,"الإعدادات","Settings"),48);
        settings.setId(R.id.aiSettingsButton);nav.addView(settings);Ui.add(page,nav,0);
        ((LinearLayout.LayoutParams)settings.getLayoutParams()).setMarginStart(Ui.dp(a,6));
        TextView title=Ui.text(a,a.getString(R.string.add_reminder),29,Ui.WHITE);title.setId(R.id.screenTitle);Ui.add(page,title,24);
        LinearLayout voice=Ui.column(a);voice.setId(R.id.voicePanel);Ui.add(page,voice,14);voice.setVisibility(View.GONE);
        TextView transcript=Ui.text(a,"",13,Ui.WHITE);transcript.setId(R.id.transcriptView);
        transcript.setTextIsSelectable(true);transcript.setVisibility(View.GONE);Ui.add(page,transcript,10);

        LinearLayout form=Ui.card(a,Ui.SURFACE);
        Ui.add(form,Ui.text(a,a.getString(R.string.what_to_remember),15,Ui.WHITE),0);
        EditText task=field(a,R.id.reminderText,a.getString(R.string.reminder_hint),false);Ui.add(form,task,8);
        LinearLayout contact=Ui.row(a);
        contact.addView(Ui.text(a,Ui.t(a,"شخص للاتصال","Someone to call"),14,Ui.WHITE));Ui.space(contact);
        MorphIconView person=Ui.icon(a,"phone",Ui.WHITE,Ui.t(a,"اختار من جهات الاتصال","Choose a contact"),48);
        person.setId(R.id.contactButton);contact.addView(person);Ui.add(form,contact,18);
        TextView name=Ui.text(a,Ui.t(a,"ابحث بالاسم العربي أو الإنجليزي أو الرقم","Search Arabic / English names or numbers"),11,Ui.SECONDARY);
        name.setId(R.id.contactName);Ui.add(form,name,4);
        EditText phone=field(a,R.id.phoneText,a.getString(R.string.phone_optional),true);Ui.add(form,phone,8);
        Ui.add(form,Ui.divider(a),18);
        Ui.add(form,Ui.text(a,a.getString(R.string.when),16,Ui.WHITE),12);
        LinearLayout days=Ui.row(a);int[] ids={R.id.todayChip,R.id.tomorrowChip,R.id.dateChip};
        int[] texts={R.string.today,R.string.tomorrow,R.string.choose_date};
        for(int i=0;i<3;i++) {
            TextView day=Ui.text(a,a.getString(texts[i]),12,Ui.WHITE);day.setId(ids[i]);
            day.setGravity(Gravity.CENTER);day.setBackground(Ui.rounded(Ui.INK,18,a));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(a,52),1);if(i>0)p.setMarginStart(Ui.dp(a,5));
            days.addView(day,p);
        }
        Ui.add(form,days,10);
        TextView time=Ui.text(a,"",28,Ui.WHITE);time.setId(R.id.timeButton);time.setGravity(Gravity.CENTER);
        time.setPadding(0,Ui.dp(a,12),0,Ui.dp(a,12));time.setBackground(Ui.rounded(Ui.INK,24,a));Ui.add(form,time,12);
        Ui.add(page,form,10);
        LinearLayout notice=Ui.row(a);notice.setPadding(Ui.dp(a,16),Ui.dp(a,16),Ui.dp(a,16),Ui.dp(a,16));
        notice.setBackground(Ui.rounded(Ui.INK,26,a));
        MorphIconView bell=Ui.icon(a,"bell",Ui.YELLOW,a.getString(R.string.one_hour_before),48);
        bell.setFocusable(false);bell.setClickable(false);notice.addView(bell);
        TextView hint=Ui.text(a,a.getString(R.string.one_hour_before),13,Ui.WHITE);
        hint.setPadding(Ui.dp(a,12),0,Ui.dp(a,8),0);notice.addView(hint,new LinearLayout.LayoutParams(0,-2,1));Ui.add(page,notice,12);
        Button save=Ui.button(a,a.getString(R.string.save_reminder),Ui.YELLOW);save.setId(R.id.saveButton);Ui.add(root,save,12);
        Ui.setup(a,root);
    }
    private static EditText field(Activity a,int id,String hint,boolean phone) {
        EditText input=new EditText(a);input.setId(id);input.setHint(hint);input.setTextColor(Ui.WHITE);
        input.setHintTextColor(Ui.SECONDARY);input.setTextSize(15);input.setBackground(Ui.rounded(Ui.INK,20,a));
        int p=Ui.dp(a,14);input.setPadding(p,p,p,p);input.setMinHeight(Ui.dp(a,56));
        input.setInputType(phone?android.text.InputType.TYPE_CLASS_PHONE:
                android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setTypeface(a.getResources().getFont(R.font.cairo));
        input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        input.setFilters(new android.text.InputFilter[]{(source,start,end,dest,dstart,dend)->{
            String before=source.subSequence(start,end).toString(),after=Digits.latin(before);
            return before.equals(after)?null:after;
        }});
        if(phone){input.setSingleLine(true);input.setTextDirection(View.TEXT_DIRECTION_LTR);}
        return input;
    }
}
