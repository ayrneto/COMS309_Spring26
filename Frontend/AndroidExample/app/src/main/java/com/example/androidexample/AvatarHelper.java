package com.example.androidexample;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

public class AvatarHelper {

    // Corner radius in dp — matches avatar_rounded_square.xml
    private static final int CORNER_DP = 12;

    public static void load(Context context,
                            String name,
                            String picUrl,
                            TextView tvInitial,
                            ImageView ivPhoto) {

        // Always set the initial as fallback
        if (tvInitial != null) {
            String initial = (name != null && !name.isEmpty())
                    ? String.valueOf(name.charAt(0)).toUpperCase()
                    : "?";
            tvInitial.setText(initial);
        }

        if (picUrl != null && !picUrl.trim().isEmpty() && ivPhoto != null) {
            ivPhoto.setVisibility(View.VISIBLE);
            if (tvInitial != null) tvInitial.setVisibility(View.INVISIBLE);

            int cornerPx = Math.round(CORNER_DP * context.getResources().getDisplayMetrics().density);

            Glide.with(context)
                    .load(picUrl.trim())
                    .apply(new RequestOptions()
                            .centerCrop()
                            .transform(new RoundedCorners(cornerPx))
                            .placeholder(R.drawable.avatar_rounded_square)
                            .error(R.drawable.avatar_rounded_square))
                    .into(ivPhoto);
        } else {
            if (ivPhoto != null) ivPhoto.setVisibility(View.GONE);
            if (tvInitial != null) tvInitial.setVisibility(View.VISIBLE);
        }
    }
}