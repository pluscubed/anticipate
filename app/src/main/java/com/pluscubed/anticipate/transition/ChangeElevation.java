package com.pluscubed.anticipate.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.view.View;
import android.view.ViewGroup;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ChangeElevation extends Transition {
    private static final String PROPNAME_ELEVATION = "anticipate:changeElevation";


    @Override
    public String[] getTransitionProperties() {
        return new String[]{PROPNAME_ELEVATION};
    }

    private void captureValues(TransitionValues values) {
        View view = values.view;

        if (view.isLaidOut() || view.getWidth() != 0 || view.getHeight() != 0) {
            values.values.put(PROPNAME_ELEVATION, view.getZ());
        }
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        return ObjectAnimator.ofFloat(endValues.view, View.Z, (Float) startValues.values.get(PROPNAME_ELEVATION), (Float) endValues.values.get(PROPNAME_ELEVATION));
    }
}
