package com.planetforge;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;

public class MainActivity extends Activity {

    private GLSurfaceView glView;
    private PlanetRenderer renderer;
    private float lastX, lastY;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);

        renderer = new PlanetRenderer();
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        glView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;

                    renderer.rotate(dx, dy);

                    lastX = event.getX();
                    lastY = event.getY();
                    return true;
            }
            return true;
        });

        setContentView(glView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
    }
}
