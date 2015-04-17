package ca.uwaterloo.stats

import java.io.PrintStream

import scala.collection.immutable.{Map => ImmutableMap}
import scala.collection.mutable.Map

object LibraryFrequencyGenerator {
  // The benchmarks
  final val dacapo = List("antlr", "bloat", "chart", "hsqldb", "luindex", "lusearch", "pmd", "xalan")
  final val specjvm = List("compress", "db", "jack", "javac", "jess", "raytrace")
  final val benchmarks = dacapo ++ specjvm

  def benchmarkFull(benchmark: String) = (if (dacapo contains benchmark) "dacapo/" else "specjvm/") + benchmark

  def main(args: Array[String]) = {
    val sparkave = Map[String, ImmutableMap[String, Int]]().withDefaultValue(ImmutableMap[String, Int]()) //.withDefaultValue(0))
    val doopave = Map[String, ImmutableMap[String, Int]]().withDefaultValue(ImmutableMap[String, Int]()) //.withDefaultValue(0))
    val walaave = Map[String, ImmutableMap[String, Int]]().withDefaultValue(ImmutableMap[String, Int]()) //.withDefaultValue(0))

    val nums = List[PrintStream](new PrintStream("sparkave.stats"),
      new PrintStream("doopave.stats"),
      new PrintStream("walaave.stats"))

    val base = "callgraphs/comparisons-call-graphs"

    for (benchmark <- benchmarks) {
      val benchFull = benchmarkFull(benchmark)

      readFrequencies("SparkAverroes - Spark", sparkave)
      readFrequencies("DoopAverroes - Doop", doopave)
      readFrequencies("WalaAverroes - Wala", walaave)

      // Read in the frequencies
      def readFrequencies(key: String, freqs: Map[String, ImmutableMap[String, Int]]) = {
        val lines = io.Source.fromFile(s"$base/$benchFull/$benchmark-comparisons.stats").getLines.toList.dropWhile(_ != key).drop(2).takeWhile(_.trim.nonEmpty)
        for (line <- lines) {
          val tokens = line.split("=")
          if (tokens.size == 2) {
            freqs(tokens(0).trim) += (benchmark -> tokens(1).trim.toInt)
          }
        }
      }
    }

    sparkave.foreach { m => nums(0).println(m._1 + "\t" + benchmarks.map(m._2.getOrElse(_, 0)).mkString("\t")) }
    doopave.foreach { m => nums(1).println(m._1 + "\t" + benchmarks.map(m._2.getOrElse(_, 0)).mkString("\t")) }
    walaave.foreach { m => nums(2).println(m._1 + "\t" + benchmarks.map(m._2.getOrElse(_, 0)).mkString("\t")) }

    nums.foreach(_.close)
  }
}