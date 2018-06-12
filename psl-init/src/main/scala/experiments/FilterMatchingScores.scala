package experiments

import better.files.File._
import better.files._
import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import io.ProductReader


object FilterMatchingScores {

  implicit object TsvFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
    override val quoteChar='\''
  }

  def main(args: Array[String]): Unit = {
    val productReader = new ProductReader()
    val mapping = productReader.readMappingFromCSV("data" / "eval" / "mapping.csv")
    val sourceIdToProduct = productReader.readProductsWithIdsFromCSV("data" / "eval" / "source.csv")
    val targetIdToProduct = productReader.readProductsWithIdsFromCSV("data" / "eval" / "target.csv")
    val lines = CSVReader.open((".." / "groovy" / "inferred-predicates" / "SameAs.txt").pathAsString).all()

    ("data" / "output" / "filter_predicate.txt").createIfNotExists().clear().appendLines(lines.drop(1).map {
      strings =>
        ((strings.head, strings(1)), strings(2))
    }.toMap.filterKeys(mapping.contains).map {
      case ((sourceKey, targetKey), value) =>
        val sourceProduct = sourceIdToProduct(sourceKey)
        println(value)
        val targetProduct = targetIdToProduct(targetKey)
        s"$sourceKey,$targetKey,$value,${sourceProduct.simName(targetProduct)},${sourceProduct.simDescription(targetProduct)},${sourceProduct.simPrice(targetProduct)}"
    }.toSeq: _*)


  }
}
