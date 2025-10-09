package cn.iinti.malenia2.service.base.safethread;
import java.util.concurrent.atomic.AtomicBoolean;

public class NoShakingValueCallback<T> implements ValueCallback<T> {
    private final ValueCallback<T> valueCallback;
    private final AtomicBoolean called = new AtomicBoolean(false);

    public NoShakingValueCallback(ValueCallback<T> valueCallback) {
        this.valueCallback = valueCallback;
    }


    @Override
    public void onReceiveValue(Value<T> value) {
        if (called.compareAndSet(false, true)) {
            valueCallback.onReceiveValue(value);
        }
    }
}