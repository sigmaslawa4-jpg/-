package com.citygrid;

/** Все настройки мода. Меняй числа и пересобирай. */
public final class GridConfig {
    private GridConfig() {}

    // ---- поломки ----
    /** Шанс (1 из N на случайный тик верхнего блока) самопроизвольной поломки. Чем меньше N - тем чаще. */
    public static final int FAULT_DRY = 400;
    public static final int FAULT_RAIN = 100;
    public static final int FAULT_THUNDER = 40;
    /** Шанс (1 из N), что в грозу молния ударит именно в этот столб. */
    public static final int LIGHTNING_CHANCE = 12;
    /** Молния от столба вызывает настоящие пожары и урон (true) или только эффект (false). */
    public static final boolean LIGHTNING_FIRE = false;
    /** Радиус поражения при ударе молнии. Ближе 2 блоков - ломается всегда. */
    public static final int STRIKE_RADIUS = 3;

    // ---- сеть ----
    /** Максимальная длина одного провода между столбами (блоков). */
    public static final int WIRE_MAX = 24;
    /** Максимум проводов на один столб/щиток/подстанцию. */
    public static final int WIRE_LINKS_MAX = 6;
    /** Радиус, в котором уличный фонарь ищет рабочий столб под напряжением. */
    public static final int STREET_RANGE = 6;
    /** Радиус действия электрощитка: лампы и ТВ в этом радиусе считаются подключёнными к дому. */
    public static final int HOUSE_RADIUS = 14;

    // ---- генератор ----
    /** Радиус, в котором генератор подхватывает щиток, если в сети пропал ток. */
    public static final int GENERATOR_RADIUS = 12;
    public static final int FUEL_COAL = 1600;        // тиков работы от 1 угля (80 сек)
    public static final int FUEL_COAL_BLOCK = 16000;
    public static final int MAX_FUEL = 72000;        // максимум в баке (1 час)
}
