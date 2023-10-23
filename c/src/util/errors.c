/*
 * errors.c -- error reporting
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <ctype.h>
#include <string.h>

#include "errors.h"
#include "string_ops.h"
#include "memory.h"

static char *leaky_type_tostring(Type *type) {
    char *buffer = allocate(1024);
    typeAsString(buffer, 1024, type);

    // Copy string into an appropriately sized buffer.
    char *trimmed = allocate(strlen(buffer));
    strcpy(trimmed, buffer);

    release(buffer);

    return trimmed;
}

static char const *binaryOperatorTostring(BinaryOperator operator) {
    switch (operator) {
        case ABSYN_OP_EQU:
            return "=";
        case ABSYN_OP_NEQ:
            return "#";
        case ABSYN_OP_LST:
            return "<";
        case ABSYN_OP_LSE:
            return "<=";
        case ABSYN_OP_GRT:
            return ">";
        case ABSYN_OP_GRE:
            return ">=";
        case ABSYN_OP_ADD:
            return "+";
        case ABSYN_OP_SUB:
            return "-";
        case ABSYN_OP_MUL:
            return "*";
        case ABSYN_OP_DIV:
            return "/";
        default:
            internalError();
    }
}

static char const *unaryOperatorTostring(UnaryOperator operator) {
    switch (operator) {
        case ABSYN_OP_UNARY_MINUS:
            return "-";
        default:
            internalError();
    }
}

_Noreturn void error(char *fmt, ...) {
    va_list ap;

    va_start(ap, fmt);
    fprintf(stderr, "An error occurred: ");
    vfprintf(stderr, fmt, ap);
    fprintf(stderr, "\n");
    va_end(ap);

    exit(1);
}

_Noreturn void notImplemented() {
    error("You have not implemented this operation yet!");
}

_Noreturn void internalError() {
    error("Internal error!");
}

static _Noreturn void splError(int errorCode, Position position, const char *fmt, ...) {
    va_list ap;

    va_start(ap, fmt);
    if(position.line > 0)
        fprintf(stderr, "An error occurred at Line %d, Column %d:\n", position.line, position.column);
    else
        fprintf(stderr, "An error occurred:\n");

    vfprintf(stderr, fmt, ap);
    fprintf(stderr, "\n");
    va_end(ap);

    exit(errorCode);
}

_Noreturn void lexicalError(Position position, char character) {
    if (isspace(character) || iscntrl(character))
        splError(99, position, "Lexical error: Unexpected character with ascii code 0x%02x.", (unsigned char) character);
    else
        splError(99, position, "Lexical error: Unexpected character '%c'.", character);
}

_Noreturn void syntaxError(Position position, char const *token, char const **expectedTokens, int expected_tokens_size) {
    qsort(expectedTokens, expected_tokens_size, sizeof(char const *), str_comparator);

    if (expectedTokens == NULL || expected_tokens_size == 0)
        splError(100, position, "Syntax error: Unexpected token '%s'.", token);
    else if (expected_tokens_size == 1)
        splError(100, position, "Syntax error: Unexpected token '%s'. Expected token '%s' instead.", token, expectedTokens[0]);
    else
        splError(100, position, "Syntax error: Unexpected token '%s'. Expected one of [%s] instead.", token, join(expectedTokens, expected_tokens_size, ", "));
}

_Noreturn void undefinedIdentifier(Position position, Identifier *name) {
    splError(101, position, "Identifier '%s' is not defined.", name->string);
}

_Noreturn void notAType(Position position, Identifier *name) {
    splError(102, position, "Identifier '%s' does not refer to a type.", name->string);
}

_Noreturn void redefinitionOfIdentifier(Position position, Identifier *name) {
    splError(103, position, "Identifier '%s' is already defined in this scope.", name->string);
}

_Noreturn void parameterMustBeReference(Position position, Identifier *name, Type* type) {
    splError(104, position, "Non-reference parameter '%s' has type '%s', which can only be passed by reference.", name->string, leaky_type_tostring(type));
}

_Noreturn void illegalAssignment(Position position, Type *left, Type *right) {
    splError(108, position, "A value of type '%s' can not be assigned to variable of type '%s'.", leaky_type_tostring(right), leaky_type_tostring(left));
}

_Noreturn void ifConditionMustBeBoolean(Position position, Type *actual) {
    splError(110, position, "'if' condition expected to be of type 'boolean', but is of type '%s'.", leaky_type_tostring(actual));
}

_Noreturn void whileConditionMustBeBoolean(Position position, Type *actual) {
    splError(111, position, "'while' condition expected to be of type 'boolean', but is of type '%s'.", leaky_type_tostring(actual));
}

_Noreturn void callOfNonProcedure(Position position, Identifier *name) {
    splError(113, position, "Identifier '%s' does not refer to a procedure.", name->string);
}

_Noreturn void argumentTypeMismatch(Position position, Identifier *name, int argumentIndex, Type *expected, Type *actual) {
    splError(114, position, "Argument type mismatch in call of procedure '%s'. Argument %d is expected to have type '%s', but has type '%s'.",
             name->string,
             argumentIndex,
             leaky_type_tostring(expected),
             leaky_type_tostring(actual));
}

_Noreturn void argumentMustBeAVariable(Position position, Identifier *name, int argumentIndex) {
    splError(115, position, "Invalid argument for reference parameter in call to procedure '%s': Argument %d must be a variable.", name->string, argumentIndex);
}

_Noreturn void argumentCountMismatch(Position position, Identifier *name, int expected, int actual) {
    if (actual < expected)
        splError(116, position, "Argument count mismatch: Procedure '%s' expects %d arguments, but only %d were provided.", name->string, expected, actual);
    else
        splError(116, position, "Argument count mismatch: Procedure '%s' expects only %d arguments, but %d were provided.", name->string, expected, actual);
}

_Noreturn void binaryOperandTypeMismatch(Position position, BinaryOperator operator, Type *left, Type *right) {
    splError(118, position, "Type mismatch in binary expression: Operator '%s' does not accept operands of types '%s' and '%s'.",
             binaryOperatorTostring(operator),
             leaky_type_tostring(left),
             leaky_type_tostring(right));
}

_Noreturn void unaryOperandTypeMismatch(Position position, UnaryOperator operator, Type *right) {
    splError(119, position, "Type mismatch in unary expression: Operator '%s' does not accept operand of type '%s'.", unaryOperatorTostring(operator), leaky_type_tostring(right));
}

_Noreturn void notAVariable(Position position, Identifier *name) {
    splError(122, position, "Identifier '%s' does not refer to a variable.", name->string);
}

_Noreturn void indexingNonArray(Position position, Type* actual) {
    splError(123, position, "Type mismatch: Invalid array access operation on non-array variable of type '%s'.", leaky_type_tostring(actual));
}

_Noreturn void indexTypeMismatch(Position position, Type* actual) {
    splError(124, position, "Type mismatch: Array index expected to be of type 'int', but is type '%s'.", leaky_type_tostring(actual));
}

_Noreturn void mainIsMissing() {
    splError(125, ERROR_POSITION, "Procedure 'main' is missing.");
}

_Noreturn void mainIsNotAProcedure() {
    splError(126, ERROR_POSITION, "Identifier 'main' does not refer to a procedure.");
}

_Noreturn void mainMustNotHaveParameters() {
    splError(127, ERROR_POSITION, "Procedure 'main' must not have any parameters.");
}

_Noreturn void registerOverflow() {
    splError(140, ERROR_POSITION, "There are not enough registers to run this program!");
}