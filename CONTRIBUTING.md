# Contributing

## Commit messages

Commit messages must follow the [Conventional Commits](https://www.conventionalcommits.org/) format, e.g.
`fix(jbr-toolchain): cache JBR reached through a marker`. Allowed types are `feat`, `fix`, `docs`, `style`,
`refactor`, `perf`, `test`, `build`, `ci`, `chore`, and `revert`. The rules live in [`.gitlint`](.gitlint).

This is enforced by a `commit-msg` git hook managed by [prek](https://github.com/j178/prek) (a drop-in
reimplementation of [pre-commit](https://pre-commit.com/)) running [gitlint](https://jorisroovers.com/gitlint/).
CI checks it too, so set the hook up locally to catch problems before pushing:

1. Install prek — for example `brew install prek`, `uv tool install prek`, or the installer from the prek
   documentation.
2. From the repository root, run `prek install`.

`prek install` wires up both the `pre-commit` and `commit-msg` hooks (the config's `default_install_hook_types`
covers the latter, which is where the message check runs). prek provisions gitlint's environment on first run, so
no separate gitlint installation is needed.

Pull requests are rebase-merged, so each commit you push lands on `master` as-is — keep every commit message
conventional, not just the pull request title.

## Versioning

Each module under `subprojects/` is versioned independently in its own `gradle.properties` and released by tagging
`<module>-<version>`. Two checks guard the versioning, both run in CI and available locally:

- `./gradlew checkApiCompatibility` fails a module whose public API changed more than its version bump allows
  (a removed or changed declaration needs a major bump, a new one a minor bump), comparing the
  binary-compatibility-validator `.api` dump against the module's last release.
- `./gradlew checkReleaseVersions` additionally fails if a changed module's dependents have not been bumped, so a
  fix in a module is always propagated into a new release of everything that depends on it.

When you change a module, bump its version accordingly and bump every module that depends on it by at least a
patch. Make the bump with the first change after a release, in the same pull request, and give it a `-SNAPSHOT`
suffix (e.g. `1.2.1-SNAPSHOT`): the suffix marks the version as in development, and both `tagRelease` and
`checkPublishedDependencies` refuse snapshot versions, so an unfinished module cannot be tagged or end up in a
dependent's published POM. Raise the bump later in the cycle if a further change requires it. The suffix is
removed by `prepareRelease` at release time and is never edited by hand; after a release the version rests at
the released value until the next change.

## Releasing

Each module is released individually, in two steps, because `master` is protected and pull requests are
rebase-merged (the release commit gets a new hash when it lands on `master`, so the tag can only be created
after the merge).

1. On a branch, run the module's `prepareRelease` task, e.g.:

   ```shell
   ./gradlew :jbr-toolchain:prepareRelease
   ```

   The task runs `checkReleaseVersions`, drops the `-SNAPSHOT` suffix from the version in the module's
   `gradle.properties`, renames the `Unreleased` section of the module's changelog to the release version and
   commits both files. Open a pull request with the commit and merge it. Release commits for several modules
   can share one pull request. If the module's files are already in their release state, the task says so and
   this step needs no pull request.

2. On `master`, updated to the merge, run the module's `tagRelease` task:

   ```shell
   ./gradlew :jbr-toolchain:tagRelease
   ```

   The task verifies the release state and creates the `<module>-<version>` tag, then prints the `git push`
   command that pushes it. Pushing the tag triggers the [publish workflow](.github/workflows/publish.yml),
   which builds and checks the module and publishes it to the Gradle Plugin Portal with credentials from the
   repository secrets `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET`.

Publishing is guarded by the module's `checkPublishedDependencies` task: because a project dependency lands in
the published POM with whatever version the dependency project has at build time, publishing fails if a
dependency is at a `-SNAPSHOT` version or at a release version that is not yet available on the Plugin Portal.
The check runs before `tagRelease` as well, so an unpublishable state surfaces before the tag exists rather
than in the publish workflow after the tag has already been pushed. When releasing several modules at once,
release them in dependency order — a dependency's release must be published (not just tagged) before its
dependents pass the check.

After a release, the version stays at the released value; bump it together with your next change to the module.
