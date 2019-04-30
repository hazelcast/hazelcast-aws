package com.hazelcast.aws.utility;

import com.hazelcast.util.QuickMath;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.hazelcast.aws.impl.Constants.SIGNATURE_METHOD_V4;

/**
 *
 */
public class Aws4RequestSignerUtils {

    private static final String UTF_8 = "UTF-8";

    private Aws4RequestSignerUtils() {
    }

    static String getCanonicalizedQueryString(List<String> list) {
        Iterator<String> it = list.iterator();
        StringBuilder result = new StringBuilder();
        if (it.hasNext()) {
            result.append(it.next());
        }
        while (it.hasNext()) {
            result.append('&').append(it.next());
        }
        return result.toString();
    }

    static void addComponents(List<String> components, Map<String, String> attributes, String key) {
        components.add(AwsURLEncoder.urlEncode(key) + '=' + AwsURLEncoder.urlEncode(attributes.get(key)));
    }

    static List<String> getListOfEntries(Map<String, String> entries) {
        List<String> components = new ArrayList<String>();
        for (String key : entries.keySet()) {
            addComponents(components, entries, key);
        }
        return components;
    }

    public static String getCanonicalizedQueryString(Map<String, String> attributes) {
        List<String> components = getListOfEntries(attributes);
        Collections.sort(components);
        return getCanonicalizedQueryString(components);
    }

    public static String sha256Hashhex(String in) {
        String payloadHash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(in.getBytes(UTF_8));
            byte[] digest = md.digest();
            payloadHash = QuickMath.bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return payloadHash;
    }

    public static String buildAuthHeader(String accessKey, String credentialScope, String signedHeaders, String signature) {
        return SIGNATURE_METHOD_V4 + " " + "Credential=" + accessKey + "/" + credentialScope + ", " + "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
    }
}
