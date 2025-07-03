package rbasamoyai.ritchiesfirearmengine.content.projectiles;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

import java.util.List;

public class RFEProjectileDamageModel {

    private static final double MINIMUM_CHANGE_BETWEEN_POINTS = 1e-1d;

    private final List<Double> distances = new DoubleArrayList();
    private final List<Double> damages = new DoubleArrayList();

    public void addPoint(double distance, double damage) {
        if (distance < 0)
            throw new IllegalStateException("Can only add a positive distance on projectile damage model");
        if (!this.distances.isEmpty()) {
            double lastDistance = this.distances.get(this.distances.size() - 1);
            if (distance - lastDistance < MINIMUM_CHANGE_BETWEEN_POINTS)
                throw new IllegalStateException("Must add a distance greater than the last distance by at least " + MINIMUM_CHANGE_BETWEEN_POINTS + " in projectile damage model (added was " + distance + ", previous was " + lastDistance + ")");
        }
        this.distances.add(distance);
        this.damages.add(damage);
    }

    public void validateDamageModel() {
        if (this.distances.isEmpty())
            throw new IllegalStateException("Must have at least one point in projectile damage model");
    }

    public double getDamage(double distance) {
        if (distance < 0)
            throw new IllegalStateException("Must query a positive distance for projectile damage model");
        this.validateDamageModel();
        if (distance < this.distances.get(0))
            return this.damages.get(0);
        int sz = this.distances.size();
        for (int i = 0; i < sz - 1; ++i) {
            double dist1 = this.distances.get(i);
            double dist2 = this.distances.get(i + 1);
            if (dist1 <= distance && distance < dist2)
                return Mth.lerp((distance - dist1) / (dist2 - dist1), this.damages.get(i), this.damages.get(i + 1));
        }
        return this.damages.get(sz - 1);
    }

    public static void toNetwork(FriendlyByteBuf buf, RFEProjectileDamageModel model) {
        int sz = model.distances.size();
        buf.writeVarInt(sz);
        for (int i = 0; i < sz; ++i) {
            buf.writeDouble(model.distances.get(i))
                    .writeDouble(model.damages.get(i));
        }
    }

    public static RFEProjectileDamageModel fromNetwork(FriendlyByteBuf buf) {
        RFEProjectileDamageModel model = new RFEProjectileDamageModel();
        int sz = buf.readVarInt();
        for (int i = 0; i < sz; ++i) {
            double distance = buf.readDouble();
            double damage = buf.readDouble();
            model.addPoint(distance, damage);
        }
        return model;
    }

}
