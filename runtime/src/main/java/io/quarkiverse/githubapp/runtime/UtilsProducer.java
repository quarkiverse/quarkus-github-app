package io.quarkiverse.githubapp.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import okhttp3.OkHttpClient;

@Singleton
public class UtilsProducer {

    @Produces
    @Singleton
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }
}
