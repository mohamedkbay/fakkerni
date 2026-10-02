package com.fakkerni.reminder;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Serves only bundled sounds at stable URIs for Android notification channels. */
public final class ToneProvider extends ContentProvider {
    private static final String AUTHORITY="com.fakkerni.reminder.tones";

    static Uri uri(AlertTone tone){
        if(tone.rawId==0)throw new IllegalArgumentException("Not a bundled sound");
        return Uri.parse("content://"+AUTHORITY+"/"+tone.id);
    }
    private AlertTone tone(Uri uri){
        if(!AUTHORITY.equals(uri.getAuthority())||uri.getPathSegments().size()!=1)return null;
        return AlertTone.fromAssetId(uri.getLastPathSegment());
    }
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return tone(uri)==null?null:"audio/mpeg";}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        AlertTone selected=tone(uri);
        if(selected==null||!"r".equals(mode))throw new FileNotFoundException();
        File directory=new File(getContext().getCacheDir(),"alert-tones");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new FileNotFoundException("Tone cache unavailable");
        File target=new File(directory,selected.id+".mp3");
        synchronized(ToneProvider.class){
            File temporary=null;
            try{
                temporary=File.createTempFile("tone-", ".tmp",directory);
                try(InputStream input=getContext().getResources().openRawResource(selected.rawId);
                    FileOutputStream output=new FileOutputStream(temporary)){
                    byte[] buffer=new byte[8192];int count;
                    while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
                }
                Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
            }catch(IOException|RuntimeException error){
                FileNotFoundException failure=new FileNotFoundException("Could not open bundled tone");
                failure.initCause(error);throw failure;
            }finally{if(temporary!=null)temporary.delete();}
        }
        return ParcelFileDescriptor.open(target,ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){return null;}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
