package com.citygrid;

/** Все настройки мода. Меняй числа и пересобирай. */
public final class GridConfig {
    private GridConfig() {}

    /** Шанс (1 из N на случайный тик верхнего блока) самопроизвольной поломки. Чем меньше N - тем чаще. */
    public static final int FAULT_DRY = 400;      // ясная погода
    public static final int FAULT_RAIN = 100;     // дождь
    public static final int FAULT_THUNDER = 40;   // гроза

    /** Шанс (1 из N на случайный тик), что в грозу молния ударит именно в этот столб. */
    public static final int LIGHTNING_CHANCE = 12;

    /** Молния от столба вызывает настоящие пожары и урон (true) или только эффект (false). */
    public static final boolean LIGHTNING_FIRE = false;

    /** Радиус (в блоках), в котором фонарь ищет рабочий электрический столб. */
    public static final int POWER_RANGE = 8;

    /** Радиус поражения при ударе молнии (в блоках). Ближе 2 блоков - ломается всегда. */
    public static final int STRIKE_RADIUS = 3;
}
