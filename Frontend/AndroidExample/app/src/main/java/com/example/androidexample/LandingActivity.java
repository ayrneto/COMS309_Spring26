package com.example.androidexample;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        LinearLayout logoContainer = findViewById(R.id.logoContainer);
        TextView      tagline      = findViewById(R.id.tagline);
        MaterialButton btnGetStarted = findViewById(R.id.btnGetStarted);
        LinearLayout  btnGoToLogin  = findViewById(R.id.btnGoToLogin);

        // Start everything invisible
        logoContainer.setAlpha(0f);
        logoContainer.setTranslationY(-60f);
        tagline.setAlpha(0f);
        btnGetStarted.setAlpha(0f);
        btnGoToLogin.setAlpha(0f);

        // ── Logo: fade in + slide down ─────────────────────────────────────
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logoContainer, View.ALPHA, 0f, 1f);
        ObjectAnimator logoSlide = ObjectAnimator.ofFloat(logoContainer, View.TRANSLATION_Y, -60f, 0f);
        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(logoAlpha, logoSlide);
        logoAnim.setDuration(700);
        logoAnim.setInterpolator(new DecelerateInterpolator());
        logoAnim.setStartDelay(200);

        // ── Tagline: fade in ───────────────────────────────────────────────
        ObjectAnimator taglineAnim = ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f);
        taglineAnim.setDuration(600);
        taglineAnim.setStartDelay(750);

        // ── Buttons: fade in ───────────────────────────────────────────────
        ObjectAnimator btnAnim1 = ObjectAnimator.ofFloat(btnGetStarted, View.ALPHA, 0f, 1f);
        btnAnim1.setDuration(500);
        btnAnim1.setStartDelay(1100);

        ObjectAnimator btnAnim2 = ObjectAnimator.ofFloat(btnGoToLogin, View.ALPHA, 0f, 1f);
        btnAnim2.setDuration(500);
        btnAnim2.setStartDelay(1300);

        // Play all
        logoAnim.start();
        taglineAnim.start();
        btnAnim1.start();
        btnAnim2.start();

        // ── Get Started → SignUp ───────────────────────────────────────────
        btnGetStarted.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class))
        );

        // ── Log In → LoginActivity ─────────────────────────────────────────
        btnGoToLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
    }
}