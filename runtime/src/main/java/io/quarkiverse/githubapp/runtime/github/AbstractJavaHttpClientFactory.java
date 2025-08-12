package io.quarkiverse.githubapp.runtime.github;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.regex.Pattern;

import io.quarkiverse.JavaHttpClientFactory;

public abstract class AbstractJavaHttpClientFactory implements JavaHttpClientFactory {

    private static final Pattern PROXY_SCHEME_PATTERN = Pattern.compile("^(http|https)://");

    protected HttpClient.Builder createDefaultClientBuilder() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(createProxySelector());
    }

    private ProxySelector createProxySelector() {
        final var proxy = createProxy();
        return new ProxySelector() {
            @Override
            public List<Proxy> select(final URI uri) {
                return List.of(proxy);
            }

            @Override
            public void connectFailed(final URI uri, final SocketAddress sa, final IOException ioe) {
                throw new IllegalStateException(String.format("Unable to connect to proxy %s to access URL %s", sa, uri), ioe);
            }
        };
    }

    private Proxy createProxy() {
        String proxyEnv = System.getenv("HTTP_PROXY");
        if (proxyEnv == null || proxyEnv.isEmpty()) {
            return Proxy.NO_PROXY;
        }

        // do not cache the pattern as it will be used only once
        String address = proxyEnv.replaceFirst("^(http|https)://", "");
        String[] parts = address.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid proxy format. Expected format: http[s]://host:port but got: " + proxyEnv);
        }

        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }
}
