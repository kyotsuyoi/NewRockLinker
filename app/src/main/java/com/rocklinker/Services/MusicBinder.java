package com.rocklinker.Services;

import android.os.Binder;

import java.lang.ref.WeakReference;

public class MusicBinder extends Binder {

    private WeakReference<PlayerService> weakService;

    /**
     * Inject service instance to weak reference.
     */
    public void onBind(PlayerService service) {
        this.weakService = new WeakReference<>(service);
    }

    public PlayerService getService() {
        return weakService == null ? null : weakService.get();
    }
}
