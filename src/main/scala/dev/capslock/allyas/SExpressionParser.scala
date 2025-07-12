package dev.capslock.allyas

import scala.util.parsing.combinator.*

object SExpressionParser {
  def apply(): SExpressionParser = new SExpressionParser()
}

enum SExpr:
  case Symbol(name: String)
  case StringLiteral(value: String)
  case List(elements: scala.List[SExpr])

class SExpressionParser extends RegexParsers {
  import SExpr.*

  def symbol: Parser[Symbol] = """[a-zA-Z][a-zA-Z0-9_-]*""".r ^^ Symbol.apply

  def stringLiteral: Parser[StringLiteral] = 
    "\"" ~> """[^"]*""".r <~ "\"" ^^ StringLiteral.apply

  def list: Parser[List] = 
    "(" ~> rep(expr) <~ ")" ^^ List.apply

  def expr: Parser[SExpr] = symbol | stringLiteral | list

  def config: Parser[Map[String, String]] = 
    list ^^ { 
      case List(Symbol("config") :: aliases) =>
        aliases.collect {
          case List(scala.List(Symbol("alias"), StringLiteral(name), StringLiteral(command))) =>
            name -> command
        }.toMap
      case _ => Map.empty
    }
}