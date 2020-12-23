package io.quarkiverse.githubapp.runtime;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.jboss.logging.Logger;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.runtime.replay.ReplayUiStaticHandler;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class GitHubAppRecorder {

    private static final Logger LOG = Logger.getLogger(GitHubEvent.class.getPackageName());

    public Handler<RoutingContext> replayUiHandler(String replayUiFinalDestination, String replayUiPath,
            ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(new CleanupReplayUiTempDirectory(replayUiFinalDestination));

        return new ReplayUiStaticHandler(replayUiFinalDestination, replayUiPath);
    }

    private static final class CleanupReplayUiTempDirectory implements Runnable {

        private final String replayUiFinalDestination;

        private CleanupReplayUiTempDirectory(String replayUiFinalDestination) {
            this.replayUiFinalDestination = replayUiFinalDestination;
        }

        @Override
        public void run() {
            try {
                Files.walkFileTree(Path.of(replayUiFinalDestination),
                        new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }
                        });
            } catch (IOException e) {
                LOG.error("Error cleaning up Replay UI temporary directory: " + replayUiFinalDestination, e);
            }
        }
    }
}
