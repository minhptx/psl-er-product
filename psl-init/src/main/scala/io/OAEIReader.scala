package io

import better.files._
import org.apache.jena.rdf.model.{ModelFactory, Statement}

import scala.collection.mutable

class OAEIReader(val fileName: File) {

  private var maxID = 0

  private val rdfModel = ModelFactory.createDefaultModel()

  private val uriToID = mutable.Map[String, Int]()

  private val attributeToIdPairList: Map[String, mutable.MutableList[(Int, Int)]] =
    Map().withDefaultValue(mutable.MutableList[(Int, Int)]())


  private val attributeToIdValueList: Map[String, mutable.MutableList[(Int, String)]] =
    Map().withDefaultValue(mutable.MutableList[(Int, String)]())

  def createMapInfoFromStatements(): Unit = {
    val iterator = rdfModel.listStatements()

    while (iterator.hasNext) {
      val statement = iterator.nextStatement()

      val sub = statement.getSubject
      val predicate = statement.getPredicate
      val obj = statement.getObject

      if (!uriToID.contains(sub.getURI)) {
        uriToID(sub.getURI) = maxID
        maxID += 1
      }

      val subID = uriToID(sub.getURI)

      if (obj.isResource) {

        if (!uriToID.contains(obj.asResource().getURI)) {
          uriToID(obj.asResource().getURI) = maxID
          maxID += 1
        }

        val objID = uriToID(obj.asResource().getURI)

        attributeToIdPairList(predicate.getLocalName) += ((subID, objID))
      }
      else if (obj.isLiteral) {
        attributeToIdValueList(predicate.getLocalName) += ((subID, obj.asLiteral().getString))
      }

    }
  }


  def readAllStatements(): mutable.MutableList[Statement] = {
    if (rdfModel.isEmpty)
      mutable.MutableList()
    else {
      val iterator = rdfModel.listStatements()

      val statements = mutable.MutableList[Statement]()

      while (iterator.hasNext) {
        val statement = iterator.nextStatement()
        statements += statement
      }
      statements
    }

  }

}
