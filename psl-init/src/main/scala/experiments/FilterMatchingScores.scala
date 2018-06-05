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
    val lines = CSVReader.open((".." / "groovy" / "inferred-predicates" / "SameAs.txt").pathAsString).all()

    ("data" / "output" / "filter_predicate.txt").createIfNotExists().appendLines(lines.drop(1).map {
      strings =>
        ((strings.head, strings(1)), strings(2))
    }.toMap.filterKeys(mapping.contains).map {
      case (key, value) => s"${key._1},${key._2},$value"
    }.toSeq: _*)
  }
}
