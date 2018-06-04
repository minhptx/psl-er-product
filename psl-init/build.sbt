name := "psl-init"

version := "0.1"

scalaVersion := "2.12.6"

resolvers += "PSL Releases" at "http://maven.linqs.org/maven/repositories/psl-releases/"

libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.5.0"

libraryDependencies += "org.apache.jena" % "apache-jena-libs" % "3.7.0"

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.5"

libraryDependencies += "info.debatty" % "java-string-similarity" % "1.1.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"