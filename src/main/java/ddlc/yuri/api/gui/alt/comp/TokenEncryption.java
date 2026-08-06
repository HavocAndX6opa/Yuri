package ddlc.yuri.api.gui.alt.comp;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenEncryption {

    private static final byte[] KEY = "YuriSexLegend!ILoveLesbians!".getBytes(StandardCharsets.UTF_8);

    public static String encrypt(String rawToken) {
        if (rawToken == null) return null;
        byte[] tokenBytes = rawToken.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = new byte[tokenBytes.length];

        for (int i = 0; i < tokenBytes.length; i++) {
            encryptedBytes[i] = (byte) (tokenBytes[i] ^ KEY[i % KEY.length]);
        }

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String encryptedTokenBase64) {
        if (encryptedTokenBase64 == null) return null;
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedTokenBase64);
            byte[] decryptedBytes = new byte[decodedBytes.length];

            for (int i = 0; i < decodedBytes.length; i++) {
                decryptedBytes[i] = (byte) (decodedBytes[i] ^ KEY[i % KEY.length]);
            }

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
