package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReloadPhase {

    protected final PhaseType phaseType;
    protected final FirearmCondition condition;
    protected final int time;
    protected final boolean chargeFirearm;
    protected final ReloadType reloadType;
    protected final boolean ammoAddedLast;
    protected final boolean replaceChamberedRound;
    protected final boolean blockMagazineReloads;
    protected final boolean endReload;
    protected final Int2IntOpenHashMap reloadDelays;
    protected final int unloadMagTime;
    protected final int reloadMagTime;
    @Nullable protected final SoundEvent sound;

    public ReloadPhase(Builder builder) {
        this.phaseType = builder.phaseType;
        this.condition = builder.condition;
        this.time = builder.time;
        this.chargeFirearm = builder.chargeFirearm;
        this.reloadType = builder.reloadType;
        this.ammoAddedLast = builder.ammoAddedLast;
        this.replaceChamberedRound = builder.replaceChamberedRound;
        this.blockMagazineReloads = builder.blockMagazineReloads;
        this.endReload = builder.endReload;
        this.reloadDelays = builder.finalMultipleReloadDelays;
        this.unloadMagTime = builder.unloadMagTime;
        this.reloadMagTime = builder.reloadMagTime;
        this.sound = builder.sound;
    }

    public void playEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.sound != null)
            entity.level().playSound(null, entity.blockPosition(), this.sound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public PhaseType phaseType() { return this.phaseType; }

    public boolean test(ItemStack itemStack, LivingEntity entity) { return this.condition.test(itemStack, entity); }

    public int time() { return this.time; }
    public boolean chargeFirearm() { return this.chargeFirearm; }
    public ReloadType reloadType() { return this.reloadType; }
    public boolean ammoAddedLast() { return this.ammoAddedLast; }
    public boolean replaceChamberedRound() { return this.replaceChamberedRound; }
    public boolean blockMagazineReloads() { return this.blockMagazineReloads; }
    public boolean endReload() { return this.endReload; }
    public int reloadsAtTime(int time) { return this.reloadDelays.getOrDefault(time, 0); }

    public int unloadMagazineTime() { return this.unloadMagTime; }
    public int reloadMagazineTime() { return this.reloadMagTime; }

    public static ReloadPhase fromJson(JsonObject obj, boolean unload) {
        Builder builder = new Builder(unload);
        String reloadKey = unload ? "unload" : "reload";

        String phaseTypeString = GsonHelper.getAsString(obj, "phase");
        PhaseType phaseType = unload ? PhaseType.byIdUnload(phaseTypeString) : PhaseType.byId(phaseTypeString);
        if (phaseType == null)
            throw new JsonParseException("Invalid " + reloadKey + " phase type '" + phaseTypeString + "', must be one of 'prepare', '" + reloadKey + "', 'finish'");
        builder.phaseType(phaseType);

        if (GsonHelper.isObjectNode(obj, "condition")) {
            FirearmCondition condition = FirearmCondition.fromJson(obj.getAsJsonObject("condition"));
            builder.condition(condition);
        } else {
            builder.condition(FirearmCondition.AlwaysTrue.ALWAYS_TRUE);
        }

        int time = GsonHelper.getAsInt(obj, "time");
        builder.time(time);
        if (!unload) {
            boolean chargeFirearm = GsonHelper.getAsBoolean(obj, "charge_firearm", false);
            builder.chargeFirearm(chargeFirearm);
        }

        if (GsonHelper.isStringValue(obj, "sound")) {
            String str = GsonHelper.getAsString(obj, "sound");
            SoundEvent evt = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.sound(evt);
        }

        if (phaseType == PhaseType.RELOAD) {
            String reloadTypeString = GsonHelper.getAsString(obj, reloadKey + "_type");
            ReloadType reloadType = ReloadType.byId(reloadTypeString);
            if (reloadType == null)
                throw new JsonParseException("Invalid " + reloadKey + " type '" + reloadTypeString +
                        "', must be one of 'rounds', 'magazines', " + (unload ? "" : "'speedloaders', ") + "'secondaries'");
            boolean endReload = GsonHelper.getAsBoolean(obj, "end_" + reloadKey, false);
            boolean blockMagazineReloads = !unload && GsonHelper.getAsBoolean(obj, "block_magazine_reloads", true);
            builder.reloadType(reloadType)
                    .endReload(endReload)
                    .blockMagazineReloads(blockMagazineReloads);
            if (reloadType != ReloadType.MAGAZINES) {
                int reloadCount = obj.has(reloadKey + "_count") ? GsonHelper.getAsInt(obj, reloadKey + "_count") : 1;
                boolean ammoAddedLast = GsonHelper.getAsBoolean(obj, unload ? "ammo_removed_last" : "ammo_added_last", false);
                boolean replaceChamberedRound = !unload && GsonHelper.getAsBoolean(obj, "replace_chambered_round", !ammoAddedLast);
                builder.reloadCount(reloadCount)
                        .ammoAddedLast(ammoAddedLast);
                if (!unload)
                        builder.replaceChamberedRound(replaceChamberedRound);
                if (obj.has(reloadKey + "_delay")) {
                    int reloadDelay = GsonHelper.getAsInt(obj, reloadKey + "_delay");
                    builder.reloadDelay(reloadDelay);
                }
                if (reloadCount > 1 && reloadType != ReloadType.SPEEDLOADERS && obj.has(reloadKey + "_delays")) {
                    JsonArray delayArr = GsonHelper.getAsJsonArray(obj, reloadKey + "_delays");
                    List<Integer> delayList = new ArrayList<>();
                    for (JsonElement el : delayArr)
                        delayList.add(el.getAsInt());
                    builder.reloadDelays(delayList);
                }
            } else {
                if (!unload && GsonHelper.isNumberValue(obj, "reload_time")) {
                    int value = GsonHelper.getAsInt(obj, "reload_time");
                    builder.reloadMagTime(value);
                }
                if (GsonHelper.isNumberValue(obj, "unload_time")) {
                    int value = GsonHelper.getAsInt(obj, "unload_time");
                    builder.unloadMagTime(value);
                }
            }
        }
        return builder.build();
    }

    public enum PhaseType implements StringRepresentable {
        PREPARE,
        RELOAD,
        FINISH;

        private static final Map<String, PhaseType> BY_ID = Arrays.stream(values())
                .collect(Collectors.toMap(PhaseType::getSerializedName, Function.identity()));

        private final String id = this.name().toLowerCase(Locale.ROOT);

        @Override public String getSerializedName() { return this.id; }

        @Nullable public static PhaseType byId(String id) { return BY_ID.get(id); }

        @Nullable
        public static PhaseType byIdUnload(String id) {
            if ("unload".equals(id))
                return RELOAD;
            if ("reload".equals(id))
                return null;
            return byId(id);
        }
    }

    public enum ReloadType implements StringRepresentable {
        ROUNDS,
        MAGAZINES,
        SPEEDLOADERS,
        SECONDARIES;

        private static final Map<String, ReloadType> BY_ID = Arrays.stream(values())
                .collect(Collectors.toMap(ReloadType::getSerializedName, Function.identity()));

        private final String id = this.name().toLowerCase(Locale.ROOT);

        @Override public String getSerializedName() { return this.id; }

        @Nullable
        public static ReloadType byId(String id) { return BY_ID.get(id); }
    }

    public static class Builder {
        protected final boolean unload;
        protected final String mode;

        protected PhaseType phaseType = null;
        protected FirearmCondition condition = null;
        protected int time = -1;
        protected boolean chargeFirearm = false;
        protected ReloadType reloadType = null;
        protected int singleReloadDelay = 1;
        private boolean setSingleDelay = false;
        protected int reloadCount = 1;
        protected boolean ammoAddedLast = false;
        protected boolean replaceChamberedRound = false;
        protected boolean blockMagazineReloads = false;
        protected boolean endReload = false;
        protected List<Integer> multipleReloadDelays = new ArrayList<>();
        protected Int2IntOpenHashMap finalMultipleReloadDelays = new Int2IntOpenHashMap();
        protected int unloadMagTime = -1;
        protected int reloadMagTime = -1;
        @Nullable protected SoundEvent sound = null;

        public Builder(boolean unload) {
            this.unload = unload;
            this.mode = unload ? "unload" : "reload";
        }

        public Builder phaseType(PhaseType phaseType) { this.phaseType = phaseType; return this; }
        public Builder condition(FirearmCondition condition) { this.condition = condition; return this; }

        public Builder time(int time) {
            if (time < 1)
                throw new IllegalStateException("Cannot have " + this.mode + " time less than 1");
            this.time = time;
            return this;
        }

        public Builder chargeFirearm(boolean chargeFirearm) {
            if (this.unload)
                throw new IllegalStateException("Cannot set charge firearm for unload");
            this.chargeFirearm = chargeFirearm;
            return this;
        }

        public Builder reloadType(ReloadType reloadType) {
            if (reloadType == ReloadType.SPEEDLOADERS && this.unload)
                throw new IllegalStateException("Cannot set unload type to 'speedloaders'");
            this.reloadType = reloadType;
            return this;
        }

        public Builder reloadDelay(int reloadDelay) {
            if (reloadDelay < 1)
                throw new IllegalStateException("Cannot have " + this.mode + " delay less than 1");
            if (!this.multipleReloadDelays.isEmpty())
                throw new IllegalStateException("Cannot have both explicit single and multiple " + this.mode + " delays");
            this.singleReloadDelay = reloadDelay;
            this.setSingleDelay = true;
            return this;
        }

        public Builder reloadCount(int reloadCount) {
            if (reloadCount < 1)
                throw new IllegalStateException("Cannot have " + this.mode + " count less than 1");
            this.reloadCount = reloadCount;
            return this;
        }

        public Builder ammoAddedLast(boolean ammoAddedLast) {
            this.ammoAddedLast = ammoAddedLast;
            if (this.ammoAddedLast)
                this.replaceChamberedRound = false;
            return this;
        }

        public Builder replaceChamberedRound(boolean replaceChamberedRound) {
            if (this.ammoAddedLast && replaceChamberedRound)
                throw new IllegalStateException("Cannot replace chambered round if ammo added last");
            if (this.unload)
                throw new IllegalStateException("Cannot define replace chambered round for unloading");
            this.replaceChamberedRound = replaceChamberedRound;
            return this;
        }

        public Builder blockMagazineReloads(boolean blockMagazineReloads) { this.blockMagazineReloads = blockMagazineReloads; return this; }

        public Builder endReload(boolean endReload) { this.endReload = endReload; return this; }

        public Builder reloadDelays(List<Integer> delays) {
            if (this.setSingleDelay)
                throw new IllegalStateException("Cannot have both explicit single and multiple " + this.mode + " delays");
            if (delays.isEmpty())
                RitchiesFirearmEngine.LOGGER.warn("Loading empty {} delays list", this.mode);
            for (int delay : delays) {
                if (delay < 1)
                    throw new IllegalStateException("Cannot have " + this.mode + " delay less than 1");
            }
            this.multipleReloadDelays = delays;
            return this;
        }

        public Builder reloadMagTime(int time) {
            if (time < 1)
                throw new IllegalStateException("Cannot have reload magazine time less than 1");
            this.reloadMagTime = time;
            return this;
        }

        public Builder unloadMagTime(int time) {
            if (time < 1)
                throw new IllegalStateException("Cannot have unload magazine time less than 1");
            this.unloadMagTime = time;
            return this;
        }

        public Builder sound(SoundEvent sound) { this.sound = sound; return this; }

        // TODO secondaries
        public ReloadPhase build() {
            if (this.phaseType == null)
                throw new IllegalStateException("Must specify " + this.mode + " phase type");
            if (this.condition == null)
                throw new IllegalStateException("Must specify " + this.mode + " phase condition");
            if (this.time < 1)
                throw new IllegalStateException("Must specify " + this.mode + " phase time");
            if (this.phaseType == PhaseType.RELOAD) {
                if (this.reloadType == null)
                    throw new IllegalStateException("Must specify " + this.mode + " type for phase type '" + this.mode + "'");
                if (this.unload && this.reloadType == ReloadType.SPEEDLOADERS)
                    throw new JsonParseException("Cannot set unload type to 'speedloaders'");
                if (this.reloadType == ReloadType.ROUNDS || this.reloadType == ReloadType.SECONDARIES) {
                    if (this.reloadCount > 1) {
                        if (this.multipleReloadDelays.isEmpty()) {
                            this.finalMultipleReloadDelays.put(this.singleReloadDelay, this.reloadCount);
                        } else {
                            int sz = this.multipleReloadDelays.size();
                            if (sz < this.reloadCount) {
                                RitchiesFirearmEngine.LOGGER.warn("{} delays is shorter than specified {} count, padding with last value",
                                        this.mode, this.mode);
                                int lastElement = this.multipleReloadDelays.get(sz - 1);
                                int pad = this.reloadCount - sz;
                                for (int i = 0; i < pad; ++i)
                                    this.multipleReloadDelays.add(lastElement);
                            } else if (sz > this.reloadCount) {
                                RitchiesFirearmEngine.LOGGER.warn("{} delays is longer than specified {} count, pruning",
                                        this.mode, this.mode);
                                this.multipleReloadDelays.subList(this.reloadCount, sz).clear();
                            }
                            for (int delay : this.multipleReloadDelays) {
                                if (delay < 1)
                                    throw new IllegalStateException("Cannot have " + this.mode + " delay less than 1");
                                this.finalMultipleReloadDelays.addTo(delay, 1);
                            }
                        }
                    } else {
                        this.finalMultipleReloadDelays.put(this.singleReloadDelay, 1);
                    }
                } else if (this.reloadType == ReloadType.SPEEDLOADERS) {
                    this.finalMultipleReloadDelays.addTo(this.singleReloadDelay, this.reloadCount);
                } else {
                    if (!this.unload && (this.reloadMagTime == -1 || this.reloadMagTime > this.time))
                        this.reloadMagTime = this.time;
                    if (this.unloadMagTime == -1)
                        this.unloadMagTime = this.unload ? this.time : this.reloadMagTime;
                    if (this.unloadMagTime > this.time)
                        this.unloadMagTime = this.time;
                    if (!this.unload && this.unloadMagTime > this.reloadMagTime)
                        throw new IllegalStateException("Unload magazine time for reload cannot be greater than reload magazine time");
                }
            } else {
                if (this.reloadType != null)
                    RitchiesFirearmEngine.LOGGER.warn("Ignoring {} phase {} type '{}' in non-{} phase '{}'",
                            this.mode, this.mode, this.reloadType.getSerializedName(), this.mode, this.phaseType.getSerializedName());
            }
            return new ReloadPhase(this);
        }
    }

}
