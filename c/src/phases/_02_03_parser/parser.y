%{

/*
 * parser.y -- SPL parser specification
 */

#include <stdlib.h>
#include <util/errors.h>
#include <table/identifier.h>
#include <types/types.h>
#include <absyn/absyn.h>
#include <phases/_01_scanner/scanner.h>
#include <phases/_02_03_parser/parser.h>

void yyerror(Program**, char *);

%}

// This section is placed into the header to make functions available to other modules
%code requires{
	/**
	  @return The name of the token class signalled by the given id.
	 */
	char const * tokenName(int token_class_id);
}

%expect 0 // TODO: Change?
%parse-param {Program** program}
%define parse.error custom
%union {
  NoVal noVal;
  IntVal intVal;
  IdentVal identVal;

  Expression *expression;
  Variable *variable;
  Statement *statement;
  TypeExpression *typeExpression;
  GlobalDefinition *globalDefinition;
  VariableDefinition *variableDefinition;
  ParameterDefinition *parameterDefinition;

  StatementList *statementList;
  ExpressionList *expressionList;
  VariableDefinitionList *variableList;
  ParameterDefinitionList *parameterList;
  GlobalDefinitionList *globalDefinitionList;
}

%token	<noVal>		ARRAY ELSE IF OF PROC
%token	<noVal>		REF TYPE VAR WHILE
%token	<noVal>		LPAREN RPAREN LBRACK
%token	<noVal>		RBRACK LCURL RCURL
%token	<noVal>		EQ NE LT LE GT GE
%token	<noVal>		ASGN COLON COMMA SEMIC
%token	<noVal>		PLUS MINUS STAR SLASH
%token	<identVal>	IDENT
%token	<intVal>	INTLIT

%start			program

%%

program			: INTLIT; //TODO (assignment 2 and 3): Just a dummy, needs to be replaced by the actual spl grammar.

%%

static int yyreport_syntax_error (const yypcontext_t *ctx, Program** program) {
    (void)program;
    int expected_token_n = yypcontext_expected_tokens(ctx, NULL, 0);

    yysymbol_kind_t* expected = allocate(expected_token_n * sizeof(yysymbol_kind_t));
    yypcontext_expected_tokens(ctx, expected, expected_token_n);
    char const ** expected_str = allocate(expected_token_n * sizeof(char const *));

    for (int i = 0; i < expected_token_n; ++i) {
        expected_str[i] = yysymbol_name(expected[i]);
    }

    syntaxError(yylval.noVal.position, yysymbol_name(ctx->yytoken), expected_str, expected_token_n);
}


void yyerror(Program** program, char *msg) {
    (void)program;
    error(msg);
}


// This function needs to be defined here because yytname and YYTRANSLATE are only available in the parser's implementation file.
char const *tokenName(int token_class_id) {
  // 0 is a special case because yysymbol_name(token) returns "end of file" instead of EOF.
  return token_class_id == 0 ? "EOF" : yysymbol_name(YYTRANSLATE(token_class_id));
}
