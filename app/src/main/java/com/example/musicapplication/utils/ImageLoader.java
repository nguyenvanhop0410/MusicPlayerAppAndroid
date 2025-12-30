package com.example.musicapplication.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicapplication.R;

/**
 * Utility class for loading images using Glide
 * Provides convenient methods for common image loading scenarios
 */
public class ImageLoader {
    
    /**
     * Load ảnh cơ bản
     */
    public static void load(Context context, String url, ImageView imageView) {
        Glide.with(context)
            .load(url)
            .placeholder(R.drawable.ic_music)
            .error(R.drawable.ic_music)
            .centerCrop()
            .into(imageView);
    }
    
    /**
     * Load ảnh với góc bo tròn
     */
    public static void loadRounded(Context context, String url, ImageView imageView, int radiusDp) {
        Glide.with(context)
            .load(url)
            .placeholder(R.drawable.ic_music)
            .error(R.drawable.ic_music)
            .transform(new CenterCrop(), new RoundedCorners(radiusDp))
            .into(imageView);
    }
    
    /**
     * Load ảnh tròn (cho avatar)
     */
    public static void loadCircle(Context context, String url, ImageView imageView) {
        Glide.with(context)
            .load(url)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .circleCrop()
            .into(imageView);
    }
    
    /**
     * Load ảnh với callback (để lấy Bitmap cho Palette)
     */
    public static void loadWithCallback(Context context, String url, ImageView imageView, 
                                       OnImageLoadedListener listener) {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                    imageView.setImageBitmap(bitmap);
                    if (listener != null) listener.onLoaded(bitmap);
                }
                
                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {}
            });
    }
    
    public interface OnImageLoadedListener {
        void onLoaded(Bitmap bitmap);
    }

    /**
     * Callback interface for bitmap loading
     */
    public interface BitmapCallback {
        void onBitmapLoaded(Bitmap bitmap);
        void onBitmapFailed();
    }

    /**
     * Load bitmap cho notification (không bind với ImageView)
     */
    public static void loadBitmap(Context context, String url, BitmapCallback callback) {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                    if (callback != null) {
                        callback.onBitmapLoaded(bitmap);
                    }
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    if (callback != null) {
                        callback.onBitmapFailed();
                    }
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {}
            });
    }
}
