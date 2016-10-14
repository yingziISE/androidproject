package chan.android.app.pocketnote.app.reminder;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import chan.android.app.pocketnote.app.Note;
import chan.android.app.pocketnote.app.NoteDateFormatter;
import chan.android.app.pocketnote.app.db.PocketNoteManager;
import chan.android.app.pocketnote.app.reminder.alarm.AlarmReceiver;
import chan.android.app.pocketnote.util.Logger;

import java.util.concurrent.TimeUnit;

/**
 * A concrete implementation of reminder scheduler to schedule a reminder
 * for note.
 */
public class NoteReminderScheduler extends AbstractReminderScheduler {

  private static final String PREFIX = "pocketnote://";
  private static AbstractReminderScheduler scheduler;

  private NoteReminderScheduler(Context context) {
    super(context);
  }

  public static AbstractReminderScheduler getScheduler(Context context) {
    if (scheduler == null) {
      scheduler = new NoteReminderScheduler(context);
    }
    return scheduler;
  }

  /**
   * Schedule a new reminder for note (with reminder of-course!)
   *
   * @param note
   */
  public void schedule(final Note note) {
    String json = note.getReminder();
    if (json == null) {
      throw new RuntimeException("您不能安排一个没有提醒的笔记!");
    }

    Reminder reminder = Reminder.fromJson(json);
    Intent intent = buildNoteIntent(note);
    PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
    if (reminder.getRepetition() == Reminder.Repetition.ONE_TIME) {
      Logger.e("一次事件开始时间: " + NoteDateFormatter.toString(reminder.getBegin()));
      alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.getBegin(), pi);
    } else {
      Logger.e("重复事件开始时间: " + NoteDateFormatter.toString(reminder.getBegin()));
      Logger.e("with interval = " + TimeUnit.MILLISECONDS.toSeconds(reminder.getRepetition().getInterval()));
      alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, reminder.getBegin(), reminder.getRepetition().getInterval(), pi);
    }
  }

  /**
   * To cancel a specific intent, construct the pending intent exact
   * the same way as we sent it
   */
  public void cancel(final Note note) {
    final Intent intent = buildNoteIntent(note);
    PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
    alarmManager.cancel(pi);
  }

  /**
   * Build a unique intent for note so that we can cancel it later
   *
   * @param note
   * @return
   */
  private Intent buildNoteIntent(final Note note) {
    final PocketNoteManager manager = PocketNoteManager.getPocketNoteManager();
    final int id = manager.getId(note);
    final Intent intent = new Intent(context, AlarmReceiver.class);
    intent.putExtra(Note.BUNDLE_KEY, note);
    intent.setData(Uri.parse(PREFIX + id));
    intent.setAction(String.valueOf(id));
    return intent;
  }
}
