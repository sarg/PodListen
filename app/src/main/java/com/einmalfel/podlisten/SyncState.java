package com.einmalfel.podlisten;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import javax.annotation.Nonnull;

/**
 * This class keeps records on sync errors and manages sync notification
 */
public class SyncState {
  private static final int NOTIFICATION_ID = 0;
  private final SyncResult syncResult;
  private final NotificationManagerCompat nm;
  private final NotificationCompat.Builder nb;
  private final int maxFeeds;
  private int errors = 0;
  private int parsed = 0;
  private int newEpisodes = 0;
  private boolean stopped = false;

  SyncState(@Nonnull Context context, @Nonnull SyncResult syncResult, int maxFeeds) {
    this.syncResult = syncResult;
    this.maxFeeds = maxFeeds;
    nm = NotificationManagerCompat.from(context);
    nb = new NotificationCompat.Builder(context);
    Intent mainActivityIntent = new Intent(context, MainActivity.class);
    nb.setSmallIcon(R.mipmap.ic_sync_green_24dp)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(PendingIntent.getActivity(context, 0, mainActivityIntent, 0));
  }

  synchronized void start() {
    nb.setContentTitle("Refreshing PodListen..")
      .setOngoing(true)
      .setProgress(0, 0, true);
    updateNotification();
  }

  synchronized void stop() {
    StringBuilder stringBuilder = new StringBuilder(newEpisodes + " episode(s) added");
    if (parsed > 0) {
      stringBuilder.append(", ").append(parsed).append(" feed(s) refreshed");
    }
    if (errors > 0) {
      stringBuilder.append(", ").append(errors).append(" feed(s) failed to refresh");
    }
    nb.setOngoing(false)
      .setProgress(0, 0, false)
      .setContentTitle("Podlisten refreshed")
      .setContentText(stringBuilder);
    updateNotification();
    stopped = true;
  }

  synchronized private void updateProgress() {
    nb.setProgress(maxFeeds, errors + parsed, false);
    updateNotification();
  }


  synchronized void signalParseError() {
    syncResult.stats.numSkippedEntries++;
    errors++;
    updateProgress();
  }

  synchronized void signalDBError() {
    syncResult.databaseError = true;
    errors++;
    updateProgress();
  }

  synchronized void signalIOError() {
    syncResult.stats.numIoExceptions++;
    errors++;
    updateProgress();
  }

  synchronized void signalFeedSuccess(String feedTitle, int episodesAdded) {
    syncResult.stats.numUpdates++;
    parsed++;
    newEpisodes += episodesAdded;
    nb.setContentText("Loaded: " + feedTitle);
    updateProgress();
  }

  synchronized private void updateNotification() {
    if (!stopped) {
      nm.notify(NOTIFICATION_ID, nb.build());
    }
  }
}