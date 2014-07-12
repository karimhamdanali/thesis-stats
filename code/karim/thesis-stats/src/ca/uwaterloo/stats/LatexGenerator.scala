package ca.uwaterloo.scalacg.experiments

import java.io.FileInputStream
import java.io.PrintStream
import java.text.DecimalFormat
import java.util.zip.GZIPInputStream

import scala.collection.JavaConversions.asScalaSet

import ca.uwaterloo.util.Math

object LatexGenerator {

  // The benchmarks
  final val dacapo = List("antlr", "bloat", "chart", "hsqldb", "luindex", "lusearch", "pmd", "xalan")
  final val specjvm = List("compress", "db", "jack", "javac", "jess", "raytrace")
  final val benchmarks = dacapo ++ specjvm

  // The various types of call graphs
  final val dynamic = "dynamic"
  final val spark = "spark"
  final val doop = "doop"
  final val whole = "whole"
  final val averroes = "averroes"

  // The various types of edges
  final val a2a = "a2a"
  final val a2l = "a2l"
  final val l2a = "l2a"
  final val l2l = "l2l"
  final val total = "total"

  // Various things we measure
  final val soundness = "soundness"
  final val imprecision = "imprecision"
  final val cgsize = "call graph size"
  final val callbackfreq = "call back frequencies"

  // Formatting
  final lazy val floatFormat = new DecimalFormat("#,###.#")
  final lazy val intFormat = "%,d"
  final lazy val perFormat = "%5s"

  // keys for table of differences
  final val valueKey = "value"
  final val perKey = "percentage"

  // Delimiters & special characters
  final val sep = "\t"
  final val perChar = "\\%"
  final val setDiff = "\\"

  def doubleLines(str: String) = {
    val tokens = str.split(';') // should yield 1 or 2 tokens exactly
    if (tokens.size == 1) s"\\rot{\\textbf{$str}}" // one line
    else if (tokens.size == 2) s"\\rot{\\textbf{${tokens(0)}}} \\rot{\\textbf{${tokens(1)}}}" // two lines
    else throw new RuntimeException("more than 2 lines is not allowed.")
  }

  def main(args: Array[String]): Unit = {
    val data = new PrintStream("tex/data.tex")

    val soundnessOut = Map[String, PrintStream](a2a -> new PrintStream(s"csv/$soundness/$a2a.csv"),
      a2l -> new PrintStream(s"csv/$soundness/$a2l.csv"),
      l2a -> new PrintStream(s"csv/$soundness/$l2a.csv"))

    val imprecisionOut = Map[String, PrintStream](a2a -> new PrintStream(s"csv/$imprecision/$a2a.csv"),
      a2l -> new PrintStream(s"csv/$imprecision/$a2l.csv"),
      l2a -> new PrintStream(s"csv/$imprecision/$l2a.csv"))

    val cgsizeOut = new PrintStream(s"csv/$cgsize.csv")

    // Emit latex files
    emitSoundnessTables

    // Close streams
    data.close
    soundnessOut.values foreach (_.close)
    imprecisionOut.values foreach (_.close)
    cgsizeOut.close

    def emitSoundnessTables = {
      
    }
  }
}