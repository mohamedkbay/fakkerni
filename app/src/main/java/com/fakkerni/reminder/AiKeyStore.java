package com.fakkerni.reminder;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Personal BYOK credentials: never bundled in the APK, logged, or backed up. */
final class AiKeyStore {
    private static final String ALIAS = "fakkerni.openai.v1";
    private static AtomicFile file(Context c,AiProvider provider) {
        return new AtomicFile(new File(c.getNoBackupFilesDir(), provider.keyFile));
    }
    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            generator.generateKey();
        }
        return (SecretKey) store.getKey(ALIAS, null);
    }
    static boolean hasKey(Context c) { return hasKey(c,AiProvider.OPENAI); }
    static boolean hasKey(Context c,AiProvider provider) { return file(c,provider).getBaseFile().exists(); }
    static void save(Context c, String value) throws Exception {
        save(c,AiProvider.OPENAI,value);
    }
    static void save(Context c,AiProvider provider,String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        AtomicFile file = file(c,provider);
        FileOutputStream out = file.startWrite();
        try {
            out.write(cipher.getIV().length);
            out.write(cipher.getIV());
            out.write(encrypted);
            file.finishWrite(out);
        } catch (Exception error) {
            file.failWrite(out);
            throw error;
        }
    }
    static String read(Context c) throws Exception {
        return read(c,AiProvider.OPENAI);
    }
    static String read(Context c,AiProvider provider) throws Exception {
        byte[] data = file(c,provider).readFully();
        if(data.length<14)throw new IllegalStateException();
        int ivLength = data[0] & 255;
        if (ivLength != 12 || data.length <= ivLength + 1) throw new IllegalStateException();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, data, 1, ivLength));
        return new String(cipher.doFinal(data, ivLength + 1, data.length - ivLength - 1), StandardCharsets.UTF_8);
    }
    static void remove(Context c) { remove(c,AiProvider.OPENAI); }
    static void remove(Context c,AiProvider provider) { file(c,provider).delete(); }
}
