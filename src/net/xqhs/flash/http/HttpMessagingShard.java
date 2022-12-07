package net.xqhs.flash.http;

import net.xqhs.flash.core.support.NameBasedMessagingShard;

public class HttpMessagingShard extends NameBasedMessagingShard {

    /**
     * The serial UID.
     */
    private static final long serialVersionUID = 2L;

    @Override
    protected void receiveMessage(String source, String destination, String content) {
        super.receiveMessage(source, destination, content);
    }
}
