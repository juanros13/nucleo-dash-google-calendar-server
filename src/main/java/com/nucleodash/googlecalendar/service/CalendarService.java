package com.nucleodash.googlecalendar.service;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class CalendarService implements ICalendarService {
    @Autowired
    private Environment env;
    // some random salt
    private static final byte[] SALT = {(byte) 0x21, (byte) 0x21, (byte) 0xF0, (byte) 0x55, (byte) 0xC3, (byte) 0x9F, (byte) 0x5A, (byte) 0x75};
    private final static int ITERATION_COUNT = 1024;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static String encode(String input) {
        if (input == null) {
            throw new IllegalArgumentException();
        }
        try {

            KeySpec keySpec = new PBEKeySpec(null, SALT, ITERATION_COUNT);
            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(SALT, ITERATION_COUNT);

            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);

            Cipher ecipher = Cipher.getInstance(key.getAlgorithm());
            ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

            byte[] enc = ecipher.doFinal(input.getBytes());

            String res = new String(Base64.encodeBase64(enc));
            // escapes for url
            res = res.replace('+', '-').replace('/', '_').replace("%", "%25").replace("\n", "%0A");

            return res;

        } catch (Exception e) {

        }

        return "";

    }

    public static String decode(String token) {
        if (token == null) {
            return null;
        }
        try {

            String input = token.replace("%0A", "\n").replace("%25", "%").replace('_', '/').replace('-', '+');

            byte[] dec = Base64.decodeBase64(input.getBytes());

            KeySpec keySpec = new PBEKeySpec(null, SALT, ITERATION_COUNT);
            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(SALT, ITERATION_COUNT);

            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);

            Cipher dcipher = Cipher.getInstance(key.getAlgorithm());
            dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

            byte[] decoded = dcipher.doFinal(dec);

            String result = new String(decoded);
            return result;

        } catch (Exception e) {
            // use logger in production code
            e.printStackTrace();
        }

        return null;
    }
    @Override
    public String getToken(String authId) {

        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
        String key = "n2r5u8x/A?D(G+KaPdSgVkYp3s6v9y$B"; // 256 bit key
        String hasesillo = "";
        Date fecha = new Date();
        boolean multipart = true;

        String hashesilloUrl = "";

        try {
            // Create key and cipher
            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            // encrypt the text
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            hasesillo = authId + "," + env.getProperty("config.security.oauth.client.secret") + "," + dt.format(fecha);

            hashesilloUrl = encode(hasesillo);



        } catch (Exception e) {
            e.printStackTrace();
        }



        return hashesilloUrl;
    }



}
