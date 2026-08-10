package ddlc.yuri.utils.misc;

public final class Translate {

    private double x, y;
    private long lastTime;

    public Translate(double x, double y) {
        this.x = x;
        this.y = y;
        this.lastTime = System.currentTimeMillis();
    }

    /**
     * Animates towards new target coordinates using delta-time exponential smoothing.
     *
     * @param targetX Destination X coordinate
     * @param targetY Destination Y coordinate
     * @param speed   Smoothing factor (recommended range: 10.0 to 25.0; higher = faster)
     */
    public void animate(double targetX, double targetY, double speed) {
        long now = System.currentTimeMillis();
        double deltaTime = (now - lastTime) / 1000.0D;
        this.lastTime = now;

        // Prevent large jumps if frames drop or GUI re-opens
        if (deltaTime > 0.1D) {
            deltaTime = 0.1D;
        }

        this.x = animateExponential(this.x, targetX, speed, deltaTime);
        this.y = animateExponential(this.y, targetY, speed, deltaTime);
    }

    public void animate(double targetX, double targetY) {
        animate(targetX, targetY, 16.0D); // Default smooth speed factor
    }

    /**
     * Frame-rate independent exponential interpolation (smooth dampening).
     */
    public static double animateExponential(double current, double target, double speed, double deltaTime) {
        double diff = target - current;

        // Snap to target when extremely close to prevent micro-stuttering/precision drift
        if (Math.abs(diff) < 0.001D) {
            return target;
        }

        // Exponential decay equation: 1 - e^(-speed * dt)
        double factor = 1.0D - Math.exp(-speed * deltaTime);
        return current + diff * factor;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        this.lastTime = System.currentTimeMillis();
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        this.lastTime = System.currentTimeMillis();
    }
}