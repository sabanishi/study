package model;

import com.google.common.io.BaseEncoding;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Arrays;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Hash implements Comparable<Hash> {
    private static final BaseEncoding BASE16L = BaseEncoding.base16().lowerCase();

    byte[] raw;

    public static Hash of(String text) {
        return new Hash(digest(text));
    }

    private static byte[] digest(String text) {
        String base16str = BASE16L.encode(text.getBytes());
        return DigestUtils.md5(base16str);
    }

    @Override
    public String toString() {
        return BASE16L.encode(raw);
    }

    @Override
    public int compareTo(final Hash other) {
        return Arrays.compare(this.raw, other.raw);
    }
}
