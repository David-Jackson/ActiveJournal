package fyi.jackson.activejournal.recycler.holder;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;

import fyi.jackson.activejournal.R;
import fyi.jackson.activejournal.data.entities.Activity;
import fyi.jackson.activejournal.ui.ItemClickListener;
import fyi.jackson.activejournal.util.ActivityTransitionNames;

public class ActivityViewHolder extends RecyclerView.ViewHolder {

    public static final String TAG = ActivityViewHolder.class.getSimpleName();

    public TextView name;
    public ImageView type;
    public ImageView map;
    public View card;

    public ActivityViewHolder(@NonNull View itemView) {
        super(itemView);
        name = itemView.findViewById(R.id.tv_activity_title);
        type = itemView.findViewById(R.id.iv_activity_type);
        map = itemView.findViewById(R.id.iv_activity_map);
        card = itemView.findViewById(R.id.card_view);
    }

    public void bindTo(final Activity activity, final ItemClickListener clickListener) {
        ActivityTransitionNames transitionNames =
                new ActivityTransitionNames(activity.getActivityId());
        ViewCompat.setTransitionName(map, transitionNames.map);
        ViewCompat.setTransitionName(name, transitionNames.title);
        ViewCompat.setTransitionName(type, transitionNames.type);
        ViewCompat.setTransitionName(card, transitionNames.container);

        name.setText(activity.getName());
        type.setImageResource(activity.getTypeResId());

        map.setImageResource(R.drawable.image_activity_map_placeholder);

        if (activity.getThumbnail() != null) {
            File f = new File(activity.getThumbnail());

            RequestCreator request = Picasso.get().load(f);

            // Only load vector drawables on API >= 21
            if (Build.VERSION.SDK_INT >= 21) {
                request.placeholder(R.drawable.image_activity_map_placeholder)
                        .error(R.drawable.image_activity_map_placeholder);
            }

            request.into(map);
        }

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onClick(activity, ActivityViewHolder.this);
            }
        });
    }
}
