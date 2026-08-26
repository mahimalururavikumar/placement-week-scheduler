package com.mirailabs.scheduler.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class DatasetConfig {

    private DatasetConfig() {
    }

    public static final int COMPANY_COUNT = 35;
    public static final int STUDENT_COUNT = 800;
    public static final int ROOM_COUNT = 20;

    public static final long RANDOM_SEED = 42L;

    public static final int TIER_1_COMPANIES = 8;
    public static final int TIER_2_COMPANIES = 12;
    public static final int TIER_3_COMPANIES = 15;

    public static final double CGPA_MIN = 6.0;
    public static final double CGPA_MAX = 9.8;

    public static final List<LocalDate> PLACEMENT_DATES = List.of(
            LocalDate.of(2026, 8, 24),
            LocalDate.of(2026, 8, 25),
            LocalDate.of(2026, 8, 26),
            LocalDate.of(2026, 8, 27)
    );

    public static final LocalTime DEFAULT_START_TIME =
            LocalTime.of(9, 0);

    public static final LocalTime DEFAULT_END_TIME =
            LocalTime.of(17, 0);

    public static final int TIER_1_MIN_SHORTLIST = 250;
    public static final int TIER_1_MAX_SHORTLIST = 450;

    public static final int TIER_2_MIN_SHORTLIST = 100;
    public static final int TIER_2_MAX_SHORTLIST = 250;

    public static final int TIER_3_MIN_SHORTLIST = 30;
    public static final int TIER_3_MAX_SHORTLIST = 150;
}