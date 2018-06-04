package io

import better.files.File
import com.github.tototoshi.csv.CSVReader
import com.typesafe.scalalogging.LazyLogging
import data.MyProduct

import scala.collection.mutable


class ProductReader extends LazyLogging {

  var idToId: mutable.Map[(String, String), Int] = mutable.Map()

  var sourceProducts: List[MyProduct] = List[MyProduct]()
  var targetProducts: List[MyProduct] = List[MyProduct]()


  def readDataSetFromFolder(folder: File): Unit = {
    for (file <- folder.list) {
      file.name match {
        case "mapping.csv" =>
          idToId = readMappingFromCSV(file)
        case "source.csv" =>
          sourceProducts = readProductsFromCSV(file)
        case "target.csv" =>
          targetProducts = readProductsFromCSV(file)
      }
    }
  }

  def writePSLFactsToFolder(folder: File): Unit = {
    folder.createIfNotExists(asDirectory = true)

    for (sourceProduct <- sourceProducts) {
      for (targetProduct <- targetProducts) {
        if(!idToId.contains((sourceProduct.id, targetProduct.id))){
          idToId((sourceProduct.id, targetProduct.id)) = 0
        }
      }
    }

    MyProduct.functions.foreach {
      case (simName, simFunction) =>
        val idToSim = mutable.Map[(String, String), Double]()
        for (sourceProduct <- sourceProducts) {
          for (targetProduct <- targetProducts) {
            //            logger.whenDebugEnabled{println(simFunction(sourceProduct, targetProduct))}
            idToSim((sourceProduct.id, targetProduct.id)) = simFunction(sourceProduct, targetProduct)
          }
        }

        (folder / s"${simName}_obs.txt").createIfNotExists().appendLines(idToSim.map {
          case ((sourceProdID, targetProdID), sim) => s"$sourceProdID	$targetProdID	$sim"
        }.toSeq: _*)
    }

//    val trainTestTuple = idToId.splitAt(Math.ceil(0.7 * idToId.size).toInt)
//    val trainData = trainTestTuple._1
//    val testData = trainTestTuple._2

//    val sameObsFile = folder / "SameAs_obs.txt"
//    sameObsFile.createIfNotExists().appendLines(trainData.map {
//      case ((sourceID, targetID), value) => s"$sourceID  $targetID  $value"
//    }.toSeq: _*)

    val sameTargetFile = folder / "SameAs_targets.txt"
    sameTargetFile.createIfNotExists().appendLines(idToId.map{
      case ((sourceID, targetID), _) => s"$sourceID  $targetID"
    }.toSeq: _*)

    val sameTruthFile = folder / "SameAs_truth.txt"
    sameTruthFile.createIfNotExists().appendLines(idToId.map {
      case ((sourceID, targetID), value) => s"$sourceID  $targetID  $value"
    }.toSeq: _*)

  }


  def readMappingFromCSV(file: File): mutable.Map[(String, String), Int] = {
    val reader = CSVReader.open(file.pathAsString)

    mutable.Map(reader.all().drop(1).map(
      strings => ((strings.head, strings(1)), 1)
    ).toMap.toSeq: _*)
  }

  def readProductsFromCSV(file: File): List[MyProduct] = {

    val reader = CSVReader.open(file.pathAsString)

    reader.all().drop(1).map(
      strings => {
        println(strings)
        MyProduct.createProductFromStrings(strings: _*)
      }
    )
  }
}
