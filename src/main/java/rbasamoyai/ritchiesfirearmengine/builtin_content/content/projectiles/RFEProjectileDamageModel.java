package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class RFEProjectileDamageModel {

    public static final Codec<RFEProjectileDamageModel> CODEC =
            Codec.pair(Codec.DOUBLE.fieldOf("distance").codec(), Codec.DOUBLE.fieldOf("damage").codec()).listOf()
                    .comapFlatMap(RFEProjectileDamageModel::createDataResult, model -> model.points);

    private static final StreamCodec<ByteBuf, Pair<Double, Double>> POINT_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Pair::getFirst, ByteBufCodecs.DOUBLE, Pair::getSecond, Pair::of);

    public static final StreamCodec<ByteBuf, RFEProjectileDamageModel> STREAM_CODEC = POINT_STREAM_CODEC.apply(ByteBufCodecs.list())
            .map(RFEProjectileDamageModel::fromPointsValid, model -> model.points).cast();

    private static final double MINIMUM_CHANGE_BETWEEN_POINTS = 1e-1d;

    private final List<Pair<Double, Double>> points = new ArrayList<>();

    private static RFEProjectileDamageModel fromPointsValid(List<Pair<Double, Double>> points) throws IllegalStateException {
        RFEProjectileDamageModel damageModel = new RFEProjectileDamageModel();
        for (Pair<Double, Double> point : points)
            damageModel.addPoint(point.getFirst(), point.getSecond());
        damageModel.validateDamageModel();
        return damageModel;
    }

    private static DataResult<RFEProjectileDamageModel> createDataResult(List<Pair<Double, Double>> points) {
        try {
            return DataResult.success(fromPointsValid(points));
        } catch (IllegalStateException e) {
            return DataResult.error(() -> "Error encountered while reading damage model: " + e.getMessage());
        }
    }

    public void addPoint(double distance, double damage) {
        if (distance < 0)
            throw new IllegalStateException("Can only add a positive distance on projectile damage model");
        if (!this.points.isEmpty()) {
            double lastDistance = this.points.get(this.points.size() - 1).getFirst();
            if (distance - lastDistance < MINIMUM_CHANGE_BETWEEN_POINTS)
                throw new IllegalStateException("Must add a distance greater than the last distance by at least " + MINIMUM_CHANGE_BETWEEN_POINTS + " in projectile damage model (added was " + distance + ", previous was " + lastDistance + ")");
        }
        this.points.add(Pair.of(distance, damage));
    }

    public void validateDamageModel() {
        if (this.points.isEmpty())
            throw new IllegalStateException("Must have at least one point in projectile damage model");
    }

    public double getDamage(double distance) {
        if (distance < 0)
            throw new IllegalStateException("Must query a positive distance for projectile damage model");
        this.validateDamageModel();
        if (distance < this.points.get(0).getFirst())
            return this.points.get(0).getSecond();
        int sz = this.points.size();
        for (int i = 0; i < sz - 1; ++i) {
            Pair<Double, Double> point1 = this.points.get(i);
            Pair<Double, Double> point2 = this.points.get(i + 1);
            double dist1 = point1.getFirst();
            double dist2 = point2.getFirst();
            if (dist1 <= distance && distance < dist2)
                return Mth.lerp((distance - dist1) / (dist2 - dist1), point1.getSecond(), point2.getSecond());
        }
        return this.points.get(sz - 1).getSecond();
    }

}
