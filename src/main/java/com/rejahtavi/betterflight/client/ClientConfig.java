package com.rejahtavi.betterflight.client;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.gui.HudLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ClientConfig
{

    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    // constants, might make some of these configurable later
    public static final int BORDER_FLASH_TICKS = 5;
    public static final float FLAP_SOUND_PITCH = 2.0f;
    public static HudLocation hudLocation = HudLocation.BAR_CENTER;
    public static double takeOffVolume;
    public static double flapVolume;
    public static boolean classicHudStyle;

    // set up config file
    static
    {
        Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = clientSpecPair.getLeft();
        CLIENT_SPEC = clientSpecPair.getRight();
    }

    // handles 'baking' the config into ram-accessible objects that are much faster
    // than ForgeConfig. This is VERY important because we rely on some of these
    // not just every tick, but *every frame*

    //@formatter:off
    public static void bake() {
        takeOffVolume               = CLIENT.takeOffVolume.get();
        flapVolume                  = CLIENT.classicFlapVolume.get();
        hudLocation                 = CLIENT.hudLocation.get();
        classicHudStyle             = CLIENT.classicHud.get();

    }

    // defines config file format
    public static class Client {

        public final ForgeConfigSpec.DoubleValue takeOffVolume;
        public final ForgeConfigSpec.DoubleValue classicFlapVolume;
        public final ForgeConfigSpec.EnumValue<HudLocation> hudLocation;
        public final ForgeConfigSpec.BooleanValue classicHud;


        public Client(ForgeConfigSpec.Builder builder) {
            builder.push(BetterFlight.MODID);

            takeOffVolume = builder
                    .comment("Loudness of the flap sound on takeoff.")
                    .defineInRange("TakeOffVolume", 1.0D, 0.0D, 1.0D);

            classicFlapVolume = builder
                    .comment("Loudness of the flap sound when flapping wings.")
                    .defineInRange("FlapVolume", 0.5D, 0.0D, 1.0D);
            
            hudLocation = builder
                    .comment("Stores preferred position of elytra widget on HUD.\n"
                            + "Options: BAR_CENTER, BAR_LEFT, BAR_RIGHT,\n"
                            + "CURSOR_BELOW, CURSOR_ABOVE, CURSOR_RIGHT, CURSOR_LEFT.")
                    .defineEnum("HudLocation", HudLocation.BAR_CENTER);
            classicHud = builder.comment("If True, enable classic Hud overlay")
                    .define("classicHud",false);
            builder.pop();
        }        
    }
}