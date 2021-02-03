package fyi.jackson.activejournal.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fyi.jackson.activejournal.R;
import fyi.jackson.activejournal.data.AppDatabase;
import fyi.jackson.activejournal.data.entities.Activity;

public class DisplayWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }


    class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private List<Activity> activities = new ArrayList<>();
        private Context context;
        private int appWidgetId;

        public StackRemoteViewsFactory(Context context, Intent intent) {
            this.context = context;
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        public void onCreate() {
        }

        public void onDestroy() {
            activities.clear();
        }

        public int getCount() {
            return activities.size();
        }

        public RemoteViews getViewAt(int position) {
            Activity activity = activities.get(position);

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_display_item);
            rv.setTextViewText(R.id.tv_activity_title, activity.getName());
            rv.setImageViewResource(R.id.iv_activity_type, activity.getTypeResId());

            try {
                if (activity.getThumbnail() == null) throw new NullPointerException("Thumbnail not set");
                File f = new File(activity.getThumbnail());
                Bitmap bitmap = Picasso.get().load(f).get();
                rv.setImageViewBitmap(R.id.iv_activity_map, bitmap);
            } catch (Exception e) {
                rv.setImageViewResource(R.id.iv_activity_map, R.drawable.image_activity_map_placeholder);
                e.printStackTrace();
            }

            Intent fillInIntent = new Intent();
            fillInIntent.putExtra(DisplayWidgetProvider.EXTRA_ACTIVITY_ID, activity.getActivityId());
            rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

            return rv;
        }

        public RemoteViews getLoadingView() {
            return null;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean hasStableIds() {
            return true;
        }

        public void onDataSetChanged() {
            activities = AppDatabase.getDatabase(context).activityDao().getAllActivities();
        }
    }

}