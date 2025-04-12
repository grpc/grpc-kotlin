package io.grpc.examples.bzlmod

/** A simple implementation of a Fibonacci service. */
class Fibonacci : FibonacciServiceGrpcKt.FibonacciServiceCoroutineImplBase() {
  override suspend fun query(request: QueryRequest): QueryResponse {
    if (request.nth <= 1) {
      return queryResponse { nthFibonacci = request.nth }
    }
    var prv = 0L
    var cur = 1L
    var nxt = 1L
    for (i in 2..request.nth) {
      prv = cur
      cur = nxt
      nxt = (prv + cur) % request.mod
    }
    return queryResponse { nthFibonacci = cur }
  }
}
