package com.rejahtavi.betterflight.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientConfig;
import com.rejahtavi.betterflight.client.ClientData;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.events.ClientEvents;
import com.rejahtavi.betterflight.util.InputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.GuiOverlayManager;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

public class ClassicHudOverlay
{

    // HUD layout constraints
    private static final int OFFSET_FROM_CURSOR = 24;
    private static final int OFFSET_TO_HOTBAR_SIDES = 105;
    private static final int OFFSET_ABOVE_HOTBAR = 44;

    // Texture Data
    private static final int TEXTURE_SIZE = 128;
    private static final int ICON_SIZE = 16;
    private static final int HALF_ICON_SIZE = 8;

    // sprite row offsets
    private static final int SPRITE_DURABILITY_FULL = 0;
    private static final int SPRITE_DURABILITY_HALF = ICON_SIZE;
    private static final int SPRITE_DURABILITY_QUARTER = 2 * ICON_SIZE;
    private static final int SPRITE_DURABILITY_LOW = 3 * ICON_SIZE;

    // sprite column offsets
    private static final int SPRITE_BORDER_BLACK = 0;
    private static final int SPRITE_BORDER_RECHARGE = ICON_SIZE;
    private static final int SPRITE_BORDER_DEPLETION = 2 * ICON_SIZE;
    private static final int SPRITE_BORDER_FLARE = 3 * ICON_SIZE;
    private static final int SPRITE_METER_EMPTY = 4 * ICON_SIZE;
    private static final int SPRITE_METER_FULL = 5 * ICON_SIZE;
    private static final int SPRITE_ALARM = 6 * ICON_SIZE;

    private static final Random random = new Random(System.currentTimeMillis());

    private static final ResourceLocation elytraIcons = new ResourceLocation(
            BetterFlight.MODID, "textures/elytraicons.png");
    private static int rechargeBorderTimer = 0;
    private static int depletionBorderTimer = 0;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event)
    {
        if (event.getOverlay() == GuiOverlayManager.findOverlay(VanillaGuiOverlay.HOTBAR.id()))
        {
            renderOverlay(event.getGuiGraphics());

        }
    }

    public static void renderOverlay(GuiGraphics guiGraphics)
    {

        // only draw hud element when elytra is both equipped and functional
        if (!ClientData.isFlightEnabled()) return;
        if (!ClientData.isWearingFunctionalWings()) return;
        if (!ClientConfig.CLIENT.classicHud.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        if (mc.player.isPassenger()) return;

        int shakeX = 0;
        int shakeY = 0;
        int scaleWidth = mc.getWindow().getGuiScaledWidth();
        int scaleHeight = mc.getWindow().getGuiScaledHeight();
        int widgetPosX = 0;
        int widgetPosY = 0;

        // calculate position on screen based on the selected config option
        switch (ClientConfig.hudLocation)
        {
            case CURSOR_BELOW:
                widgetPosX = scaleWidth / 2 - HALF_ICON_SIZE;
                widgetPosY = scaleHeight / 2 + OFFSET_FROM_CURSOR - HALF_ICON_SIZE;
                break;

            case CURSOR_LEFT:
                widgetPosX = scaleWidth / 2 - OFFSET_FROM_CURSOR - HALF_ICON_SIZE;
                widgetPosY = scaleHeight / 2 - HALF_ICON_SIZE;
                break;

            case CURSOR_RIGHT:
                widgetPosX = scaleWidth / 2 + OFFSET_FROM_CURSOR - HALF_ICON_SIZE;
                widgetPosY = scaleHeight / 2 - HALF_ICON_SIZE;
                break;

            case CURSOR_ABOVE:
                widgetPosX = scaleWidth / 2 - HALF_ICON_SIZE;
                widgetPosY = scaleHeight / 2 - OFFSET_FROM_CURSOR - HALF_ICON_SIZE;
                break;

            case BAR_LEFT:
                widgetPosX = scaleWidth / 2 - OFFSET_TO_HOTBAR_SIDES - HALF_ICON_SIZE;
                widgetPosY = scaleHeight - 12 - HALF_ICON_SIZE;
                break;

            case BAR_RIGHT:
                widgetPosX = scaleWidth / 2 + OFFSET_TO_HOTBAR_SIDES - HALF_ICON_SIZE;
                widgetPosY = scaleHeight - 12 - HALF_ICON_SIZE;
                break;

            case BAR_CENTER:
            default:
                widgetPosX = scaleWidth / 2 - HALF_ICON_SIZE;
                widgetPosY = scaleHeight - OFFSET_ABOVE_HOTBAR - HALF_ICON_SIZE;
                break;

        }

        // determine which damage variant to show
        int durabilityOffset = SPRITE_DURABILITY_FULL;
        if (ClientEvents.elytraDurability > 0.50f) durabilityOffset = SPRITE_DURABILITY_HALF;
        if (ClientEvents.elytraDurability > 0.75f) durabilityOffset = SPRITE_DURABILITY_QUARTER;
        if (ClientEvents.elytraDurability > 0.90f) durabilityOffset = SPRITE_DURABILITY_LOW;

        // determine which border to show
        int borderOffset = SPRITE_BORDER_BLACK;

        // critical durability takes precedence over all other borders
        // elytraDurability works backwards, this triggers when 5% use is left.
        if (ClientEvents.elytraDurability > 0.95)
        {

            if (mc.level != null)
            {
                long thisTick = mc.level.getGameTime();

                // flash border white and red every 5 ticks
                borderOffset = (int) (((thisTick / 5) % 2) * ICON_SIZE) + SPRITE_ALARM;

                // move icon randomly +/- 1 pixel in each direction, with uneven timing for X and Y
                // very chaotic and attention grabbing
                if (((thisTick / 3) % 2) > 0)
                {
                    // left-right moves on ticks divisible by 3
                    shakeX = random.nextInt(3) - 1;
                } else
                {
                    // up-down moves on all other ticks
                    shakeY = random.nextInt(3) - 1;
                }
            }
        }

        // second priority is flaring. this is the yellow border when the meter is being drained to slow down
        else if (ClientData.isFlaring())
        {
            borderOffset = SPRITE_BORDER_FLARE;
        }

        // third priority is depletion. this is the red flash for losing a meter tick
        else if (depletionBorderTimer > 0)
        {
            borderOffset = SPRITE_BORDER_DEPLETION;
        }

        // lowest priority is recharge. this is the white flash gaining a meter tick
        else if (rechargeBorderTimer > 0)
        {
            borderOffset = SPRITE_BORDER_RECHARGE;
        }

        // determine how much of the meter has been emptied
        int drainedPixels = (int) Math.floor(
                (1.0f - (float) InputHandler.charge / (float) BetterFlightCommonConfig.maxCharge) * ICON_SIZE);

        // finally, we are ready to draw the

        // switch to the widget sprite sheet
        //mc.getTextureManager().getTexture(elytraIcons).bind();
        RenderSystem.setShaderTexture(0, elytraIcons);

        // draw the full meter as the background
        guiGraphics.blit(elytraIcons, widgetPosX + shakeX, widgetPosY + shakeY,
                SPRITE_METER_FULL, durabilityOffset,
                ICON_SIZE, ICON_SIZE,
                TEXTURE_SIZE, TEXTURE_SIZE);

        // draw the elytra meter drain level on top
        guiGraphics.blit(elytraIcons, widgetPosX + shakeX, widgetPosY + shakeY,
                SPRITE_METER_EMPTY, durabilityOffset,
                ICON_SIZE, drainedPixels,
                TEXTURE_SIZE, TEXTURE_SIZE);

        // finally, draw the border
        guiGraphics.blit(elytraIcons, widgetPosX + shakeX, widgetPosY + shakeY,
                borderOffset, durabilityOffset,
                ICON_SIZE, ICON_SIZE,
                TEXTURE_SIZE, TEXTURE_SIZE);

        // switch back to the normal HUD icons texture before we give
        // control back to the HUD renderer, so we don't corrupt the HUD
        //mc.getTextureManager().getTexture(Gui.GUI_ICONS_LOCATION).bind();
        RenderSystem.setShaderTexture(0, elytraIcons);

        // mc.font.drawShadow(stack, "charge: " + ClientEvents.charge, 0, 0, 0xFFFFFFFF);
        // mc.font.drawShadow(stack, "drainPixels: " + drainedPixels, 0, mc.font.lineHeight, 0xFFFFFFFF);
        // mc.font.drawShadow(stack, "charge: " + ActionHandler.charge, 0, mc.font.lineHeight * 2, 0xFFFFFFFF);
        // mc.font.drawShadow(stack, "max: " + Config.elytraMaxCharge, 0, mc.font.lineHeight * 3, 0xFFFFFFFF);
    }

    public static void borderTick()
    {
        if (depletionBorderTimer > 0) depletionBorderTimer--;
        if (rechargeBorderTimer > 0) rechargeBorderTimer--;
    }

    public static void setDepletionBorderTimer(int ticks)
    {
        depletionBorderTimer = ticks;
    }

    public static void setRechargeBorderTimer(int ticks)
    {
        rechargeBorderTimer = ticks;
    }

    public static void cycleWidgetLocation()
    {

        switch (ClientConfig.hudLocation)
        {
            case BAR_CENTER -> ClientConfig.hudLocation = HudLocation.BAR_LEFT;
            case BAR_LEFT -> ClientConfig.hudLocation = HudLocation.BAR_RIGHT;
            case BAR_RIGHT -> ClientConfig.hudLocation = HudLocation.CURSOR_ABOVE;
            case CURSOR_ABOVE -> ClientConfig.hudLocation = HudLocation.CURSOR_RIGHT;
            case CURSOR_RIGHT -> ClientConfig.hudLocation = HudLocation.CURSOR_BELOW;
            case CURSOR_BELOW -> ClientConfig.hudLocation = HudLocation.CURSOR_LEFT;
            case CURSOR_LEFT -> ClientConfig.hudLocation = HudLocation.BAR_CENTER;
        }

        ClientConfig.CLIENT.hudLocation.set(ClientConfig.hudLocation);
        ClientConfig.CLIENT.hudLocation.save();
    }


    // TODO: Add speedometer and/or optimal glide slope indicator?
    //  Perhaps 'flight googles' as a helmet / curio to show them?

}
