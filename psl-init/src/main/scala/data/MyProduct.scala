package data

import info.debatty.java.stringsimilarity.{Cosine, Jaccard}

class MyProduct(val id: String, val name: String,
                val description: String, val manufacturer: String, val price: Double = -1.0) {


  def simName(product: MyProduct): Double = {
    val jaccard = new Jaccard(3)

    1- jaccard.distance(name, product.name)
  }

  def simDescription(product: MyProduct): Double = {
    val cosine = new Cosine()

    1 - cosine.distance(description, product.description)
  }

  def simPrice(product: MyProduct): Double = {
    val result = (price - product.price) / Math.max(price, product.price)
    if (result.isInfinite || result.isNaN) 0
    else if (result > 1) 0
    else if (result < 0) 1
    else 1 - result
  }
}

object MyProduct {

  val simDescription: (MyProduct, MyProduct) => Double = _.simDescription(_)
  val simName: (MyProduct, MyProduct) => Double = _.simName(_)
  val simPrice: (MyProduct, MyProduct) => Double = _.simPrice(_)

  val functions = Map("SIMDESCRIPTION" -> simDescription, "SIMNAME" -> simName, "SIMPRICE" -> simPrice)

  def createProductFromStrings(strings: String*): MyProduct = {
    if (strings.size == 4) {
      try {
        new MyProduct(strings.head, strings(1), strings(2), "", strings(3).toInt)
      }
      catch {
        case _: Throwable =>
          new MyProduct(strings.head, strings(1), strings(2), "", -1)
      }
    }
    else {
      try {
        new MyProduct(strings.head, strings(1), strings(2), strings(3), strings(4).toInt)
      }
      catch {
        case _: Throwable =>
          new MyProduct(strings.head, strings(1), strings(2), strings(3), -1)
      }
    }
  }
}
