package com.myai.companion;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnimeWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new AnimeEngine();
    }

    private class AnimeEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private boolean visible = false;
        private float touchX = -1, touchY = -1;
        private boolean isHappy = false;
        private long lastTouchTime = 0;
        private float animTick = 0;

        private final Paint bgPaint = new Paint();
        private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint blushPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final List<Particle> particles = new ArrayList<>();

        private final Runnable drawRunnable = new Runnable() {
            @Override
            public void run() {
                drawFrame();
            }
        };

        AnimeEngine() {
            bgPaint.setColor(Color.parseColor("#0F101A"));
            bodyPaint.setColor(Color.parseColor("#FFE4D6"));
            hairPaint.setColor(Color.parseColor("#C4D6ED")); // Silver pastel hair
            eyePaint.setColor(Color.parseColor("#7A5CFF"));  // Glowing amethyst violet eyes
            blushPaint.setColor(Color.parseColor("#66FF729F"));
            particlePaint.setColor(Color.parseColor("#FFD1DC"));
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                drawFrame();
            } else {
                handler.removeCallbacks(drawRunnable);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            visible = false;
            handler.removeCallbacks(drawRunnable);
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchX = event.getX();
                touchY = event.getY();
                isHappy = true;
                lastTouchTime = System.currentTimeMillis();

                // Generate burst of cute sparkle particles on touch
                for (int i = 0; i < 12; i++) {
                    particles.add(new Particle(touchX, touchY));
                }
            }
            super.onTouchEvent(event);
        }

        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    animTick += 0.05f;

                    if (isHappy && (System.currentTimeMillis() - lastTouchTime > 2000)) {
                        isHappy = false;
                    }

                    renderScene(c);
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }

            handler.removeCallbacks(drawRunnable);
            if (visible) {
                handler.postDelayed(drawRunnable, 16); // Smooth ~60 FPS
            }
        }

        private void renderScene(Canvas c) {
            int w = c.getWidth();
            int h = c.getHeight();

            // Background
            c.drawRect(0, 0, w, h, bgPaint);

            // Subtle gentle breathing effect
            float breath = (float) Math.sin(animTick) * 8f;
            float centerX = w / 2f;
            float centerY = (h / 2f) + breath;

            // Hair back layer
            c.drawCircle(centerX, centerY - 80, 240, hairPaint);

            // Neck & Shoulder
            c.drawRect(centerX - 40, centerY + 100, centerX + 40, centerY + 220, bodyPaint);
            Paint dressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dressPaint.setColor(Color.parseColor("#1F2232"));
            c.drawRoundRect(centerX - 180, centerY + 200, centerX + 180, h, 60, 60, dressPaint);

            // Head (Chibi / Anime proportions)
            c.drawCircle(centerX, centerY, 190, bodyPaint);

            // Blush circles
            c.drawCircle(centerX - 100, centerY + 40, 32, blushPaint);
            c.drawCircle(centerX + 100, centerY + 40, 32, blushPaint);

            // Eyes (Blinking + Touch reaction)
            boolean blinking = ((int)(animTick * 10)) % 70 == 0;
            if (isHappy) {
                // Happy curved eyes "^ ^"
                Paint happyEye = new Paint(Paint.ANTI_ALIAS_FLAG);
                happyEye.setColor(Color.parseColor("#222233"));
                happyEye.setStyle(Paint.Style.STROKE);
                happyEye.setStrokeWidth(12f);
                c.drawArc(centerX - 130, centerY - 40, centerX - 50, centerY + 20, 200, 140, false, happyEye);
                c.drawArc(centerX + 50, centerY - 40, centerX + 130, centerY + 20, 200, 140, false, happyEye);
            } else if (blinking) {
                // Closed line eyes
                Paint closedEye = new Paint(Paint.ANTI_ALIAS_FLAG);
                closedEye.setColor(Color.parseColor("#222233"));
                closedEye.setStrokeWidth(10f);
                c.drawLine(centerX - 120, centerY - 10, centerX - 60, centerY - 10, closedEye);
                c.drawLine(centerX + 60, centerY - 10, centerX + 120, centerY - 10, closedEye);
            } else {
                // Big Anime Eyes
                c.drawOval(centerX - 130, centerY - 50, centerX - 50, centerY + 30, eyePaint);
                c.drawOval(centerX + 50, centerY - 50, centerX + 130, centerY + 30, eyePaint);

                // Eye lights / specular shine
                Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
                shine.setColor(Color.WHITE);
                c.drawCircle(centerX - 100, centerY - 25, 18, shine);
                c.drawCircle(centerX + 80, centerY - 25, 18, shine);
                c.drawCircle(centerX - 75, centerY + 5, 8, shine);
                c.drawCircle(centerX + 105, centerY + 5, 8, shine);
            }

            // Anime Hair Bangs (Front Layer)
            Path hairPath = new Path();
            hairPath.moveTo(centerX - 210, centerY - 60);
            hairPath.quadTo(centerX - 120, centerY + 70, centerX - 140, centerY + 130);
            hairPath.quadTo(centerX - 70, centerY + 20, centerX, centerY + 30);
            hairPath.quadTo(centerX + 70, centerY + 20, centerX + 140, centerY + 130);
            hairPath.quadTo(centerX + 120, centerY + 70, centerX + 210, centerY - 60);
            hairPath.quadTo(centerX, centerY - 260, centerX - 210, centerY - 60);
            c.drawPath(hairPath, hairPaint);

            // Cute small smile
            Paint mouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mouthPaint.setColor(Color.parseColor("#AA3355"));
            mouthPaint.setStyle(Paint.Style.STROKE);
            mouthPaint.setStrokeWidth(7f);
            if (isHappy) {
                mouthPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                c.drawArc(centerX - 25, centerY + 70, centerX + 25, centerY + 110, 0, 180, true, mouthPaint);
            } else {
                c.drawArc(centerX - 20, centerY + 75, centerX + 20, centerY + 95, 20, 140, false, mouthPaint);
            }

            // Render & Update Sparkle Particles
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update();
                particlePaint.setAlpha((int) (p.life * 255));
                c.drawCircle(p.x, p.y, p.size, particlePaint);
                if (p.isDead()) {
                    it.remove();
                }
            }
        }
    }

    private static class Particle {
        float x, y, vx, vy, size, life;

        Particle(float startX, float startY) {
            x = startX;
            y = startY;
            double angle = Math.random() * Math.PI * 2;
            float speed = (float) (Math.random() * 8 + 3);
            vx = (float) Math.cos(angle) * speed;
            vy = (float) Math.sin(angle) * speed;
            size = (float) (Math.random() * 12 + 6);
            life = 1.0f;
        }

        void update() {
            x += vx;
            y += vy;
            life -= 0.03f;
        }

        boolean isDead() {
            return life <= 0;
        }
    }
}
