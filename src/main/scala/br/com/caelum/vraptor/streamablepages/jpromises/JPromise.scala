package br.com.caelum.vraptor.streamablepages.jpromises

import Context._
import scala.concurrent.{ future, promise }
import scala.concurrent.Promise

object JPromise {

  def apply[T]():JPromise[T] = new JPromise(promise[T])
  
}

class JPromise[T](val scalaPromise:Promise[T]) {
  
  def success(value:T) = scalaPromise.success(value)
  
  def onSuccess(runnable:Runnable) = {
    val future = scalaPromise.future
    future.onSuccess {
      case response => runnable.run()      
    }
    future.onFailure {
      case error:Exception => println(s"Something went wrong $error . Please change me and use a Logger")
    }
  }
}