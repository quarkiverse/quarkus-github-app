package io.quarkiverse.githubapp.runtime.sse;

import java.net.http.HttpResponse;

/**
 * Interface to implement event stream listeners for the {@link HttpEventStreamClient}
 *
 * @author LupCode.com (Luca Vogels)
 * @since 2020-12-22
 */
public interface EventStreamListener {

    /**
     * Gets called if a new event has been received
     *
     * @param client Event stream client that received the event
     * @param event Event that has been received
     */
    public void onEvent(HttpEventStreamClient client, HttpEventStreamClient.Event event);

    /**
     * Gets called if an error has occurred
     *
     * @param client Event stream client that caused the error
     * @param throwable Error that occurred
     */
    public void onError(HttpEventStreamClient client, Throwable throwable);

    /**
     * Gets called if {@link HttpEventStreamClient} lost connection and will reconnect
     *
     * @param client Event stream client that reconnects
     * @param response Last response received from server (may be null)
     * @param hasReceivedEvents True if at least one event has been received since the last (re-)connect
     * @param lastEventID ID of last event that was received
     */
    public void onReconnect(HttpEventStreamClient client, HttpResponse<Void> response, boolean hasReceivedEvents,
            long lastEventID);

    /**
     * Gets called if client has been closed
     *
     * @param client Event stream client that has closed
     * @param response Last response received from server (may be null)
     */
    public void onClose(HttpEventStreamClient client, HttpResponse<Void> response);
}
