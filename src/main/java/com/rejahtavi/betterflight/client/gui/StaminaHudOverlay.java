package com.rejahtavi.betterflight.client.gui;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientConfig;
import com.rejahtavi.betterflight.client.ClientData;
import com.rejahtavi.betterflight.events.ClientEvents;
import com.rejahtavi.betterflight.util.InputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;


public class StaminaHudOverlay
{
    private static final int SPRITE_WIDTH = 9;
    private static final int SPRITE_HEIGHT = 9;

    //columns
    private static final int NONE = 0;
    private static final int FILL_FULL = 9;
    private static final int FILL_HALF = 18;
    private static final int WHITE_OUTLINE = 27;
    private static final int FLARE_OUTLINE = 36;
    private static final int RED_OUTLINE = 45;

    //Row durability states
    private static final int FULL_DURABILITY = 0;
    private static final int HALF_DURABILITY = 9;
    private static final int QUARTER_DURABILITY = 18;
    private static final int LOW_DURABILITY = 27;

    private static final Random random = new Random(System.currentTimeMillis());

    private static final ResourceLocation staminaIcons =
            new ResourceLocation(BetterFlight.MODID, "textures/elytraspritesheet.png");
    private static int shakeEffectTimer = 0;
    private static int regenEffectTimer = 0;

    @Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents
    {
        @SubscribeEvent
        public static void registerGuiOverlay(RegisterGuiOverlaysEvent event)
        {
            event.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), BetterFlight.MODID, BFSTAMINA);
        }
    }

    public static final IGuiOverlay BFSTAMINA = ((gui, guiGraphics, partialTick, screenWidth, screenHeight) ->
    {
        if (!ClientData.isFlightEnabled() || !ClientData.isWearingFunctionalWings()) return;
        if(ClientConfig.CLIENT.classicHud.get()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!gui.shouldDrawSurvivalElements() || minecraft.options.hideGui) return;
        if(minecraft.player != null && minecraft.player.isPassenger()) return;

        int x = screenWidth / 2;
        int y = screenHeight;
        int shakeX = 0;
        int shakeY = 0;
        int rightOffset = gui.rightHeight;


        int durability = getDurabilityState(ClientEvents.elytraDurability);

        if (durability == LOW_DURABILITY)
        {
            if (minecraft.level != null && !minecraft.isPaused())
            {
                long thisTick = minecraft.level.getGameTime();
                if (shakeEffectTimer > 0)
                {
                    if (thisTick % 3 == 0)
                    {
                        shakeX = random.nextInt(2) - 1;
                    } else if (thisTick % 3 == 1)
                    {
                        shakeY = random.nextInt(2) - 1;
                    }
                    shakeEffectTimer--;
                } else if (thisTick % 20 == 0)
                {
                    shakeEffectTimer = 20;
                }
            }
        } else
        {
            shakeEffectTimer = 0;
        }

        //Render Empty Resources
        for (int i = 0; i < 10; i++)
        {
            guiGraphics.blit(staminaIcons, getXPos(x, i) + shakeX, y - rightOffset + shakeY,
                    NONE, durability,
                    SPRITE_WIDTH, SPRITE_HEIGHT,
                    256, 256);
        }

        //Render filled resources
        for (int i = 0; i < 10; i++)
        {
            if ((i + 1) <= Math.ceil((double) InputHandler.charge / 2.0d)
                    && InputHandler.charge > 0)
            {
                int type = ((i + 1 == Math.ceil((double) InputHandler.charge / 2.0d)
                        && (InputHandler.charge % 2 != 0)) ? FILL_HALF : FILL_FULL);
                guiGraphics.blit(staminaIcons, getXPos(x, i) + shakeX, y - rightOffset + shakeY,
                        type, durability,
                        SPRITE_WIDTH, SPRITE_HEIGHT,
                        256, 256);
            }
        }
        if (ClientData.isFlaring())
        {
            for (int i = 0; i < 10; i++)
            {
                guiGraphics.blit(staminaIcons, getXPos(x, i) + shakeX, y - rightOffset + shakeY,
                        FLARE_OUTLINE, durability,
                        SPRITE_WIDTH, SPRITE_HEIGHT,
                        256, 256);
            }
        }
        if (shakeEffectTimer > 0)
        {
            for (int i = 0; i < 10; i++)
            {
                guiGraphics.blit(staminaIcons, getXPos(x, i) + shakeX, y - rightOffset + shakeY,
                        RED_OUTLINE, durability,
                        SPRITE_WIDTH, SPRITE_HEIGHT,
                        256, 256);
            }
        }
        if (regenEffectTimer > 0)
        {
            for (int i = 0; i < 10; i++)
            {
                guiGraphics.blit(staminaIcons, getXPos(x, i) + shakeX, y - rightOffset + shakeY,
                        WHITE_OUTLINE, durability,
                        SPRITE_WIDTH, SPRITE_HEIGHT,
                        256, 256);
            }
            regenEffectTimer--;
        }
    });

    private static int getDurabilityState(double durability)
    {
        if (durability > 0.95f)
            return LOW_DURABILITY;
        else if (durability > 0.75f)
        {
            return QUARTER_DURABILITY;
        } else if (durability > 0.50f)
        {
            return HALF_DURABILITY;
        }
        return FULL_DURABILITY;
    }

    private static int getXPos(int x, int i)
    {
        return x + 90 - ((i + 1) * (SPRITE_WIDTH - 1));
    }

    public static void startRegenAnimation()
    {
        if (regenEffectTimer == 0)
            regenEffectTimer = 10;
    }
    //int x = screenWidth / 2;
    //widgetPosX = scaleWidth / 2
    //int y = screenHeight;
    //int rightOffset = FeathersClientConfig.AFFECTED_BY_RIGHT_HEIGHT.get() ? gui.rightHeight : 0;
    // gui.rightHeight states how tall the right GUI already is.

    //guiGraphics.blit(resource location,
    //                left x screen, top y screen,
    //                left x in png, top y in png,
    //                width, height,
    //                256, 256);

}
