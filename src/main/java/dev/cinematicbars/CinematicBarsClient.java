package dev.cinematicbars;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

public class CinematicBarsClient implements ClientModInitializer {
    // Соотношение сторон "киношного" кадра
    private static final float TARGET_ASPECT = 2.39f;
    // Время появления/исчезновения полос, секунды
    private static final float ANIM_SECONDS = 0.4f;

    private static KeyBinding toggleKey;
    private static boolean cinematic = false;
    private static float progress = 0f;
    private static long lastTime = 0L;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cinematicbars.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F1,
                "key.categories.cinematicbars"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggleKey.wasPressed()) {
                if (mc.world == null) continue;
                cinematic = !cinematic;
                mc.options.hudHidden = cinematic;
            }
            // Если интерфейс вернули вручную, выключаем киношный режим
            if (cinematic && (!mc.options.hudHidden || mc.world == null)) {
                cinematic = false;
            }
        });

        HudRenderCallback.EVENT.register(CinematicBarsClient::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();

        long now = Util.getMeasuringTimeMs();
        float dt = lastTime == 0L ? 0f : (now - lastTime) / 1000f;
        lastTime = now;

        boolean active = cinematic && mc.world != null && mc.currentScreen == null;

        float step = dt / ANIM_SECONDS;
        progress = active ? Math.min(1f, progress + step) : Math.max(0f, progress - step);
        if (progress <= 0f) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        float fullBar = Math.max(0f, (h - w / TARGET_ASPECT) / 2f);
        float eased = progress * progress * (3f - 2f * progress);
        int bar = Math.round(fullBar * eased);
        if (bar <= 0) return;

        ctx.fill(0, 0, w, bar, 0xFF000000);
        ctx.fill(0, h - bar, w, h, 0xFF000000);
    }
}
