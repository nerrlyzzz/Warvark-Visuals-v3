package dev.warax.visuals.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.warax.visuals.hud.Notifications;
import dev.warax.visuals.module.Module.Category;
import dev.warax.visuals.render.Render3D;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    // --- Визуалы ---
    public static Module ASPECT, FOV, ZOOM, CROSSHAIR, VIEWMODEL, SLOWSWING, OLDEQUIP, HITPARTICLES, HITBOXES, SELFNAME,
            NOHURT, NOBOB;
    // --- Мир ---
    public static Module FULLBRIGHT, TIME, WEATHER, NOFOG, WORLDCOLOR, JUMPCIRCLES, CHINAHAT, TRAILS, BLOCKOUTLINE,
            NOBLOCKPARTICLES;
    // --- Экран ---
    public static Module NOFIRE, NOWATER, NOPUMPKIN, NOVIGNETTE, NOPORTAL, NOBOSSBAR, NOSCOREBOARD, NOTOTEM;
    // --- HUD ---
    public static Module WATERMARK, MODLIST, FPS, CPS, COORDS, PING, CLOCK, BPS, SERVER, SESSION, KEYSTROKES, ARMOR,
            POTIONS, TARGETHUD, ITEMCOUNTER, HELDDUR, COMPASS, NOTIFY;
    // --- Меню ---
    public static Module MENU;
    public static Module PARTICLES;
    public static Setting.Mode ptShape, ptColor, hitColor;
    public static Setting.Num ptCount, ptSize, ptLife, ptGravity;
    public static Setting.Bool ptWalk, ptJump, ptAmbient, ptReplace, ptCollide;

    public static final String[] COLORS = {"Тема", "Радуга", "Белый", "Красный", "Зелёный", "Голубой"};

    public static Setting.Mode aspectMode;
    public static Setting.Num aspectNum, fovNum, zoomFactor, timeNum;
    public static Setting.Bool zoomSmooth, zoomCam;
    public static Setting.Num chSize, chGap, chThick;
    public static Setting.Bool chDot, chOutline;
    public static Setting.Mode chColor;
    public static Setting.Mode themeMode, wmStyle;
    public static Setting.Bool customTitle;
    public static Setting.Bool blurBg, glassUi;
    public static Setting.Num blurPower, glowPower;

    public static Setting.Num vmX, vmY, vmZ, vmScale, swingMul, hitCount;
    public static Setting.Mode hitType;
    public static Setting.Bool fogFluids, wcSky, wcFog;
    public static Setting.Mode wcColor, jcColor, hatColor, trColor, boColor;
    public static Setting.Num jcRadius, jcTime, hatSize, trLen, trHeight, boWidth;
    public static Setting.Bool boFill, potHide, thHead;

    private static final double[] ASPECT_PRESETS = {0, 4.0 / 3.0, 5.0 / 4.0, 16.0 / 10.0, 16.0 / 9.0, 21.0 / 9.0, 1.0};

    private static double savedGamma = -1;
    private static boolean wasZoom;
    private static double zoomCur = 1.0;
    private static boolean lastHitboxes;
    private static boolean wasOnGround = true;
    private static final Random RNG = new Random();

    /** Скорость игрока, блоков/сек (сглаженная). */
    public static double bps;
    /** Время запуска клиента — для «Времени сессии». */
    public static final long START_TIME = System.currentTimeMillis();
    /** Последняя ударенная цель — для Target HUD. */
    public static LivingEntity target;
    public static long targetTime;

    private ModuleManager() {
    }

    private static Module reg(Module m) {
        MODULES.add(m);
        return m;
    }

    public static void init() {
        // ===== Визуалы =====
        ASPECT = reg(new Module("Соотношение сторон", "Растягивает картинку: 4:3, 16:10 или своё значение.", Category.VISUAL));
        aspectMode = ASPECT.add(new Setting.Mode("Пресет", new String[]{"Своё", "4:3", "5:4", "16:10", "16:9", "21:9", "1:1"}, 1));
        aspectNum = ASPECT.add(new Setting.Num("Своё значение", 0.5, 3.0, 0.01, 1.33));

        FOV = reg(new Module("Угол обзора", "FOV вне лимита ползунка из настроек (30–130).", Category.VISUAL));
        fovNum = FOV.add(new Setting.Num("FOV", 30, 130, 1, 90));

        ZOOM = reg(new Module("Зум", "Приближение, пока зажата клавиша (по умолчанию C).", Category.VISUAL));
        ZOOM.hold = true;
        ZOOM.enabled = true;
        ZOOM.key = GLFW.GLFW_KEY_C;
        zoomFactor = ZOOM.add(new Setting.Num("Кратность", 1.5, 10, 0.5, 4));
        zoomSmooth = ZOOM.add(new Setting.Bool("Плавный зум", true));
        zoomCam = ZOOM.add(new Setting.Bool("Плавная камера при зуме", true));

        CROSSHAIR = reg(new Module("Свой прицел", "Заменяет ванильный прицел на настраиваемый.", Category.VISUAL));
        chSize = CROSSHAIR.add(new Setting.Num("Длина", 1, 15, 1, 5));
        chGap = CROSSHAIR.add(new Setting.Num("Зазор", 0, 10, 1, 3));
        chThick = CROSSHAIR.add(new Setting.Num("Толщина", 1, 4, 1, 1));
        chDot = CROSSHAIR.add(new Setting.Bool("Точка в центре", false));
        chOutline = CROSSHAIR.add(new Setting.Bool("Контур", true));
        chColor = CROSSHAIR.add(new Setting.Mode("Цвет", new String[]{"Тема", "Белый", "Красный", "Зелёный", "Голубой"}, 0));

        VIEWMODEL = reg(new Module("Положение руки", "ViewModel: сдвиг и размер предметов в руках.", Category.VISUAL));
        vmX = VIEWMODEL.add(new Setting.Num("Сдвиг X", -1, 1, 0.05, 0));
        vmY = VIEWMODEL.add(new Setting.Num("Сдвиг Y", -1, 1, 0.05, 0));
        vmZ = VIEWMODEL.add(new Setting.Num("Сдвиг Z", -1, 1, 0.05, 0));
        vmScale = VIEWMODEL.add(new Setting.Num("Размер", 0.3, 1.5, 0.05, 1));

        SLOWSWING = reg(new Module("Анимация взмаха", "Замедляет или ускоряет взмах рукой (только визуально).", Category.VISUAL));
        swingMul = SLOWSWING.add(new Setting.Num("Длительность", 0.5, 4, 0.25, 2));

        OLDEQUIP = reg(new Module("Без анимации смены", "Предмет не опускается при смене слота и ударе, как в 1.8.", Category.VISUAL));

        HITPARTICLES = reg(new Module("Частицы при ударе", "Дополнительные частицы на цели при ударе.", Category.VISUAL));
        hitType = HITPARTICLES.add(new Setting.Mode("Форма", dev.warax.visuals.render.Particles3D.SHAPES, 0));
        hitColor = HITPARTICLES.add(new Setting.Mode("Цвет", COLORS, 0));
        hitCount = HITPARTICLES.add(new Setting.Num("Количество", 1, 30, 1, 10));

        HITBOXES = reg(new Module("Хитбоксы", "Показывает хитбоксы сущностей (как F3+B).", Category.VISUAL));
        SELFNAME = reg(new Module("Свой ник", "Ваш ник над головой при виде от 3-го лица.", Category.VISUAL));
        NOHURT = reg(new Module("Без тряски при уроне", "Камера не дёргается при получении урона.", Category.VISUAL));
        NOBOB = reg(new Module("Без покачивания камеры", "Отключает покачивание при ходьбе.", Category.VISUAL));

        // ===== Мир =====
        FULLBRIGHT = reg(new Module("Яркость", "Максимальная яркость: ночью и в пещерах светло.", Category.WORLD));
        TIME = reg(new Module("Время суток", "Фиксирует время на клиенте. 0 — утро, 6000 — день, 13000 — ночь.", Category.WORLD));
        timeNum = TIME.add(new Setting.Num("Время", 0, 24000, 250, 6000));
        WEATHER = reg(new Module("Ясная погода", "Скрывает дождь и грозу (только у вас на экране).", Category.WORLD));

        NOFOG = reg(new Module("Без тумана", "Убирает туман на краю прорисовки.", Category.WORLD));
        fogFluids = NOFOG.add(new Setting.Bool("И в воде/лаве", false));

        WORLDCOLOR = reg(new Module("Цвет неба и тумана", "Окрашивает небо и туман в выбранный цвет.", Category.WORLD));
        wcColor = WORLDCOLOR.add(new Setting.Mode("Цвет", COLORS, 0));
        wcSky = WORLDCOLOR.add(new Setting.Bool("Небо", true));
        wcFog = WORLDCOLOR.add(new Setting.Bool("Туман", true));

        JUMPCIRCLES = reg(new Module("Круги при прыжке", "Расходящееся кольцо под ногами при прыжке.", Category.WORLD));
        jcColor = JUMPCIRCLES.add(new Setting.Mode("Цвет", COLORS, 0));
        jcRadius = JUMPCIRCLES.add(new Setting.Num("Радиус", 0.5, 3, 0.1, 1.2));
        jcTime = JUMPCIRCLES.add(new Setting.Num("Длительность, с", 0.3, 3, 0.1, 1.2));

        CHINAHAT = reg(new Module("Шляпа (China Hat)", "Конусная шляпа над головой при виде от 3-го лица.", Category.WORLD));
        hatColor = CHINAHAT.add(new Setting.Mode("Цвет", COLORS, 0));
        hatSize = CHINAHAT.add(new Setting.Num("Размер", 0.3, 1.2, 0.05, 0.65));

        TRAILS = reg(new Module("След за игроком", "Цветная лента-след за вами.", Category.WORLD));
        trColor = TRAILS.add(new Setting.Mode("Цвет", COLORS, 0));
        trLen = TRAILS.add(new Setting.Num("Длина", 5, 100, 1, 30));
        trHeight = TRAILS.add(new Setting.Num("Высота", 0.05, 1.8, 0.05, 0.3));

        BLOCKOUTLINE = reg(new Module("Обводка блока", "Своя цветная обводка блока под прицелом.", Category.WORLD));
        boColor = BLOCKOUTLINE.add(new Setting.Mode("Цвет", COLORS, 0));
        boFill = BLOCKOUTLINE.add(new Setting.Bool("Заливка", true));
        boWidth = BLOCKOUTLINE.add(new Setting.Num("Толщина линий", 1, 5, 0.5, 2));

        PARTICLES = reg(new Module("Частицы Warax", "Свои светящиеся частицы с физикой вместо ванильных: шаги, прыжки, светлячки, криты.", Category.WORLD));
        PARTICLES.enabled = true;
        ptShape = PARTICLES.add(new Setting.Mode("Форма", dev.warax.visuals.render.Particles3D.SHAPES, 0));
        ptColor = PARTICLES.add(new Setting.Mode("Цвет", COLORS, 0));
        ptCount = PARTICLES.add(new Setting.Num("Количество", 2, 40, 1, 12));
        ptSize = PARTICLES.add(new Setting.Num("Размер", 0.04, 0.4, 0.01, 0.12));
        ptLife = PARTICLES.add(new Setting.Num("Время жизни", 0.3, 4, 0.1, 1.2));
        ptGravity = PARTICLES.add(new Setting.Num("Гравитация", 0, 15, 0.5, 6));
        ptReplace = PARTICLES.add(new Setting.Bool("Заменять ванильные (криты, урон)", true));
        ptWalk = PARTICLES.add(new Setting.Bool("При ходьбе", false));
        ptJump = PARTICLES.add(new Setting.Bool("При прыжке", true));
        ptAmbient = PARTICLES.add(new Setting.Bool("Светлячки вокруг", false));
        ptCollide = PARTICLES.add(new Setting.Bool("Отскок от блоков", true));

        NOBLOCKPARTICLES = reg(new Module("Без частиц блоков", "Убирает частицы при ломании блоков (+FPS).", Category.WORLD));

        // ===== Экран =====
        NOFIRE = reg(new Module("Без огня на экране", "Убирает огонь, закрывающий обзор, когда горите.", Category.SCREEN));
        NOWATER = reg(new Module("Без воды на экране", "Убирает синий оверлей под водой.", Category.SCREEN));
        NOPUMPKIN = reg(new Module("Без тыквы на экране", "Убирает оверлей тыквы, надетой на голову.", Category.SCREEN));
        NOVIGNETTE = reg(new Module("Без виньетки", "Убирает затемнение по краям экрана.", Category.SCREEN));
        NOPORTAL = reg(new Module("Без эффекта портала", "Убирает фиолетовый оверлей в портале.", Category.SCREEN));
        NOBOSSBAR = reg(new Module("Скрыть босс-бар", "Прячет полоски боссов и серверные бары сверху.", Category.SCREEN));
        NOSCOREBOARD = reg(new Module("Скрыть скорборд", "Прячет таблицу справа.", Category.SCREEN));
        NOTOTEM = reg(new Module("Без анимации тотема", "Тотем не закрывает экран при срабатывании.", Category.SCREEN));

        // ===== HUD =====
        WATERMARK = reg(new Module("Водяной знак", "Плашка по центру сверху: логин аккаунта, FPS, пинг, время.", Category.HUD));
        WATERMARK.enabled = true;
        wmStyle = WATERMARK.add(new Setting.Mode("Стиль", new String[]{"Pulse", "Классика"}, 0));
        MODLIST = reg(new Module("Список модулей", "Включённые модули справа вверху.", Category.HUD));
        MODLIST.enabled = true;
        FPS = reg(new Module("FPS", "Счётчик кадров в секунду.", Category.HUD));
        FPS.enabled = true;
        CPS = reg(new Module("CPS", "Клики в секунду: ЛКМ | ПКМ.", Category.HUD));
        CPS.enabled = true;
        COORDS = reg(new Module("Координаты", "XYZ и направление взгляда.", Category.HUD));
        COORDS.enabled = true;
        PING = reg(new Module("Пинг", "Задержка до сервера.", Category.HUD));
        CLOCK = reg(new Module("Часы", "Реальное время.", Category.HUD));
        BPS = reg(new Module("Скорость", "Скорость движения в блоках в секунду.", Category.HUD));
        SERVER = reg(new Module("Сервер", "Адрес сервера, на котором вы играете.", Category.HUD));
        SESSION = reg(new Module("Время сессии", "Сколько вы в игре с момента запуска.", Category.HUD));
        KEYSTROKES = reg(new Module("Клавиши", "WASD, пробел и кнопки мыши на экране.", Category.HUD));
        ARMOR = reg(new Module("Броня", "Предметы брони и их прочность.", Category.HUD));
        POTIONS = reg(new Module("Эффекты", "Активные эффекты зелий с таймером.", Category.HUD));
        potHide = POTIONS.add(new Setting.Bool("Скрыть ванильные иконки", true));
        TARGETHUD = reg(new Module("Target HUD", "Панель с ником, HP и бронёй того, кого вы ударили.", Category.HUD));
        thHead = TARGETHUD.add(new Setting.Bool("Голова игрока", true));
        ITEMCOUNTER = reg(new Module("Счётчик предметов", "Тотемы, яблоки, жемчуг и пузырьки опыта у хотбара.", Category.HUD));
        HELDDUR = reg(new Module("Прочность в руке", "Прочность предмета в руке слева от хотбара.", Category.HUD));
        COMPASS = reg(new Module("Компас", "Полоса сторон света вверху экрана.", Category.HUD));
        NOTIFY = reg(new Module("Уведомления", "Всплывающее сообщение при вкл/выкл модуля.", Category.HUD));
        NOTIFY.enabled = true;

        // ===== Секрет =====
        SecretFx.init();
        ExtraFx.init();

        // ===== Меню =====
        MENU = reg(new Module("Оформление меню", "Тема главного меню, ClickGUI и HUD — одна на всё.", Category.MISC));
        MENU.alwaysOn = true;
        MENU.enabled = true;
        themeMode = MENU.add(new Setting.Mode("Тема", Theme.NAMES, 0));
        customTitle = MENU.add(new Setting.Bool("Своё главное меню", true));
        glassUi = MENU.add(new Setting.Bool("Стеклянный стиль", true));
        blurBg = MENU.add(new Setting.Bool("Размытие фона", true));
        blurPower = MENU.add(new Setting.Num("Сила размытия", 1, 3, 1, 2));
        glowPower = MENU.add(new Setting.Num("Свечение", 0, 5, 1, 3));
    }

    // ---------- Цвета ----------

    /** Цвет по выбранному режиму. t (0..1) — позиция в градиенте/радуге. */
    public static int color(Setting.Mode mode, float t) {
        switch (mode.index) {
            case 1:
                return rainbow(t);
            case 2:
                return 0xFFFFFFFF;
            case 3:
                return 0xFFFF3B3B;
            case 4:
                return 0xFF3BFF6A;
            case 5:
                return 0xFF3BD7FF;
            default:
                return Theme.grad(t);
        }
    }

    public static int rainbow(float offset) {
        float h = ((System.currentTimeMillis() % 4000L) / 4000f + offset) % 1f;
        return 0xFF000000 | hsv(h, 0.65f, 1f);
    }

    private static int hsv(float h, float s, float v) {
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    // ---------- Значения для миксинов ----------

    public static double aspectRatio() {
        int i = aspectMode.index;
        return i == 0 ? aspectNum.value : ASPECT_PRESETS[i];
    }

    /** Применяет FOV-модуль и зум к значению поля зрения. Вызывается раз в кадр. */
    public static double modifyFov(double fov) {
        if (FOV.isActive()) {
            double base = MinecraftClient.getInstance().options.fov;
            if (base > 0) {
                fov = fov * (fovNum.value / base);
            }
        }
        double target = ZOOM.isActive() ? zoomFactor.value : 1.0;
        zoomCur = zoomSmooth.value ? zoomCur + (target - zoomCur) * 0.25 : target;
        if (Math.abs(zoomCur - target) < 0.001) {
            zoomCur = target;
        }
        return zoomCur > 1.001 ? fov / zoomCur : fov;
    }

    /** Камера не в жидкости (для тумана/цвета мира). */
    public static boolean cameraDry() {
        try {
            return MinecraftClient.getInstance().gameRenderer.getCamera().getSubmergedFluidState().isEmpty();
        } catch (RuntimeException e) {
            return true;
        }
    }

    /** Вызывается миксином при ударе по сущности. */
    public static void onAttack(Entity e) {
        if (e instanceof LivingEntity) {
            target = (LivingEntity) e;
            targetTime = System.currentTimeMillis();
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (HITPARTICLES.isActive() && mc.world != null) {
            dev.warax.visuals.render.Particles3D.onEntity(e, hitCount.i(), hitType.index, hitColor);
        }
    }

    /** Вызывается при включении/выключении модуля. */
    public static void onToggle(Module m) {
        if (NOTIFY != null && NOTIFY.enabled && m != NOTIFY) {
            Notifications.push(m.name, m.enabled);
        }
    }

    // ---------- Тики ----------

    public static void tick(MinecraftClient mc) {
        SecretFx.tick(mc);
        ExtraFx.tick(mc);
        dev.warax.visuals.render.Particles3D.tick(mc);
        // бинды модулей
        if (mc.currentScreen == null) {
            long handle = mc.getWindow().getHandle();
            for (Module m : MODULES) {
                if (m.hold || m.key < 0) {
                    continue;
                }
                boolean down = InputUtil.isKeyPressed(handle, m.key);
                if (down && !m.prevDown) {
                    m.toggle();
                }
                m.prevDown = down;
            }
        }

        // фулбрайт
        if (FULLBRIGHT.isActive()) {
            if (savedGamma < 0) {
                savedGamma = mc.options.gamma;
            }
            mc.options.gamma = 16.0;
        } else {
            restoreGamma(mc);
        }

        // плавная камера при зуме
        boolean zooming = ZOOM.isActive();
        if (zooming != wasZoom) {
            if (zoomCam.value) {
                mc.options.smoothCameraEnabled = zooming;
            }
            wasZoom = zooming;
        }

        // время суток
        if (TIME.isActive() && mc.world != null) {
            mc.world.getLevelProperties().setTimeOfDay((long) timeNum.value);
        }

        // хитбоксы — переключаем только при смене состояния, чтобы F3+B продолжал работать
        boolean hb = HITBOXES.isActive();
        if (hb != lastHitboxes) {
            mc.getEntityRenderDispatcher().setRenderHitboxes(hb);
            lastHitboxes = hb;
        }

        ClientPlayerEntity p = mc.player;
        if (p == null) {
            Render3D.clearTrail();
            target = null;
            return;
        }
        // скорость
        double dx = p.getX() - p.prevX, dz = p.getZ() - p.prevZ;
        double cur = Math.sqrt(dx * dx + dz * dz) * 20.0;
        bps += (cur - bps) * 0.3;

        // круги при прыжке
        boolean ground = p.isOnGround();
        if (wasOnGround && !ground && p.getVelocity().y > 0.2 && JUMPCIRCLES.isActive()) {
            Render3D.addCircle(p.getPos());
        }
        wasOnGround = ground;

        // след
        if (TRAILS.isActive()) {
            Render3D.addTrail(p.getPos(), trLen.i());
        } else {
            Render3D.clearTrail();
        }
    }

    private static void restoreGamma(MinecraftClient mc) {
        if (savedGamma >= 0) {
            mc.options.gamma = savedGamma;
            savedGamma = -1;
        }
    }

    public static void shutdown(MinecraftClient mc) {
        restoreGamma(mc);
        save();
    }

    // ---------- Конфиг ----------

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("waraxvisuals.json");
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(p)) {
            JsonObject root = new JsonParser().parse(r).getAsJsonObject();
            JsonObject mods = root.getAsJsonObject("modules");
            if (mods == null) {
                return;
            }
            for (Module m : MODULES) {
                if (!mods.has(m.name)) {
                    continue;
                }
                JsonObject o = mods.getAsJsonObject(m.name);
                try {
                    if (o.has("enabled") && !m.alwaysOn) {
                        m.enabled = o.get("enabled").getAsBoolean();
                    }
                    if (o.has("key")) {
                        m.key = o.get("key").getAsInt();
                    }
                    JsonObject ss = o.getAsJsonObject("settings");
                    if (ss != null) {
                        for (Setting s : m.settings) {
                            JsonElement e = ss.get(s.name);
                            if (e != null) {
                                try {
                                    s.load(e);
                                } catch (RuntimeException ignored) {
                                    // битое значение — остаётся по умолчанию
                                }
                            }
                        }
                    }
                } catch (RuntimeException ignored) {
                    // битая запись модуля — пропускаем
                }
            }
        } catch (Exception ignored) {
            // нет доступа или битый файл — работаем с настройками по умолчанию
        }
    }

    public static void save() {
        try {
            JsonObject mods = new JsonObject();
            for (Module m : MODULES) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled);
                o.addProperty("key", m.key);
                JsonObject ss = new JsonObject();
                for (Setting s : m.settings) {
                    ss.add(s.name, s.save());
                }
                o.add("settings", ss);
                mods.add(m.name, o);
            }
            JsonObject root = new JsonObject();
            root.add("modules", mods);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Path p = path();
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                gson.toJson(root, w);
            }
        } catch (Exception ignored) {
            // не критично
        }
    }
}
