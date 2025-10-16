package pt.gongas.box.util;

public record Result<T>(T value, boolean success, String errorMessage) {

    public static <T> Result<T> ok(T value) {
        return new Result<>(value, true, null);
    }

    public static <T> Result<T> fail(String errorMessage) {
        return new Result<>(null, false, errorMessage);
    }

    public boolean isEmpty() {
        return value == null;
    }

}
