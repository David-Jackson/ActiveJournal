package fyi.jackson.activejournal.fragment;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import fyi.jackson.activejournal.R;
import fyi.jackson.activejournal.animation.EndAnimatorListener;
import fyi.jackson.activejournal.data.AppViewModel;
import fyi.jackson.activejournal.data.entities.Stats;
import fyi.jackson.activejournal.service.RecordingService;
import fyi.jackson.activejournal.service.ServiceConstants;
import fyi.jackson.activejournal.util.Formatter;

public class RecordingFragment extends Fragment {

    public static final String TAG = RecordingFragment.class.getSimpleName();

    private static final int STATUS_STANDBY = 0;
    private static final int STATUS_ACTIVE = 1;

    private int status = STATUS_STANDBY;

    private Unbinder unbinder;

    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.bottom_sheet) View bottomSheetView;
    @BindView(R.id.iv_pause) ImageView pauseImageButton;
    @BindView(R.id.iv_stop) ImageView stopImageButton;
    @BindView(R.id.tv_points) TextView pointsTextView;
    @BindView(R.id.tv_time) TextView durationTextView;
    @BindView(R.id.tv_speed) TextView speedTextView;

    private BottomSheetBehavior bottomSheetBehavior;

    public RecordingFragment() {}

    public static RecordingFragment newInstance() {
        return new RecordingFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recording, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        unbinder = ButterKnife.bind(this, view);

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        final AppViewModel viewModel = ViewModelProviders.of(this).get(AppViewModel.class);

        final Observer<List<Stats>> statsObserver = new Observer<List<Stats>>() {
            @Override
            public void onChanged(@Nullable List<Stats> stats) {
                if (stats.size() == 0) {
                    if (status == STATUS_ACTIVE) {
                        status = STATUS_STANDBY;
                        updateVisibilities();
                    }
                } else {
                    if (status == STATUS_STANDBY) {
                        status = STATUS_ACTIVE;
                        updateVisibilities();
                    } else {
                        updateStats(stats.get(0));
                    }
                }
            }
        };

        // We want to observe the Stats after the view has been drawn on the screen,
        // because we might want to expand the bottom sheet right away
        bottomSheetView.post(new Runnable() {
            @Override
            public void run() {
                viewModel.getStatistics().observe(RecordingFragment.this, statsObserver);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });
        pauseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (status == STATUS_ACTIVE) {
                    pauseRecording();
                } else {
                    resumeRecording();
                }
            }
        });

        stopImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
            }
        });

        bottomSheetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleBottomSheet(view);
            }
        });

        fab.setContentDescription(getString(R.string.access_fab));
        pauseImageButton.setContentDescription(getString(R.string.access_pause));
        stopImageButton.setContentDescription(getString(R.string.access_stop));

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void updateStats(Stats stats) {
        pointsTextView.setText(getString(R.string.point_count, stats.getPointCount()));
        durationTextView.setText(Formatter.millisToDurationString(stats.getDuration()));
        speedTextView.setText(Formatter.speedToString(stats.getAverageSpeed()));
    }

    private void startRecording() {
        Intent service = new Intent(getContext(), RecordingService.class);
        service.setAction(ServiceConstants.ACTION.START_FOREGROUND);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(service);
        } else {
            getContext().startService(service);
        }

        status = STATUS_ACTIVE;
        updateVisibilities();
    }

    private void pauseRecording() {
        Intent service = new Intent(getContext(), RecordingService.class);
        service.setAction(ServiceConstants.ACTION.PAUSE_FOREGROUND);
        getContext().startService(service);

        setStopButtonVisibility(true);
        pauseImageButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
        pauseImageButton.setContentDescription(getString(R.string.access_play));

        status = STATUS_STANDBY;
    }

    private void resumeRecording() {
        Intent service = new Intent(getContext(), RecordingService.class);
        service.setAction(ServiceConstants.ACTION.RESUME_FOREGROUND);
        getContext().startService(service);

        setStopButtonVisibility(false);
        pauseImageButton.setImageResource(R.drawable.ic_pause_black_24dp);
        pauseImageButton.setContentDescription(getString(R.string.access_pause));
        status = STATUS_ACTIVE;
    }

    private void stopRecording() {
        Intent service = new Intent(getContext(), RecordingService.class);
        service.setAction(ServiceConstants.ACTION.STOP_FOREGROUND);
        getContext().startService(service);

        setStopButtonVisibility(false);
        pauseImageButton.setImageResource(R.drawable.ic_pause_black_24dp);
        pauseImageButton.setContentDescription(getString(R.string.access_pause));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        status = STATUS_STANDBY;
        updateVisibilities();
    }

    private void setStopButtonVisibility(boolean visible) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TransitionManager.beginDelayedTransition((ViewGroup) bottomSheetView);
        }

        stopImageButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);

        int unset = ConstraintLayout.LayoutParams.UNSET;
        int parentId = ConstraintLayout.LayoutParams.PARENT_ID;

        ConstraintLayout.LayoutParams layoutParams =
                (ConstraintLayout.LayoutParams) pauseImageButton.getLayoutParams();
        layoutParams.rightToRight = visible ? unset : parentId;
        layoutParams.endToEnd = visible ? unset : parentId;
        layoutParams.rightToLeft = visible ? R.id.iv_stop : unset;
        layoutParams.endToStart = visible ? R.id.iv_stop : unset;
        pauseImageButton.setLayoutParams(layoutParams);
    }

    @SuppressLint("RestrictedApi")
    private void updateVisibilities() {

        boolean isActive = status == STATUS_ACTIVE;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateBottomSheetWithReveal();
            updateFabWithReveal();
        } else {
            bottomSheetView.setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
            fab.setVisibility(isActive ? View.INVISIBLE : View.VISIBLE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updateBottomSheetWithReveal() {

        boolean isClosing = status == STATUS_STANDBY;

        int cx = fab.getLeft() + ((fab.getRight() - fab.getLeft()) / 2);
        int cy = ((fab.getBottom() - fab.getTop()) / 2) + fab.getTop() - bottomSheetView.getTop();

        float finalRadius = (float) Math.hypot(cx - bottomSheetView.getLeft(), bottomSheetView.getBottom() - cy);
        float startRadius = 0f;

        if (isClosing) {
            float t = startRadius;
            startRadius = finalRadius;
            finalRadius = t;
        }

        Animator anim =
                ViewAnimationUtils.createCircularReveal(bottomSheetView, cx, cy, startRadius, finalRadius);

        if (isClosing) {
            anim.addListener(new EndAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    bottomSheetView.setVisibility(View.INVISIBLE);
                }
            });
        } else {
            bottomSheetView.setVisibility(View.VISIBLE);
        }

        anim.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("RestrictedApi")
    private void updateFabWithReveal() {

        boolean isClosing = status == STATUS_ACTIVE;

        int cx = (fab.getRight() - fab.getLeft()) / 2;
        int cy = (fab.getBottom() - fab.getTop()) / 2;

        float finalRadius = fab.getWidth() / 2f;
        float startRadius = 0f;

        if (isClosing) {
            float t = startRadius;
            startRadius = finalRadius;
            finalRadius = t;
        }

        Animator anim =
                ViewAnimationUtils.createCircularReveal(fab, cx, cy, startRadius, finalRadius);

        if (isClosing) {
            anim.addListener(new EndAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    fab.setVisibility(View.INVISIBLE);
                }
            });
        } else {
            fab.setVisibility(View.VISIBLE);
        }

        anim.start();
    }

    public void toggleBottomSheet(View view) {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
}
