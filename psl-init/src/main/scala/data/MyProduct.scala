package data

import info.debatty.java.stringsimilarity.{Cosine, Jaccard}

class MyProduct(val id: String, val name: String,
                val description: String, val manufacturer: String, val price: Double) {


  def simName(product: MyProduct): Double = {
    val jaccard = new Jaccard(3)

    jaccard.distance(name, product.name)
  }

  def simDescription(product: MyProduct): Double = {
    val cosine = new Cosine()

    cosine.distance(description, product.description)
  }

  def simPrice(product: MyProduct): Double = {
    (price - product.price) / Math.max(price, product.price)
  }
}

object MyProduct{

  val simDescription: (MyProduct, MyProduct) => Double = _.simDescription(_)
  val simName: (MyProduct, MyProduct) => Double = _.simName(_)
  val simPrice: (MyProduct, MyProduct) => Double = _.simPrice(_)

  val functions = Map("SimDescription" -> simDescription, "SimName" -> simName, "SimPrice" -> simPrice)

  def createProductFromStrings(strings: String*): MyProduct ={
    if(strings.size == 4){
      try {
        new MyProduct(strings.head, strings(1), strings(2), "", strings(3).toInt)
      }
      catch {
        case _: Throwable =>
          new MyProduct(strings.head, strings(1), strings(2), "", 0)
      }
    }
    else{
      try {
        new MyProduct(strings.head, strings(1), strings(2), strings(3), strings(4).toInt)
      }
      catch {
        case _: Throwable =>
          new MyProduct(strings.head, strings(1), strings(2), strings(3), 0)
      }
    }
  }
}
