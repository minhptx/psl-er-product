package experiments

import better.files._
import File._
import io.{PaperReader, ProductReader}

object PSLInit {

  def main(args: Array[String]): Unit = {
    run("learn")
    run("eval")
  }

  def run(dataSetName: String): Unit = {
    val paperReader = new PaperReader()
    paperReader.readDataSetFromFolder("data" / "paper" / dataSetName)
    paperReader.writePSLFactsToFolder("../psl" / dataSetName)
  }
}
