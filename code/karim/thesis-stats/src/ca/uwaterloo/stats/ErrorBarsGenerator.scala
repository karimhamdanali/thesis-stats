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

    emitDoopTime(isAve = false)
    emitDoopTime(isAve = true)
    //    emitDoopMemory(isAve = false)
    //    emitDoopMemory(isAve = true)
  }

  def emitDoopTime(isAve: Boolean) = {
    emitDoopAnalysisTime(isAve)
    emitDoopOverheadTime(isAve)
  }

  def emitDoopAnalysisTime(isAve: Boolean) = {
    val title = if (isAve) "DoopAve Analysis Time" else "Doop Analysis Time"

    println(title)
    println("=" * title.length)

    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val cg = if (isAve) "doop-averroes-call-graphs" else "doop-call-graphs"
      val stats = if (isAve) s"$prog-doopAverroes.stats" else s"$prog-doop.stats"
      val log = io.Source.fromFile(s"all-output/$iteration/callgraphs/$cg/$benchmark/$stats").getLines.toList

      val analysis = doopExtractNumber(_ startsWith "MBBENCH logicblox START", log)

      if (prog != benchmarks.last) print(analysis + "\t")
      else print(analysis + "\n")
    }

    println
    println
  }

  def emitDoopOverheadTime(isAve: Boolean) = {
    val title = if (isAve) "DoopAve Overhead Time" else "Doop Overhead Time"

    println(title)
    println("=" * title.length)

    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val cg = if (isAve) "doop-averroes-call-graphs" else "doop-call-graphs"
      val stats = if (isAve) s"$prog-doopAverroes.stats" else s"$prog-doop.stats"
      val log = io.Source.fromFile(s"all-output/$iteration/callgraphs/$cg/$benchmark/$stats").getLines.toList

      var total = 0d
      
      total += doopExtractNumber(_ startsWith "Adding archive for resolving", log)
      total += doopExtractNumber(_ startsWith "creating database in", log)
      total += doopExtractNumber(_ startsWith "loading fact declarations ...", log)
      total += doopExtractNumber(_ startsWith "loading facts ...", log)
      total += doopExtractNumber(_ startsWith "loading context-insensitive declarations...", log)
      total += doopExtractNumber(_ startsWith "loading context-insensitive delta rules...", log)
      total += doopExtractNumber(_ startsWith "loading reflection delta rules...", log)
      total += doopExtractNumber(_ startsWith "loading client delta rules...", log)
      
      val overhead = floatFormat format total

      if (prog != benchmarks.last) print(overhead + "\t")
      else print(overhead + "\n")
    }

    println
    println
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
      val log = io.Source.fromFile(s"all-output/$iteration/callgraphs/$cg/$benchmark/$stats").getLines.toList

      val analysis = log.find(_ startsWith "[Spark] Solution found in").get.split(" ").dropRight(1).last.trim.toFloat
      val total = log.find(_ startsWith "Total time to finish").get.split(":").last.trim.toFloat
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
      var memory = 0d

      // Get the max memory used
      for (line <- log) {
        val pattern = "->(\\d+)K".r
        val value = pattern.findFirstMatchIn(line).get.group(1)
        val next = roundAt2(value.toDouble / 1024)
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
  def roundAt(p: Int)(n: Double): Double = { val s = math pow (10, p); (math round n * s) / s }
  def roundAt2(n: Double) = roundAt(2)(n)
  def extractNumber(line: String) = "\\d+(.\\d+)?".r.findFirstMatchIn(line).get.matched
  def getLineAfter(p: String => Boolean, log: List[String]) = { val index = log.indexWhere(p); log(index + 1) }
  def doopExtractNumber(p: String => Boolean, log: List[String]) = { val line = getLineAfter(p, log); roundAt2(extractNumber(line).toDouble) }

}