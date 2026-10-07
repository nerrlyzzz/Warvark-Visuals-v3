package dev.warax.visuals.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Настройка модуля. Хранится в конфиге по имени. */
public abstract class Setting {
    public final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement e);

    /** Переключатель вкл/выкл. */
    public static class Bool extends Setting {
        public boolean value;

        public Bool(String name, boolean def) {
            super(name);
            this.value = def;
        }

        @Override
        public JsonElement save() {
            return new JsonPrimitive(value);
        }

        @Override
        public void load(JsonElement e) {
            value = e.getAsBoolean();
        }
    }

    /** Числовой ползунок. */
    public static class Num extends Setting {
        public double value;
        public final double min, max, step;

        public Num(String name, double min, double max, double step, double def) {
            super(name);
            this.min = min;
            this.max = max;
            this.step = step;
            this.value = def;
        }

        public void set(double v) {
            double snapped = Math.round((v - min) / step) * step + min;
            value = Math.max(min, Math.min(max, snapped));
        }

        public float f() {
            return (float) value;
        }

        public int i() {
            return (int) Math.round(value);
        }

        public String text() {
            return step >= 1.0 ? String.valueOf(i()) : String.format(java.util.Locale.ROOT, "%.2f", value);
        }

        @Override
        public JsonElement save() {
            return new JsonPrimitive(value);
        }

        @Override
        public void load(JsonElement e) {
            set(e.getAsDouble());
        }
    }

    /** Выбор из списка. */
    public static class Mode extends Setting {
        public final String[] options;
        public int index;

        public Mode(String name, String[] options, int def) {
            super(name);
            this.options = options;
            this.index = def;
        }

        public String get() {
            return options[index];
        }

        public void next() {
            index = (index + 1) % options.length;
        }

        public void prev() {
            index = (index + options.length - 1) % options.length;
        }

        @Override
        public JsonElement save() {
            return new JsonPrimitive(get());
        }

        @Override
        public void load(JsonElement e) {
            String s = e.getAsString();
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(s)) {
                    index = i;
                    return;
                }
            }
        }
    }
}
