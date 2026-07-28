package ddlc.yuri.api.events.annotations;

// disgusting
public interface Cancellable {

    /**
     * Checks if the object is cancelled.
     *
     * @return {@code true} if the object is cancelled, {@code false} otherwise.
     */
    boolean isCancelled();

    /**
     * Sets the cancellation state of the object.
     *
     * @param state {@code true} to cancel the object, {@code false} to uncancel it.
     */
    void setCancelled(boolean state);
}
