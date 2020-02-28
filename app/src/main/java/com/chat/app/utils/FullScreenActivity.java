package com.chat.app.utils;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.chat.app.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class FullScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_screen);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String imgsUrl = extras.getString("img_url");
        ImageView imageView = findViewById(R.id.ID_full_screen_img);
        Picasso.get().load(imgsUrl).into(imageView, new Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(Exception e) {
                imageView.setImageResource(R.drawable.ic_error_1);
            }
        });

    }
}
