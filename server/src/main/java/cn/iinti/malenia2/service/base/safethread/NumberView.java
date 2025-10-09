package cn.iinti.malenia2.service.base.safethread;

import java.util.function.Supplier;

/**
 * helper for gauge monitor
 */
public class NumberView extends Number {

    private final Supplier<Number> supplier;

    public NumberView(Supplier<Number> supplier) {
        this.supplier = supplier;
    }

    @Override
    public int intValue() {
        return supplier.get().intValue();
    }

    @Override
    public long longValue() {
        return supplier.get().longValue();
    }

    @Override
    public float floatValue() {
        return supplier.get().floatValue();
    }

    @Override
    public double doubleValue() {
        return supplier.get().doubleValue();
    }
}
