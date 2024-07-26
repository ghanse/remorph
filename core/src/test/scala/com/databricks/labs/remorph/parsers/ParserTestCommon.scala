package com.databricks.labs.remorph.parsers

import com.databricks.labs.remorph.parsers.{intermediate => ir}
import com.databricks.labs.remorph.utils.Strings
import org.antlr.v4.runtime._
import org.antlr.v4.runtime.tree.ParseTreeVisitor
import org.scalatest.{Assertion, Assertions}

trait ParserTestCommon[P <: Parser] { self: Assertions =>

  protected def makeLexer(chars: CharStream): TokenSource
  protected def makeParser(tokens: TokenStream): P
  protected def makeErrHandler(chars: String): ErrorCollector = new DefaultErrorCollector
  protected def astBuilder: ParseTreeVisitor[_]
  protected var errHandler: ErrorCollector = _

  protected def parseString[R <: RuleContext](input: String, rule: P => R): R = {
    val inputString = CharStreams.fromString(input)
    val lexer = makeLexer(inputString)
    val tokenStream = new CommonTokenStream(lexer)
    val parser = makeParser(tokenStream)
    errHandler = makeErrHandler(input)
    parser.removeErrorListeners()
    parser.addErrorListener(errHandler)
    val tree = rule(parser)

    // uncomment the following line if you need a peek in the Snowflake/TSQL AST
    // println(tree.toStringTree(parser))
    tree
  }

  protected def comparePlans(a: ir.LogicalPlan, b: ir.LogicalPlan): Unit = {
    val expected = reorderComparisons(a)
    val actual = reorderComparisons(b)
    if (expected != actual) {
      fail(s"""
           |== FAIL: Plans do not match ===
           |${Strings.sideBySide(expected.treeString, actual.treeString).mkString("\n")}
         """.stripMargin)
    }
  }

  protected def reorderComparisons(plan: ir.LogicalPlan): ir.LogicalPlan = {
    plan transformAllExpressions {
      case ir.Equals(l, r) if l.hashCode() > r.hashCode() => ir.Equals(r, l)
      case ir.GreaterThan(l, r) if l.hashCode() > r.hashCode() => ir.LessThan(r, l)
      case ir.GreaterThanOrEqual(l, r) if l.hashCode() > r.hashCode() => ir.LessThanOrEqual(r, l)
      case ir.LessThan(l, r) if l.hashCode() > r.hashCode() => ir.GreaterThan(r, l)
      case ir.LessThanOrEqual(l, r) if l.hashCode() > r.hashCode() => ir.GreaterThanOrEqual(r, l)
    }
  }

  protected def example[R <: RuleContext](query: String, rule: P => R, expectedAst: ir.LogicalPlan) = {
    val sfTree = parseString(query, rule)
    if (errHandler != null && errHandler.errorCount != 0) {
      errHandler.logErrors()
      fail(s"${errHandler.errorCount} errors found in the child string")
    }

    val result = astBuilder.visit(sfTree)
    comparePlans(expectedAst, result.asInstanceOf[ir.LogicalPlan])
  }

  protected def exampleExpr[R <: RuleContext](query: String, rule: P => R, expectedAst: ir.Expression) = {
    val sfTree = parseString(query, rule)
    if (errHandler != null && errHandler.errorCount != 0) {
      errHandler.logErrors()
      fail(s"${errHandler.errorCount} errors found in the child string")
    }
    val result = astBuilder.visit(sfTree)
    val wrapExpr = (expr: ir.Expression) => ir.Filter(ir.NoopNode, expr)
    comparePlans(wrapExpr(expectedAst), wrapExpr(result.asInstanceOf[ir.Expression]))
  }

  /**
   * Used to pass intentionally bad syntax to the parser and check that it fails with an expected error
   * @param query
   * @param rule
   * @tparam R
   * @return
   */
  protected def checkError[R <: RuleContext](query: String, rule: P => R, errContains: String): Assertion = {
    parseString(query, rule)
    if (errHandler != null && errHandler.errorCount == 0) {
      fail(s"Expected an error in the child string\n$query\nbut no errors were found")
    }

    val errors = errHandler.formatErrors
    assert(
      errors.exists(_.contains(errContains)),
      s"Expected error containing '$errContains' but got:\n${errors.mkString("\n")}")
  }

}
