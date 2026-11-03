package com.goldsilver.livecalc.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Utility for smooth, full-screen crossfade transitions when changing themes.
 * Takes a snapshot of the decorView into a Bitmap, places it as an overlay,
 * executes the theme update, and fades the overlay to 0 over 350ms.
 */
public final class ThemeTransition {

    private static ImageView sActiveOverlay = null;
    private static Bitmap sActiveBitmap = null;

    private ThemeTransition() {
        // Utility class
    }

    /**
     * Crossfade transition between themes.
     *
     * @param activity       The hosting activity.
     * @param applyNewTheme  Runnable executing the theme application.
     */
    public static void crossfade(final Activity activity, final Runnable applyNewTheme) {
        if (activity == null) {
            if (applyNewTheme != null) {
                applyNewTheme.run();
            }
            return;
        }

        // Ensure execution on UI thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    crossfade(activity, applyNewTheme);
                }
            });
            return;
        }

        Window window = activity.getWindow();
        if (window == null) {
            if (applyNewTheme != null) {
                applyNewTheme.run();
            }
            return;
        }

        final View decorView = window.getDecorView();
        final int width = decorView.getWidth();
        final int height = decorView.getHeight();

        if (width <= 0 || height <= 0) {
            if (applyNewTheme != null) {
                applyNewTheme.run();
            }
            return;
        }

        // Cancel and clean up any ongoing transition overlay
        if (sActiveOverlay != null) {
            sActiveOverlay.animate().cancel();
            if (sActiveOverlay.getParent() instanceof ViewGroup) {
                ((ViewGroup) sActiveOverlay.getParent()).removeView(sActiveOverlay);
            }
            sActiveOverlay = null;
        }
        if (sActiveBitmap != null && !sActiveBitmap.isRecycled()) {
            sActiveBitmap.recycle();
            sActiveBitmap = null;
        }

        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            decorView.draw(canvas);
        } catch (Throwable t) {
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
        }

        // If snapshot failed, apply theme directly without transition
        if (bitmap == null) {
            if (applyNewTheme != null) {
                applyNewTheme.run();
            }
            return;
        }

        final Bitmap finalBitmap = bitmap;
        final ImageView overlay = new ImageView(activity);
        overlay.setImageBitmap(finalBitmap);
        overlay.setScaleType(ImageView.ScaleType.FIT_XY);
        overlay.setClickable(true);
        overlay.setFocusable(true);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        final ViewGroup root = (ViewGroup) decorView;
        root.addView(overlay, params);

        sActiveOverlay = overlay;
        sActiveBitmap = finalBitmap;

        // Apply the new theme synchronously
        if (applyNewTheme != null) {
            applyNewTheme.run();
        }

        // Crossfade: fade the snapshot overlay to 0 over 350ms
        overlay.animate()
                .alpha(0f)
                .setDuration(350L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (overlay.getParent() instanceof ViewGroup) {
                            ((ViewGroup) overlay.getParent()).removeView(overlay);
                        }
                        if (finalBitmap != null && !finalBitmap.isRecycled()) {
                            finalBitmap.recycle();
                        }
                        if (sActiveOverlay == overlay) {
                            sActiveOverlay = null;
                            sActiveBitmap = null;
                        }
                    }
                })
                .start();
    }

    /**
     * Maps theme names to their primary background color ARGB integer.
     */
    public static int getThemeBackgroundColorInt(String theme) {
        if (theme == null) return 0xFF0D0E15;
        switch (theme) {
            case "Ivory": return 0xFFFAF9F6;
            case "Cream": return 0xFFF9F6EE;
            case "Mist": return 0xFFF0F4F8;
            case "Sage": return 0xFFF1F4F2;
            case "Lavender": return 0xFFF5F3F7;
            case "Peach": return 0xFFFAF4F0;
            case "Sky Blue": return 0xFFEDF4F9;
            case "Sand": return 0xFFF5EFEB;
            case "Night":
            default:
                return 0xFF0D0E15;
        }
    }

    /**
     * Synchronously applies window background and system bar styles to match the theme.
     */
    public static void applyThemeToWindow(Activity activity, String theme) {
        if (activity == null || activity.getWindow() == null) return;
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        int bgColor = getThemeBackgroundColorInt(theme);
        boolean isNight = "Night".equalsIgnoreCase(theme);

        decorView.setBackgroundColor(bgColor);
        window.setBackgroundDrawable(new ColorDrawable(bgColor));

        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        if (activity instanceof ComponentActivity) {
            ComponentActivity ca = (ComponentActivity) activity;
            if (isNight) {
                EdgeToEdge.enable(ca,
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT));
            } else {
                EdgeToEdge.enable(ca,
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT));
            }
        }

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(window, decorView);
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(!isNight);
            insetsController.setAppearanceLightNavigationBars(!isNight);
        }
    }
}
