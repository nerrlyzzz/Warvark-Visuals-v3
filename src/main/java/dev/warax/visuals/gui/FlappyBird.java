package dev.warax.visuals.gui;

import dev.warax.visuals.module.Theme;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Мини-игра «Flappy Bird»: клик/пробел — взмах, цель — пролететь между трубами. */
public class FlappyBird extends Screen {
    private static final int PW = 52, GAP = 150, SPEED = 3, SPAWN = 230, GROUND = 64;
    private static final int R = 11;
    private static final double GRAV = 0.42, FLAP = -7.2;

    private final List<int[]> pipes = new ArrayList<>(); // x, gapY, passed
    private int score;
    private double birdY, vel;
    private int state; // 0 = меню, 1 = игра, 2 = гейм овер

    public FlappyBird() {
        super(new LiteralText("Flappy Bird"));
    }

    @Override
    public void init() {
        super.init();
        reset();
    }

    private int birdX() {
        return (int) (width * 0.35);
    }

    private void reset() {
        pipes.clear();
        score = 0;
        birdY = height / 2.0;
        vel = 0;
        state = 0;
    }

    private int[] newPipe(int x) {
        int maxGap = height - GROUND - 120;
        int gapY = 120 + (int) (Math.random() * (maxGap - 120));
        return new int[]{x, gapY, 0};
    }

    private boolean hit(int[] p) {
        int bx = birdX(), by = (int) birdY;
        if (bx + R > p[0] && bx - R < p[0] + PW && by - R < p[1] - GAP / 2) {
            return true;
        }
        return bx + R > p[0] && bx - R < p[0] + PW && by + R > p[1] + GAP / 2;
    }

    @Override
    public void tick() {
        if (state != 1) {
            return;
        }
        birdY += vel;
        vel += GRAV;
        if (birdY - R <= 0 || birdY + R >= height - GROUND) {
            state = 2;
            return;
        }
        if (pipes.isEmpty()) {
            pipes.add(newPipe(width + 40));
        } else {
            int[] last = pipes.get(pipes.size() - 1);
            if (last[0] < width - SPAWN) {
                pipes.add(newPipe(last[0] + SPAWN));
            }
        }
        for (int i = 0; i < pipes.size(); i++) {
            int[] p = pipes.get(i);
            p[0] -= SPEED;
            if (hit(p)) {
                state = 2;
                return;
            }
            if (p[2] == 0 && p[0] + PW < birdX()) {
                p[2] = 1;
                score++;
            }
        }
        pipes.removeIf(p -> p[0] + PW < 0);
    }

    private int restartBtnX() {
        return width / 2 - 65;
    }

    private int restartBtnY() {
        return height / 2 + 34;
    }

    private boolean inRestart(double mx, double my) {
        return mx >= restartBtnX() && mx <= restartBtnX() + 130 && my >= restartBtnY() && my <= restartBtnY() + 32;
    }

    private void flap() {
        if (state == 0) {
            state = 1;
        }
        if (state == 1) {
            vel = FLAP;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (state == 2) {
            if (inRestart(mx, my)) {
                reset();
                state = 1;
            }
        } else {
            flap();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_ENTER) {
            if (state == 2) {
                reset();
                state = 1;
            } else {
                flap();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.openScreen(new MenuScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float delta) {
        int w = width, h = height;
        Gfx.vgrad(ms, 0, 0, w, h, Theme.bgTop(), Theme.bgBottom());
        int gy = h - GROUND;
        Gfx.rect(ms, 0, gy, w, h, 0xFF25301f);
        Gfx.rect(ms, 0, gy, w, gy + 4, 0xFF57c04f);
        for (int[] p : pipes) {
            drawPipe(ms, p);
        }
        drawBird(ms);
        String sc = "Счёт: " + score;
        Fonts.shadow(ms, sc, (w - Fonts.width(sc)) / 2f, 14, 0xFFFFFFFF);

        if (state == 0) {
            String hint = "Клик / Пробел — взмах";
            Fonts.shadow(ms, hint, (w - Fonts.width(hint)) / 2f, h / 2 + 46, 0xFFdddddd);
        }
        if (state == 2) {
            Gfx.rect(ms, 0, 0, w, h, 0x90000000);
            String over = "Гейм овер";
            Fonts.shadow(ms, over, (w - Fonts.width(over)) / 2f, h / 2 - 44, 0xFFFFFFFF);
            Fonts.shadow(ms, sc, (w - Fonts.width(sc)) / 2f, h / 2 - 24, Theme.hi());
            int bx = restartBtnX(), by = restartBtnY();
            boolean hv = inRestart(mx, my);
            Gfx.glass(ms, bx, by, bx + 130, by + 32, 8, hv ? Theme.alpha(Theme.a1(), 0x45) : 0x50000000);
            String again = "Заново";
            Fonts.shadow(ms, again, bx + 65 - Fonts.width(again) / 2f, by + 12, 0xFFFFFFFF);
        }
    }

    private void drawPipe(MatrixStack ms, int[] p) {
        int gy = height - GROUND;
        Gfx.rect(ms, p[0], 0, p[0] + PW, p[1] - GAP / 2, 0xFF4daf42);
        Gfx.rect(ms, p[0] - 3, p[1] - GAP / 2 - 14, p[0] + PW + 3, p[1] - GAP / 2, 0xFF57c04f);
        Gfx.rect(ms, p[0], p[1] + GAP / 2, p[0] + PW, gy, 0xFF4daf42);
        Gfx.rect(ms, p[0] - 3, p[1] + GAP / 2, p[0] + PW + 3, p[1] + GAP / 2 + 14, 0xFF57c04f);
    }

    private void drawBird(MatrixStack ms) {
        int bx = birdX(), by = (int) birdY;
        Gfx.glow(ms, bx - R, by - R, bx + R, by + R, 3, 0xFFf7c83d, 4, 0x60);
        Gfx.circle(ms, bx, by, R, 0xFFf7c83d);
        Gfx.circle(ms, bx + 4, by - 4, 3, 0xFFFFFFFF);
        Gfx.circle(ms, bx + 5, by - 4, 1, 0xFF000000);
        Gfx.rect(ms, bx + R - 2, by, bx + R + 7, by + 3, 0xFFf07b1f);
    }
}
