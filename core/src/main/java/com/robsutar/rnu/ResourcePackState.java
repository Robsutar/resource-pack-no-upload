package com.robsutar.rnu;

public interface ResourcePackState {
    final class Loaded implements ResourcePackState {
        private final ResourcePackInfo resourcePackInfo;
        private final byte[] bytes;

        public Loaded(ResourcePackInfo resourcePackInfo, byte[] bytes) {
            this.resourcePackInfo = resourcePackInfo;
            this.bytes = bytes;
        }

        public ResourcePackInfo resourcePackInfo() {
            return resourcePackInfo;
        }

        public byte[] bytes() {
            return bytes;
        }
    }

    class FailedToLoad implements ResourcePackState {

    }

    class Loading implements ResourcePackState {
    }

    final class LoadedPendingProvider implements ResourcePackState {
        private final Loaded loaded;

        public LoadedPendingProvider(Loaded loaded) {
            this.loaded = loaded;
        }

        public Loaded loaded() {
            return loaded;
        }
    }
}
