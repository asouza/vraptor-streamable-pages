package br.com.caelum.vraptor.streamablepages.jpromises

import scala.concurrent.ExecutionContext

object Context {
  
  implicit val ex = ExecutionContext.Implicits.global

}