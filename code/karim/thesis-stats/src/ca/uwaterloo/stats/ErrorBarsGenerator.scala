package ca.uwaterloo.stats

import java.io.PrintStream

object ErrorBarsGenerator {
  // The benchmarks
  final val dacapo = List("antlr", "bloat", "chart", "hsqldb", "luindex", "lusearch", "pmd", "xalan")
  final val specjvm = List("compress", "db", "jack", "javac", "jess", "raytrace")
  final val benchmarks = dacapo ++ specjvm

  def main(args: Array[String]) = {
    emitSparkTime
  }
  
  def emitSparkTime = {
    for{
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val total = io.Source.fromFile(s"all-output/$iteration/spark-call-graphs/$benchmark/$prog-spark.stats").getLines.toList.find(_ startsWith "Total time to finish").get.split(":").last.trim.toFloat
      if(prog != benchmarks.last) print(total + "\t")
      else print(total + "\n")
    }
  }
  
  def benchmarkFull(benchmark: String) = (if (dacapo contains benchmark) "dacapo/" else "specjvm/") + benchmark
}