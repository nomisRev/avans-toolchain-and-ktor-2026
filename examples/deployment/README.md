# Live demo: Ktor → Docker Hub → Render

The deck compares Gradle packaging with the copied Kotlin Toolchain plugins, then deploys the real [auth demo](../../auth-demo/README.md). Allow 3–5 minutes after accounts and dependencies are prepared.

## Build the local demo

Run from `avans-college/`:

```sh
./kotlin check
./kotlin do buildImage
```

The tarball is `build/tasks/_auth-demo_buildImage@ktor-plugin/jib-image.tar`. To run with Docker Desktop, set a random signing key privately and use the local task:

```sh
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
./kotlin do runDocker
```

This forwards the key from the host environment, publishes port 8080 and uses the configured `HOST=0.0.0.0`. Stop the demo with Ctrl-C. The image itself contains no signing key.

## Publish through Docker Desktop credentials

The module configures `credHelper: desktop` and destination `docker.io/vergauwensimon/avans-college-auth:demo-1`. Docker Desktop's helper was checked for a stored Docker Hub login matching that namespace, without exposing its secret. Ensure that account can push to the repository. Choose public visibility for a demo without Render pull credentials.

When ready to publish:

```sh
./kotlin do publishImage
```

This performs a real registry write and publishes both `demo-1` and `latest`. Jib assembles/pushes directly; it does not need a Dockerfile or the Docker daemon for the image build. The Desktop credential helper must be available and able to access the stored login.

The Gradle comparison uses `./gradlew publishImage`, `ktor.docker.externalRegistry` and the `dockerHub` helper with environment-backed `DOCKERHUB_USERNAME`/`DOCKERHUB_TOKEN`, as shown on the slide. This runnable demo uses Toolchain rather than a second Gradle build definition.

## Run on Render

Use [auth-demo/render.yaml](../../auth-demo/render.yaml) as a Blueprint after publishing, or create **New → Web Service → Existing Image** manually:

1. Image: `docker.io/vergauwensimon/avans-college-auth:demo-1`.
2. Select Frankfurt and your compute plan. The blueprint selects free compute.
3. Set `HOST=0.0.0.0`, `PORT=10000`, and `JAVA_TOOL_OPTIONS=-Xmx256m -XX:MaxDirectMemorySize=64m`.
4. Generate a random 32-byte Base64 key privately (`openssl rand -base64 32`), and enter it as `JWT_SECRET_BASE64`. Preserve it for subsequent deploys.
5. Set `/health` as health check; leave Docker Command empty for Jib's entrypoint.
6. Deploy, open the assigned HTTPS URL, then run `auth-demo/demo.http` with that base URL.

Render requires `linux/amd64`, which the module explicitly selects, and terminates TLS. Private images require a registry credential in Render; the local Desktop helper does not configure remote pull access. Users disappear on restart because storage is in memory. Re-register before the login walkthrough.

## Turn the manual deploy into a webhook

The “Build, publish, deploy” slide advances through five states with four clicks: build, publish, manual pull, webhook connection, automated deployment. Purple arrows carry the image; the pink dashed arrow carries the deploy notification.

In Render's service settings, copy the secret Deploy Hook URL. In the Docker Hub repository's **Webhooks** tab, add that URL as a webhook destination. After the next image push, Docker Hub sends a POST; Render's hook triggers a new pull and deployment. This composes the two documented HTTP interfaces; rehearse the end-to-end delivery before the talk.

For this demo, keep both the published tag and Render's configured reference at `:demo-1`. Render does not use the tag in Docker Hub's JSON body to choose a different image. Use a dedicated demo repository because pushes to other tags can also trigger its webhook. Ktor/Jib may publish `latest` alongside the configured tag, producing multiple notifications. For versioned deployments, prefer a CI call with Render's `imgURL` parameter set to the intended tag or digest. Keep the hook URL secret.

Sources: [Docker Hub outgoing webhooks](https://docs.docker.com/docker-hub/repos/manage/webhooks/), [Render deploy hooks](https://render.com/docs/deploy-hooks).

Free instances sleep after 15 minutes idle and take about a minute to wake. Open the service before presenting; use a paid compute instance if idle sleep would disrupt the session. Rehearse the exact image once and retain its reference as a fallback. Remove the demo service when finished if it is no longer needed.

The local linux/amd64 image passed a container smoke test (health, register, login and typed `/me`) with a 512 MiB limit. Remote publication, Render deployment and webhook delivery still need a live rehearsal.

Sources: [Ktor packaging tasks](https://ktor.io/docs/docker.html), [Jib configuration](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin#extended-usage), [Render image deployment](https://render.com/docs/deploying-an-image), [port binding](https://render.com/docs/web-services#port-binding), [free compute limits](https://render.com/docs/free).
