package almond.scalapy.examples

import scala.concurrent.duration.DurationInt

/** Runs the notebooks of `examples/notebooks`, and checks their outputs didn't change.
  *
  * Those of `notebooks/<kernel id>` are run with that kernel. Set `ALMOND_SCALAPY_UPDATE_EXAMPLES`
  * to `1` to write the new outputs to the notebooks instead of failing.
  */
class Examples extends munit.FunSuite {

  override def munitTimeout = 5.minutes

  private def pathProp(name: String): os.Path = {
    val key = s"almond.scalapy.examples.$name"
    os.Path(sys.props.getOrElse(key, sys.error(s"Expected $key to be set")))
  }

  private lazy val notebooksDir = pathProp("notebooks")
  private lazy val uvProject    = pathProp("uv-project")
  private lazy val uv           = pathProp("uv")
  private lazy val jupyterPath  = pathProp("jupyter-path")
  private lazy val outputDir    = pathProp("output-dir")

  private def update =
    sys.env.get("ALMOND_SCALAPY_UPDATE_EXAMPLES").exists(v => v == "1" || v == "true")

  private lazy val notebooks =
    for {
      kernelDir <- os.list(notebooksDir).filter(os.isDir(_))
      notebook  <- os.list(kernelDir).filter(_.last.endsWith(".ipynb")).filter(os.isFile(_))
    } yield (kernelDir.last, notebook)

  for ((kernelId, notebook) <- notebooks)
    test(s"$kernelId/${notebook.last.stripSuffix(".ipynb")}") {

      val output = outputDir / kernelId / notebook.last
      os.makeDir.all(output / os.up)

      // Jupyter is run from the uv-managed environment described by examples/pyproject.toml:
      // uv fetches Python and the Jupyter packages pinned in examples/uv.lock on the fly
      os.proc(
        uv,
        "run",
        "--project",
        uvProject,
        "--frozen",
        "jupyter",
        "nbconvert",
        "--to",
        "notebook",
        "--execute",
        s"--ExecutePreprocessor.kernel_name=$kernelId",
        notebook,
        s"--output=$output"
      ).call(
        cwd = uvProject,
        env = Map(
          "JUPYTER_PATH"          -> jupyterPath.toString,
          "ALMOND_USE_RANDOM_IDS" -> "false"
        ),
        stdin = os.Inherit,
        stdout = os.Inherit,
        stderr = os.Inherit
      )

      // Clear the metadata of the cells, that holds the times they were run at
      val json = ujson.read(os.read(output))
      for (cell <- json("cells").arr if cell("cell_type").str == "code")
        cell("metadata") = ujson.Obj()
      val result = json.render(1)
      os.write.over(output, result)

      val expected = os.read(notebook)
      if (result != expected) {
        System.err.println(s"$kernelId/${notebook.last} differs:")
        os.proc("diff", "-u", notebook, output)
          .call(check = false, stdin = os.Inherit, stdout = os.Inherit)
        if (update) {
          System.err.println(s"Updating $kernelId/${notebook.last}")
          os.copy.over(output, notebook)
        }
        fail("Output notebook differs from original")
      }
    }
}
