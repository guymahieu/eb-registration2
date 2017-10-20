package org.clarent.griezel.model;

/**
 * @author Guy Mahieu
 * @since 2017-09-25
 */
public enum TimeBlock {

    START_AT_17_30("17:30"),
    START_AT_17_40("17:40", 32),
    START_AT_17_50("17:50"),
    START_AT_18_00("18:00"),
    START_AT_18_10("18:10"),
    START_AT_18_20("18:20"),
    START_AT_18_30("18:30"),
    START_AT_18_40("18:40"),
    START_AT_18_50("18:50"),
    START_AT_19_00("19:00"),
    START_AT_19_10("19:10"),
    START_AT_19_20("19:20"),
    START_AT_19_30("19:30"),
    START_AT_19_40("19:40"),
    START_AT_19_50("19:50"),
    START_AT_20_00("20:00"),
    START_AT_20_10("20:10"),
    START_AT_20_20("20:20"),
    START_AT_20_30("20:30")
    ;

    private static final int MAX_PEOPLE = 30;

    private final String display;
    private final int maxPeople;

    TimeBlock(String display) {
        this(display, MAX_PEOPLE);
    }

    TimeBlock(String display, int maxPeople) {
        this.display = display;
        this.maxPeople = maxPeople;
    }

    public String getDisplay() {
        return display;
    }

    public int getMaxPeople() {
        return maxPeople;
    }
}
