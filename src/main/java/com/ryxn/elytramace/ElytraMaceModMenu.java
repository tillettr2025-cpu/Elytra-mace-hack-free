package com.ryxn.elytramace;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class ElytraMaceModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }

    private static class ConfigScreen extends Screen {

        private final Screen parent;

        protected ConfigScreen(Screen parent) {
            super(Text.literal("Elytra Mace Settings"));
            this.parent = parent;
        }

        @Override
        protected void init() {

            int centerX = width / 2;
            int y = 35;

            /*
             * =========================
             * TOGGLES
             * =========================
             */

            addDrawableChild(
                    toggle(
                            centerX,
                            y,
                            "Enabled",
                            ElytraMaceClient.CONFIG.enabled,
                            value -> ElytraMaceClient.CONFIG.enabled = value
                    )
            );

            y += 25;

            addDrawableChild(
                    toggle(
                            centerX,
                            y,
                            "Auto Aim",
                            ElytraMaceClient.CONFIG.autoAim,
                            value -> ElytraMaceClient.CONFIG.autoAim = value
                    )
            );

            y += 25;

            addDrawableChild(
                    toggle(
                            centerX,
                            y,
                            "Auto Target",
                            ElytraMaceClient.CONFIG.autoTarget,
                            value -> ElytraMaceClient.CONFIG.autoTarget = value
                    )
            );

            y += 25;

            addDrawableChild(
                    toggle(
                            centerX,
                            y,
                            "Require Target",
                            ElytraMaceClient.CONFIG.requireTarget,
                            value -> ElytraMaceClient.CONFIG.requireTarget = value
                    )
            );

            y += 25;

            addDrawableChild(
                    toggle(
                            centerX,
                            y,
                            "Auto Start Elytra",
                            ElytraMaceClient.CONFIG.autoStartGliding,
                            value -> ElytraMaceClient.CONFIG.autoStartGliding = value
                    )
            );

            y += 25;

            addDrawableChild(
                    toggle(
                            centerX,
                            y,
                            "Auto Firework",
                            ElytraMaceClient.CONFIG.autoFirework,
                            value -> ElytraMaceClient.CONFIG.autoFirework = value
                    )
            );

            y += 25;

            addDrawableChild(
                    toggle(
                            centerX,
                            y,
                            "Descending Only",
                            ElytraMaceClient.CONFIG.attackOnlyWhileDescending,
                            value -> ElytraMaceClient.CONFIG.attackOnlyWhileDescending = value
                    )
            );

            y += 25;

            addDrawableChild(
                    toggle(
                            centerX,
                            y,
                            "Restore Hotbar Slot",
                            ElytraMaceClient.CONFIG.restoreSlot,
                            value -> ElytraMaceClient.CONFIG.restoreSlot = value
                    )
            );

            y += 30;

            /*
             * =========================
             * TARGET RANGE
             * =========================
             */

            addDrawableChild(
                    new DoubleSlider(
                            centerX - 100,
                            y,
                            "Target Range",
                            4.0,
                            64.0,
                            ElytraMaceClient.CONFIG.targetRange,
                            value -> ElytraMaceClient.CONFIG.targetRange = value
                    )
            );

            y += 25;

            /*
             * =========================
             * AIM FOV
             * =========================
             */

            addDrawableChild(
                    new DoubleSlider(
                            centerX - 100,
                            y,
                            "Aim FOV",
                            5.0,
                            180.0,
                            ElytraMaceClient.CONFIG.aimFov,
                            value -> ElytraMaceClient.CONFIG.aimFov = value
                    )
            );

            y += 25;

            /*
             * =========================
             * AIM SPEED
             * =========================
             */

            addDrawableChild(
                    new DoubleSlider(
                            centerX - 100,
                            y,
                            "Aim Speed",
                            0.05,
                            1.0,
                            ElytraMaceClient.CONFIG.aimSpeed,
                            value -> ElytraMaceClient.CONFIG.aimSpeed = value
                    )
            );

            y += 25;

            /*
             * =========================
             * TARGET LEAD
             * =========================
             */

            addDrawableChild(
                    new DoubleSlider(
                            centerX - 100,
                            y,
                            "Target Lead",
                            0.0,
                            8.0,
                            ElytraMaceClient.CONFIG.leadTicks,
                            value -> ElytraMaceClient.CONFIG.leadTicks = value
                    )
            );

            y += 25;

            /*
             * =========================
             * ATTACK DELAY
             * =========================
             */

            addDrawableChild(
                    new IntSlider(
                            centerX - 100,
                            y,
                            "Attack Delay",
                            1,
                            10,
                            ElytraMaceClient.CONFIG.attackDelayTicks,
                            value -> ElytraMaceClient.CONFIG.attackDelayTicks = value
                    )
            );

            y += 25;

            /*
             * =========================
             * FIREWORK DELAY
             * =========================
             */

            addDrawableChild(
                    new IntSlider(
                            centerX - 100,
                            y,
                            "Firework Delay",
                            1,
                            20,
                            ElytraMaceClient.CONFIG.fireworkDelayTicks,
                            value -> ElytraMaceClient.CONFIG.fireworkDelayTicks = value
                    )
            );

            /*
             * =========================
             * DONE BUTTON
             * =========================
             */

            addDrawableChild(
                    ButtonWidget.builder(
                            Text.literal("Done"),
                            button -> close()
                    )
                            .dimensions(
                                    centerX - 100,
                                    height - 30,
                                    200,
                                    20
                            )
                            .build()
            );
        }

        /*
         * =========================
         * TOGGLE CREATOR
         * =========================
         *
         * IMPORTANT:
         * This returns CyclingButtonWidget<Boolean>,
         * NOT ButtonWidget.
         */

        private static CyclingButtonWidget<Boolean> toggle(
                int centerX,
                int y,
                String name,
                boolean value,
                Consumer<Boolean> setter
        ) {

            return CyclingButtonWidget
                    .onOffBuilder(value)
                    .build(
                            centerX - 100,
                            y,
                            200,
                            20,
                            Text.literal(name),
                            (button, newValue) -> setter.accept(newValue)
                    );
        }

        /*
         * =========================
         * CLOSE
         * =========================
         */

        @Override
        public void close() {

            ElytraMaceClient.CONFIG.save();

            if (client != null) {
                client.setScreen(parent);
            }
        }

        /*
         * =========================
         * INTEGER SLIDER
         * =========================
         */

        private static class IntSlider extends SliderWidget {

            private final String label;
            private final int min;
            private final int max;
            private final IntConsumer setter;

            IntSlider(
                    int x,
                    int y,
                    String label,
                    int min,
                    int max,
                    int current,
                    IntConsumer setter
            ) {

                super(
                        x,
                        y,
                        200,
                        20,
                        Text.empty(),
                        (current - min)
                                / (double) (max - min)
                );

                this.label = label;
                this.min = min;
                this.max = max;
                this.setter = setter;

                updateMessage();
            }

            @Override
            protected void updateMessage() {

                int current =
                        (int) Math.round(
                                value * (max - min) + min
                        );

                setMessage(
                        Text.literal(
                                label + ": " + current
                        )
                );
            }

            @Override
            protected void applyValue() {

                int current =
                        (int) Math.round(
                                value * (max - min) + min
                        );

                setter.accept(current);

                updateMessage();
            }
        }

        /*
         * =========================
         * DOUBLE SLIDER
         * =========================
         */

        private static class DoubleSlider extends SliderWidget {

            private final String label;
            private final double min;
            private final double max;
            private final DoubleConsumer setter;

            DoubleSlider(
                    int x,
                    int y,
                    String label,
                    double min,
                    double max,
                    double current,
                    DoubleConsumer setter
            ) {

                super(
                        x,
                        y,
                        200,
                        20,
                        Text.empty(),
                        (current - min)
                                / (max - min)
                );

                this.label = label;
                this.min = min;
                this.max = max;
                this.setter = setter;

                updateMessage();
            }

            @Override
            protected void updateMessage() {

                double current =
                        value * (max - min) + min;

                setMessage(
                        Text.literal(
                                label
                                        + ": "
                                        + String.format(
                                                "%.2f",
                                                current
                                        )
                        )
                );
            }

            @Override
            protected void applyValue() {

                double current =
                        value * (max - min) + min;

                setter.accept(current);

                updateMessage();
            }
        }
    }
}
