package jonathan.mason.birdcalllibrarian;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.DateFormat;

import jonathan.mason.birdcalllibrarian.Database.Birdcall;

/**
 * Implementation of App Widget functionality.
 */
public class Widget extends AppWidgetProvider {
    /**
     * Update all instances of widget to show details of recently recorded birdcall.
     * <p>Based on answer to "Update Android Widget From Activity" by Atul O Holic:
     * https://stackoverflow.com/questions/4073907/update-android-widget-from-activity/4074665.</p>
     * @param context Context.
     * @appWidgetManager App widget manager.
     * @param birdcall Recently recorded birdcall.
     */
    static void updateAllAppWidgets(Context context, AppWidgetManager appWidgetManager, Birdcall birdcall) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setTextViewText(R.id.widget_info, context.getString(R.string.widget_recetly_active_birdcall, DateFormat.getInstance().format(birdcall.getDateAndTime())));

        // Launch MainActivity screen if play icon of widget is clicked.
        Intent playIntent = new Intent(context, MainActivity.class);
        PendingIntent playPendingIntent = PendingIntent.getActivity(context, 0, playIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_launch_play, playPendingIntent);

        // Launch RecordActivity screen if record icon of widget is clicked.
        Intent recordIntent = new Intent(context, RecordActivity.class);
        recordIntent.putExtra(RecordActivity.LAUNCHED_BY_WIDGET, true); // Indicate launched by widget as extra data in intent.
        PendingIntent recordPendingIntent = PendingIntent.getActivity(context, 0, recordIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_launch_record, recordPendingIntent);

        // Instruct the widget manager to update the widgets
        appWidgetManager.updateAppWidget(new ComponentName(context, Widget.class), views);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        // Launch MainActivity screen if play icon of widget is clicked.
        Intent playIntent = new Intent(context, MainActivity.class);
        PendingIntent playPendingIntent = PendingIntent.getActivity(context, 0, playIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_launch_play, playPendingIntent);

        // Launch RecordActivity screen if record icon of widget is clicked.
        Intent recordIntent = new Intent(context, RecordActivity.class);
        recordIntent.putExtra(RecordActivity.LAUNCHED_BY_WIDGET, true); // Indicate launched by widget as extra data in intent.
        PendingIntent recordPendingIntent = PendingIntent.getActivity(context, 0, recordIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_launch_record, recordPendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

