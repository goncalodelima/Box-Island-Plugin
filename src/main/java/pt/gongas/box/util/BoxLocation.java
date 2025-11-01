package pt.gongas.box.util;

public record BoxLocation(double x, double y, double z, float yaw, float pitch) {

    public String serialize() {
        return x + "," + y + "," + z + "," + yaw + "," + pitch;
    }

    public static String serialize(BoxLocation location) {

        if (location == null) {
            return null;
        }

        return location.x() + "," + location.y() + "," + location.z() + "," + location.yaw + "," + location.pitch;
    }

    public static BoxLocation deserialize(String data) throws IllegalArgumentException {

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Input data is null or empty");
        }

        String[] parts = data.split(",");

        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid data format: expected 3 values");
        }

        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            float yaw = Float.parseFloat(parts[3]);
            float pitch = Float.parseFloat(parts[4]);
            return new BoxLocation(x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format", e);
        }

    }

}
