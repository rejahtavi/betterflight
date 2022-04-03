package com.rejahtavi.betterflight.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.rejahtavi.betterflight.BetterFlight;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

public class Config {

    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    // constants, might make some of these configurable later
    public static final int ELYTRA_MAX_CHARGE = 7;
    public static final int BORDER_FLASH_TICKS = 5;
    public static final double TAKE_OFF_SPEED = 0.270D;
    public static final double TAKE_OFF_THRUST = 2.0D;
    public static final double TAKE_OFF_FOOD_MULTIPLIER = 2.0D;
    public static final double FLAP_THRUST = 0.65D;
    public static final double FLARE_DRAG = 0.025;
    public static final double TAKE_OFF_VOLUME = 0.5D;
    public static final double FLAP_VOLUME = 0.25D;

    // elytra costs
    public static double elytraExhaustionRate = 4.0D;
    public static int elytraRechargeTicks = 100;
    public static int elytraCooldownTicks = 20;
    public static int elytraFlareTicksPerChargePoint = 20;

    // list of items that count as elytra
    public static List<Item> elytraItems;

    // HUD icon position
    public enum WidgetLocation {
        BAR_CENTER,
        BAR_LEFT,
        BAR_RIGHT,
        CURSOR_BELOW,
        CURSOR_LEFT,
        CURSOR_RIGHT,
        CURSOR_ABOVE
    }

    public static WidgetLocation widgetLocation = WidgetLocation.BAR_CENTER;

    // set up config file
    static {
        Pair<Common, ForgeConfigSpec> serverSpecPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = serverSpecPair.getLeft();
        COMMON_SPEC = serverSpecPair.getRight();
    }

    // handles 'baking' the config into ram-accessible objects that are much faster
    // than ForgeConfig. This is VERY important because we rely on some of these
    // not just every tick, but *every frame*
    public static void bake() {
        elytraExhaustionRate = COMMON.elytraExhaustionRate.get();
        elytraRechargeTicks = COMMON.elytraRechargeTicks.get();
        elytraCooldownTicks = COMMON.elytraCooldownTicks.get();
        elytraFlareTicksPerChargePoint = COMMON.elytraFlareTicksPerChargePoint.get();
        widgetLocation = COMMON.widgetLocation.get();
        elytraItems = new ArrayList<>();

        for (String id : COMMON.elytraItems.get()) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            if (item != null && item != Items.AIR) {
                elytraItems.add(item);
            }
        }
    }

    // defines config file format
    public static class Common {

        public final ForgeConfigSpec.DoubleValue elytraExhaustionRate;
        public final ForgeConfigSpec.IntValue elytraRechargeTicks;
        public final ForgeConfigSpec.IntValue elytraCooldownTicks;
        public final ForgeConfigSpec.IntValue elytraFlareTicksPerChargePoint;
        public final ForgeConfigSpec.EnumValue<WidgetLocation> widgetLocation;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> elytraItems;

        //@formatter:off
        public Common(ForgeConfigSpec.Builder builder) {
            builder.push(BetterFlight.MODNAME + " settings");

            elytraExhaustionRate = builder
                    .comment("Controls food consumption of various flight actions.")
                    .defineInRange("elytraExhaustionRate", 4.0D, 0.0D, 10.0D);
            
            elytraRechargeTicks = builder
                    .comment("Controls how long it takes the elytra meter to recharge.")
                    .defineInRange("elytraRechargeTicks", 100, 1, 600);
 
            elytraCooldownTicks = builder
                    .comment("Controls how long players must wait between wing flaps.")
                    .defineInRange("elytraCooldownTicks", 20, 1, 200);
            
            elytraFlareTicksPerChargePoint = builder
                    .comment("Controls how rapidly flaring depletes the elytra meter.")
                    .defineInRange("elytraFlareTicksPerChargePoint", 20, 1, 600);
            
            widgetLocation = builder
                    .comment("Stores preferred position of elytra widget on HUD.\n"
                            + "Options: BAR_CENTER, BAR_LEFT, BAR_RIGHT,\n"
                            + "CURSOR_BELOW, CURSOR_ABOVE, CURSOR_RIGHT, CURSOR_LEFT.")
                    .defineEnum("widgetLocation", WidgetLocation.BAR_CENTER);
            
            elytraItems = builder
                    .comment("A list of modid:itemname registry keys that count as an Elytra.")
                    .defineList("elytraItems",
                            new ArrayList<>(Arrays.asList(
                            "minecraft:elytra",
                            "mekanism:hdpe_elytra",
                            "alexsmobs:tarantula_hawk_elytra",
                            "tconstruct:slime_chestplate")),
                            s -> s instanceof String);

            builder.pop();
        }        
    }
}