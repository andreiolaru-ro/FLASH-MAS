/* Lexer specification to be used with jflex, in order to generate the lexical analyzer of S-Claim.
To generate the Lexer, in Linux, run the script "generateLexer.sh", included in the directory */
package tatami.sclaim.parser.generation;

%%
%class Yylex
%unicode
%byaccj
%line
%column

%{
  /* store a reference to the parser object */
  private ParserSClaim yyparser;

  /* constructor taking an additional ParserSClaim object */
  public Yylex(java.io.Reader r, ParserSClaim yyparser) {
    this(r);
    this.yyparser = yyparser;
  }
  
  public String getPosition()
  {
  	return new String("at line "+(yyline+1)+" and column "+(yycolumn+1));
  }

  public String getLine()
  {
  	return new String("at line "+(yyline+1));
  }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment} | {DocumentationComment}

TraditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment     = "//" {InputCharacter}* {LineTerminator}
DocumentationComment = "/**" {CommentContent} "*"+ "/"
CommentContent       = ( [^*] | \*+ [^/*] )*

D		=	[0-9]
L		=	[a-zA-Z_]

StringLiteral = \"(\\.|[^\\\"]|[^\\\n])*\" 
Variable = "?"({L}|{D})+
AffectableVariable = "??"({L}|{D})+
/*Constant = ({L}|{D}|"~"|"!"|"@"|"#"|"$"|"%"|"^"|"&"|"*"|"_"|"+"|"-"|"="|"`"|"}"|"{"|"]"|"["|"|"|":"|"\\"|";"|"\""|"'"|">"|"<"|"."|","|"/")+*/
Constant = ([^?)(\t\f \r\n])+

%%
/* the reference to the instantiated agent */
"this"			{ return ParserSClaim.THIS; }

/* the reference to the parent of the agent */
"parent"			{ return ParserSClaim.PARENT; }

/* special claim keywords */
"struct"		{ return ParserSClaim.STRUCT; }
"agent"			{ return ParserSClaim.AGENT; }
"behavior"		{ return ParserSClaim.BEHAVIOR; }

/* input output functions */
"input"		{ return ParserSClaim.INPUT; }
"output"	{ return ParserSClaim.OUTPUT; }
"print"		{ return ParserSClaim.PRINT; }

/* behavior types */
"initial"		{ return ParserSClaim.INITIAL; }
"reactive"		{ return ParserSClaim.REACTIVE; }
"cyclic"		{ return ParserSClaim.CYCLIC; }
"proactive"		{ return ParserSClaim.PROACTIVE; }


/* Constructs that do something based on a condition and their associated keywords */
"if"			{ return ParserSClaim.IF; }
"condition"		{ return ParserSClaim.CONDITION; }
"then"			{ return ParserSClaim.THEN; }
"else"			{ return ParserSClaim.ELSE; }

/* functions to be used in order to communicate - syntax similar to the function calls */
"receive"		{ return ParserSClaim.RECEIVE; }
"send"			{ return ParserSClaim.SEND; }
"message"		{ return ParserSClaim.MESSAGE; }

/* Interaction with the knowledge base - syntax similar to the function calls */
"addK"			{ return ParserSClaim.ADDK; }
"readK"			{ return ParserSClaim.READK; }
"removeK"		{ return ParserSClaim.REMOVEK; }

/* actions that affect the topology of the population of the agents - syntax similar to the function calls */
"in"		{ return ParserSClaim.IN; }
"out"		{ return ParserSClaim.OUT; }
"open"		{ return ParserSClaim.OPEN; }
"acid"		{ return ParserSClaim.ACID; }
"new"		{ return ParserSClaim.NEW; }

/* proactive behavior */
"aGoal"		{ return ParserSClaim.AGOAL; } /* Achievement Goal */
"mGoal"		{ return ParserSClaim.MGOAL; } /* Maintain Goal */
"pGoal"		{ return ParserSClaim.PGOAL; } /* Perform Goal */

/* GOAL KEYWORDS */
"achieve" 	{ return ParserSClaim.ACHIEVE; }
"target"	{ return ParserSClaim.TARGET; }
"maintain"	{ return ParserSClaim.MAINTAIN; }
"action"	{ return ParserSClaim.ACTION; }

/* looping constructs */
"forAllK"	{ return ParserSClaim.FORALLK; }
"while"		{ return ParserSClaim.WHILE; }

/* other keywords - syntax similar to the function calls */
"wait"		{ return ParserSClaim.WAIT; }

"("			{ return (int) yycharat(0); }
")"			{ return (int) yycharat(0); }

{Variable}	{ 
				/* yyparser.yylval = new ParserSClaimVal(yytext().substring(1));*/
				return ParserSClaim.VARIABLE; 
			}
{AffectableVariable}	{ 
				/* yyparser.yylval = new ParserSClaimVal(yytext().substring(1));*/
				return ParserSClaim.AFFECTABLE_VARIABLE; 
			}
{StringLiteral}
			{
				return ParserSClaim.STRING_LITERAL;
			}

{Comment}               { /* ignore */ }

{WhiteSpace}            { /* ignore */ }

{Constant}	{ 
				/* yyparser.yylval = new ParserSClaimVal(yytext());*/
				return ParserSClaim.CONSTANT; 
			}

.			{ /* ignore bad characters */ }

