package com.pluscubed.anticipate.transition;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.transition.ChangeBounds;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.pluscubed.anticipate.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * A transition that morphs a rectangle into a circle, changing it's background color.
 * <p/>
 * Modified from Plaid
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MorphDialogToFab extends ChangeBounds {

    private static final String PROPERTY_COLOR = "plaid:rectMorph:color";
    private static final String PROPERTY_CORNER_RADIUS = "plaid:rectMorph:cornerRadius";
    private static final String[] TRANSITION_PROPERTIES = {
            PROPERTY_COLOR,
            PROPERTY_CORNER_RADIUS
    };
    private
    @ColorInt
    int endColor = Color.TRANSPARENT;
    private int endCornerRadius = -1;

    public MorphDialogToFab(@ColorInt int endColor) {
        super();
        setEndColor(endColor);
    }

    public MorphDialogToFab(@ColorInt int endColor, int endCornerRadius) {
        super();
        setEndColor(endColor);
        setEndCornerRadius(endCornerRadius);
    }

    public MorphDialogToFab(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEndColor(@ColorInt int endColor) {
        this.endColor = endColor;
    }

    public void setEndCornerRadius(int endCornerRadius) {
        this.endCornerRadius = endCornerRadius;
    }

    @Override
    public String[] getTransitionProperties() {
        ArrayList<String> boundsTransitionProps = new ArrayList<>(Arrays.asList(super.getTransitionProperties()));
        Collections.addAll(boundsTransitionProps, TRANSITION_PROPERTIES);
        return boundsTransitionProps.toArray(new String[boundsTransitionProps.size()]);
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        final View view = transitionValues.view;
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return;
        }
        transitionValues.values.put(PROPERTY_COLOR, Color.WHITE);
        transitionValues.values.put(PROPERTY_CORNER_RADIUS, view.getResources()
                .getDimensionPixelSize(R.dimen.dialog_corners));
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        final View view = transitionValues.view;
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return;
        }
        transitionValues.values.put(PROPERTY_COLOR, endColor);
        transitionValues.values.put(PROPERTY_CORNER_RADIUS,
                endCornerRadius >= 0 ? endCornerRadius : view.getHeight() / 2);
    }

    @Override
    public Animator createAnimator(final ViewGroup sceneRoot,
                                   TransitionValues startValues,
                                   TransitionValues endValues) {
        Animator changeBounds = super.createAnimator(sceneRoot, startValues, endValues);
        if (startValues == null || endValues == null || changeBounds == null) {
            return null;
        }

        Integer startColor = (Integer) startValues.values.get(PROPERTY_COLOR);
        Integer startCornerRadius = (Integer) startValues.values.get(PROPERTY_CORNER_RADIUS);
        Integer endColor = (Integer) endValues.values.get(PROPERTY_COLOR);
        Integer endCornerRadius = (Integer) endValues.values.get(PROPERTY_CORNER_RADIUS);

        if (startColor == null || startCornerRadius == null || endColor == null ||
                endCornerRadius == null) {
            return null;
        }

        MorphDrawable background = new MorphDrawable(startColor, startCornerRadius);
        endValues.view.setBackground(background);

        Animator color = ObjectAnimator.ofArgb(background, MorphDrawable.COLOR, endColor);
        Animator corners = ObjectAnimator.ofFloat(background, MorphDrawable.CORNER_RADIUS,
                endCornerRadius);

        // hide child views (offset down & fade out)
        if (endValues.view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) endValues.view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                v.setVisibility(View.GONE);

                //TODO: This clips the children to the final FAB size, so we only see the small part animate
                /*v.animate()
                        .alpha(0f)
                        .translationY(v.getHeight() / 3)
                        .setStartDelay(0L)
                        .setDuration(50L)
                        .setInterpolator(AnimationUtils.loadInterpolator(vg.getContext(),
                                android.R.interpolator.fast_out_linear_in))
                        .start();*/
            }
        }

        AnimatorSet transition = new AnimatorSet();
        transition.playTogether(changeBounds, corners, color);
        transition.setDuration(500);
        transition.setInterpolator(AnimationUtils.loadInterpolator(sceneRoot.getContext(),
                android.R.interpolator.fast_out_slow_in));
        return transition;
    }
}