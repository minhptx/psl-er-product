package experiments

import better.files._
import File._
import io.ProductReader

object PSLRun {

  def main(args: Array[String]): Unit = {
    run("Amz-Ggl")
    run("Abt-Buy")
  }

  def run(dataSetName: String): Unit = {
    val productReader = new ProductReader()
    productReader.readDataSetFromFolder("data" / dataSetName)
    productReader.writePSLFactsToFolder("psl" / dataSetName)
  }
}
