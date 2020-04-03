/*
 * Copyright 2020 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.hazelcast.aws;

import com.hazelcast.internal.util.QuickMath;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static com.hazelcast.aws.CloudyUtility.prepareCanonicalizedQueryString;
import static java.lang.String.format;

class AwsEc2RequestSigner {
    private static final String NEW_LINE = "\n";
    private static final String API_TERMINATOR = "aws4_request";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String UTF_8 = "UTF-8";
    private static final int DATE_LENGTH = 8;
    private static final String EC2_SERVICE = "ec2";
    static final String SIGNATURE_METHOD_V4 = "AWS4-HMAC-SHA256";


    AwsEc2RequestSigner() {

    }


    private String getCredentialScope(String region, String timestamp) {
        // datestamp/region/service/API_TERMINATOR
        String dateStamp = timestamp.substring(0, DATE_LENGTH);
        return format("%s/%s/%s/%s", dateStamp, region, EC2_SERVICE, API_TERMINATOR);
    }

    private String getSignedHeaders() {
        return "host";
    }

    String sign(Map<String, String> attributes, String region, String endpoint, AwsCredentials credentials,
                String timestamp) {

        String canonicalRequest = getCanonicalizedRequest(attributes, endpoint);
        String stringToSign = createStringToSign(canonicalRequest, region, timestamp);
        byte[] signingKey = deriveSigningKey(region, credentials, timestamp);

        return createSignature(stringToSign, signingKey);
    }

    /* Task 1 */
    private String getCanonicalizedRequest(Map<String, String> attributes, String endpoint) {
        return "GET" + NEW_LINE + '/' + NEW_LINE + prepareCanonicalizedQueryString(attributes) + NEW_LINE
            + getCanonicalHeaders(endpoint) + NEW_LINE + getSignedHeaders() + NEW_LINE + sha256Hashhex("");
    }

    /* Task 2 */
    private String createStringToSign(String canonicalRequest, String region, String timestamp) {
        return SIGNATURE_METHOD_V4 + NEW_LINE + timestamp + NEW_LINE + getCredentialScope(region,
            timestamp) + NEW_LINE + sha256Hashhex(
            canonicalRequest);
    }

    /* Task 3 */
    private byte[] deriveSigningKey(String region, AwsCredentials credentials, String timestamp) {
        String signKey = credentials.getSecretKey();
        String dateStamp = timestamp.substring(0, DATE_LENGTH);
        // this is derived from
        // http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html#signature-v4-examples-python

        try {
            String key = "AWS4" + signKey;
            Mac mDate = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec skDate = new SecretKeySpec(key.getBytes(UTF_8), HMAC_SHA256);
            mDate.init(skDate);
            byte[] kDate = mDate.doFinal(dateStamp.getBytes(UTF_8));

            Mac mRegion = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec skRegion = new SecretKeySpec(kDate, HMAC_SHA256);
            mRegion.init(skRegion);
            byte[] kRegion = mRegion.doFinal(region.getBytes(UTF_8));

            Mac mService = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec skService = new SecretKeySpec(kRegion, HMAC_SHA256);
            mService.init(skService);
            byte[] kService = mService.doFinal(EC2_SERVICE.getBytes(UTF_8));

            Mac mSigning = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec skSigning = new SecretKeySpec(kService, HMAC_SHA256);
            mSigning.init(skSigning);

            return mSigning.doFinal("aws4_request".getBytes(UTF_8));
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidKeyException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private String createSignature(String stringToSign, byte[] signingKey) {
        byte[] signature;
        try {
            Mac signMac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec signKS = new SecretKeySpec(signingKey, HMAC_SHA256);
            signMac.init(signKS);
            signature = signMac.doFinal(stringToSign.getBytes(UTF_8));
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidKeyException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return QuickMath.bytesToHex(signature);
    }

    private String getCanonicalHeaders(String endpoint) {
        return format("host:%s%s", endpoint, NEW_LINE);
    }

    private String sha256Hashhex(String in) {
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
}
