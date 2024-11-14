package model;

import com.google.common.io.BaseEncoding;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Hash implements Comparable<Hash> {
    private static final BaseEncoding BASE16L = BaseEncoding.base16().lowerCase();

    byte[] raw;

    public static Hash of(String text) {
        return new Hash(digest(text));
    }

    public static Hash of(final Consumer<MessageDigest> fn){
        return new Hash(digest(fn));
    }

    public static Hash of(Stream<Hash> hashes){
        return Hash.of(md ->{
            hashes.forEach(h -> {
                if(h!=null){
                    md.update(h.getRaw());
                }else{
                    md.update((byte)0);
                }
            });
        });
    }

    private static byte[] digest(String text) {
        return digest(md -> md.update(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] digest(final Consumer<MessageDigest> fn){
        try{
            final MessageDigest md = MessageDigest.getInstance("MD5");
            fn.accept(md);
            return md.digest();
        }catch(NoSuchAlgorithmException e){
            log.error(e.getMessage(),e);
            return null;
        }
    }

    public Hash copy(){
        return new Hash(Arrays.copyOf(raw, raw.length));
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName(){
        return BASE16L.encode(raw);
    }

    @Override
    public int compareTo(final Hash other) {
        return Arrays.compare(this.raw, other.raw);
    }
}
