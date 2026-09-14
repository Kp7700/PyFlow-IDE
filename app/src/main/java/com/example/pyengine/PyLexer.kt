package com.example.pyengine

class PyLexerException(message: String, val line: Int, val column: Int) : Exception("Line $line, Col $column: $message")

class PyLexer(private val source: String) {
    private val tokens = mutableListOf<PyToken>()
    private var index = 0
    private var line = 1
    private var col = 1

    private val indentStack = ArrayDeque<Int>().apply { add(0) }
    private var openBrackets = 0

    private val keywords = mapOf(
        "def" to TokenType.KW_DEF,
        "class" to TokenType.KW_CLASS,
        "return" to TokenType.KW_RETURN,
        "if" to TokenType.KW_IF,
        "elif" to TokenType.KW_ELIF,
        "else" to TokenType.KW_ELSE,
        "while" to TokenType.KW_WHILE,
        "for" to TokenType.KW_FOR,
        "in" to TokenType.KW_IN,
        "try" to TokenType.KW_TRY,
        "except" to TokenType.KW_EXCEPT,
        "finally" to TokenType.KW_FINALLY,
        "raise" to TokenType.KW_RAISE,
        "import" to TokenType.KW_IMPORT,
        "from" to TokenType.KW_FROM,
        "as" to TokenType.KW_AS,
        "pass" to TokenType.KW_PASS,
        "break" to TokenType.KW_BREAK,
        "continue" to TokenType.KW_CONTINUE,
        "lambda" to TokenType.KW_LAMBDA,
        "global" to TokenType.KW_GLOBAL,
        "nonlocal" to TokenType.KW_NONLOCAL,
        "and" to TokenType.KW_AND,
        "or" to TokenType.KW_OR,
        "not" to TokenType.KW_NOT,
        "is" to TokenType.KW_IS,
        "with" to TokenType.KW_WITH,
        "yield" to TokenType.KW_YIELD,
        "assert" to TokenType.KW_ASSERT,
        "True" to TokenType.KW_TRUE,
        "False" to TokenType.KW_FALSE,
        "None" to TokenType.KW_NONE
    )

    fun tokenize(): List<PyToken> {
        var atLineStart = true

        while (index < source.length) {
            val c = peek()

            if (atLineStart) {
                if (c == '\n' || c == '\r') {
                    advance()
                    continue
                }
                if (c == '#') {
                    skipComment()
                    continue
                }
                if (c == ' ' || c == '\t') {
                    val indentLevel = countIndentation()
                    // Check if line is empty or comment
                    if (index < source.length && (peek() == '\n' || peek() == '\r' || peek() == '#')) {
                        continue
                    }
                    if (openBrackets == 0) {
                        handleIndentation(indentLevel)
                    }
                    atLineStart = false
                    continue
                } else {
                    if (openBrackets == 0) {
                        handleIndentation(0)
                    }
                    atLineStart = false
                }
            }

            if (index >= source.length) break

            when (val ch = peek()) {
                ' ', '\t' -> advance()
                '\r' -> advance()
                '\n' -> {
                    advance()
                    if (openBrackets == 0) {
                        // Avoid consecutive NEWLINE tokens
                        if (tokens.isNotEmpty() && tokens.last().type != TokenType.NEWLINE) {
                            tokens.add(PyToken(TokenType.NEWLINE, "\n", line - 1, col))
                        }
                    }
                    atLineStart = true
                }
                '#' -> skipComment()
                '(' -> { addToken(TokenType.LPAREN, "("); openBrackets++; advance() }
                ')' -> { addToken(TokenType.RPAREN, ")"); if (openBrackets > 0) openBrackets--; advance() }
                '[' -> { addToken(TokenType.LBRACKET, "["); openBrackets++; advance() }
                ']' -> { addToken(TokenType.RBRACKET, "]"); if (openBrackets > 0) openBrackets--; advance() }
                '{' -> { addToken(TokenType.LBRACE, "{"); openBrackets++; advance() }
                '}' -> { addToken(TokenType.RBRACE, "}"); if (openBrackets > 0) openBrackets--; advance() }
                ':' -> { addToken(TokenType.COLON, ":"); advance() }
                ',' -> { addToken(TokenType.COMMA, ","); advance() }
                ';' -> { addToken(TokenType.SEMICOLON, ";"); advance() }
                '~' -> { addToken(TokenType.TILDE, "~"); advance() }
                '.' -> {
                    if (peekAhead(1)?.isDigit() == true) {
                        lexNumber()
                    } else {
                        addToken(TokenType.DOT, ".")
                        advance()
                    }
                }
                '+' -> {
                    advance()
                    if (match('=')) addToken(TokenType.PLUS_EQUAL, "+=")
                    else addToken(TokenType.PLUS, "+")
                }
                '-' -> {
                    advance()
                    if (match('>')) addToken(TokenType.ARROW, "->")
                    else if (match('=')) addToken(TokenType.MINUS_EQUAL, "-=")
                    else addToken(TokenType.MINUS, "-")
                }
                '*' -> {
                    advance()
                    if (match('*')) {
                        if (match('=')) addToken(TokenType.STAR_STAR_EQUAL, "**=")
                        else addToken(TokenType.STAR_STAR, "**")
                    } else if (match('=')) {
                        addToken(TokenType.STAR_EQUAL, "*=")
                    } else {
                        addToken(TokenType.STAR, "*")
                    }
                }
                '/' -> {
                    advance()
                    if (match('/')) {
                        if (match('=')) addToken(TokenType.DOUBLE_SLASH_EQUAL, "//=")
                        else addToken(TokenType.DOUBLE_SLASH, "//")
                    } else if (match('=')) {
                        addToken(TokenType.SLASH_EQUAL, "/=")
                    } else {
                        addToken(TokenType.SLASH, "/")
                    }
                }
                '%' -> {
                    advance()
                    if (match('=')) addToken(TokenType.PERCENT_EQUAL, "%=")
                    else addToken(TokenType.PERCENT, "%")
                }
                '=' -> {
                    advance()
                    if (match('=')) addToken(TokenType.DOUBLE_EQUAL, "==")
                    else addToken(TokenType.EQUAL, "=")
                }
                '!' -> {
                    advance()
                    if (match('=')) addToken(TokenType.NOT_EQUAL, "!=")
                    else throw PyLexerException("Unexpected character '!'", line, col)
                }
                '<' -> {
                    advance()
                    if (match('<')) addToken(TokenType.LSHIFT, "<<")
                    else if (match('=')) addToken(TokenType.LESS_EQUAL, "<=")
                    else addToken(TokenType.LESS, "<")
                }
                '>' -> {
                    advance()
                    if (match('>')) addToken(TokenType.RSHIFT, ">>")
                    else if (match('=')) addToken(TokenType.GREATER_EQUAL, ">=")
                    else addToken(TokenType.GREATER, ">")
                }
                '&' -> { addToken(TokenType.AMPERSAND, "&"); advance() }
                '|' -> { addToken(TokenType.PIPE, "|"); advance() }
                '^' -> { addToken(TokenType.CARET, "^"); advance() }
                '"', '\'' -> lexString()
                else -> {
                    if (ch.isLetter() || ch == '_') {
                        lexIdentifierOrKeyword()
                    } else if (ch.isDigit()) {
                        lexNumber()
                    } else {
                        throw PyLexerException("Invalid character: '$ch'", line, col)
                    }
                }
            }
        }

        // Finish pending newlines & dedents
        if (tokens.isNotEmpty() && tokens.last().type != TokenType.NEWLINE) {
            tokens.add(PyToken(TokenType.NEWLINE, "\n", line, col))
        }

        while (indentStack.size > 1) {
            indentStack.removeLast()
            tokens.add(PyToken(TokenType.DEDENT, "", line, col))
        }

        tokens.add(PyToken(TokenType.EOF, "", line, col))
        return tokens
    }

    private fun handleIndentation(currentIndent: Int) {
        val lastIndent = indentStack.last()
        if (currentIndent > lastIndent) {
            indentStack.add(currentIndent)
            tokens.add(PyToken(TokenType.INDENT, currentIndent.toString(), line, col))
        } else if (currentIndent < lastIndent) {
            while (indentStack.size > 1 && indentStack.last() > currentIndent) {
                indentStack.removeLast()
                tokens.add(PyToken(TokenType.DEDENT, "", line, col))
            }
            if (indentStack.last() != currentIndent) {
                throw PyLexerException("Unindent does not match any outer indentation level", line, col)
            }
        }
    }

    private fun countIndentation(): Int {
        var count = 0
        while (index < source.length) {
            val c = source[index]
            if (c == ' ') {
                count++
                advance()
            } else if (c == '\t') {
                count += 4
                advance()
            } else {
                break
            }
        }
        return count
    }

    private fun skipComment() {
        while (index < source.length && peek() != '\n') {
            advance()
        }
    }

    private fun lexIdentifierOrKeyword() {
        val startCol = col
        val startLine = line
        val sb = StringBuilder()

        // Check for f-string or raw-string prefixes
        val firstChar = peek()
        if ((firstChar == 'f' || firstChar == 'F' || firstChar == 'r' || firstChar == 'R') &&
            (peekAhead(1) == '"' || peekAhead(1) == '\'')) {
            val prefix = advance()
            lexPrefixedString(prefix, startLine, startCol)
            return
        }

        while (index < source.length && (peek().isLetterOrDigit() || peek() == '_')) {
            sb.append(advance())
        }
        val text = sb.toString()
        val tokenType = keywords[text] ?: TokenType.IDENTIFIER
        tokens.add(PyToken(tokenType, text, startLine, startCol))
    }

    private fun lexPrefixedString(prefix: Char, startLine: Int, startCol: Int) {
        val quote = advance()
        val isTriple = peek() == quote && peekAhead(1) == quote
        if (isTriple) {
            advance(); advance()
        }

        val sb = StringBuilder()
        var closed = false

        while (index < source.length) {
            if (isTriple) {
                if (peek() == quote && peekAhead(1) == quote && peekAhead(2) == quote) {
                    advance(); advance(); advance()
                    closed = true
                    break
                }
            } else {
                if (peek() == quote) {
                    advance()
                    closed = true
                    break
                }
            }

            if (peek() == '\\' && (prefix != 'r' && prefix != 'R')) {
                advance()
                if (index < source.length) {
                    val escaped = advance()
                    when (escaped) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        '\\', '\'', '"' -> sb.append(escaped)
                        else -> { sb.append('\\'); sb.append(escaped) }
                    }
                }
            } else {
                val c = advance()
                sb.append(c)
            }
        }

        if (!closed) {
            throw PyLexerException("Unterminated string literal", startLine, startCol)
        }

        val type = if (prefix == 'f' || prefix == 'F') TokenType.FSTRING_LITERAL else TokenType.STRING_LITERAL
        tokens.add(PyToken(type, sb.toString(), startLine, startCol))
    }

    private fun lexString() {
        val startCol = col
        val startLine = line
        val quote = advance()
        val isTriple = peek() == quote && peekAhead(1) == quote
        if (isTriple) {
            advance(); advance()
        }

        val sb = StringBuilder()
        var closed = false

        while (index < source.length) {
            if (isTriple) {
                if (peek() == quote && peekAhead(1) == quote && peekAhead(2) == quote) {
                    advance(); advance(); advance()
                    closed = true
                    break
                }
            } else {
                if (peek() == quote) {
                    advance()
                    closed = true
                    break
                }
                if (peek() == '\n') {
                    throw PyLexerException("EOL while scanning single-line string literal", startLine, startCol)
                }
            }

            if (peek() == '\\') {
                advance()
                if (index < source.length) {
                    val escaped = advance()
                    when (escaped) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        '\\', '\'', '"' -> sb.append(escaped)
                        else -> { sb.append('\\'); sb.append(escaped) }
                    }
                }
            } else {
                sb.append(advance())
            }
        }

        if (!closed) {
            throw PyLexerException("Unterminated string literal", startLine, startCol)
        }

        tokens.add(PyToken(TokenType.STRING_LITERAL, sb.toString(), startLine, startCol))
    }

    private fun lexNumber() {
        val startCol = col
        val startLine = line
        val sb = StringBuilder()

        // Check for hex or binary
        if (peek() == '0' && (peekAhead(1) == 'x' || peekAhead(1) == 'X')) {
            sb.append(advance())
            sb.append(advance())
            while (index < source.length && (peek().isDigit() || peek() in 'a'..'f' || peek() in 'A'..'F' || peek() == '_')) {
                val c = advance()
                if (c != '_') sb.append(c)
            }
            tokens.add(PyToken(TokenType.INT_LITERAL, sb.toString(), startLine, startCol))
            return
        }

        if (peek() == '0' && (peekAhead(1) == 'b' || peekAhead(1) == 'B')) {
            sb.append(advance())
            sb.append(advance())
            while (index < source.length && (peek() == '0' || peek() == '1' || peek() == '_')) {
                val c = advance()
                if (c != '_') sb.append(c)
            }
            tokens.add(PyToken(TokenType.INT_LITERAL, sb.toString(), startLine, startCol))
            return
        }

        var isFloat = false
        while (index < source.length && (peek().isDigit() || peek() == '_')) {
            val c = advance()
            if (c != '_') sb.append(c)
        }

        if (peek() == '.' && peekAhead(1)?.isDigit() == true) {
            isFloat = true
            sb.append(advance())
            while (index < source.length && (peek().isDigit() || peek() == '_')) {
                val c = advance()
                if (c != '_') sb.append(c)
            }
        }

        if (peek() == 'e' || peek() == 'E') {
            isFloat = true
            sb.append(advance())
            if (peek() == '+' || peek() == '-') {
                sb.append(advance())
            }
            while (index < source.length && peek().isDigit()) {
                sb.append(advance())
            }
        }

        val type = if (isFloat) TokenType.FLOAT_LITERAL else TokenType.INT_LITERAL
        tokens.add(PyToken(type, sb.toString(), startLine, startCol))
    }

    private fun peek(): Char = if (index < source.length) source[index] else '\u0000'

    private fun peekAhead(offset: Int): Char? {
        val target = index + offset
        return if (target < source.length) source[target] else null
    }

    private fun advance(): Char {
        val c = source[index++]
        if (c == '\n') {
            line++
            col = 1
        } else {
            col++
        }
        return c
    }

    private fun match(expected: Char): Boolean {
        if (peek() == expected) {
            advance()
            return true
        }
        return false
    }

    private fun addToken(type: TokenType, value: String) {
        tokens.add(PyToken(type, value, line, col))
    }
}
