package com.pluscubed.anticipate.trasitions;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.transition.ChangeBounds;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

/**
 * http://hujiaweibujidao.github.io/blog/2015/12/13/Fab-and-Dialog-Morphing-Animation/
 * <p/>
 * MorphTransition扩展自ChangeBounds(共享元素的动画的一种)，它在原有动画基础上添加了color和cornerRadius的动画效果，这个类实际上是整合了MorphFabToDialog和MorphDialogToFab两个类的作用
 * <p/>
 * A transition that morphs a circle into a rectangle, changing it's background color.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MorphTransition extends ChangeBounds {

    private static final String PROPERTY_COLOR = "color";
    private static final String PROPERTY_CORNER_RADIUS = "cornerRadius";
    private static final String[] TRANSITION_PROPERTIES = {
            PROPERTY_COLOR,
            PROPERTY_CORNER_RADIUS
    };

    private int startColor = Color.TRANSPARENT;
    private int endColor = Color.TRANSPARENT;
    private int startCornerRadius = 0;
    private int endCornerRadius = 0;
    private boolean isShowViewGroup = false;

    public MorphTransition(int startColor, int endColor, int startCornerRadius, int endCornerRadius, boolean isShowViewGroup) {
        super();
        setStartColor(startColor);
        setEndColor(endColor);
        setStartCornerRadius(startCornerRadius);
        setEndCornerRadius(endCornerRadius);
        setIsShowViewGroup(isShowViewGroup);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MorphTransition(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public String[] getTransitionProperties() {
        return TRANSITION_PROPERTIES;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        final View view = transitionValues.view;
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return;
        }
        transitionValues.values.put(PROPERTY_COLOR, startColor);
        transitionValues.values.put(PROPERTY_CORNER_RADIUS, startCornerRadius);//view.getHeight() / 2
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        final View view = transitionValues.view;
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return;
        }
        transitionValues.values.put(PROPERTY_COLOR, endColor);//ContextCompat.getColor(view.getContext(), R.color.dialog_background_color)
        transitionValues.values.put(PROPERTY_CORNER_RADIUS, endCornerRadius);
    }

    @Override
    public Animator createAnimator(final ViewGroup sceneRoot, TransitionValues startValues, final TransitionValues endValues) {
        Animator changeBounds = super.createAnimator(sceneRoot, startValues, endValues);
        if (startValues == null || endValues == null || changeBounds == null) {
            return null;
        }

        Integer startColor = (Integer) startValues.values.get(PROPERTY_COLOR);
        Integer startCornerRadius = (Integer) startValues.values.get(PROPERTY_CORNER_RADIUS);
        Integer endColor = (Integer) endValues.values.get(PROPERTY_COLOR);
        Integer endCornerRadius = (Integer) endValues.values.get(PROPERTY_CORNER_RADIUS);

        if (startColor == null || startCornerRadius == null || endColor == null || endCornerRadius == null) {
            return null;
        }

        MorphDrawable background = new MorphDrawable(startColor, startCornerRadius);
        endValues.view.setBackground(background);

        Animator color = ObjectAnimator.ofArgb(background, background.COLOR, endColor);
        Animator corners = ObjectAnimator.ofFloat(background, background.CORNER_RADIUS, endCornerRadius);

        ////......

        AnimatorSet transition = new AnimatorSet();
        transition.playTogether(changeBounds, corners, color);
        transition.setDuration(300);
        transition.setInterpolator(AnimationUtils.loadInterpolator(sceneRoot.getContext(), android.R.interpolator.fast_out_slow_in));
        return transition;
    }

    public void setEndColor(int endColor) {
        this.endColor = endColor;
    }

    public void setEndCornerRadius(int endCornerRadius) {
        this.endCornerRadius = endCornerRadius;
    }

    public void setStartColor(int startColor) {
        this.startColor = startColor;
    }

    public void setStartCornerRadius(int startCornerRadius) {
        this.startCornerRadius = startCornerRadius;
    }

    public void setIsShowViewGroup(boolean isShowViewGroup) {
        this.isShowViewGroup = isShowViewGroup;
    }
}
