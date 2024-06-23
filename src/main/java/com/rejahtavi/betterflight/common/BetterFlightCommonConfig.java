package com.rejahtavi.betterflight.common;

import com.rejahtavi.betterflight.BetterFlight;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class BetterFlightCommonConfig
{

    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    // constants, might make some of these configurable later
    public static final double TAKE_OFF_SPEED = 0.170D;
    public static final double TAKE_OFF_THRUST = 1.0D;
    public static final double CLASSIC_FLAP_THRUST = 0.65D;
    public static final double FLARE_DRAG = 0.08D;
    public static final int TAKE_OFF_JUMP_DELAY = 4;

    // elytra costs
    public static int maxCharge;
    public static int takeOffCost;
    public static int flapCost;
    public static int rechargeTicksInAir;
    public static int rechargeTicksOnGround;
    public static int flareTicksPerChargePoint;
    public static double exhaustionPerChargePoint;
    public static int minFood;
    public static int cooldownTicks;
    public static int softCeiling;
    public static int hardCeiling;
    public static int ceilingRange;
    public static boolean classicMode;

    // set up config file
    static
    {
        Pair<Server, ForgeConfigSpec> serverSpecPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = serverSpecPair.getLeft();
        SERVER_SPEC = serverSpecPair.getRight();
    }

    // handles 'baking' the config into ram-accessible objects that are much faster
    // than ForgeConfig. This is VERY important because we rely on some of these
    // not just every tick, but *every frame*

    //@formatter:off
    public static void bake() {
        maxCharge = SERVER.maxCharge.get();
        takeOffCost = SERVER.takeOffCost.get();
        flapCost = SERVER.flapCost.get();
        rechargeTicksInAir = SERVER.rechargeTicksInAir.get();
        rechargeTicksOnGround = SERVER.rechargeTicksOnGround.get();
        flareTicksPerChargePoint = SERVER.flareTicksPerChargePoint.get();
        exhaustionPerChargePoint = SERVER.exhaustionPerChargePoint.get();
        minFood = SERVER.minFood.get();
        cooldownTicks = SERVER.cooldownTicks.get();
        softCeiling = SERVER.softCeiling.get();
        hardCeiling = SERVER.hardCeiling.get();
        classicMode = SERVER.classicMode.get();

        // ensure that soft ceiling is always below or equal to the hard ceiling
        if (softCeiling > hardCeiling) {
            softCeiling = hardCeiling;
            SERVER.softCeiling.set(hardCeiling);
        }
        ceilingRange = hardCeiling - softCeiling;
    }

    // defines config file format
    public static class Server {

        public final ForgeConfigSpec.IntValue maxCharge;
        public final ForgeConfigSpec.IntValue takeOffCost;
        public final ForgeConfigSpec.IntValue flapCost;
        public final ForgeConfigSpec.IntValue rechargeTicksInAir;
        public final ForgeConfigSpec.IntValue rechargeTicksOnGround;
        public final ForgeConfigSpec.IntValue flareTicksPerChargePoint;
        public final ForgeConfigSpec.DoubleValue exhaustionPerChargePoint;
        public final ForgeConfigSpec.IntValue minFood;
        public final ForgeConfigSpec.IntValue cooldownTicks;
        public final ForgeConfigSpec.IntValue softCeiling;
        public final ForgeConfigSpec.IntValue hardCeiling;
        public final ForgeConfigSpec.BooleanValue classicMode;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push(BetterFlight.MODID);

            maxCharge = builder
                    .comment("Maximum points of charge in a 'full' elytra meter.")
                    .defineInRange("MaxCharge", 20, 3, 255);

            takeOffCost = builder
                    .comment("Meter point cost to take off.")
                    .defineInRange("TakeOffCost", 3, 0, 255);

            flapCost = builder
                    .comment("Meter point cost to flap wings.")
                    .defineInRange("FlapCost", 2, 0, 255);

            rechargeTicksInAir = builder
                    .comment("Time, in ticks, it takes to recharge 1 point on the meter while in the air.")
                    .defineInRange("RechargeTicksInAir", 80, 5, 600);

            rechargeTicksOnGround = builder
                    .comment("Time, in ticks, it takes to recharge 1 point on the meter while on the ground.")
                    .defineInRange("RechargeTicksOnGround", 10, 5, 600);

            flareTicksPerChargePoint = builder
                    .comment("Time, in ticks, players can flare per point on the meter.")
                    .defineInRange("FlareTicksPerChargePoint", 40, 5, 600);

            exhaustionPerChargePoint = builder
                    .comment("How much food it costs to recharge a point on the meter.")
                    .defineInRange("ExhaustionPerChargePoint", 4.0D, 0.0D, 20.0D);

            minFood = builder
                    .comment("Minimum food required on hunger bar to recharge elytra meter. (6 = same as sprint)")
                    .defineInRange("MinFood", 6, 0, 20);

            cooldownTicks = builder
                    .comment("Time, in ticks, players must wait between wing flaps.")
                    .defineInRange("CooldownTicks", 10, 5, 200);

            softCeiling = builder
                    .comment("Height above which flapping begins to be less effective. (Must be less than hardCeiling).")
                    .defineInRange("softCeiling", 256, 0, 10000);

            hardCeiling = builder
                    .comment("Height above which flapping no longer provides any thrust.")
                    .defineInRange("hardCeiling", 400, 0, 10000);

            classicMode = builder.comment("If True, enable pre-2.0.0 flight system")
                            .define("classicMode",false);
            builder.pop();
        }
    }
}