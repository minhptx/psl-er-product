package data

import info.debatty.java.stringsimilarity.{Cosine, JaroWinkler}

class Paper(val id: String, val title: String,
            val author: String, val venue: String, val year: Int = 0) {

  def simTitle(paper: Paper): Double = {
    val cosine = new Cosine()

    cosine.similarity(paper.title, title)
  }

  def simAuthor(paper: Paper): Double = {
    val jaroWinkler = new JaroWinkler()

    jaroWinkler.similarity(paper.author, author)
  }

  def simVenue(paper: Paper): Double = {
    val jaroWinkler = new JaroWinkler()

    jaroWinkler.similarity(paper.venue, venue)
  }

  def simYear(paper: Paper): Double = {
    if (year == 0) 0
    else if (year == paper.year) 1
    else 0
  }
}

object Paper {

  val simTitle: (Paper, Paper) => Double = _.simTitle(_)
  val simAuthor: (Paper, Paper) => Double = _.simAuthor(_)
  val simVenue: (Paper, Paper) => Double = _.simVenue(_)
  val simYear: (Paper, Paper) => Double = _.simYear(_)


  val functions = Map("SIMTITLE" -> simTitle, "SIMAUTHOR" -> simAuthor, "SIMVENUE" -> simVenue, "SIMYEAR" -> simYear)

  def createPaperFromStrings(strings: String*): Paper = {

    var year = 0

    try {
      year = strings(4).toInt
    }
    catch {
      case _: Throwable =>
    }

    new Paper(strings.head.slice(0, 200), strings(1), strings(2), strings(3), year)
  }
}
