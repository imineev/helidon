package io.helidon.messaging;

public interface Session<T> {

    T getSession();

    default <C> C unwrap(Class<C> unwrapType) {
        if (unwrapType == null) {
            throw new IllegalArgumentException("The target class must not be `null`");
        }
        try {
            return unwrapType.cast(this);
        }
        catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot unwrap an instance of " + this.getClass().getName()
                    + " to " + unwrapType.getName(), e);
        }
    }
}
