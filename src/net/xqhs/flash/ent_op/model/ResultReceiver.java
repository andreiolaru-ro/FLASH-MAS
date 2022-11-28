package net.xqhs.flash.ent_op.model;

public interface ResultReceiver {
    /**
     * @param result
     *            - the result of the operation.
     */
    void resultNotification(Object result);
}
