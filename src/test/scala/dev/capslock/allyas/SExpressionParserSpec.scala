package dev.capslock.allyas

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class SExpressionParserSpec extends AnyFunSpec with Matchers:
  
  describe("SExpressionParser"):
    val parser = SExpressionParser()
    
    describe("symbol parser"):
      it("should parse a valid symbol"):
        val result = parser.parse(parser.symbol, "git")
        result.successful should be(true)
        result.get should be(SExpr.Symbol("git"))
      
      it("should parse symbols with hyphens and underscores"):
        val result = parser.parse(parser.symbol, "my-command_test")
        result.successful should be(true)
        result.get should be(SExpr.Symbol("my-command_test"))
    
    describe("stringLiteral parser"):
      it("should parse a quoted string"):
        val result = parser.parse(parser.stringLiteral, "\"git status\"")
        result.successful should be(true)
        result.get should be(SExpr.StringLiteral("git status"))
      
      it("should parse an empty string"):
        val result = parser.parse(parser.stringLiteral, "\"\"")
        result.successful should be(true)
        result.get should be(SExpr.StringLiteral(""))
    
    describe("list parser"):
      it("should parse an empty list"):
        val result = parser.parse(parser.list, "()")
        result.successful should be(true)
        result.get should be(SExpr.List(List.empty))
      
      it("should parse a list with symbols"):
        val result = parser.parse(parser.list, "(alias git)")
        result.successful should be(true)
        result.get should be(SExpr.List(List(
          SExpr.Symbol("alias"),
          SExpr.Symbol("git")
        )))
      
      it("should parse a list with mixed types"):
        val result = parser.parse(parser.list, "(alias \"git\" \"git status\")")
        result.successful should be(true)
        result.get should be(SExpr.List(List(
          SExpr.Symbol("alias"),
          SExpr.StringLiteral("git"),
          SExpr.StringLiteral("git status")
        )))
    
    describe("config parser"):
      it("should parse a configuration with multiple aliases"):
        val input = """(config
                      |  (alias "git" "git status")
                      |  (alias "ls" "ls -la")
                      |  (alias "test" "echo test"))""".stripMargin
        val result = parser.parse(parser.config, input)
        result.successful should be(true)
        result.get should be(Map(
          "git" -> "git status",
          "ls" -> "ls -la",
          "test" -> "echo test"
        ))
      
      it("should parse an empty configuration"):
        val input = "(config)"
        val result = parser.parse(parser.config, input)
        result.successful should be(true)
        result.get should be(Map.empty[String, String])
      
      it("should ignore invalid alias entries"):
        val input = """(config
                      |  (alias "git" "git status")
                      |  (invalid entry)
                      |  (alias "ls" "ls -la"))""".stripMargin
        val result = parser.parse(parser.config, input)
        result.successful should be(true)
        result.get should be(Map(
          "git" -> "git status",
          "ls" -> "ls -la"
        ))