package com.rejahtavi.betterflight.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rejahtavi.betterflight.BetterFlight;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class Keybinding {
    public static final String KEY_CATEGORY_BETTERFLIGHT = BetterFlight.MODID;
    public static final String KEY_TAKEOFF = "Takeoff";
    public static final String KEY_FLAP = "Flap";
    public static final String KEY_FLARE = "Flare";
    public static final String KEY_WIDGET_POS = "Toggle Widget Position";

    // key mappings
    //public static final KeyMapping takeOffKey = new KeyMapping(KEY_TAKEOFF, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, KEY_CATEGORY_BETTERFLIGHT);
    public static final KeyMapping flapKey = new KeyMapping(KEY_FLAP,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, KEY_CATEGORY_BETTERFLIGHT);
    public static final KeyMapping flareKey = new KeyMapping(KEY_FLARE,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, KEY_CATEGORY_BETTERFLIGHT);
    public static final KeyMapping widgetPosKey = new KeyMapping(KEY_WIDGET_POS,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F10, KEY_CATEGORY_BETTERFLIGHT);
}
