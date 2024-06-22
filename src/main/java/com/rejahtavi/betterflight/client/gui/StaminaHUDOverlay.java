package com.rejahtavi.betterflight.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientData;
import com.rejahtavi.betterflight.events.ClientEvents;
import com.rejahtavi.betterflight.util.InputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


public class StaminaHUDOverlay
{
    private static final int SPRITE_WIDTH = 9;
    private static final int SPRITE_HEIGHT = 9;

    //columns
    private static final int NONE = 0;
    private static final int FILL_FULL = 9;
    private static final int FILL_HALF = 18;
    private static final int RECHARGE_OUTLINE = 27;
    private static final int FLARE_OUTLINE = 36;

    //Row durability states
    private static final int FULL_DURABILITY = 0;
    private static final int HALF_DURABILITY = 1;
    private static final int QUARTER_DURABILITY = 2;
    private static final int LOW_DURABILITY = 3;

    private static final ResourceLocation staminaIcons =
            new ResourceLocation(BetterFlight.MODID, "textures/elytraspritesheet.png");

    @Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void registerGuiOverlay(RegisterGuiOverlaysEvent event)
        {
            event.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), BetterFlight.MODID,BFSTAMINA);
        }
    }

    public static final IGuiOverlay BFSTAMINA = ((gui, guiGraphics, partialTick, screenWidth, screenHeight) ->
    {
        if (!ClientData.isFlightEnabled() || !ClientData.isWearingFunctionalWings()) return;

        Minecraft minecraft = Minecraft.getInstance();
        int x = screenWidth / 2;
        int y = screenHeight;
        int rightOffset = gui.rightHeight;

        int durability = getDurabilityState(ClientEvents.elytraDurability);

        //Render Empty Resources
        for(int i = 0; i< 10; i++)
        {
            guiGraphics.blit(staminaIcons, x+90-((i+1)*(SPRITE_WIDTH-1)), y - rightOffset,
                    NONE,durability,
                    SPRITE_WIDTH,SPRITE_HEIGHT,
                    256, 256);
        }

        //Render filled resources
        for (int i = 0; i < 10; i++)
        {
            if ((i + 1) <= Math.ceil((double) InputHandler.charge / 2.0d)
                    && InputHandler.charge > 0)
            {
                int type = ((i + 1 == Math.ceil((double) InputHandler.charge/ 2.0d)
                        && (InputHandler.charge % 2 != 0)) ? FILL_HALF : FILL_FULL);
                guiGraphics.blit(staminaIcons, x+90-((i+1)*(SPRITE_WIDTH-1)), y - rightOffset,
                        type,durability,
                        SPRITE_WIDTH,SPRITE_HEIGHT,
                        256, 256);
            }
        }
        if(ClientData.isFlaring())
        {
            for(int i = 0; i < 10; i++)
            {
                guiGraphics.blit(staminaIcons, x+90-((i+1)*(SPRITE_WIDTH-1)), y - rightOffset,
                        FLARE_OUTLINE,durability,
                        SPRITE_WIDTH,SPRITE_HEIGHT,
                        256, 256);
            }
        }

    } );

    private static int getDurabilityState(double durability)
    {
        if(durability > 0.95f)
            return LOW_DURABILITY*SPRITE_HEIGHT;
        else if (durability > 0.75f)
        {
            return QUARTER_DURABILITY*SPRITE_HEIGHT;
        } else if (durability > 0.50f)
        {
            return HALF_DURABILITY*SPRITE_HEIGHT;
        }
        return FULL_DURABILITY;
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
