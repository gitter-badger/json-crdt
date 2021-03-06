package eu.timepit.crjdt

import eu.timepit.crjdt.Cmd._
import eu.timepit.crjdt.Expr.Var
import eu.timepit.crjdt.Operation.Mutation

final case class LocalState(replicaId: ReplicaId,
                            opsCounter: BigInt,
                            variables: Map[Var, Cursor],
                            processedOps: Set[Id],
                            generatedOps: Vector[Operation]) {

  def addVar(x: Var, cur: Cursor): LocalState =
    copy(variables = variables.updated(x, cur))

  def applyCmd(cmd: Cmd): LocalState =
    cmd match {
      case Let(x, expr) => // LET
        addVar(x, applyExpr(expr))

      case Assign(expr, v) => // MAKE-ASSIGN
        makeOp(applyExpr(expr), Mutation.Assign(v))

      case Insert(expr, v) => // MAKE-INSERT
        makeOp(applyExpr(expr), Mutation.Insert(v))

      case Delete(expr) => // MAKE-DELETE
        makeOp(applyExpr(expr), Mutation.Delete)

      case Yield =>
        ???

      case Sequence(cmd1, cmd2) => // EXEC
        applyCmd(cmd1).applyCmd(cmd2)
    }

  def applyExpr(expr: Expr): Cursor =
    expr match {
      case _ => ???
    }

  // APPLY-LOCAL
  def applyLocal(op: Operation): LocalState = {
    // TODO: evaluate op to produce a modified local state
    copy(processedOps = processedOps + op.id,
         generatedOps = generatedOps :+ op)
  }

  def currentId: Id =
    Id(opsCounter, replicaId)

  // VAR
  def getVar(x: Var): Option[Cursor] =
    variables.get(x)

  def increaseCounterTo(c: BigInt): LocalState =
    if (opsCounter < c) copy(opsCounter = c) else this

  def incrementCounter: LocalState =
    copy(opsCounter = opsCounter + 1)

  // MAKE-OP
  def makeOp(cur: Cursor, mut: Mutation): LocalState = {
    val newState = incrementCounter
    val op = Operation(newState.currentId, newState.processedOps, cur, mut)
    newState.applyLocal(op)
  }
}

object LocalState {
  def empty(replicaId: ReplicaId): LocalState =
    LocalState(replicaId, 0, Map.empty, Set.empty, Vector.empty)
}
