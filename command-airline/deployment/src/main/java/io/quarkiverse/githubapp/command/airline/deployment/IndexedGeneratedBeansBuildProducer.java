package io.quarkiverse.githubapp.command.airline.deployment;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;

class IndexedGeneratedBeansBuildProducer implements BuildProducer<GeneratedBeanBuildItem> {

    private final BuildProducer<GeneratedBeanBuildItem> delegate;
    private final Indexer indexer;

    private boolean empty = true;

    public IndexedGeneratedBeansBuildProducer(BuildProducer<GeneratedBeanBuildItem> delegate) {
        this.delegate = delegate;
        this.indexer = new Indexer();
    }

    @Override
    public void produce(GeneratedBeanBuildItem generatedBean) {
        delegate.produce(generatedBean);
        try {
            indexer.index(new ByteArrayInputStream(generatedBean.getData()));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to index generated class " + generatedBean.getName());
        }
        empty = false;
    }

    public IndexView getIndex() {
        return indexer.complete();
    }

    public boolean isEmpty() {
        return empty;
    }
}
