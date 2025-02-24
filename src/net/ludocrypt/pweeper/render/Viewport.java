package net.ludocrypt.pweeper.render;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Viewport {

    Matrix4f mat;
    Matrix4f transMat;

    boolean locked;

    JPanel drawPanel;

    public Viewport(JPanel drawPanel) {
        mat = new Matrix4f();
        transMat = new Matrix4f();
        this.drawPanel = drawPanel;
    }

    public void lock() {
        locked = true;
    }

    public void unlock() {
        locked = !locked;
    }

    public void setMouse(double x, double y) {
        if (!locked) {
            transMat = new Matrix4f();
            transMat.translate((float) x, (float) y, 0);
        }
    }

    public void pushMouse() {
        Matrix4f mat = new Matrix4f();
        mat.mul(this.transMat);
        mat.mul(this.mat);

        this.mat = mat;

        setMouse(0, 0);
    }

    public void zoom(double x, double y, double zoomScale, int dir) {
        if (!locked) {
            zoomScale += Math.abs(dir / 10.0);

            if (dir > 0) {
                zoomScale = 1 / zoomScale;
            }

            float scaleX = mat.m00();
            float scaleY = mat.m11();

            float translateX = mat.m30();
            float translateY = mat.m31();

            float worldX = ((float) x - translateX) / scaleX;
            float worldY = ((float) y - translateY) / scaleY;

            mat.m00(scaleX * (float) zoomScale);
            mat.m11(scaleY * (float) zoomScale);

            mat.m30((float) x - worldX * mat.m00());
            mat.m31((float) y - worldY * mat.m11());
        }
    }

    public Matrix4f composeMat(boolean inv) {
        Matrix4f projMat = new Matrix4f();
        projMat.mul(transMat);
        projMat.mul(mat);

        if (inv) {
            projMat.invert();
        }

        return projMat;
    }

    public Matrix4f composeMat() {
        return composeMat(false);
    }

    public void drawTransformedImage(Graphics2D g, BufferedImage image) {
        Matrix4f mat = composeMat();

        Vector3f scale = new Vector3f();
        scale = mat.getScale(scale);

        Vector3f translation = new Vector3f();
        translation = mat.getTranslation(translation);

        AffineTransform at = new AffineTransform(scale.x, 0, 0, scale.y, translation.x * drawPanel.getWidth(), translation.y * drawPanel.getHeight());
        g.drawImage(image, at, null);
    }

}