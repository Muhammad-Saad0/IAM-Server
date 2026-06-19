package com.example.iam.testsupport;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public final class TestRsaKeys {
    private static final KeyPair KEY_PAIR = generateKeyPair();

    private TestRsaKeys() {
    }

    public static RSAPrivateKey privateKey() {
        return (RSAPrivateKey) KEY_PAIR.getPrivate();
    }

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEY_PAIR.getPublic();
    }

    public static String privateKeyPem() {
        return pem("PRIVATE KEY", privateKey().getEncoded());
    }

    public static String publicKeyPem() {
        return pem("PUBLIC KEY", publicKey().getEncoded());
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("RSA is unavailable", exception);
        }
    }

    private static String pem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded)
                + "\n-----END " + type + "-----";
    }
}
