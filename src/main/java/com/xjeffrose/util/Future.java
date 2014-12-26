package com.xjeffrose.util;

import java.util.logging.*;
import java.util.concurrent.atomic.*;

import com.xjeffrose.log.*;

//TODO: This should be Callable not Runnable
public class Future<T extends Runnable> {
  private static final Logger log = Log.getLogger(Future.class.getName());

  private final T t;

  public AtomicBoolean isReady = new AtomicBoolean(false);
  public AtomicBoolean isError = new AtomicBoolean(false);

  public Future(T t) {
    this.t = t;

    Thread futureWorker = new Thread(t);
    futureWorker.start();
    isReady.set(true);
  }

  /* (timeout: Duration) : A */
  /* Block, but only as long as the given Timeout if a timeout is given, then return timeout error. */
  public void apply() {}

  /* (p: (A) ⇒ Boolean) : Future[A] */
  /* Converts this to a Throw if the predicate does not obtain. */
  public void filter() {}

  /* [R2, AlsoThis[R2] >: This[R2] <: This[R2]] (f: (A) ⇒ AlsoThis[R2]) : AlsoThis[R2] */
  /* Returns the given function applied to the value from this Return or returns this if this is a Throw. */
  public void flatmap() {}

  /* Returns the Results from a list of futures in */
  /* as a list in a new future */
  public T collect() {
    return t;
  }

  /* (k: (A) ⇒ Unit) : Unit */
  /* Invoke the callback only if the Future returns sucessfully. */
  public void foreach() {}

  /* (timeout: Duration) : Try[A] */
  /* Demands that the result of the future be available within timeout. */
  /* Block, but only as long as the given Timeout if a timeout is given, then return timeout error. */
  public T get() {
    return t;
  }

  /* [B] (other: Future[B]) : Future[(A, B)] */
  /* Combines two Futures into one Future of the Tuple of the two results. */
  public void join() {}

  /* [X] (f: (A) ⇒ X) : Future[X] */
  /* Maps the given function to the value from this Return or returns this if this is a Throw */
  public void map() {}

  /* (rescueException: (Throwable) ⇒ Unit) : Future[A] */
  /* Invoke the funciton on the error, if the computation was unsuccessful. */
  public void onFailure() {}

  /* (f: (A) ⇒ Unit) : Future[A] */
  /* Invoke the function on the result, if the computation was successful. */
  public void onSuccess() {}

  /* [B >: A] (other: Promise[B]) : Unit */
  /* Send updates from this Future to the other. */
  public void proxyTo() {}

}
