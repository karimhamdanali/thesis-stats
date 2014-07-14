package ca.uwaterloo.stats

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
  //  final lazy val floatFormat = new DecimalFormat("#,###.#")
  final lazy val intFormat = "%,d"
  //  final lazy val perFormat = "%5s"

  // keys for table of differences
  final val valueKey = "value"
  //  final val perKey = "percentage"

  // Delimiters & special characters
  final val sep = "\t"
  //  final val perChar = "\\%"

  def doubleLines(str: String) = {
    val tokens = str.split(';') // should yield 1 or 2 tokens exactly
    if (tokens.size == 1) s"\\rot{\\textbf{$str}}" // one line
    else if (tokens.size == 2) s"\\rot{\\textbf{${tokens(0)}}} \\rot{\\textbf{${tokens(1)}}}" // two lines
    else throw new RuntimeException("more than 2 lines is not allowed.")
  }

  def main(args: Array[String]) = {
    val data = new PrintStream("tex/data.tex")

    val edgesFull = Map[String, String](a2a -> "application call graph edges",
      a2l -> "library call graph edges",
      l2a -> "library call back edges")

    // Emit latex files
    emitSoundnessTables
    emitPrecisionTables
    //    emitLibraryCallBackFrequencies
    emitLibraryCallBackSummaries("sparkave")
    emitLibraryCallBackSummaries("doopave")

    // Close streams
    data.close

    def emitLibraryCallBackSummaries(analysis: String) = {
      val table = new PrintStream(s"tex/table-freqs-$analysis.tex")
      val log = io.Source.fromFile(s"stats/summaries/$analysis.num").getLines.toList.drop(1)
      val tool = if (analysis == "sparkave") "\\spark" else "\\doop"

      // Emit Header
      table.println("\\begin{table}[!t]")
      table.println("  \\centering")
      table.println("  \\caption{Frequencies of extra library call back edges in \\ave-based " + tool + " compared to " + tool + ". \\italicize{Other} methods include all methods that are encountered only in one benchmark}")
      table.println("  \\label{table:freqs:" + analysis + "}")
      table.println("  \\begin{tabular}{lrrrrrrrrrrrrrr>{\\bfseries}r}")
      table.println("    \\toprule")
      table.println("    & " + benchmarks.map(b => s"\\rot{\\$b}").mkString(" & ") + "& \\rot{Total} \\\\")
      table.println("    \\midrule")

      for (l <- log) {
        var row = new StringBuilder("    ")
        val line = l.split("\t")
        val name = line.head

        // append method names, special treatment for "Other" and "Total"
        if (name == "Other") row append s"\\italicize{$name}"
        else if (line.head == "Total") row append s"\\boldify{$name}"
        else row append s"\\code{$name}"

        // append the values
        for((t, b) <- line.tail zip (benchmarks :+ "Total")) {
          emit(t.toInt, b)
        }
        
        row append " \\\\"
        if (l != log.last) row append " \\midrule" // Midrule except for the last row
        table.println(row)

        def emit(v: Int, benchmark: String) = {
          val key = s"$analysis $name $benchmark $callbackfreq"
          val value = intFormat format v
          
          // ignore 0 frequencies, clutters the table
          if (Set("Total", "Other")(name) || v != 0) data.println(s"\\pgfkeyssetvalue{$key}{$value}")

          // add the key to the current results row
          if (name == "Other") row append s" & \\italicize{\\pgfkeysvalueof{$key}}"
          else if (name == "Total") row append s" & \\boldify{\\pgfkeysvalueof{$key}}"
          else row append s" & \\pgfkeysvalueof{$key}"
        }
      }

      // Emit Footer
      table.println("    \\bottomrule")
      table.println("  \\end{tabular}")
      table.println("\\end{table}")
      table.close
    }

    def emitLibraryCallBackFrequencies = {
      import scala.collection.mutable.Map
      import scala.collection.immutable.{ Map => ImmutableMap }

      val sparkave = Map[String, ImmutableMap[String, Int]]().withDefaultValue(ImmutableMap[String, Int]()) //.withDefaultValue(0))
      val doopave = Map[String, ImmutableMap[String, Int]]().withDefaultValue(ImmutableMap[String, Int]()) //.withDefaultValue(0))

      val base = "callgraphs/comparisons-call-graphs"

      for (benchmark <- benchmarks) {
        val benchFull = benchmarkFull(benchmark)

        readFrequencies("SparkAverroes - Spark", sparkave)
        readFrequencies("DoopAverroes - Doop", doopave)

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

      sparkave.foreach { m => println(m._1 + "\t" + benchmarks.map(m._2.getOrElse(_, 0)).mkString("\t")) }
      println("\n\n mama 7elwa \n\n")
      doopave.foreach { m => println(m._1 + "\t" + benchmarks.map(m._2.getOrElse(_, 0)).mkString("\t")) }
    }

    def emitSoundnessTables = {
      //      emitSoundnessTable(a2a)
      //      emitSoundnessTable(a2l)
      //      emitSoundnessTable(l2a)
      emitSoundnessTableTotal
    }

    def emitSoundnessTable(tpe: String) = {
      val table = new PrintStream(s"tex/table-$soundness-$tpe.tex")
      val base = "stats"
      val edFull = edgesFull(tpe)

      // Emit Header
      table.println("\\begin{table}[!t]")
      table.println("  \\centering")
      table.println("  \\caption{Comparing the soundness of \\spark and \\doop when analyzing the whole program vs using \\ave with respect to " + edFull + "}")
      table.println("  \\label{table:soundness:" + tpe + "}")
      table.println("  \\begin{tabularx}{\\textwidth}{lRRRRR}")
      table.println("    \\toprule")
      table.println("    & & \\multicolumn{2}{c}{\\mathify{\\dynamic \\setdiff \\spark}} & \\multicolumn{2}{c}{\\mathify{\\dynamic \\setdiff \\doop}} \\\\")
      table.println("    \\cmidrule(l){3-4} \\cmidrule(l){5-6}")
      table.println("    & \\dynamic & \\whole & \\ave  & \\whole  & \\ave  \\\\")
      table.println("    \\cmidrule(l){2-2} \\cmidrule(l){3-4} \\cmidrule(l){5-6}")

      for (benchmark <- benchmarks) {
        var row = new StringBuilder("    ")
        val benchFull = benchmarkFull(benchmark)

        // add benchmark name in italics
        row append s"\\$benchmark"

        // Read the edges info
        emit(dynamic, edges_dyn)
        emit(s"$spark $whole", edges_spark)
        emit(s"$spark $averroes", edges_sparkave)
        emit(s"$doop $whole", edges_doop)
        emit(s"$doop $averroes", edges_doopave)
        row append " \\\\"
        if (benchmark != benchmarks.last) row append " \\midrule" // Midrule except for the last row
        table.println(row)

        def emit(analysis: String, v: Int) = {
          val key = s"$analysis $benchmark $soundness $tpe"
          val value = intFormat format v
          data.println(s"\\pgfkeyssetvalue{$key}{$value}")
          row append s" & \\pgfkeysvalueof{$key}" // add the key to the current results row
        }

        def extractEdges(log: List[String], key: String = s"unsound $edFull") = log.find(_ contains key).get.split("=").last.trim.toInt
        lazy val edges_dyn = extractEdges(io.Source.fromFile(s"$base/dynamic/$benchFull/dyn.stats").getLines.toList, edFull)
        lazy val edges_spark = extractEdges(io.Source.fromFile(s"$base/spark/$benchFull/spark.stats").getLines.toList)
        lazy val edges_sparkave = extractEdges(io.Source.fromFile(s"$base/spark-averroes/$benchFull/sparkave.stats").getLines.toList)
        lazy val edges_doop = extractEdges(io.Source.fromFile(s"$base/doop/$benchFull/doop.stats").getLines.toList)
        lazy val edges_doopave = extractEdges(io.Source.fromFile(s"$base/doop-averroes/$benchFull/doopave.stats").getLines.toList)
      }

      // Emit Footer
      table.println("    \\bottomrule")
      table.println("  \\end{tabularx}")
      table.println("\\end{table}")
      table.close
    }

    def emitSoundnessTableTotal = {
      val table = new PrintStream(s"tex/table-$soundness.tex")
      val base = "stats"

      // Emit Header
      table.println("\\begin{table}[!t]")
      table.println("  \\centering")
      table.println("  \\caption{Comparing the soundness of \\spark and \\doop when analyzing the whole program to using \\ave with respect to the dynamic call graphs}")
      table.println("  \\label{table:soundness}")
      table.println("  \\begin{tabularx}{\\textwidth}{lRRRRR}")
      table.println("    \\toprule")
      table.println("    & & \\multicolumn{2}{c}{\\mathify{\\dynamic \\setdiff \\spark}} & \\multicolumn{2}{c}{\\mathify{\\dynamic \\setdiff \\doop}} \\\\")
      table.println("    \\cmidrule(l){3-4} \\cmidrule(l){5-6}")
      table.println("    & \\dynamic & \\whole & \\ave  & \\whole  & \\ave  \\\\")
      table.println("    \\cmidrule(l){2-2} \\cmidrule(l){3-4} \\cmidrule(l){5-6}")

      for (benchmark <- benchmarks) {
        var row = new StringBuilder("    ")
        val benchFull = benchmarkFull(benchmark)

        // add benchmark name in italics
        row append s"\\$benchmark"

        // Read the edges info
        emit(dynamic, edges_dyn(a2a) + edges_dyn(a2l) + edges_dyn(l2a))
        emit(s"$spark $whole", edges_spark(a2a) + edges_spark(a2l) + edges_spark(l2a))
        emit(s"$spark $averroes", edges_sparkave(a2a) + edges_sparkave(a2l) + edges_sparkave(l2a))
        emit(s"$doop $whole", edges_doop(a2a) + edges_doop(a2l) + edges_doop(l2a))
        emit(s"$doop $averroes", edges_doopave(a2a) + edges_doopave(a2l) + edges_doopave(l2a))
        row append " \\\\"
        if (benchmark != benchmarks.last) row append " \\midrule" // Midrule except for the last row
        table.println(row)

        def emit(analysis: String, v: Int) = {
          val key = s"$analysis $benchmark $soundness"
          val value = intFormat format v
          data.println(s"\\pgfkeyssetvalue{$key}{$value}")
          row append s" & \\pgfkeysvalueof{$key}" // add the key to the current results row
        }

        def extractEdges(log: List[String], tpe: String) = log.find(_ contains s"unsound ${edgesFull(tpe)}").get.split("=").last.trim.toInt
        def edges_dyn(tpe: String) = io.Source.fromFile(s"$base/dynamic/$benchFull/dyn.stats").getLines.toList.find(_ contains edgesFull(tpe)).get.split("=").last.trim.toInt
        def edges_spark(tpe: String) = extractEdges(io.Source.fromFile(s"$base/spark/$benchFull/spark.stats").getLines.toList, tpe)
        def edges_sparkave(tpe: String) = extractEdges(io.Source.fromFile(s"$base/spark-averroes/$benchFull/sparkave.stats").getLines.toList, tpe)
        def edges_doop(tpe: String) = extractEdges(io.Source.fromFile(s"$base/doop/$benchFull/doop.stats").getLines.toList, tpe)
        def edges_doopave(tpe: String) = extractEdges(io.Source.fromFile(s"$base/doop-averroes/$benchFull/doopave.stats").getLines.toList, tpe)
      }

      // Emit Footer
      table.println("    \\bottomrule")
      table.println("  \\end{tabularx}")
      table.println("\\end{table}")
      table.close
    }

    def emitPrecisionTables = {
      emitPrecisionTable(a2a)
      emitPrecisionTable(a2l)
      emitPrecisionTable(l2a)
      //      emitPrecisionTableTotal
    }

    def emitPrecisionTable(tpe: String) = {
      val table = new PrintStream(s"tex/table-$imprecision-$tpe.tex")
      val base = "stats"
      val edFull = edgesFull(tpe)

      // Emit Header
      table.println("\\begin{table}[!t]")
      table.println("  \\centering")
      table.println("  \\caption{Comparing the precision of using \\ave to analyzing the whole program in both \\spark and \\doop with respect to " + edFull + "}")
      table.println("  \\label{table:precision:" + tpe + "}")
      table.println("  \\begin{tabularx}{\\textwidth}{lRRRR}")
      table.println("    \\toprule")
      table.println("    & \\spark & \\small \\mathify{\\ave \\setdiff \\spark} & \\doop & \\small \\mathify{\\ave \\setdiff \\doop} \\\\")
      table.println("    \\cmidrule(l){2-3} \\cmidrule(l){4-5}")

      for (benchmark <- benchmarks) {
        var row = new StringBuilder("    ")
        val benchFull = benchmarkFull(benchmark)

        // add benchmark name in italics
        row append s"\\$benchmark"

        // Read the edges info
        emit(s"$spark $whole", edges_spark)
        emit(s"$spark $averroes", edges_sparkave)
        emit(s"$doop $whole", edges_doop)
        emit(s"$doop $averroes", edges_doopave)
        row append " \\\\"
        if (benchmark != benchmarks.last) row append " \\midrule" // Midrule except for the last row
        table.println(row)

        def emit(analysis: String, v: Int) = {
          val key = s"$analysis $benchmark $soundness $tpe"
          val value = intFormat format v
          data.println(s"\\pgfkeyssetvalue{$key}{$value}")
          row append s" & \\pgfkeysvalueof{$key}" // add the key to the current results row
        }

        def extractEdges(log: List[String], key: String = s"imprecise $edFull") = log.find(_ contains key).get.split("=").last.trim.toInt
        lazy val edges_spark = extractEdges(io.Source.fromFile(s"$base/spark/$benchFull/spark.stats").getLines.toList, edFull)
        lazy val edges_sparkave = extractEdges(io.Source.fromFile(s"$base/spark-averroes/$benchFull/sparkave.stats").getLines.toList)
        lazy val edges_doop = extractEdges(io.Source.fromFile(s"$base/doop/$benchFull/doop.stats").getLines.toList, edFull)
        lazy val edges_doopave = extractEdges(io.Source.fromFile(s"$base/doop-averroes/$benchFull/doopave.stats").getLines.toList)
      }

      // Emit Footer
      table.println("    \\bottomrule")
      table.println("  \\end{tabularx}")
      table.println("\\end{table}")
      table.close
    }

    def benchmarkFull(benchmark: String) = (if (dacapo contains benchmark) "dacapo/" else "specjvm/") + benchmark
  }
}