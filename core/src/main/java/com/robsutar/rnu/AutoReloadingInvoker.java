package com.robsutar.rnu;

import com.robsutar.rnu.util.OC;

import java.util.Map;
import java.util.Objects;

public class AutoReloadingInvoker<E> {
    private final Class<? extends E> eventClass;
    private final int delay;
    private final int repeatCooldown;

    public AutoReloadingInvoker(Class<? extends E> eventClass, int delay, int repeatCooldown) {
        this.eventClass = eventClass;
        this.delay = delay;
        this.repeatCooldown = repeatCooldown;
    }

    public Class<? extends E> eventClass() {
        return eventClass;
    }

    public int delay() {
        return delay;
    }

    public int repeatCooldown() {
        return repeatCooldown;
    }

    @SuppressWarnings("unchecked")
    public static <E> AutoReloadingInvoker<E> deserialize(Map<String, Object> raw) {
        Class<? extends E> eventClass;
        try {
            eventClass = Objects.requireNonNull((Class<? extends E>) Class.forName(OC.str(raw.get("eventClass"))));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        int delay = OC.intValueOr(raw.get("delay"), 1);
        int repeatCooldown = OC.intValueOr(raw.get("repeatCooldown"), 0);
        return new AutoReloadingInvoker<>(eventClass, delay, repeatCooldown);
    }
}
