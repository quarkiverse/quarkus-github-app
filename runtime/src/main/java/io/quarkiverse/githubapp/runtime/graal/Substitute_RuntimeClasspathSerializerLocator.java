package io.quarkiverse.githubapp.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.jsonwebtoken.io.JacksonSerializer;
import io.jsonwebtoken.io.Serializer;

@TargetClass(className = "io.jsonwebtoken.impl.io.RuntimeClasspathSerializerLocator")
public final class Substitute_RuntimeClasspathSerializerLocator {

    @Substitute
    protected Serializer<Object> locate() {
        return new JacksonSerializer<Object>();
    }
}
