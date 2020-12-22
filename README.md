# Quarkiverse GitHub App
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

**Develop your GitHub Apps in Java with Quarkus.**

Quarkiverse GitHub App is a [Quarkus](https://quarkus.io) extension
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

## How?

The Quarkiverse GitHub App extension uses [the Hub4j GitHub API](https://github.com/hub4j/github-api)
to parse the webhook payloads and handle the GitHub REST API calls.

The rest of the extension is Quarkus magic - mostly code generation with [Gizmo](https://github.com/quarkusio/gizmo/) -
to get everything wired.

It also leverages [Reactive Routes](https://quarkus.io/guides/reactive-routes),
[CDI events (both sync and async)](https://quarkus.io/guides/cdi#events-and-observers),
and [Caffeine](https://quarkus.io/guides/cache).

## Status

The extension is still experimental but is already well polished and stable.

Work is still needed on two fronts:

* Documentation
* Test infrastructure

We are making steady progress on both of them.

## Quick'n'dirty setup

### Create a new Smee.io channel

* Go to https://smee.io/
* Click on `Start a new channel`
* Save the provided URL

### Create an application

You need to create your own instance of the application.

To create your application, go to: https://github.com/settings/apps/new and confirm your password.

Please avoid using a name too generic.
Using your GitHub username as a suffix is a good idea.

`Homepage URL` is mandatory, it can be whatever you want.

Put the Smee.io channel URL in `Webhook URL`.
Generate a unique secret by typing random letters or output of something like: `ruby -rsecurerandom -e 'puts SecureRandom.hex(20)'`
Take note of that secret, you will need it when running in production/from a jar.

Define some sensible repository permissions for your tests.

Typically having:

* Issues - `Read & Write`
* Pull Requests - `Read & Write`
* Contents - `Read`

makes sense for a start.
You can adjust the permissions later.

Once done, you will arrive on the application page settings.

Take note of your numeric `App ID`, you will need it.

Click on `Generate a private key` to generate a new private key.
Your browser will download the new key.

### Create a `.env` file

At the root of the repository, create a `.env` file containing:

[source]
------
QUARKUS_GITHUB_APP_APP_ID=<your numeric app id>
QUARKUS_GITHUB_APP_WEBHOOK_SECRET=<entered secret>
QUARKUS_GITHUB_APP_WEBHOOK_PROXY_URL=<your Smee.io channel URL>
QUARKUS_GITHUB_APP_PRIVATE_KEY=-----BEGIN RSA PRIVATE KEY-----\
                                                                \
                        YOUR PRIVATE KEY                        \
                                                                \
-----END RSA PRIVATE KEY-----
------

Then you can start your application as usual with `./mvnw clean quarkus:dev`.

It will connect to the Smee.io channel and redirect the requests received there to your local running application.

### Redeliver payloads

It might be quite practical to redeliver a payload when testing or even have a look at the generated payload.

You can do that here: https://github.com/settings/apps/<your app name>/advanced .


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
