package com.planetforge;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PlanetRenderer implements GLSurfaceView.Renderer {

    private FloatBuffer vertices;
    private ShortBuffer indices;
    private int indexCount;
    private int program;

    private float rotationX = 0;
    private float rotationY = 0;

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];

    private int positionHandle;
    private int mvpHandle;

    private final String vertexShader =
        "uniform mat4 uMVP;" +
        "attribute vec4 aPosition;" +
        "varying vec3 vPosition;" +
        "void main() {" +
        "  vPosition = aPosition.xyz;" +
        "  gl_Position = uMVP * aPosition;" +
        "}";

    private final String fragmentShader =
        "precision mediump float;" +
        "varying vec3 vPosition;" +
        "void main() {" +
        "  float n = sin(vPosition.x * 13.0) * " +
        "            cos(vPosition.y * 11.0) * " +
        "            sin(vPosition.z * 9.0);" +
        "  vec3 ocean = vec3(0.025, 0.18, 0.42);" +
        "  vec3 land = vec3(0.12, 0.48, 0.24);" +
        "  vec3 color = mix(ocean, land, smoothstep(0.0, 0.3, n));" +
        "  float light = max(dot(normalize(vPosition)," +
        "                     normalize(vec3(-0.5, 0.6, 1.0))), 0.0);" +
        "  gl_FragColor = vec4(color * (0.25 + light * 0.85), 1.0);" +
        "}";

    public void rotate(float dx, float dy) {
        rotationY += dx * 0.5f;
        rotationX += dy * 0.5f;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.015f, 0.02f, 0.06f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        program = createProgram(vertexShader, fragmentShader);
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVP");

        createSphere(48, 48);
    }

    private void createSphere(int stacks, int slices) {
        float[] data = new float[(stacks + 1) * (slices + 1) * 3];
        short[] faces = new short[stacks * slices * 6];

        int p = 0;
        for (int i = 0; i <= stacks; i++) {
            double lat = Math.PI * (-0.5 + (double)i / stacks);

            for (int j = 0; j <= slices; j++) {
                double lon = 2.0 * Math.PI * j / slices;

                data[p++] = (float)(Math.cos(lat) * Math.cos(lon));
                data[p++] = (float)Math.sin(lat);
                data[p++] = (float)(Math.cos(lat) * Math.sin(lon));
            }
        }

        int f = 0;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = i * (slices + 1) + j;
                int b = a + slices + 1;

                faces[f++] = (short)a;
                faces[f++] = (short)b;
                faces[f++] = (short)(a + 1);

                faces[f++] = (short)(a + 1);
                faces[f++] = (short)b;
                faces[f++] = (short)(b + 1);
            }
        }

        vertices = ByteBuffer.allocateDirect(data.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        vertices.put(data).position(0);

        indices = ByteBuffer.allocateDirect(faces.length * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer();
        indices.put(faces).position(0);

        indexCount = faces.length;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float)width / Math.max(height, 1);
        Matrix.perspectiveM(projection, 0, 45, ratio, 0.1f, 100f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT |
            GLES20.GL_DEPTH_BUFFER_BIT
        );

        GLES20.glUseProgram(program);

        Matrix.setLookAtM(
            view, 0,
            0, 0, 3.2f,
            0, 0, 0,
            0, 1, 0
        );

        Matrix.setIdentityM(model, 0);
        Matrix.rotateM(model, 0, rotationX, 1, 0, 0);
        Matrix.rotateM(model, 0, rotationY, 0, 1, 0);

        float[] temp = new float[16];
        Matrix.multiplyMM(temp, 0, view, 0, model, 0);
        Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0);

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0);

        vertices.position(0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(
            positionHandle, 3,
            GLES20.GL_FLOAT, false, 12, vertices
        );

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indexCount,
            GLES20.GL_UNSIGNED_SHORT,
            indices
        );

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private int createShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private int createProgram(String vertex, String fragment) {
        int vs = createShader(GLES20.GL_VERTEX_SHADER, vertex);
        int fs = createShader(GLES20.GL_FRAGMENT_SHADER, fragment);

        int result = GLES20.glCreateProgram();
        GLES20.glAttachShader(result, vs);
        GLES20.glAttachShader(result, fs);
        GLES20.glLinkProgram(result);

        return result;
    }
}
