package com.pluscubed.anticipate.transition;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.transition.ArcMotion;
import android.transition.TransitionSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

/**
 * Helper class for setting up Fab <-> Dialog shared element transitions.
 * <p/>
 * Modified from Plaid
 */
public class FabDialogMorphSetup {

    public static final String EXTRA_SHARED_ELEMENT_START_COLOR =
            "EXTRA_SHARED_ELEMENT_START_COLOR";
    public static final String EXTRA_SHARED_ELEMENT_START_CORNER_RADIUS =
            "EXTRA_SHARED_ELEMENT_START_CORNER_RADIUS";

    private FabDialogMorphSetup() {
    }

    /**
     * Configure the shared element transitions for morphin from a fab <-> dialog. We need to do
     * this in code rather than declaratively as we need to supply the color to transition from/to
     * and the dialog corner radius which is dynamically supplied depending upon where this screen
     * is launched from.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setupSharedElementTransitions(@NonNull Activity activity,
                                                     @Nullable View mockFab,
                                                     int dialogCornerRadius) {
        if (!activity.getIntent().hasExtra(EXTRA_SHARED_ELEMENT_START_COLOR)) return;

        int startCornerRadius = activity.getIntent()
                .getIntExtra(EXTRA_SHARED_ELEMENT_START_CORNER_RADIUS, -1);

        ArcMotion arcMotion = new ArcMotion();
        arcMotion.setMinimumHorizontalAngle(5f);
        arcMotion.setMinimumVerticalAngle(10f);
        arcMotion.setMaximumAngle(10f);
        int color = activity.getIntent().getIntExtra(EXTRA_SHARED_ELEMENT_START_COLOR, Color.TRANSPARENT);
        Interpolator easeInOut =
                AnimationUtils.loadInterpolator(activity, android.R.interpolator.fast_out_slow_in);
        MorphFabToDialog sharedEnter =
                new MorphFabToDialog(color, dialogCornerRadius, startCornerRadius);
        sharedEnter.setPathMotion(arcMotion);
        sharedEnter.setInterpolator(easeInOut);
        MorphDialogToFab sharedReturn = new MorphDialogToFab(color, startCornerRadius);
        sharedReturn.setPathMotion(arcMotion);
        sharedReturn.setInterpolator(easeInOut);
        if (mockFab != null) {
            sharedEnter.addTarget(mockFab);
            sharedReturn.addTarget(mockFab);
        }

        TransitionSet set = new TransitionSet();
        set.addTransition(sharedEnter);

        //TODO: Elevation flicker is due to FAB still visible
        set.addTransition(new ChangeElevation());

        TransitionSet set2 = new TransitionSet();
        set2.addTransition(sharedReturn);
        set2.addTransition(new ChangeElevation());

        activity.getWindow().setSharedElementEnterTransition(set);
        activity.getWindow().setSharedElementReturnTransition(set2);
    }

}