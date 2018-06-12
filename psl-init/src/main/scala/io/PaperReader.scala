package io

import better.files.File
import com.github.tototoshi.csv.CSVReader
import com.typesafe.scalalogging.LazyLogging
import data.{MyProduct, Paper}

import scala.collection.mutable


class PaperReader extends LazyLogging {

  var idToId: mutable.Map[(String, String), Int] = mutable.Map()

  var sourcePapers: List[Paper] = List[Paper]()
  var targetPapers: List[Paper] = List[Paper]()


  def readDataSetFromFolder(folder: File): Unit = {
    for (file <- folder.list) {
      file.name match {
        case "mapping.csv" =>
          idToId = readMappingFromCSV(file)
        case "source.csv" =>
          sourcePapers = readPapersFromCSV(file)
        case "target.csv" =>
          targetPapers = readPapersFromCSV(file)
      }
    }
  }

  def writePSLFactsToFolder(folder: File): Unit = {
    folder.createIfNotExists(asDirectory = true).clear()

    //    folder.createIfNotExists(asDirectory = true)

    sourcePapers = sourcePapers.take(300)
    targetPapers = targetPapers.take(300)

//    var count = 0

    //        while (count < 299) {
    //          if (!idToId.contains((sourceProducts(count + 1).id, targetProducts(count).id))) {
    //            idToId((sourceProducts(count + 1).id, targetProducts(count).id)) = 0
    //          }
    //          count += 1
    //        }
    var newIdToId: mutable.Map[(String, String), Int] = mutable.Map()


    for (sourcePaper <- sourcePapers) {
      for (targetPaper <- targetPapers) {
        if (!idToId.contains((sourcePaper.id, targetPaper.id))) {
          newIdToId((sourcePaper.id, targetPaper.id)) = 0
        }
        else{
          newIdToId((sourcePaper.id, targetPaper.id)) = 1
        }
      }
    }

    Paper.functions.foreach {
      case (simName, simFunction) =>
        val idToSim = mutable.Map[(String, String), Double]()
        for (sourcePaper <- sourcePapers) {
          for (targetPaper <- targetPapers) {
            //            logger.whenDebugEnabled{println(simFunction(sourceProduct, targetProduct))}
            if (newIdToId.contains((sourcePaper.id, targetPaper.id))) {
              idToSim((sourcePaper.id, targetPaper.id)) = simFunction(sourcePaper, targetPaper)
            }
          }
        }

        (folder / s"${simName}_obs.txt").createIfNotExists().appendLines(idToSim.map {
          case ((sourceID, targetID), sim) => s"$sourceID\t$targetID\t$sim"
        }.toSeq: _*)
    }

    //    val trainTestTuple = idToId.splitAt(Math.ceil(0.7 * idToId.size).toInt)
    //    val trainData = trainTestTuple._1
    //    val testData = trainTestTuple._2

    //    val sameObsFile = folder / "SameAs_obs.txt"
    //    sameObsFile.createIfNotExists().appendLines(trainData.map {
    //      case ((sourceID, targetID), value) => s"$sourceID  $targetID  $value"
    //    }.toSeq: _*)

    val sameTargetFile = folder / "SAMEAS_targets.txt"
    sameTargetFile.createIfNotExists().appendLines(newIdToId.map {
      case ((sourceID, targetID), _) => s"$sourceID\t$targetID"
    }.toSeq: _*)

    val sameTruthFile = folder / "SAMEAS_truth.txt"
    sameTruthFile.createIfNotExists().appendLines(newIdToId.map {
      case ((sourceID, targetID), value) => s"$sourceID\t$targetID\t$value"
    }.toSeq: _*)

    (folder / "SAMEAS_obs.txt").createIfNotExists()
  }


  def readMappingFromCSV(file: File): mutable.Map[(String, String), Int] = {
    val reader = CSVReader.open(file.pathAsString)

    mutable.Map(reader.all().drop(1).map(
      strings => ((strings.head.slice(0, 200), strings(1).slice(0, 200)), 1)
    ).toMap.toSeq: _*)
  }

  def readPapersFromCSV(file: File): List[Paper] = {

    val reader = CSVReader.open(file.pathAsString)

    reader.all().drop(1).map(
      strings => {
        println(strings)
        Paper.createPaperFromStrings(strings: _*)
      }
    )
  }

  def readPapersWithIdsFromCSV(file: File): Map[String, Paper] = {

    val reader = CSVReader.open(file.pathAsString)

    reader.all().drop(1).map(
      strings => {
        val product = Paper.createPaperFromStrings(strings: _*)
        (product.id, product)
      }
    ).toMap
  }
}
