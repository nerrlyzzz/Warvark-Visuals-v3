package dev.warax.visuals.gui;

import dev.warax.visuals.module.Theme;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Мини-игра «Doodle Jump»: стрелки ←/→ — движение, прыгайте по платформам как можно выше. */
public class DoodleJump extends Screen {
    private static final int PW = 20, PH = 26;
    private static final int PL_W = 56, PL_H = 8;
    private static final double GRAV = 0.30, JUMP = 9.6;
    private static final double MAX_FALL = -12;
    private static final int H_SPEED = 5;

    private final List<int[]> platforms = new ArrayList<>(); // x, y(мир), w, type (0 обычная,1 хрупкая,2 пружина)
    private double pX, pY, vy, scroll;
    private int bestScore, topGen;
    private int state; // 0 меню, 1 игра, 2 гейм овер
    private boolean left, right;

    public DoodleJump() {
        super(new LiteralText("Doodle Jump"));
    }

    @Override
    public void init() {
        super.init();
        reset();
    }

    private void reset() {
        platforms.clear();
        pX = width / 2.0;
        pY = 0;
        vy = 0;
        scroll = 0;
        bestScore = 0;
        topGen = 0;
        state = 0;
        left = right = false;
        platforms.add(new int[]{width / 2 - PL_W / 2, 0, PL_W, 0});
        while (topGen < height * 1.2) {
            genPlatform();
        }
    }

    private int screenY(double wy) {
        return (int) (height * 0.62 - (wy - scroll));
    }

    private void genPlatform() {
        int y = topGen + 46 + (int) (Math.random() * 46);
        int x = 6 + (int) (Math.random() * Math.max(1, width - PL_W - 12));
        double r = Math.random();
        int type = r < 0.18 ? 2 : (r < 0.34 ? 1 : 0);
        platforms.add(new int[]{x, y, PL_W, type});
        topGen = y;
    }

    private boolean horizOverlap(int[] pl) {
        return pX + PW > pl[0] && pX < pl[0] + pl[2];
    }

    @Override
    public void tick() {
        if (state != 1) {
            return;
        }
        if (left) {
            pX -= H_SPEED;
        }
        if (right) {
            pX += H_SPEED;
        }
        if (pX < -PW) {
            pX = width;
        } else if (pX > width) {
            pX = -PW;
        }
        pY += vy;
        vy -= GRAV;
        if (vy < MAX_FALL) {
            vy = MAX_FALL;
        }
        if (vy <= 0) {
            for (int i = 0; i < platforms.size(); i++) {
                int[] pl = platforms.get(i);
                if (pY >= pl[1] - 2 && pY <= pl[1] + 8 && horizOverlap(pl)) {
                    if (pl[3] == 2) {
                        vy = JUMP * 1.7;
                    } else {
                        vy = JUMP;
                    }
                    if (pl[3] == 1) {
                        platforms.remove(i);
                    }
                    break;
                }
            }
        }
        if (pY > scroll) {
            scroll = pY;
        }
        if ((int) pY > bestScore) {
            bestScore = (int) pY;
        }
        while (topGen < scroll + height * 1.2) {
            genPlatform();
        }
        platforms.removeIf(pl -> screenY(pl[1]) > height + 40);
        if (scroll - pY > height * 1.0) {
            state = 2;
        }
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

    private void start() {
        reset();
        state = 1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (state == 2) {
            if (inRestart(mx, my)) {
                start();
            }
        } else if (state == 0) {
            state = 1;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            left = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            right = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_UP) {
            if (state == 0) {
                state = 1;
            } else if (state == 2) {
                start();
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
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            left = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            right = false;
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float delta) {
        int w = width, h = height;
        Gfx.vgrad(ms, 0, 0, w, h, Theme.bgTop(), Theme.bgBottom());
        float t = WaraxBaseScreen.time();
        for (int i = 0; i < 24; i++) {
            int sx = (i * 137 % w);
            int sy = (int) ((i * 211 % h) + t * (6 + i % 4)) % h;
            Gfx.rect(ms, sx, sy, sx + 1, sy + 1, Theme.alpha(0xFFFFFF, 0x30));
        }
        for (int[] pl : platforms) {
            drawPlatform(ms, pl);
        }
        drawPlayer(ms);

        String sc = "Счёт: " + bestScore;
        Fonts.shadow(ms, sc, 10, 10, 0xFFFFFFFF);
        String hi = "Высота: " + (int) pY;
        Fonts.shadow(ms, hi, w - Fonts.width(hi) - 10, 10, Theme.hi());

        if (state == 0) {
            String hint = "←/→ — движение, пробел — старт";
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

    private void drawPlatform(MatrixStack ms, int[] pl) {
        int x = pl[0], sy = screenY(pl[1]);
        int type = pl[3];
        int body, cap;
        if (type == 2) {
            body = 0xFFe93c8a;
            cap = 0xFFff8fd0;
        } else if (type == 1) {
            body = 0xFFd39a3f;
            cap = 0xFFe8c06a;
        } else {
            body = 0xFF4daf42;
            cap = 0xFF57c04f;
        }
        Gfx.rect(ms, x, sy, x + pl[2], sy + PL_H, 0x60000000);
        Gfx.round(ms, x, sy, x + pl[2], sy + PL_H, 3, body);
        Gfx.round(ms, x + 2, sy, x + pl[2] - 2, sy + 3, 2, cap);
        if (type == 2) {
            Gfx.glow(ms, x, sy, x + pl[2], sy + PL_H, 3, body, 3, 0x50);
        }
    }

    private void drawPlayer(MatrixStack ms) {
        int sx = (int) pX, sy = (int) (height * 0.62);
        Gfx.glow(ms, sx - 2, sy - 2, sx + PW + 2, sy + PH + 2, 5, Theme.a1(), 3, 0x55);
        Gfx.round(ms, sx, sy, sx + PW, sy + PH, 6, Theme.grad(Gfx.wave(0)));
        Gfx.round(ms, sx + 4, sy + 4, sx + PW - 4, sy + 6, 2, Theme.alpha(0xFFFFFF, 0x60));
        Gfx.circle(ms, sx + 6, sy + 12, 2, 0xFF000000);
        Gfx.circle(ms, sx + PW - 6, sy + 12, 2, 0xFF000000);
        Gfx.circle(ms, sx + 6, sy + 11, 1, 0xFFFFFFFF);
        Gfx.circle(ms, sx + PW - 6, sy + 11, 1, 0xFFFFFFFF);
    }
}
