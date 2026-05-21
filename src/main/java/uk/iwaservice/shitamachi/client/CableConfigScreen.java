package uk.iwaservice.shitamachi.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import uk.iwaservice.shitamachi.block.ConnectionData;
import uk.iwaservice.shitamachi.network.ServerboundSetCableConfigPacket;

@OnlyIn(Dist.CLIENT)
public class CableConfigScreen extends Screen {

    private static final int SLIDER_W = 200;
    private static final int SLIDER_H = 20;
    // thickness: 0.005 ~ 0.100
    private static final float T_MIN = 0.005f, T_MAX = 0.100f;
    // sag factor: 0.00 ~ 0.30
    private static final float S_MIN = 0.00f,  S_MAX = 0.30f;

    private float thickness;
    private float sag;

    public CableConfigScreen(ItemStack stack) {
        super(Component.translatable("screen.shitamachi.cable_config"));
        thickness = ConnectionData.DEFAULT_THICKNESS;
        sag       = ConnectionData.DEFAULT_SAG;
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("cable_thickness")) thickness = tag.getFloat("cable_thickness");
            if (tag.contains("cable_sag"))       sag       = tag.getFloat("cable_sag");
        }
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        addRenderableWidget(new ThicknessSlider(cx - SLIDER_W / 2, cy - 30));
        addRenderableWidget(new SagSlider(cx - SLIDER_W / 2, cy + 10));
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"), btn -> onClose())
                .bounds(cx - 50, cy + 50, 100, SLIDER_H)
                .build());
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new ServerboundSetCableConfigPacket(thickness, sag));
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static double normalize(float v, float min, float max) {
        return (v - min) / (max - min);
    }

    private static float denormalize(double t, float min, float max) {
        return (float) (min + t * (max - min));
    }

    private class ThicknessSlider extends AbstractSliderButton {
        ThicknessSlider(int x, int y) {
            super(x, y, SLIDER_W, SLIDER_H, Component.empty(), normalize(thickness, T_MIN, T_MAX));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.shitamachi.cable_thickness",
                    String.format("%.3f", thickness)));
        }

        @Override
        protected void applyValue() {
            thickness = denormalize(value, T_MIN, T_MAX);
        }
    }

    private class SagSlider extends AbstractSliderButton {
        SagSlider(int x, int y) {
            super(x, y, SLIDER_W, SLIDER_H, Component.empty(), normalize(sag, S_MIN, S_MAX));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.shitamachi.cable_sag",
                    String.format("%.3f", sag)));
        }

        @Override
        protected void applyValue() {
            sag = denormalize(value, S_MIN, S_MAX);
        }
    }
}
