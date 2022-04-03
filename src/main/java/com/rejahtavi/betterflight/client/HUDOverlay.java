package com.rejahtavi.betterflight.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.config.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class HUDOverlay {

    private static final int ICON_SIZE = 16;
    private static final int HALF_ICON_SIZE = 8;
    private static final int TEXTURE_SIZE = 128;
    private static final int OFFSET_FROM_CURSOR = 24;
    private static final int OFFSET_TO_HOTBAR_SIDES = 105;
    private static final int OFFSET_ABOVE_HOTBAR = 40;

    public static final ResourceLocation elytraIcons = new ResourceLocation(BetterFlight.MODID,
            "textures/elytraicons.png");

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {

        // called every frame to draw the new elytra HUD
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            renderOverlay(event.getMatrixStack());
        }
    }

    public static void renderOverlay(MatrixStack stack) {

        // only draw hud element when elytra is both equipped and functional
        if (InputHandler.isElytraEquipped == false) return;
        if (InputHandler.elytraDamageValue <= 1) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        int shakeX = 0;
        int shakeY = 0;
        int scaleWidth = mc.getWindow().getGuiScaledWidth();
        int scaleHeight = mc.getWindow().getGuiScaledHeight();
        int widgetPosX = 0;
        int widgetPosY = 0;

        // calculate position on screen based on the selected config option
        switch (Config.widgetLocation) {
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

        // determine which charge glyph to show (selects a column)
        int elytraChargeOffset = InputHandler.elytraCharge;

        // determine which damage variant to show (selects a row)
        int elytraDurabilityOffset = 0;
        if (InputHandler.elytraDurability > 0.50f) elytraDurabilityOffset = 1;
        if (InputHandler.elytraDurability > 0.75f) elytraDurabilityOffset = 2;
        if (InputHandler.elytraDurability > 0.90f) elytraDurabilityOffset = 3;

        // The following IF blocks choose a border color
        int borderColorOffset = 0;

        // critical durability takes precedence over all other borders
        // elytraDurability works backwards, this triggers when 5% use is left.
        if (InputHandler.elytraDurability > 0.95) {

            if (mc.level != null) {
                long thisTick = mc.level.getGameTime();

                // flash border white and red (column 6 and 7 borders)
                borderColorOffset = (int) ((thisTick / 5) % 2) + 6;

                // move icon randomly +/- 1 pixel in each direction,
                // unevenly. very chaotic and attention grabbing
                if (((thisTick / 3) % 2) > 0) {
                    // left-right moves on ticks divisible by 3
                    shakeX = InputHandler.random.nextInt(3) - 1;
                }
                else {
                    // up-down moves on all other ticks
                    shakeY = InputHandler.random.nextInt(3) - 1;
                }
            }
        }

        // second priority is flaring. this is the yellow border when the meter is being
        // drained to slow down
        else if (InputHandler.isFlaring) {
            borderColorOffset = 4;
        }

        // third priority is depletion. this is the red flash for losing a meter tick
        else if (InputHandler.showDepletionTicks > 0) {
            borderColorOffset = 2;
        }

        // lowest priority is recharge. this is the white flash gaining a meter tick
        else if (InputHandler.showRechargeTicks > 0) {
            borderColorOffset = 1;
        }

        // finally, we are ready to draw the icon.

        // switch to the widget atlas texture
        mc.getTextureManager().bind(elytraIcons);

        // draw the elytra meter itself
        IngameGui.blit(stack, widgetPosX + shakeX, widgetPosY + shakeY,
                elytraChargeOffset * ICON_SIZE, elytraDurabilityOffset * ICON_SIZE,
                ICON_SIZE, ICON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        // draw the colored border on top
        IngameGui.blit(stack, widgetPosX + shakeX, widgetPosY + shakeY,
                borderColorOffset * ICON_SIZE, (elytraDurabilityOffset + 4) * ICON_SIZE,
                ICON_SIZE, ICON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        // switch back to the normal HUD icons texture before we give
        // control back to the HUD renderer, so we don't corrupt the HUD
        mc.getTextureManager().getTexture(AbstractGui.GUI_ICONS_LOCATION).bind();
    }

    // TODO: Add speedometer and/or optimal glide slope indicator?
    // Perhaps 'flight googles' as a helmet / curio to show them?

    // Unused text drawing routine
    // private static void drawText(MatrixStack stack, Minecraft mc, String text,
    // float posX, float posY, int color) {
    // mc.font.drawShadow(stack, text, posX, posY, 0xFFFFFFFF);
    // }
}
