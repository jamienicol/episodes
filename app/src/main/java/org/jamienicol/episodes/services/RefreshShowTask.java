package org.jamienicol.episodes.services;

import android.content.Context;

import org.jamienicol.episodes.EpisodesApplication;
import org.jamienicol.episodes.RefreshShowUtil;

import java.util.concurrent.Callable;

public class RefreshShowTask implements Callable<Void> {
    private static final String TAG = DeleteShowTask.class.getName();
    private int showId;
    private Context context;

    public RefreshShowTask(int showId) {
        this.showId = showId;
        this.context = EpisodesApplication.getInstance().getApplicationContext();
    }

    @Override
    public Void call() {
        RefreshShowUtil.refreshShow(showId, this.context.getContentResolver());
        return null;
    }
}
