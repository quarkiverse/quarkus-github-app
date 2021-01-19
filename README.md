# Quarkus GitHub App

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.githubapp/quarkus-github-app?logo=apache-maven&style=for-the-badge)](https://search.maven.org/artifact/io.quarkiverse.githubapp/quarkus-github-app)
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=for-the-badge)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

**Develop your GitHub Apps in Java with Quarkus.**

Quarkus GitHub App is a [Quarkus](https://quarkus.io) extension
that allows to create GitHub Apps in Java with very little boilerplate.

Think of it as [Probot](https://probot.github.io) for Java.

And yes, it supports generating native executables with GraalVM or Mandrel.

Your application will look like:

```java
class MyGitHubApp {

	void onOpen(@Issue.Opened GHEventPayload.Issue issuePayload) throws IOException {
		issuePayload.getIssue().comment("Hello from MyGitHubApp");
	}
}
```

And that's it.

The code above listens to the `issues.opened` GitHub event and posts a comment in each opened issue.

That's for the basics but it also supports YAML or JSON config files in your repository.

Finally, it supports using a [Smee.io proxy](https://smee.io) during the development of the app.

## Documentation

To get you started (and more!), please refer to [the documentation](https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-app/dev/index.html).

Anything missing in the documentation? Please [open an issue](https://github.com/quarkiverse/quarkus-github-app/issues/new).

## How?

The Quarkus GitHub App extension uses [the Hub4j GitHub API](https://github.com/hub4j/github-api)
to parse the webhook payloads and handle the GitHub REST API calls.

The rest of the extension is Quarkus magic - mostly code generation with [Gizmo](https://github.com/quarkusio/gizmo/) -
to get everything wired.

It also leverages [Reactive Routes](https://quarkus.io/guides/reactive-routes),
[CDI events (both sync and async)](https://quarkus.io/guides/cdi#events-and-observers),
and [Caffeine](https://quarkus.io/guides/cache).

## Status

The extension is already well polished and stable.

Work is still needed on the test infrastructure though.

We are making steady progress on it.

## License

This project is licensed under the [Apache License Version 2.0](./LICENSE.txt).

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://www.redhat.com/"><img src="https://avatars1.githubusercontent.com/u/1279749?v=4" width="100px;" alt=""/><br /><sub><b>Guillaume Smet</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkiverse-github-app/commits?author=gsmet" title="Code">💻</a> <a href="#maintenance-gsmet" title="Maintenance">🚧</a></td>
    <td align="center"><a href="https://github.com/yrodiere"><img src="https://avatars1.githubusercontent.com/u/412878?v=4" width="100px;" alt=""/><br /><sub><b>Yoann Rodière</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkiverse-github-app/commits?author=yrodiere" title="Code">💻</a></td>
  </tr>
</table>

<!-- markdownlint-enable -->
<!-- prettier-ignore-end -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
