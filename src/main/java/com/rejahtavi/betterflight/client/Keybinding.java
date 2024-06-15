package com.rejahtavi.betterflight.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rejahtavi.betterflight.BetterFlight;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class Keybinding
{
    public static final String KEY_CATEGORY_BETTERFLIGHT = BetterFlight.MODID;
    public static final String KEY_TOGGLE_FLIGHT = "Toggle Flight";
    public static final String KEY_FLAP = "Flap";
    public static final String KEY_FLARE = "Flare";
    public static final String KEY_WIDGET_POS = "Toggle Widget Position";

    // key mappings
    public static final KeyMapping toggleKey = new KeyMapping(KEY_TOGGLE_FLIGHT,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, KEY_CATEGORY_BETTERFLIGHT);
    public static final KeyMapping flapKey = new KeyMapping(KEY_FLAP,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, KEY_CATEGORY_BETTERFLIGHT);
    public static final KeyMapping flareKey = new KeyMapping(KEY_FLARE,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, KEY_CATEGORY_BETTERFLIGHT);
    public static final KeyMapping widgetPosKey = new KeyMapping(KEY_WIDGET_POS,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F10, KEY_CATEGORY_BETTERFLIGHT);
}
