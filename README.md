# almond-scalapy

[ScalaPy](https://scalapy.dev) integration for the [almond](https://github.com/almond-sh/almond)
Scala Jupyter kernel: lets Python objects handled via ScalaPy be displayed in notebooks with
their rich representations (HTML, images, …).

Used to live in the almond repository, up to almond 0.15.0.

## Usage

The almond kernel loads `almond-scalapy` automatically when ScalaPy is added as a library. It can
also be added manually:
```scala
import $ivy.`sh.almond::almond-scalapy:0.15.0`
```

Then call
```scala
almond.scalapy.initDisplay
```
to have the kernel display ScalaPy values with their Python rich representations.

## Building

```text
$ ./mill __.compile
$ ./mill __.mimaReportBinaryIssues
$ ./mill __.publishLocal
```

`almond-scalapy` is built once per binary Scala version (2.12, 2.13, 3), with the oldest Scala
version the almond kernels support for it, so that it can be used from all of them.

## Examples

The notebooks under `examples/notebooks/scala-<binary Scala version>` are run by
```text
$ ./mill examples.test
```
with released almond kernels (see `almondVersion` in `examples/package.mill`), which load the
almond-scalapy of this repository. Their outputs are compared with the ones they were committed
with. Run
```text
$ ALMOND_SCALAPY_UPDATE_EXAMPLES=1 ./mill examples.test
```
to write the new outputs to the notebooks instead.

Jupyter runs from the environment described by `examples/pyproject.toml`, with exact versions
pinned in `examples/uv.lock`. It is managed with [uv](https://docs.astral.sh/uv/), that the build
downloads, so that nothing needs to be installed beforehand. To update the pinned Python
versions, run `uv lock --upgrade` from `examples`.

## Releasing

Pushing a `v*` tag uploads a release to Maven Central, to be published from the Central Portal. Every push to `main` publishes a
snapshot, whose version is derived from the latest `v*` tag (or from 0.15.0, the last version
published from the almond repository, if there's none).
