package com.ryxn.elytramace;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ElytraMaceModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigScreen(parent);
    }

    private static class ConfigScreen extends Screen {
        private final Screen parent;
        ConfigScreen(Screen parent) { super(Text.literal("Elytra Mace Settings")); this.parent = parent; }

        @Override protected void init() {
            int cx = width / 2;
            int y = 45;
            addDrawableChild(CyclingButtonWidget.onOffBuilder()
                    .initiallyChecked(ElytraMaceClient.CONFIG.enabled)
                    .build(cx - 100, y, 200, 20, Text.literal("Enabled"), (b, v) -> ElytraMaceClient.CONFIG.enabled = v));
            y += 28;
            addDrawableChild(CyclingButtonWidget.onOffBuilder()
                    .initiallyChecked(ElytraMaceClient.CONFIG.requireCrosshairEntity)
                    .build(cx - 100, y, 200, 20, Text.literal("Require target"), (b, v) -> ElytraMaceClient.CONFIG.requireCrosshairEntity = v));
            y += 28;
            addDrawableChild(CyclingButtonWidget.onOffBuilder()
                    .initiallyChecked(ElytraMaceClient.CONFIG.autoStartGliding)
                    .build(cx - 100, y, 200, 20, Text.literal("Auto glide assist"), (b, v) -> ElytraMaceClient.CONFIG.autoStartGliding = v));
            y += 28;
            addDrawableChild(CyclingButtonWidget.onOffBuilder()
                    .initiallyChecked(ElytraMaceClient.CONFIG.autoFirework)
                    .build(cx - 100, y, 200, 20, Text.literal("Auto fireworks"), (b, v) -> ElytraMaceClient.CONFIG.autoFirework = v));
            y += 35;
            addDrawableChild(new IntSlider(cx - 100, y, "Attack delay", 1, 10, ElytraMaceClient.CONFIG.attackDelayTicks,
                    v -> ElytraMaceClient.CONFIG.attackDelayTicks = v));
            y += 28;
            addDrawableChild(new IntSlider(cx - 100, y, "Launch delay", 0, 30, ElytraMaceClient.CONFIG.launchDelayTicks,
                    v -> ElytraMaceClient.CONFIG.launchDelayTicks = v));
            addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(cx - 100, height - 35, 200, 20).build());
        }
        private void close() { ElytraMaceClient.CONFIG.save(); client.setScreen(parent); }
        @Override public void close() { ElytraMaceClient.CONFIG.save(); client.setScreen(parent); }

        private static class IntSlider extends SliderWidget {
            private final String label; private final int min, max; private final java.util.function.IntConsumer setter;
            IntSlider(int x,int y,String label,int min,int max,int value,java.util.function.IntConsumer setter) {
                super(x,y,200,20,Text.empty(),(value-min)/(double)(max-min)); this.label=label;this.min=min;this.max=max;this.setter=setter; updateMessage(); }
            @Override protected void updateMessage() { setMessage(Text.literal(label + ": " + Math.round(value*(max-min)+min))); }
            @Override protected void applyValue() { setter.accept((int)Math.round(value*(max-min)+min)); updateMessage(); }
        }
    }
  }
              
