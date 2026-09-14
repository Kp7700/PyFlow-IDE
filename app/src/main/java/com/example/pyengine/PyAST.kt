package com.example.pyengine

sealed interface ASTNode {
    val line: Int
}

sealed interface Stmt : ASTNode

data class FunctionDef(
    val name: String,
    val params: List<String>,
    val defaults: Map<String, Expr>,
    val vararg: String? = null,
    val kwarg: String? = null,
    val body: List<Stmt>,
    override val line: Int
) : Stmt

data class ClassDef(
    val name: String,
    val bases: List<String>,
    val body: List<Stmt>,
    override val line: Int
) : Stmt

data class ReturnStmt(
    val value: Expr?,
    override val line: Int
) : Stmt

data class IfStmt(
    val branches: List<Pair<Expr, List<Stmt>>>,
    val elseBranch: List<Stmt>? = null,
    override val line: Int
) : Stmt

data class WhileStmt(
    val condition: Expr,
    val body: List<Stmt>,
    val elseBody: List<Stmt>? = null,
    override val line: Int
) : Stmt

data class ForStmt(
    val target: Expr,
    val iter: Expr,
    val body: List<Stmt>,
    val elseBody: List<Stmt>? = null,
    override val line: Int
) : Stmt

data class ExceptHandler(
    val type: String?,
    val name: String?,
    val body: List<Stmt>,
    val line: Int
)

data class TryStmt(
    val body: List<Stmt>,
    val handlers: List<ExceptHandler>,
    val elseBody: List<Stmt>? = null,
    val finallyBody: List<Stmt>? = null,
    override val line: Int
) : Stmt

data class RaiseStmt(
    val exc: Expr?,
    override val line: Int
) : Stmt

data class WithItem(
    val contextExpr: Expr,
    val optionalVars: Expr?
)

data class WithStmt(
    val items: List<WithItem>,
    val body: List<Stmt>,
    override val line: Int
) : Stmt

data class AssignStmt(
    val targets: List<Expr>,
    val value: Expr,
    override val line: Int
) : Stmt

data class AugAssignStmt(
    val target: Expr,
    val op: String,
    val value: Expr,
    override val line: Int
) : Stmt

data class ExprStmt(
    val expr: Expr,
    override val line: Int
) : Stmt

data class BreakStmt(override val line: Int) : Stmt
data class ContinueStmt(override val line: Int) : Stmt
data class PassStmt(override val line: Int) : Stmt

data class ImportStmt(
    val names: List<Pair<String, String?>>, // module -> alias
    val isFrom: Boolean = false,
    val fromModule: String? = null,
    override val line: Int
) : Stmt

data class GlobalStmt(
    val names: List<String>,
    override val line: Int
) : Stmt

data class NonlocalStmt(
    val names: List<String>,
    override val line: Int
) : Stmt

data class AssertStmt(
    val test: Expr,
    val msg: Expr? = null,
    override val line: Int
) : Stmt

sealed interface Expr : ASTNode

data class BinOp(
    val left: Expr,
    val op: String,
    val right: Expr,
    override val line: Int
) : Expr

data class UnaryOp(
    val op: String,
    val operand: Expr,
    override val line: Int
) : Expr

data class CompareOp(
    val left: Expr,
    val comparisons: List<Pair<String, Expr>>,
    override val line: Int
) : Expr

data class BoolOp(
    val op: String, // "and", "or"
    val values: List<Expr>,
    override val line: Int
) : Expr

data class CallExpr(
    val callee: Expr,
    val args: List<Expr>,
    val kwargs: List<Pair<String, Expr>> = emptyList(),
    override val line: Int
) : Expr

data class AttributeExpr(
    val value: Expr,
    val attr: String,
    override val line: Int
) : Expr

data class SubscriptExpr(
    val value: Expr,
    val slice: SliceExpr,
    override val line: Int
) : Expr

data class SliceExpr(
    val lower: Expr? = null,
    val upper: Expr? = null,
    val step: Expr? = null,
    val isSingleIndex: Boolean = false,
    override val line: Int
) : Expr

data class ListExpr(
    val elements: List<Expr>,
    override val line: Int
) : Expr

data class TupleExpr(
    val elements: List<Expr>,
    override val line: Int
) : Expr

data class DictExpr(
    val entries: List<Pair<Expr, Expr>>,
    override val line: Int
) : Expr

data class SetExpr(
    val elements: List<Expr>,
    override val line: Int
) : Expr

data class ListCompExpr(
    val elt: Expr,
    val target: Expr,
    val iter: Expr,
    val ifs: List<Expr>,
    override val line: Int
) : Expr

data class DictCompExpr(
    val key: Expr,
    val value: Expr,
    val target: Expr,
    val iter: Expr,
    val ifs: List<Expr>,
    override val line: Int
) : Expr

data class SetCompExpr(
    val elt: Expr,
    val target: Expr,
    val iter: Expr,
    val ifs: List<Expr>,
    override val line: Int
) : Expr

data class LambdaExpr(
    val params: List<String>,
    val defaults: Map<String, Expr>,
    val body: Expr,
    override val line: Int
) : Expr

data class TernaryExpr(
    val condition: Expr,
    val trueExpr: Expr,
    val falseExpr: Expr,
    override val line: Int
) : Expr

data class FStringExpr(
    val parts: List<Any>, // String or Expr
    override val line: Int
) : Expr

data class NameExpr(
    val name: String,
    override val line: Int
) : Expr

data class IntLiteralExpr(
    val value: Long,
    override val line: Int
) : Expr

data class FloatLiteralExpr(
    val value: Double,
    override val line: Int
) : Expr

data class StringLiteralExpr(
    val value: String,
    override val line: Int
) : Expr

data class BoolLiteralExpr(
    val value: Boolean,
    override val line: Int
) : Expr

data class NoneLiteralExpr(
    override val line: Int
) : Expr
