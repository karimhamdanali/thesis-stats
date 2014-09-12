package ca.uwaterloo.stats

import java.io.PrintStream
import java.text.DecimalFormat

object ErrorBarsGenerator {
  // The benchmarks
  final val dacapo = List("antlr", "bloat", "chart", "hsqldb", "luindex", "lusearch", "pmd", "xalan")
  final val specjvm = List("compress", "db", "jack", "javac", "jess", "raytrace")
  final val benchmarks = dacapo ++ specjvm

  def main(args: Array[String]) = {
    emitSparkTime(isAve = false)
    emitSparkTime(isAve = true)

    emitSparkMemory(isAve = false)
    emitSparkMemory(isAve = true)
  }

  def emitSparkTime(isAve: Boolean) = {
    emitSparkAnalysisTime(isAve)
    emitSparkOverheadTime(isAve)
  }

  def emitSparkAnalysisTime(isAve: Boolean) = {
    val title = if (isAve) "SparkAve Analysis Time" else "Spark Analysis Time"
    println(title)
    println("=" * title.length)
    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val cg = if (isAve) "spark-averroes-call-graphs" else "spark-call-graphs"
      val stats = if (isAve) s"$prog-sparkAverroes.stats" else s"$prog-spark.stats"
      val analysis = io.Source.fromFile(s"all-output/$iteration/callgraphs/$cg/$benchmark/$stats").getLines.toList.find(_ startsWith "[Spark] Solution found in").get.split(" ").dropRight(1).last.trim.toFloat
      if (prog != benchmarks.last) print(analysis + "\t")
      else print(analysis + "\n")
    }
    println
    println
  }

  def emitSparkOverheadTime(isAve: Boolean) = {
    val title = if (isAve) "SparkAve Overhead Time" else "Spark Overhead Time"
    println(title)
    println("=" * title.length)
    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val cg = if (isAve) "spark-averroes-call-graphs" else "spark-call-graphs"
      val stats = if (isAve) s"$prog-sparkAverroes.stats" else s"$prog-spark.stats"
      val analysis = io.Source.fromFile(s"all-output/$iteration/callgraphs/$cg/$benchmark/$stats").getLines.toList.find(_ startsWith "[Spark] Solution found in").get.split(" ").dropRight(1).last.trim.toFloat
      val total = io.Source.fromFile(s"all-output/$iteration/callgraphs/$cg/$benchmark/$stats").getLines.toList.find(_ startsWith "Total time to finish").get.split(":").last.trim.toFloat
      val overhead = floatFormat format (total - analysis)
      if (prog != benchmarks.last) print(overhead + "\t")
      else print(overhead + "\n")
    }
    println
    println
  }

  def emitSparkMemory(isAve: Boolean) = {
    val title = if (isAve) "SparkAve Memory" else "Spark Memory"
    println(title)
    println("=" * title.length)
    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val cg = if (isAve) "spark-averroes-call-graphs" else "spark-call-graphs"
      val log = io.Source.fromFile(s"all-output/$iteration/callgraphs/$cg/$benchmark/$prog-gc.stats").getLines.toList.filter(_ contains "Full GC")
      var memory = 0

      // Get the max memory used
      for (line <- log) {
        val pattern = "(\\d+)K".r
        val pattern(v1, v2, v3) = line
        val next = v2.toInt
        if (next > memory) memory = next
      }
      
      if (prog != benchmarks.last) print(memory + "\t")
      else print(memory + "\n")
    }
    println
    println
  }

  def benchmarkFull(benchmark: String) = (if (dacapo contains benchmark) "dacapo/" else "specjvm/") + benchmark
  final lazy val floatFormat = new DecimalFormat("#,###.##")
}