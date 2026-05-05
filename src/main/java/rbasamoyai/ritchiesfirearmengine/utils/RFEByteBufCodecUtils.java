package rbasamoyai.ritchiesfirearmengine.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class RFEByteBufCodecUtils {

    public static final StreamCodec<ByteBuf, Vec3> VEC3_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, ImmutableList<V>> immutableList() {
        return immutableList(Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, ImmutableList<V>> immutableList(int maxSize) {
        return codec -> new StreamCodec<>() {
            @Override
            public ImmutableList<V> decode(B buffer) {
                int sz = ByteBufCodecs.readCount(buffer, maxSize);
                ImmutableList.Builder<V> builder = ImmutableList.builderWithExpectedSize(Math.min(sz, 65536));
                for (int i = 0; i < sz; ++i)
                    builder.add(codec.decode(buffer));
                return builder.build();
            }

            @Override
            public void encode(B buffer, ImmutableList<V> list) {
                ByteBufCodecs.writeCount(buffer, list.size(), maxSize);
                for (V v : list)
                    codec.encode(buffer, v);
            }
        };
    }

    public static <B extends ByteBuf, K, V> StreamCodec<B, ImmutableMap<K, V>> immutableMap(StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec) {
        return immutableMap(keyCodec, valueCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, K, V> StreamCodec<B, ImmutableMap<K, V>> immutableMap(StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec, int maxSize) {
        return new StreamCodec<>() {
            @Override
            public ImmutableMap<K, V> decode(B buffer) {
                int sz = ByteBufCodecs.readCount(buffer, maxSize);
                ImmutableMap.Builder<K, V> builder = ImmutableMap.builderWithExpectedSize(Math.min(sz, 65536));
                for (int i = 0; i < sz; ++i)
                    builder.put(keyCodec.decode(buffer), valueCodec.decode(buffer));
                return builder.build();
            }

            @Override
            public void encode(B buffer, ImmutableMap<K, V> value) {
                ByteBufCodecs.writeCount(buffer, value.size(), maxSize);
                for (Map.Entry<K, V> e : value.entrySet()) {
                    keyCodec.encode(buffer, e.getKey());
                    valueCodec.encode(buffer, e.getValue());
                }
            }
        };
    }

    private RFEByteBufCodecUtils() {}

}
