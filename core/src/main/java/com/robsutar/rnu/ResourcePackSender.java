package com.robsutar.rnu;

import com.robsutar.rnu.util.OC;

import java.util.Map;

public interface ResourcePackSender {
    String type();

    static ResourcePackSender deserialize(Map<String, Object> raw) {
        if (raw.get("type") == null) throw new IllegalArgumentException("Missing loader type");
        String type = OC.str(raw.get("type"));
        switch (type) {
            case Delayed.TYPE:
                return new Delayed(raw);
            case PaperPropertyInjector.TYPE:
                return new PaperPropertyInjector();
            default:
                throw new IllegalArgumentException("Invalid loader type: " + type);
        }
    }

    class Delayed implements ResourcePackSender {
        public static final String TYPE = "Delayed";

        private final int sendingDelay;
        private final int resendingDelay;

        public Delayed(Map<String, Object> raw) {
            sendingDelay = OC.intValue(raw.get("sendingDelay"));
            resendingDelay = OC.intValue(raw.get("resendingDelay"));
        }

        @Override
        public String type() {
            return TYPE;
        }

        public int sendingDelay() {
            return sendingDelay;
        }

        public int resendingDelay() {
            return resendingDelay;
        }
    }

    class PaperPropertyInjector implements ResourcePackSender {
        public static final String TYPE = "PaperPropertyInjector";

        @Override
        public String type() {
            return TYPE;
        }
    }
}
