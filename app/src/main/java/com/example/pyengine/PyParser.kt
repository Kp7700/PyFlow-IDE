package com.example.pyengine

class PyParseException(message: String, val line: Int, val column: Int) : Exception("SyntaxError at line $line, col $column: $message")

class PyParser(private val tokens: List<PyToken>) {
    private var index = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        skipNewlines()
        while (!isAtEnd()) {
            statements.add(parseStatement())
            skipNewlines()
        }
        return statements
    }

    private fun parseStatement(): Stmt {
        skipNewlines()
        val token = peek()
        return when (token.type) {
            TokenType.KW_DEF -> parseFunctionDef()
            TokenType.KW_CLASS -> parseClassDef()
            TokenType.KW_IF -> parseIfStmt()
            TokenType.KW_WHILE -> parseWhileStmt()
            TokenType.KW_FOR -> parseForStmt()
            TokenType.KW_TRY -> parseTryStmt()
            TokenType.KW_WITH -> parseWithStmt()
            TokenType.KW_RAISE -> parseRaiseStmt()
            TokenType.KW_RETURN -> parseReturnStmt()
            TokenType.KW_BREAK -> { advance(); expectNewline(); BreakStmt(token.line) }
            TokenType.KW_CONTINUE -> { advance(); expectNewline(); ContinueStmt(token.line) }
            TokenType.KW_PASS -> { advance(); expectNewline(); PassStmt(token.line) }
            TokenType.KW_IMPORT, TokenType.KW_FROM -> parseImportStmt()
            TokenType.KW_GLOBAL -> parseGlobalStmt()
            TokenType.KW_NONLOCAL -> parseNonlocalStmt()
            TokenType.KW_ASSERT -> parseAssertStmt()
            else -> parseSimpleStatement()
        }
    }

    private fun parseFunctionDef(): Stmt {
        val defToken = consume(TokenType.KW_DEF, "Expected 'def'")
        val nameToken = consume(TokenType.IDENTIFIER, "Expected function name")
        consume(TokenType.LPAREN, "Expected '(' after function name")

        val params = mutableListOf<String>()
        val defaults = mutableMapOf<String, Expr>()
        var vararg: String? = null
        var kwarg: String? = null

        if (!check(TokenType.RPAREN)) {
            while (true) {
                if (match(TokenType.STAR)) {
                    val varargToken = consume(TokenType.IDENTIFIER, "Expected identifier after '*'")
                    vararg = varargToken.value
                } else if (match(TokenType.STAR_STAR)) {
                    val kwargToken = consume(TokenType.IDENTIFIER, "Expected identifier after '**'")
                    kwarg = kwargToken.value
                } else {
                    val paramToken = consume(TokenType.IDENTIFIER, "Expected parameter name")
                    params.add(paramToken.value)
                    // Type annotations e.g. x: int
                    if (match(TokenType.COLON)) {
                        parseExpression() // skip type annotation
                    }
                    if (match(TokenType.EQUAL)) {
                        val defaultVal = parseExpression()
                        defaults[paramToken.value] = defaultVal
                    }
                }

                if (!match(TokenType.COMMA)) break
                if (check(TokenType.RPAREN)) break
            }
        }
        consume(TokenType.RPAREN, "Expected ')' after parameters")

        // Return type annotation e.g. -> int
        if (match(TokenType.ARROW)) {
            parseExpression()
        }

        consume(TokenType.COLON, "Expected ':' after function header")
        val body = parseBlock()
        return FunctionDef(nameToken.value, params, defaults, vararg, kwarg, body, defToken.line)
    }

    private fun parseClassDef(): Stmt {
        val classToken = consume(TokenType.KW_CLASS, "Expected 'class'")
        val nameToken = consume(TokenType.IDENTIFIER, "Expected class name")
        val bases = mutableListOf<String>()

        if (match(TokenType.LPAREN)) {
            if (!check(TokenType.RPAREN)) {
                do {
                    val baseToken = consume(TokenType.IDENTIFIER, "Expected base class name")
                    bases.add(baseToken.value)
                } while (match(TokenType.COMMA) && !check(TokenType.RPAREN))
            }
            consume(TokenType.RPAREN, "Expected ')' after base classes")
        }

        consume(TokenType.COLON, "Expected ':' after class header")
        val body = parseBlock()
        return ClassDef(nameToken.value, bases, body, classToken.line)
    }

    private fun parseIfStmt(): Stmt {
        val ifToken = consume(TokenType.KW_IF, "Expected 'if'")
        val branches = mutableListOf<Pair<Expr, List<Stmt>>>()

        val ifCond = parseExpression()
        consume(TokenType.COLON, "Expected ':' after if condition")
        val ifBody = parseBlock()
        branches.add(Pair(ifCond, ifBody))

        skipNewlines()
        while (match(TokenType.KW_ELIF)) {
            val elifCond = parseExpression()
            consume(TokenType.COLON, "Expected ':' after elif condition")
            val elifBody = parseBlock()
            branches.add(Pair(elifCond, elifBody))
            skipNewlines()
        }

        var elseBody: List<Stmt>? = null
        if (match(TokenType.KW_ELSE)) {
            consume(TokenType.COLON, "Expected ':' after else")
            elseBody = parseBlock()
        }

        return IfStmt(branches, elseBody, ifToken.line)
    }

    private fun parseWhileStmt(): Stmt {
        val whileToken = consume(TokenType.KW_WHILE, "Expected 'while'")
        val condition = parseExpression()
        consume(TokenType.COLON, "Expected ':' after while condition")
        val body = parseBlock()

        skipNewlines()
        var elseBody: List<Stmt>? = null
        if (match(TokenType.KW_ELSE)) {
            consume(TokenType.COLON, "Expected ':' after else")
            elseBody = parseBlock()
        }

        return WhileStmt(condition, body, elseBody, whileToken.line)
    }

    private fun parseForStmt(): Stmt {
        val forToken = consume(TokenType.KW_FOR, "Expected 'for'")
        val target = parseForTarget()
        consume(TokenType.KW_IN, "Expected 'in' in for loop")
        val iter = parseExpression()
        consume(TokenType.COLON, "Expected ':' after for loop header")
        val body = parseBlock()

        skipNewlines()
        var elseBody: List<Stmt>? = null
        if (match(TokenType.KW_ELSE)) {
            consume(TokenType.COLON, "Expected ':' after else")
            elseBody = parseBlock()
        }

        return ForStmt(target, iter, body, elseBody, forToken.line)
    }

    private fun parseForTarget(): Expr {
        val first = parsePrimary()
        if (match(TokenType.COMMA)) {
            val elements = mutableListOf(first)
            do {
                if (check(TokenType.KW_IN)) break
                elements.add(parsePrimary())
            } while (match(TokenType.COMMA))
            return TupleExpr(elements, first.line)
        }
        return first
    }

    private fun parseTryStmt(): Stmt {
        val tryToken = consume(TokenType.KW_TRY, "Expected 'try'")
        consume(TokenType.COLON, "Expected ':' after try")
        val body = parseBlock()

        val handlers = mutableListOf<ExceptHandler>()
        skipNewlines()

        while (check(TokenType.KW_EXCEPT)) {
            val exceptToken = advance()
            var excType: String? = null
            var excName: String? = null

            if (!check(TokenType.COLON)) {
                val typeToken = consume(TokenType.IDENTIFIER, "Expected exception type")
                excType = typeToken.value
                if (match(TokenType.KW_AS)) {
                    val nameToken = consume(TokenType.IDENTIFIER, "Expected identifier after 'as'")
                    excName = nameToken.value
                }
            }

            consume(TokenType.COLON, "Expected ':' after except")
            val handlerBody = parseBlock()
            handlers.add(ExceptHandler(excType, excName, handlerBody, exceptToken.line))
            skipNewlines()
        }

        var elseBody: List<Stmt>? = null
        if (match(TokenType.KW_ELSE)) {
            consume(TokenType.COLON, "Expected ':' after else")
            elseBody = parseBlock()
            skipNewlines()
        }

        var finallyBody: List<Stmt>? = null
        if (match(TokenType.KW_FINALLY)) {
            consume(TokenType.COLON, "Expected ':' after finally")
            finallyBody = parseBlock()
        }

        return TryStmt(body, handlers, elseBody, finallyBody, tryToken.line)
    }

    private fun parseWithStmt(): Stmt {
        val withToken = consume(TokenType.KW_WITH, "Expected 'with'")
        val items = mutableListOf<WithItem>()

        while (true) {
            val contextExpr = parseExpression()
            var optionalVars: Expr? = null
            if (match(TokenType.KW_AS)) {
                optionalVars = parseExpression()
            }
            items.add(WithItem(contextExpr, optionalVars))
            if (!match(TokenType.COMMA)) break
        }

        consume(TokenType.COLON, "Expected ':' after with statement")
        val body = parseBlock()
        return WithStmt(items, body, withToken.line)
    }

    private fun parseRaiseStmt(): Stmt {
        val raiseToken = consume(TokenType.KW_RAISE, "Expected 'raise'")
        val exc = if (!check(TokenType.NEWLINE) && !check(TokenType.EOF)) parseExpression() else null
        expectNewline()
        return RaiseStmt(exc, raiseToken.line)
    }

    private fun parseReturnStmt(): Stmt {
        val retToken = consume(TokenType.KW_RETURN, "Expected 'return'")
        val value = if (!check(TokenType.NEWLINE) && !check(TokenType.EOF) && !check(TokenType.DEDENT)) {
            parseExpressionList()
        } else null
        expectNewline()
        return ReturnStmt(value, retToken.line)
    }

    private fun parseImportStmt(): Stmt {
        val line = peek().line
        if (match(TokenType.KW_FROM)) {
            val moduleToken = consume(TokenType.IDENTIFIER, "Expected module name")
            consume(TokenType.KW_IMPORT, "Expected 'import' after from module")
            val names = mutableListOf<Pair<String, String?>>()
            do {
                val name = consume(TokenType.IDENTIFIER, "Expected imported name").value
                var alias: String? = null
                if (match(TokenType.KW_AS)) {
                    alias = consume(TokenType.IDENTIFIER, "Expected alias after 'as'").value
                }
                names.add(Pair(name, alias))
            } while (match(TokenType.COMMA))
            expectNewline()
            return ImportStmt(names, isFrom = true, fromModule = moduleToken.value, line = line)
        } else {
            consume(TokenType.KW_IMPORT, "Expected 'import'")
            val names = mutableListOf<Pair<String, String?>>()
            do {
                val module = consume(TokenType.IDENTIFIER, "Expected module name").value
                var alias: String? = null
                if (match(TokenType.KW_AS)) {
                    alias = consume(TokenType.IDENTIFIER, "Expected alias after 'as'").value
                }
                names.add(Pair(module, alias))
            } while (match(TokenType.COMMA))
            expectNewline()
            return ImportStmt(names, isFrom = false, line = line)
        }
    }

    private fun parseGlobalStmt(): Stmt {
        val line = peek().line
        consume(TokenType.KW_GLOBAL, "Expected 'global'")
        val names = mutableListOf<String>()
        do {
            names.add(consume(TokenType.IDENTIFIER, "Expected identifier in global statement").value)
        } while (match(TokenType.COMMA))
        expectNewline()
        return GlobalStmt(names, line)
    }

    private fun parseNonlocalStmt(): Stmt {
        val line = peek().line
        consume(TokenType.KW_NONLOCAL, "Expected 'nonlocal'")
        val names = mutableListOf<String>()
        do {
            names.add(consume(TokenType.IDENTIFIER, "Expected identifier in nonlocal statement").value)
        } while (match(TokenType.COMMA))
        expectNewline()
        return NonlocalStmt(names, line)
    }

    private fun parseAssertStmt(): Stmt {
        val line = peek().line
        consume(TokenType.KW_ASSERT, "Expected 'assert'")
        val test = parseExpression()
        var msg: Expr? = null
        if (match(TokenType.COMMA)) {
            msg = parseExpression()
        }
        expectNewline()
        return AssertStmt(test, msg, line)
    }

    private fun parseSimpleStatement(): Stmt {
        val expr = parseExpressionList()

        if (match(TokenType.EQUAL)) {
            val targets = mutableListOf(expr)
            var right = parseExpressionList()
            while (match(TokenType.EQUAL)) {
                targets.add(right)
                right = parseExpressionList()
            }
            expectNewline()
            return AssignStmt(targets, right, expr.line)
        }

        // Augmented assignments
        val augOps = mapOf(
            TokenType.PLUS_EQUAL to "+=",
            TokenType.MINUS_EQUAL to "-=",
            TokenType.STAR_EQUAL to "*=",
            TokenType.SLASH_EQUAL to "/=",
            TokenType.DOUBLE_SLASH_EQUAL to "//=",
            TokenType.PERCENT_EQUAL to "%=",
            TokenType.STAR_STAR_EQUAL to "**="
        )

        for ((tokenType, opStr) in augOps) {
            if (match(tokenType)) {
                val right = parseExpressionList()
                expectNewline()
                return AugAssignStmt(expr, opStr, right, expr.line)
            }
        }

        expectNewline()
        return ExprStmt(expr, expr.line)
    }

    private fun parseExpressionList(): Expr {
        val first = parseExpression()
        if (match(TokenType.COMMA)) {
            val elements = mutableListOf(first)
            while (!check(TokenType.NEWLINE) && !check(TokenType.EOF) && !check(TokenType.RPAREN) && !check(TokenType.RBRACKET) && !check(TokenType.RBRACE)) {
                elements.add(parseExpression())
                if (!match(TokenType.COMMA)) break
            }
            return TupleExpr(elements, first.line)
        }
        return first
    }

    private fun parseBlock(): List<Stmt> {
        if (match(TokenType.NEWLINE)) {
            consume(TokenType.INDENT, "Expected indented block")
            val statements = mutableListOf<Stmt>()
            while (!check(TokenType.DEDENT) && !isAtEnd()) {
                skipNewlines()
                if (check(TokenType.DEDENT) || isAtEnd()) break
                statements.add(parseStatement())
                skipNewlines()
            }
            consume(TokenType.DEDENT, "Expected dedent at end of block")
            return statements
        } else {
            // Single-line block (e.g., if cond: return 1)
            val stmt = parseSimpleStatement()
            return listOf(stmt)
        }
    }

    fun parseExpression(): Expr = parseLambdaOrTernary()

    private fun parseLambdaOrTernary(): Expr {
        if (match(TokenType.KW_LAMBDA)) {
            val line = peek().line
            val params = mutableListOf<String>()
            val defaults = mutableMapOf<String, Expr>()
            if (!check(TokenType.COLON)) {
                do {
                    val p = consume(TokenType.IDENTIFIER, "Expected parameter name").value
                    params.add(p)
                    if (match(TokenType.EQUAL)) {
                        defaults[p] = parseExpression()
                    }
                } while (match(TokenType.COMMA))
            }
            consume(TokenType.COLON, "Expected ':' in lambda expression")
            val body = parseExpression()
            return LambdaExpr(params, defaults, body, line)
        }

        val expr = parseLogicalOr()
        if (match(TokenType.KW_IF)) {
            val cond = parseLogicalOr()
            consume(TokenType.KW_ELSE, "Expected 'else' in ternary expression")
            val falseExpr = parseExpression()
            return TernaryExpr(cond, expr, falseExpr, expr.line)
        }
        return expr
    }

    private fun parseLogicalOr(): Expr {
        var expr = parseLogicalAnd()
        val values = mutableListOf(expr)
        while (match(TokenType.KW_OR)) {
            values.add(parseLogicalAnd())
        }
        return if (values.size > 1) BoolOp("or", values, expr.line) else expr
    }

    private fun parseLogicalAnd(): Expr {
        var expr = parseLogicalNot()
        val values = mutableListOf(expr)
        while (match(TokenType.KW_AND)) {
            values.add(parseLogicalNot())
        }
        return if (values.size > 1) BoolOp("and", values, expr.line) else expr
    }

    private fun parseLogicalNot(): Expr {
        if (match(TokenType.KW_NOT)) {
            val line = peek().line
            val operand = parseLogicalNot()
            return UnaryOp("not", operand, line)
        }
        return parseComparison()
    }

    private fun parseComparison(): Expr {
        var left = parseBitwiseOr()
        val comps = mutableListOf<Pair<String, Expr>>()

        while (true) {
            val op = when {
                match(TokenType.DOUBLE_EQUAL) -> "=="
                match(TokenType.NOT_EQUAL) -> "!="
                match(TokenType.LESS_EQUAL) -> "<="
                match(TokenType.LESS) -> "<"
                match(TokenType.GREATER_EQUAL) -> ">="
                match(TokenType.GREATER) -> ">"
                match(TokenType.KW_IN) -> "in"
                match(TokenType.KW_NOT) -> {
                    consume(TokenType.KW_IN, "Expected 'in' after 'not'")
                    "not in"
                }
                match(TokenType.KW_IS) -> {
                    if (match(TokenType.KW_NOT)) "is not" else "is"
                }
                else -> null
            } ?: break

            val right = parseBitwiseOr()
            comps.add(Pair(op, right))
        }

        return if (comps.isNotEmpty()) CompareOp(left, comps, left.line) else left
    }

    private fun parseBitwiseOr(): Expr {
        var expr = parseBitwiseXor()
        while (match(TokenType.PIPE)) {
            val right = parseBitwiseXor()
            expr = BinOp(expr, "|", right, expr.line)
        }
        return expr
    }

    private fun parseBitwiseXor(): Expr {
        var expr = parseBitwiseAnd()
        while (match(TokenType.CARET)) {
            val right = parseBitwiseAnd()
            expr = BinOp(expr, "^", right, expr.line)
        }
        return expr
    }

    private fun parseBitwiseAnd(): Expr {
        var expr = parseShift()
        while (match(TokenType.AMPERSAND)) {
            val right = parseShift()
            expr = BinOp(expr, "&", right, expr.line)
        }
        return expr
    }

    private fun parseShift(): Expr {
        var expr = parseAddSub()
        while (true) {
            val op = when {
                match(TokenType.LSHIFT) -> "<<"
                match(TokenType.RSHIFT) -> ">>"
                else -> null
            } ?: break
            val right = parseAddSub()
            expr = BinOp(expr, op, right, expr.line)
        }
        return expr
    }

    private fun parseAddSub(): Expr {
        var expr = parseMulDiv()
        while (true) {
            val op = when {
                match(TokenType.PLUS) -> "+"
                match(TokenType.MINUS) -> "-"
                else -> null
            } ?: break
            val right = parseMulDiv()
            expr = BinOp(expr, op, right, expr.line)
        }
        return expr
    }

    private fun parseMulDiv(): Expr {
        var expr = parseUnary()
        while (true) {
            val op = when {
                match(TokenType.STAR) -> "*"
                match(TokenType.SLASH) -> "/"
                match(TokenType.DOUBLE_SLASH) -> "//"
                match(TokenType.PERCENT) -> "%"
                else -> null
            } ?: break
            val right = parseUnary()
            expr = BinOp(expr, op, right, expr.line)
        }
        return expr
    }

    private fun parseUnary(): Expr {
        if (match(TokenType.PLUS)) return UnaryOp("+", parseUnary(), peek().line)
        if (match(TokenType.MINUS)) return UnaryOp("-", parseUnary(), peek().line)
        if (match(TokenType.TILDE)) return UnaryOp("~", parseUnary(), peek().line)
        return parsePower()
    }

    private fun parsePower(): Expr {
        val expr = parsePostfix()
        if (match(TokenType.STAR_STAR)) {
            val right = parseUnary()
            return BinOp(expr, "**", right, expr.line)
        }
        return expr
    }

    private fun parsePostfix(): Expr {
        var expr = parsePrimary()
        while (true) {
            when {
                match(TokenType.LPAREN) -> {
                    val args = mutableListOf<Expr>()
                    val kwargs = mutableListOf<Pair<String, Expr>>()
                    if (!check(TokenType.RPAREN)) {
                        while (true) {
                            if (peek().type == TokenType.IDENTIFIER && peekAhead(1)?.type == TokenType.EQUAL) {
                                val kwName = advance().value
                                advance() // '='
                                val valExpr = parseExpression()
                                kwargs.add(Pair(kwName, valExpr))
                            } else {
                                args.add(parseExpression())
                            }
                            if (!match(TokenType.COMMA)) break
                            if (check(TokenType.RPAREN)) break
                        }
                    }
                    consume(TokenType.RPAREN, "Expected ')' after argument list")
                    expr = CallExpr(expr, args, kwargs, expr.line)
                }
                match(TokenType.DOT) -> {
                    val attr = consume(TokenType.IDENTIFIER, "Expected attribute name after '.'").value
                    expr = AttributeExpr(expr, attr, expr.line)
                }
                match(TokenType.LBRACKET) -> {
                    val slice = parseSlice()
                    consume(TokenType.RBRACKET, "Expected ']' after subscript")
                    expr = SubscriptExpr(expr, slice, expr.line)
                }
                else -> break
            }
        }
        return expr
    }

    private fun parseSlice(): SliceExpr {
        val line = peek().line
        if (match(TokenType.COLON)) {
            // [:end:step]
            var upper: Expr? = null
            var step: Expr? = null
            if (!check(TokenType.COLON) && !check(TokenType.RBRACKET)) {
                upper = parseExpression()
            }
            if (match(TokenType.COLON)) {
                if (!check(TokenType.RBRACKET)) {
                    step = parseExpression()
                }
            }
            return SliceExpr(null, upper, step, false, line)
        }

        val first = parseExpression()
        if (match(TokenType.COLON)) {
            var upper: Expr? = null
            var step: Expr? = null
            if (!check(TokenType.COLON) && !check(TokenType.RBRACKET)) {
                upper = parseExpression()
            }
            if (match(TokenType.COLON)) {
                if (!check(TokenType.RBRACKET)) {
                    step = parseExpression()
                }
            }
            return SliceExpr(first, upper, step, false, line)
        }

        return SliceExpr(first, null, null, true, line)
    }

    private fun parsePrimary(): Expr {
        val token = peek()
        return when (token.type) {
            TokenType.INT_LITERAL -> {
                advance()
                val radix = when {
                    token.value.startsWith("0x", true) -> 16
                    token.value.startsWith("0b", true) -> 2
                    else -> 10
                }
                val raw = if (radix != 10) token.value.substring(2) else token.value
                val value = raw.toLongOrNull(radix) ?: 0L
                IntLiteralExpr(value, token.line)
            }
            TokenType.FLOAT_LITERAL -> {
                advance()
                val value = token.value.toDoubleOrNull() ?: 0.0
                FloatLiteralExpr(value, token.line)
            }
            TokenType.STRING_LITERAL -> {
                advance()
                StringLiteralExpr(token.value, token.line)
            }
            TokenType.FSTRING_LITERAL -> {
                advance()
                parseFString(token.value, token.line)
            }
            TokenType.KW_TRUE -> { advance(); BoolLiteralExpr(true, token.line) }
            TokenType.KW_FALSE -> { advance(); BoolLiteralExpr(false, token.line) }
            TokenType.KW_NONE -> { advance(); NoneLiteralExpr(token.line) }
            TokenType.IDENTIFIER -> { advance(); NameExpr(token.value, token.line) }
            TokenType.LPAREN -> {
                advance()
                if (match(TokenType.RPAREN)) return TupleExpr(emptyList(), token.line)
                val expr = parseExpression()
                if (match(TokenType.COMMA)) {
                    val elements = mutableListOf(expr)
                    while (!check(TokenType.RPAREN) && !isAtEnd()) {
                        elements.add(parseExpression())
                        if (!match(TokenType.COMMA)) break
                    }
                    consume(TokenType.RPAREN, "Expected ')' after tuple elements")
                    TupleExpr(elements, token.line)
                } else {
                    consume(TokenType.RPAREN, "Expected ')' after expression")
                    expr
                }
            }
            TokenType.LBRACKET -> {
                advance()
                if (match(TokenType.RBRACKET)) return ListExpr(emptyList(), token.line)
                val first = parseExpression()
                if (match(TokenType.KW_FOR)) {
                    // List comprehension: [elt for x in iter if cond]
                    val target = parseForTarget()
                    consume(TokenType.KW_IN, "Expected 'in' in list comprehension")
                    val iter = parseExpression()
                    val ifs = mutableListOf<Expr>()
                    while (match(TokenType.KW_IF)) {
                        ifs.add(parseExpression())
                    }
                    consume(TokenType.RBRACKET, "Expected ']' after list comprehension")
                    ListCompExpr(first, target, iter, ifs, token.line)
                } else {
                    val elements = mutableListOf(first)
                    while (match(TokenType.COMMA)) {
                        if (check(TokenType.RBRACKET)) break
                        elements.add(parseExpression())
                    }
                    consume(TokenType.RBRACKET, "Expected ']' after list elements")
                    ListExpr(elements, token.line)
                }
            }
            TokenType.LBRACE -> {
                advance()
                if (match(TokenType.RBRACE)) return DictExpr(emptyList(), token.line)
                val first = parseExpression()
                if (match(TokenType.COLON)) {
                    val firstVal = parseExpression()
                    if (match(TokenType.KW_FOR)) {
                        // Dict comprehension: {k: v for x in iter if cond}
                        val target = parseForTarget()
                        consume(TokenType.KW_IN, "Expected 'in' in dict comprehension")
                        val iter = parseExpression()
                        val ifs = mutableListOf<Expr>()
                        while (match(TokenType.KW_IF)) {
                            ifs.add(parseExpression())
                        }
                        consume(TokenType.RBRACE, "Expected '}' after dict comprehension")
                        DictCompExpr(first, firstVal, target, iter, ifs, token.line)
                    } else {
                        val entries = mutableListOf(Pair(first, firstVal))
                        while (match(TokenType.COMMA)) {
                            if (check(TokenType.RBRACE)) break
                            val k = parseExpression()
                            consume(TokenType.COLON, "Expected ':' in dictionary entry")
                            val v = parseExpression()
                            entries.add(Pair(k, v))
                        }
                        consume(TokenType.RBRACE, "Expected '}' after dictionary entries")
                        DictExpr(entries, token.line)
                    }
                } else {
                    // Set comprehension or Set literal
                    if (match(TokenType.KW_FOR)) {
                        val target = parseForTarget()
                        consume(TokenType.KW_IN, "Expected 'in' in set comprehension")
                        val iter = parseExpression()
                        val ifs = mutableListOf<Expr>()
                        while (match(TokenType.KW_IF)) {
                            ifs.add(parseExpression())
                        }
                        consume(TokenType.RBRACE, "Expected '}' after set comprehension")
                        SetCompExpr(first, target, iter, ifs, token.line)
                    } else {
                        val elements = mutableListOf(first)
                        while (match(TokenType.COMMA)) {
                            if (check(TokenType.RBRACE)) break
                            elements.add(parseExpression())
                        }
                        consume(TokenType.RBRACE, "Expected '}' after set elements")
                        SetExpr(elements, token.line)
                    }
                }
            }
            else -> throw PyParseException("Unexpected token '${token.value}' (${token.type})", token.line, token.column)
        }
    }

    private fun parseFString(raw: String, line: Int): FStringExpr {
        val parts = mutableListOf<Any>()
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '{') {
                if (i + 1 < raw.length && raw[i + 1] == '{') {
                    sb.append('{')
                    i += 2
                } else {
                    if (sb.isNotEmpty()) {
                        parts.add(sb.toString())
                        sb.clear()
                    }
                    i++ // skip '{'
                    val exprSb = StringBuilder()
                    var depth = 1
                    while (i < raw.length && depth > 0) {
                        val ch = raw[i]
                        if (ch == '{') depth++
                        else if (ch == '}') {
                            depth--
                            if (depth == 0) { i++; break }
                        }
                        exprSb.append(ch)
                        i++
                    }
                    val innerCode = exprSb.toString().trim()
                    if (innerCode.isNotEmpty()) {
                        try {
                            val lexer = PyLexer(innerCode)
                            val subParser = PyParser(lexer.tokenize())
                            val expr = subParser.parseExpression()
                            parts.add(expr)
                        } catch (e: Exception) {
                            parts.add("{$innerCode}")
                        }
                    }
                }
            } else if (c == '}' && i + 1 < raw.length && raw[i + 1] == '}') {
                sb.append('}')
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        if (sb.isNotEmpty()) {
            parts.add(sb.toString())
        }
        return FStringExpr(parts, line)
    }

    private fun skipNewlines() {
        while (match(TokenType.NEWLINE)) { /* pass */ }
    }

    private fun expectNewline() {
        if (!check(TokenType.EOF) && !check(TokenType.DEDENT)) {
            if (match(TokenType.SEMICOLON)) return
            consume(TokenType.NEWLINE, "Expected newline at end of statement")
        }
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return type == TokenType.EOF
        return peek().type == type
    }

    private fun match(type: TokenType): Boolean {
        if (check(type)) {
            advance()
            return true
        }
        return false
    }

    private fun consume(type: TokenType, message: String): PyToken {
        if (check(type)) return advance()
        val token = peek()
        throw PyParseException(message, token.line, token.column)
    }

    private fun peek(): PyToken = tokens.getOrElse(index) { PyToken(TokenType.EOF, "", 0, 0) }

    private fun peekAhead(offset: Int): PyToken? {
        val target = index + offset
        return if (target < tokens.size) tokens[target] else null
    }

    private fun advance(): PyToken {
        if (!isAtEnd()) index++
        return tokens[index - 1]
    }

    private fun isAtEnd(): Boolean = index >= tokens.size || tokens[index].type == TokenType.EOF
}
