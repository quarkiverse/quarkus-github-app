package io.quarkiverse.githubapp.runtime.sse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * HTTP Client that can listen for Server-Sent Events (SSE).
 * Implements full protocol and supports automatic reconnect.
 *
 * @author LupCode.com (Luca Vogels)
 * @since 2020-12-26
 */
public class HttpEventStreamClient {

    /**
     * Event that gets received by the {@link HttpEventStreamClient}
     *
     * @author LupCode.com (Luca Vogels)
     * @since 2020-12-22
     */
    public class Event {
        private final long id;
        private final String event;
        private final String data;

        protected Event(long id, String event, String data) {
            this.id = id;
            this.event = event;
            this.data = data;
        }

        /**
         * Returns the ID of the event that has been received
         * (same as {@link HttpEventStreamClient#getLastEventID()}
         *
         * @return ID of received event
         */
        public long getID() {
            return id;
        }

        /**
         * Event type/description that has been received (can be null)
         *
         * @return Type/description of the event or null
         */
        public String getEvent() {
            return event;
        }

        /**
         * Data that has been received (UTF-8)
         *
         * @return Received data
         */
        public String getData() {
            return data;
        }

        @Override
        public String toString() {
            return new StringBuilder(getClass().getSimpleName()).append("{id=").append(id).append("; event=\"").append(event)
                    .append("\"; data=\"").append(data).append("\"}").toString();
        }
    }

    protected abstract class InternalEventStreamAdapter extends EventStreamAdapter {
        public void onStartFirst(HttpResponse<Void> lastResponse) {
        }

        public void onStartLast(HttpResponse<Void> lastResponse, HttpRequest.Builder builder) {
        }
    }

    protected URI uri;
    protected HttpRequestMethod method = HttpRequestMethod.GET;
    protected BodyPublisher requestBody = null;
    protected HttpClient.Version version = null;
    protected TreeMap<String, String> headers = new TreeMap<>();
    protected long timeout, retryCooldown;
    protected int maxReconnectsWithoutEvents;
    protected final AtomicBoolean hasReceivedEvents = new AtomicBoolean(false); // internal use
    protected final AtomicInteger reconnectWithoutEvents = new AtomicInteger(0); // internal use

    protected HttpClient client = null;
    protected long lastEventID = 0;
    protected boolean resetEventIDonReconnect;
    protected HashSet<EventStreamListener> listeners = new HashSet<>();
    protected HashSet<InternalEventStreamAdapter> internalListeners = new HashSet<>();
    protected CompletableFuture<HttpResponse<Void>> running = null;

    /**
     * Creates a HTTP client that listens for Server-Sent Events (SSE).
     * Starts listening after calling {@link HttpEventStreamClient#start()}
     *
     * @param url URL the client should listen at
     * @param listener Event stream listeners that listen for arriving events (optional)
     */
    public HttpEventStreamClient(String url, EventStreamListener... listener) {
        this(url, null, null, null, null, -1, -1, -1, false, null, listener);
    }

    /**
     * Creates a HTTP client that listens for Server-Sent Events (SSE).
     * Starts listening after calling {@link HttpEventStreamClient#start()}
     *
     * @param url URL the client should listen at
     * @param headers HTTP headers that should be set for the request.
     *        SSE specific headers will get overwritten [Accept, Cache-Control, Last-Event-ID] (optional)
     * @param listener Event stream listeners that listen for arriving events (optional)
     */
    public HttpEventStreamClient(String url, Map<String, String> headers, EventStreamListener... listener) {
        this(url, null, null, null, headers, -1, -1, -1, false, null, listener);
    }

    /**
     * Creates a HTTP client that listens for Server-Sent Events (SSE).
     * Starts listening after calling {@link HttpEventStreamClient#start()}
     *
     * @param url URL the client should listen at
     * @param method HTTP method that should be used to request the event stream (default GET)
     * @param requestBody HTTP request body that gets sent along the request (optional)
     * @param headers HTTP headers that should be set for the request.
     *        SSE specific headers will get overwritten [Accept, Cache-Control, Last-Event-ID] (optional)
     * @param listener Event stream listeners that listen for arriving events (optional)
     */
    public HttpEventStreamClient(String url, HttpRequestMethod method, BodyPublisher requestBody, Map<String, String> headers,
            EventStreamListener... listener) {
        this(url, method, requestBody, null, headers, -1, -1, -1, false, null, listener);
    }

    /**
     * Creates a HTTP client that listens for Server-Sent Events (SSE).
     * Starts listening after calling {@link HttpEventStreamClient#start()}
     *
     * @param url URL the client should listen at
     * @param method HTTP method that should be used to request the event stream (default GET)
     * @param requestBody HTTP request body that gets sent along the request (optional)
     * @param headers HTTP headers that should be set for the request.
     *        SSE specific headers will get overwritten [Accept, Cache-Control, Last-Event-ID] (optional)
     * @param timeout Timeout in milliseconds for the HTTP client before it reconnects (if negative then ignored)
     * @param retryCooldown Cooldown in milliseconds after connection loss before starting to reconnect (negative for no
     *        cooldown)
     * @param listener Event stream listeners that listen for arriving events (optional)
     */
    public HttpEventStreamClient(String url, HttpRequestMethod method, BodyPublisher requestBody, Map<String, String> headers,
            long timeout, long retryCooldown, EventStreamListener... listener) {
        this(url, method, requestBody, null, headers, timeout, retryCooldown, -1, false, null, listener);
    }

    /**
     * Creates a HTTP client that listens for Server-Sent Events (SSE).
     * Starts listening after calling {@link HttpEventStreamClient#start()}
     *
     * @param url URL the client should listen at
     * @param method HTTP method that should be used to request the event stream (default GET)
     * @param requestBody HTTP request body that gets sent along the request (optional)
     * @param version Specific HTTP version that should be used to request (optional)
     * @param headers HTTP headers that should be set for the request.
     *        SSE specific headers will get overwritten [Accept, Cache-Control, Last-Event-ID] (optional)
     * @param timeout Timeout in milliseconds for the HTTP client before it reconnects (if negative then ignored)
     * @param retryCooldown Cooldown in milliseconds after connection loss before starting to reconnect (negative for no
     *        cooldown)
     * @param maxReconnectsWithoutEvents How often client can reconnect
     *        without receiving events before it stops (zero for no reconnect, negative for infinitely)
     * @param resetEventIDonReconnect If true then event id will be set back to zero on a reconnect (default false)
     * @param client HTTP client that should be used (optional)
     * @param listener Event stream listeners that listen for arriving events (optional)
     */
    public HttpEventStreamClient(String url, HttpRequestMethod method, BodyPublisher requestBody, HttpClient.Version version,
            Map<String, String> headers, long timeout, long retryCooldown, int maxReconnectsWithoutEvents,
            boolean resetEventIDonReconnect, HttpClient client, EventStreamListener... listener) {
        this.uri = URI.create(url);
        this.method = method != null ? method : this.method;
        this.requestBody = requestBody;
        this.version = version;
        this.timeout = timeout;
        this.retryCooldown = retryCooldown;
        this.maxReconnectsWithoutEvents = maxReconnectsWithoutEvents;
        this.resetEventIDonReconnect = resetEventIDonReconnect;
        this.client = client;
        setHeaders(headers);
        addListener(listener);
    }

    /**
     * URI this client listens for events
     *
     * @return URI that is used to listen for events
     */
    public URI getURI() {
        return uri;
    }

    /**
     * URL string this client listens for events
     *
     * @return URL string that is used to listen for events
     */
    public String getURL() {
        return uri.toString();
    }

    /**
     * Sets the URL that will be used after the next reconnect.
     * If change should immediately take place call {@link HttpEventStreamClient#start()}
     * afterwards
     *
     * @param url URL the client should listen at
     */
    public void setURL(String url) {
        this.uri = URI.create(url);
    }

    /**
     * Sets the URI that will be used after the next reconnect.
     * If change should immediately take place call {@link HttpEventStreamClient#start()}
     * afterwards
     *
     * @param uri URI the client should listen at
     */
    public void setURI(URI uri) {
        if (uri == null)
            throw new NullPointerException("URI cannot be null");
        this.uri = uri;
    }

    /**
     * Returns the HTTP method type that client uses for HTTP requests
     *
     * @return HTTP request method type
     */
    public HttpRequestMethod getHttpMethod() {
        return method;
    }

    /**
     * Sets the HTTP method type that client uses for HTTP requests
     *
     * @param method HTTP request method type used for HTTP requests
     */
    public void setHttpMethod(HttpRequestMethod method) {
        this.method = method != null ? method : HttpRequestMethod.GET;
    }

    /**
     * Returns the HTTP body used for requests (can be null)
     *
     * @return HTTP request body or null
     */
    public BodyPublisher getHttpRequestBody() {
        return requestBody;
    }

    /**
     * Sets a HTTP body used for requests.
     * Only needed for certain HTTP request methods otherwise ignored.
     *
     * @param requestBody HTTP request body or null
     */
    public void setHttpRequestBody(BodyPublisher requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Returns HTTP version if a specific one is set that should be used
     *
     * @return HTTP version that is forced to be used or null
     */
    public HttpClient.Version getHttpVersion() {
        return version;
    }

    /**
     * Sets a specific HTTP version that should be used.
     * If null then HTTP client will automatically determine appropriate version
     *
     * @param version HTTP version that is forced to be used or null
     */
    public void setHttpVersion(HttpClient.Version version) {
        this.version = version;
    }

    /**
     * Returns HTTP headers that will be used for HTTP requests
     *
     * @return HTTP headers map
     */
    public synchronized Map<String, String> getHeaders() {
        return new TreeMap<>(headers);
    }

    /**
     * Adds HTTP headers that will be used for HTTP requests (overwrites existing ones).
     * SSE specific headers cannot be overwritten (Accept, Cache-Control, Last-Event-ID)
     *
     * @param headers HTTP headers that should be added
     */
    public synchronized void addHeaders(Map<String, String> headers) {
        if (headers == null)
            return;
        for (Entry<String, String> entry : headers.entrySet())
            this.headers.put(entry.getKey().trim().toLowerCase(), entry.getValue());
    }

    /**
     * Sets HTTP headers that will be used for HTTP requests (removes all existing ones).
     * SSE specific headers cannot be overwritten (Accept, Cache-Control, Last-Event-ID)
     *
     * @param headers HTTP headers that should be added
     */
    public synchronized void setHeaders(Map<String, String> headers) {
        if (headers == null)
            return;
        this.headers.clear();
        addHeaders(headers);
    }

    /**
     * Sets/Removes a HTTP header that will be used for HTTP requests.
     * SSE specific headers cannot be overwritten (Accept, Cache-Control, Last-Event-ID)
     *
     * @param key Key of the header (cannot be null or blank)
     * @param value Value that should be set (if null then key gets removed)
     */
    public synchronized void setHeader(String key, String value) {
        if (key == null || key.isBlank())
            throw new NullPointerException("Key cannot be null or blank");
        if (value != null && !value.isBlank())
            this.headers.put(key.trim().toLowerCase(), value);
        else
            this.headers.remove(key.trim().toLowerCase());
    }

    /**
     * Returns the value of the HTTP headers for a specific key
     *
     * @param key Key the value should be returned for (cannot be null or empty)
     * @return Value that is set for the HTTP header or null if not set
     */
    public synchronized String getHeader(String key) {
        return key != null ? this.headers.get(key.trim().toLowerCase()) : null;
    }

    /**
     * Removes a HTTP header so it gets no longer used for HTTP requests
     *
     * @param key Key of header that should be removed
     * @return Previously set value or null if not previously set
     */
    public synchronized String removeHeader(String key) {
        if (key == null)
            return null;
        return this.headers.remove(key.trim().toLowerCase());
    }

    /**
     * Removes multiple HTTP headers so they no longer will be used for HTTP requests
     *
     * @param keys Keys of the HTTP headers
     */
    public synchronized void removeHeaders(String... keys) {
        if (keys == null)
            return;
        for (String key : keys)
            this.headers.remove(key.trim().toLowerCase());
    }

    /**
     * Removes all HTTP headers so no custom HTTP headers will be sent in the HTTP requests
     */
    public synchronized void clearHeaders() {
        this.headers.clear();
    }

    /**
     * Returns the timeout in milliseconds for the HTTP client before it reconnects (if negative then ignored)
     *
     * @return Timeout in milliseconds
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout in milliseconds for the HTTP client before it reconnects (if negative then ignored)
     *
     * @param timeout Timeout in milliseconds
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns the cooldown in milliseconds that this client
     * will wait before reconnecting after a connection loss
     *
     * @return Cooldown in milliseconds
     */
    public long getRetryCooldown() {
        return this.retryCooldown;
    }

    /**
     * Sets the cooldown in milliseconds that this client
     * will wait before reconnection after a connection loss
     *
     * @param retryCooldown Cooldown in milliseconds (negative for no cooldown)
     */
    public void setRetryCooldown(long retryCooldown) {
        this.retryCooldown = retryCooldown;
    }

    /**
     * Returns true if client automatically stops after a certain amount
     * of reconnects without receiving events in between
     *
     * @return True if auto stop enabled
     */
    public boolean isAutoStopIfNoEventsEnabled() {
        return maxReconnectsWithoutEvents > 0;
    }

    /**
     * Returns true if client automatically reconnects if connection is lost
     *
     * @return True if reconnect on connection loss
     */
    public boolean isReconnectEnabled() {
        return maxReconnectsWithoutEvents != 0;
    }

    /**
     * Returns how often client can reconnect without receiving events in between
     * before it automatically stops.
     * If zero then client will not reconnect after a connection loss.
     * If negative then auto stop is disabled and client keeps reconnecting for ever
     *
     * @return Max reconnects without events before stopping
     */
    public int getAutoStopThreshold() {
        return maxReconnectsWithoutEvents;
    }

    /**
     * Sets how often the client can reconnect without receiving events in between
     * before it automatically stops.
     * If zero then client will not reconnect after a connection loss.
     * If negative then client will keep reconnecting for ever
     *
     * @param maxReconnectsWithoutEvents How often client can reconnect
     *        without receiving events before it stops (zero for no reconnect, negative for infinitely)
     */
    public void setAutoStopThreshold(int maxReconnectsWithoutEvents) {
        this.maxReconnectsWithoutEvents = maxReconnectsWithoutEvents;
    }

    /**
     * Returns how often client reconnected so far without receiving any events in between.
     * Gets reset to zero if client receives an event
     *
     * @return Reconnection count without events
     */
    public int getReconnectsWithoutEvents() {
        return reconnectWithoutEvents.get();
    }

    /**
     * {@link HttpClient} that gets used for HTTP requests.
     * May be null if not specified and not started yet
     *
     * @return {@link HttpClient} that is used for HTTP requests (may be null)
     */
    public HttpClient getHttpClient() {
        return client;
    }

    /**
     * Sets if a specific {@link HttpClient} should be used for HTTP requests.
     * If null a new {@link HttpClient} instance will be created
     *
     * @param client HTTP client that should be used (null for new instance)
     */
    public void setHttpClient(HttpClient client) {
        this.client = client;
    }

    /**
     * Returns ID of the latest event
     *
     * @return ID of latest event
     */
    public long getLastEventID() {
        return lastEventID;
    }

    /**
     * Sets the event id that should be sent on next start/reconnect.
     * May be overwritten by the server in the mean time.
     * Call {@link HttpEventStreamClient#start()} to force sending of new id
     *
     * @param id Event id that should be sent in HTTP header (Last-Event-ID)
     */
    public void setLastEventID(long id) {
        this.lastEventID = id;
    }

    /**
     * Returns if last event id gets reset to zero on reconnect
     *
     * @return True if set to zero on a reconnect
     */
    public boolean isResetLastEventIDonReconnect() {
        return resetEventIDonReconnect;
    }

    /**
     * Sets if the last event it should be set to zero on a reconnect
     *
     * @param reset If true then last event it will be reset on a reconnect
     */
    public void setResetLastEventIDonReconnect(boolean reset) {
        this.resetEventIDonReconnect = reset;
    }

    /**
     * Returns a set containing all added listeners
     *
     * @return Set of all added listeners
     */
    public Set<EventStreamListener> getListeners() {
        return new HashSet<>(listeners);
    }

    /**
     * Removes all listeners so they no longer get called
     */
    public synchronized void removeAllListeners() {
        listeners.clear();
    }

    /**
     * Adds a listener so it gets called on new events.
     * Multiple adding of same listener will only add once
     *
     * @param listener Listener(s) that should be added
     */
    public synchronized void addListener(EventStreamListener... listener) {
        for (EventStreamListener l : listener)
            if (l != null)
                this.listeners.add(l);
    }

    /**
     * Removes the listeners so they no longer get called
     *
     * @param listener Listeners that should be removed
     */
    public synchronized void removeListener(EventStreamListener... listener) {
        for (EventStreamListener l : listener)
            if (l != null)
                this.listeners.remove(l);
    }

    /**
     * Returns if client is currently listening for SSE events
     *
     * @return
     */
    public boolean isRunning() {
        return running != null && !running.isDone();
    }

    /**
     * Starts listening for SSE events and immediately returns.
     * If client looses connection then automatically reconnects.
     * Multiple calls will not start multiple listening
     * but calls {@link EventStreamListener#onReconnect()} on listeners
     *
     * @return This client instance
     */
    public synchronized HttpEventStreamClient start() {
        for (InternalEventStreamAdapter listener : internalListeners)
            try {
                listener.onStartFirst((running != null && running.isDone()) ? running.get() : null);
            } catch (InterruptedException | ExecutionException ex) {
                for (InternalEventStreamAdapter l : internalListeners)
                    try {
                        l.onError(this, ex);
                    } catch (Exception ex1) {
                    }
            }
        if (running != null) {
            final long leid = lastEventID;
            if (resetEventIDonReconnect)
                lastEventID = 0;
            for (InternalEventStreamAdapter listener : internalListeners)
                try {
                    listener.onReconnect(this, running.isDone() ? running.get() : null, hasReceivedEvents.get(), leid);
                } catch (Exception ex) {
                    for (EventStreamListener l : internalListeners)
                        try {
                            l.onError(this, ex);
                        } catch (Exception ex1) {
                        }
                }
            for (EventStreamListener listener : listeners)
                try {
                    listener.onReconnect(this, running.isDone() ? running.get() : null, hasReceivedEvents.get(), leid);
                } catch (Exception ex) {
                    for (EventStreamListener l : listeners)
                        try {
                            l.onError(this, ex);
                        } catch (Exception ex1) {
                        }
                }
        }
        hasReceivedEvents.set(false);

        if (client == null)
            client = HttpClient.newHttpClient();
        HttpRequest.Builder request = HttpRequest.newBuilder(uri);
        switch (method) {
            case GET:
                request.GET();
                break;
            case POST:
                request.POST(requestBody);
                break;
            case PUT:
                request.PUT(requestBody);
                break;
            case DELETE:
                request.DELETE();
                break;
            default:
                break;
        }
        if (version != null)
            request.version(version);
        for (Entry<String, String> entry : headers.entrySet())
            request.setHeader(entry.getKey(), entry.getValue());
        request.setHeader("Accept", "text/event-stream");
        request.setHeader("Cache-Control", "no-cache");
        if (lastEventID > 0)
            request.setHeader("Last-Event-ID", lastEventID + "");
        if (timeout >= 0)
            request.timeout(Duration.ofMillis(timeout));

        for (InternalEventStreamAdapter listener : internalListeners)
            try {
                listener.onStartLast((running != null && running.isDone()) ? running.get() : null, request);
            } catch (InterruptedException | ExecutionException e1) {
            }

        running = client.sendAsync(request.build(), BodyHandlers.ofByteArrayConsumer(new Consumer<Optional<byte[]>>() {
            StringBuilder sb = new StringBuilder(), data = new StringBuilder();
            String event = null;

            @Override
            public void accept(Optional<byte[]> t) {
                if (t.isPresent()) {
                    hasReceivedEvents.set(true);
                    reconnectWithoutEvents.set(0);

                    sb.append(new String(t.get(), StandardCharsets.UTF_8));
                    int index;
                    while ((index = sb.indexOf("\n\n")) >= 0) {
                        String[] lines = sb.substring(0, index).split("\n");
                        sb.delete(0, index + 2); // delete first block including "\n\n"
                        boolean hasDataOrEvent = false, updatedEventID = false;
                        for (String line : lines) {
                            int idx = line.indexOf(':');
                            if (idx <= 0)
                                continue; // ignore invalids or comments
                            String key = line.substring(0, idx), value = line.substring(idx + 1).trim();
                            switch (key.trim().toLowerCase()) {
                                case "event":
                                    this.event = value;
                                    hasDataOrEvent = true;
                                    break;

                                case "data":
                                    if (data.length() > 0)
                                        data.append("\n");
                                    data.append(value);
                                    hasDataOrEvent = true;
                                    break;

                                case "id":
                                    try {
                                        lastEventID = Long.parseLong(value);
                                        updatedEventID = true;
                                    } catch (Exception ex) {
                                        for (InternalEventStreamAdapter l : internalListeners)
                                            try {
                                                l.onError(HttpEventStreamClient.this, ex);
                                            } catch (Exception ex1) {
                                            }
                                        for (EventStreamListener l : listeners)
                                            try {
                                                l.onError(HttpEventStreamClient.this, ex);
                                            } catch (Exception ex1) {
                                            }
                                    }
                                    break;

                                case "retry":
                                    try {
                                        retryCooldown = Long.parseLong(value);
                                    } catch (Exception ex) {
                                        for (InternalEventStreamAdapter l : internalListeners)
                                            try {
                                                l.onError(HttpEventStreamClient.this, ex);
                                            } catch (Exception ex1) {
                                            }
                                        for (EventStreamListener l : listeners)
                                            try {
                                                l.onError(HttpEventStreamClient.this, ex);
                                            } catch (Exception ex1) {
                                            }
                                    }
                                    break;

                                default:
                                    break;
                            }
                        }

                        if (hasDataOrEvent) {
                            if (!updatedEventID)
                                lastEventID++;
                            Event event = new Event(lastEventID, this.event, this.data.toString());
                            for (InternalEventStreamAdapter listener : internalListeners)
                                try {
                                    listener.onEvent(HttpEventStreamClient.this, event);
                                } catch (Exception ex) {
                                    for (InternalEventStreamAdapter l : internalListeners)
                                        try {
                                            l.onError(HttpEventStreamClient.this, ex);
                                        } catch (Exception ex1) {
                                        }
                                }
                            for (EventStreamListener listener : listeners)
                                try {
                                    listener.onEvent(HttpEventStreamClient.this, event);
                                } catch (Exception ex) {
                                    for (EventStreamListener l : listeners)
                                        try {
                                            l.onError(HttpEventStreamClient.this, ex);
                                        } catch (Exception ex1) {
                                        }
                                }
                            this.data.setLength(0);
                        }
                    }
                }
            }
        }));
        running.handleAsync(new BiFunction<HttpResponse<Void>, Throwable, Void>() {

            @Override
            public Void apply(HttpResponse<Void> t, Throwable u) {
                if (u != null) {
                    for (InternalEventStreamAdapter listener : internalListeners)
                        try {
                            listener.onError(HttpEventStreamClient.this, u);
                        } catch (Exception e) {
                        }
                    for (EventStreamListener listener : listeners)
                        try {
                            listener.onError(HttpEventStreamClient.this, u);
                        } catch (Exception e) {
                        }
                }

                if (!hasReceivedEvents.get())
                    reconnectWithoutEvents.incrementAndGet();

                if (maxReconnectsWithoutEvents < 0 || reconnectWithoutEvents.get() < maxReconnectsWithoutEvents) {
                    if (running != null) {
                        if (retryCooldown > 0)
                            try {
                                Thread.sleep(retryCooldown);
                            } catch (Exception e) {
                            }
                        start();
                    }
                } else {
                    stop();
                }
                return null;
            }
        });
        return this;
    }

    /**
     * Blocks until this client has stopped listening.
     * If not listening then returns immediately
     *
     * @return This client instance
     */
    public HttpEventStreamClient join() {
        while (running != null)
            try {
                running.join();
            } catch (Exception e) {
            }
        lastEventID = 1;
        return this;
    }

    /**
     * Stops without reconnecting.
     * Executes {@link EventStreamListener#onClose()} on listeners
     *
     * @return This client instance
     */
    public synchronized HttpEventStreamClient stop() {
        CompletableFuture<HttpResponse<Void>> run = running;
        running = null;
        HttpResponse<Void> response = null;
        if (run != null) {
            if (run.isDone()) {
                if (!run.isCancelled() && !run.isCompletedExceptionally()) {
                    response = run.getNow(null);
                }
            } else {
                run.cancel(true);
            }
        }
        for (InternalEventStreamAdapter listener : internalListeners)
            try {
                listener.onClose(this, response);
            } catch (Exception e) {
            }
        for (EventStreamListener listener : listeners)
            try {
                listener.onClose(this, response);
            } catch (Exception e) {
            }
        return this;
    }
}
