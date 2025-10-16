package pt.gongas.box.model.box;

import pt.gongas.box.model.level.BoxLevel;

public record BoxData(
        String boxName,
        String ownerName,
        String centerLocation,
        Integer level,
        BoxLevel boxLevel,
        String firstTime,
        String lastTime
) {

    /**
     * Merge this BoxData with another, preferring non-null (or non-zero) values from the other BoxData.
     */
    public BoxData merge(BoxData other) {
        if (other == null) return this;

        return new BoxData(
                other.boxName() != null ? other.boxName() : this.boxName(),
                other.ownerName() != null ? other.ownerName() : this.ownerName(),
                other.centerLocation() != null ? other.centerLocation() : this.centerLocation(),
                other.level() != null ? other.level() : this.level(),
                other.boxLevel() != null ? other.boxLevel() : this.boxLevel(),
                other.firstTime() != null ? other.firstTime() : this.firstTime(),
                other.lastTime() != null ? other.lastTime() : this.lastTime()
        );
    }

    public static BoxData withBoxName(String boxName) {
        return new BoxData(boxName, null, null, null, null, null, null);
    }

    public static BoxData withOwnerName(String ownerName) {
        return new BoxData(null, ownerName, null, null, null, null, null);
    }

    public static BoxData withCenterLocation(String centerLocation) {
        return new BoxData(null, null, centerLocation, null, null, null, null);
    }

    public static BoxData withLevel(Integer level) {
        return new BoxData(null, null, null, level, null, null, null);
    }

    public static BoxData withBoxLevel(BoxLevel boxLevel) {
        return new BoxData(null, null, null, null, boxLevel, null, null);
    }

    public static BoxData withFirstTime(String firstTime) {
        return new BoxData(null, null, null, null, null, firstTime, null);
    }

    public static BoxData withLastTime(String lastTime) {
        return new BoxData(null, null, null, null, null, null, lastTime);
    }

}
