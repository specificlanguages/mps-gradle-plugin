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

When you change a module, bump its version accordingly and bump every module that depends on it by at least a patch.
