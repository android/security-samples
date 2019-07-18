/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.fido.example.fido2apiexample.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;


public class CustomItemAnimator extends SimpleItemAnimator {
    private static final String TAG = "CustomItemAnimator";

    private List<RecyclerView.ViewHolder> mPendingAdd = new ArrayList<RecyclerView.ViewHolder>();
    private List<RecyclerView.ViewHolder> mPendingRemove = new ArrayList<RecyclerView.ViewHolder>();

    @Override
    public void runPendingAnimations() {
        Log.i(TAG, "runPendingAnimations");
        int animationDuration = 300;
        if (!mPendingAdd.isEmpty()) {
            for (final RecyclerView.ViewHolder viewHolder : mPendingAdd) {
                View target = viewHolder.itemView;
                target.setPivotX(target.getMeasuredWidth() / 2);
                target.setPivotY(target.getMeasuredHeight() / 2);

                AnimatorSet animator = new AnimatorSet();

                animator.playTogether(
                        ObjectAnimator.ofFloat(target, "translationX", -target.getMeasuredWidth(), 0.0f),
                        ObjectAnimator.ofFloat(target, "alpha", target.getAlpha(), 1.0f)
                );

                animator.setTarget(target);
                animator.setDuration(animationDuration);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setStartDelay((animationDuration * viewHolder.getPosition()) / 10);
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mPendingAdd.remove(viewHolder);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animator.start();
            }
        }
        if (!mPendingRemove.isEmpty()) {
            for (final RecyclerView.ViewHolder viewHolder : mPendingRemove) {
                View target = viewHolder.itemView;
                target.setPivotX(target.getMeasuredWidth() / 2);
                target.setPivotY(target.getMeasuredHeight() / 2);

                AnimatorSet animator = new AnimatorSet();

                animator.playTogether(
                        ObjectAnimator.ofFloat(target, "translationX", 0.0f, target.getMeasuredWidth()),
                        ObjectAnimator.ofFloat(target, "alpha", target.getAlpha(), 0.0f)
                );

                animator.setTarget(target);
                animator.setDuration(animationDuration);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setStartDelay((animationDuration * viewHolder.getPosition()) / 10);
                animator.start();
            }
        }
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder viewHolder) {
        Log.i(TAG, "animateRemove");
        mPendingRemove.add(viewHolder);
        return false;
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder viewHolder) {
        Log.i(TAG, "animateAdd");
        viewHolder.itemView.setAlpha(0.0f);
        return mPendingAdd.add(viewHolder);
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder viewHolder, int i, int i2, int i3, int i4) {
        Log.i(TAG, "animateMove");
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2, int i, int i2, int i3, int i4) {
        Log.i(TAG, "animateChange");
        return false;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder viewHolder) {
        Log.i(TAG, "endAnimation");
    }

    @Override
    public void endAnimations() {
        Log.i(TAG, "endAnimations");
    }

    @Override
    public boolean isRunning() {
        Log.i(TAG, "isRunning");
        return !mPendingAdd.isEmpty() || !mPendingRemove.isEmpty();
    }

}
