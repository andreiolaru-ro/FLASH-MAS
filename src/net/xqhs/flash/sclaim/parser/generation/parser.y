/* Parser specification to be used with byacc/j, in order to generate the syntactic analyzer of S-Claim
To generate the Parser, in Linux, run the script "generateParser.sh", included in the directory */

%{
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.UnitComponentExt;
import net.xqhs.util.logging.logging.Logging;
import tatami.core.util.platformUtils.PlatformUtils;
import tatami.sclaim.constructs.basic.*;
%}

%token VARIABLE AFFECTABLE_VARIABLE CONSTANT STRING_LITERAL THIS PARENT
%token STRUCT AGENT BEHAVIOR
%token INITIAL REACTIVE CYCLIC PROACTIVE 
%token RECEIVE SEND MESSAGE 
%token CONDITION FORALLK WHILE
%token ADDK READK REMOVEK
%token IF THEN ELSE
%token INPUT OUTPUT PRINT
%token IN OUT OPEN ACID NEW
%token WAIT
%token AGOAL MGOAL PGOAL
%token ACHIEVE ACTION MAINTAIN TARGET

%start agent_specification
%%

variable
	: VARIABLE
		{
			//log.info("variable -> VARIABLE:"+lexer.yytext().substring(1));
			$$ = new ParserSClaimVal(new ClaimVariable(lexer.yytext().substring(1)));
		}
	| AFFECTABLE_VARIABLE
		{
			//log.info("variable -> AFFECTABLE_VARIABLE:"+lexer.yytext().substring(2));
			$$ = new ParserSClaimVal(new ClaimVariable(lexer.yytext().substring(2),true));
		}

claim_variable
	: variable
		{
			//log.info("claim_variable -> variable");
			$$ = $1;
		}
	| THIS
		{
			//log.info("claim_variable -> THIS");
			$$ = new ParserSClaimVal(new ClaimVariable("this"));
		}
	| PARENT
		{
			//log.info("claim_variable -> PARENT");
			$$ = new ParserSClaimVal(new ClaimVariable("parent",true));
		}
	;

constant
	: STRING_LITERAL
		{
			/*//log.info("constant -> STRING_LITERAL:"+lexer.yytext());*/
			String content = lexer.yytext();
			yyval = new ParserSClaimVal(new ClaimValue(content.substring(1,content.length()-1)));
		}
	|	CONSTANT
		{
			//log.info("constant -> CONSTANT: "+lexer.yytext());
			$$ = new ParserSClaimVal(new ClaimValue(lexer.yytext()));
		}
	;
	

structure_field
	: claim_variable
		{
			//log.info("structure_field -> claim_variable");
			$$ = $1;
		}
	| constant
		{
			//log.info("structure_field -> constant: "+lexer.yytext());
			$$ = $1;
		}
	| structure
		{
			//log.info("structure_field -> structure");
			$$ = $1;
		}
	;

structure_field_list
	: structure_field
		{
			//log.info("structure_field_list -> structure_field");
			$$ = new ParserSClaimVal(new Vector<ClaimConstruct>());
			$$.claimConstructVector.add($1.claimConstruct);
		}
	| structure_field_list structure_field
		{
			//log.info("structure_field_list -> structure_field_list structure_field");
			$$ = $1;
			$$.claimConstructVector.add($2.claimConstruct);
		}
	;
	
structure
	/*: '(' STRUCT ')'
		{
			//log.info("structure -> '(' STRUCT ')'");
			$$ = new ParserSClaimVal(new ClaimStructure(null));
		}
	|*/: '(' STRUCT structure_field_list ')'
		{
			//log.info("structure -> '(' STRUCT structure_field_list ')'");
			$$ = new ParserSClaimVal(new ClaimStructure($3.claimConstructVector));
		}
	;

argument
	: claim_variable
		{
			//log.info("argument -> claim_variable");
			$$ = $1;
		}
	| constant
		{
			//log.info("argument -> constant: "+lexer.yytext());
			$$ = $1;
		}
	| structure
		{
			//log.info("argument -> structure");
			$$ = $1;
		}
	| function
		{
			//log.info("argument -> functions");
			$$ = $1;
		}
	| readK_function
		{
			//log.info("argument -> readK_function");
			$$ = $1;
		}
	;

argument_list
	: argument
		{
			//log.info("argument_list -> argument");
			$$ = new ParserSClaimVal(new Vector<ClaimConstruct>());
			$$.claimConstructVector.add($1.claimConstruct);
		}
	| argument_list argument
		{
			//log.info("argument_list -> argument_list argument");
			$$ = $1;
			$$.claimConstructVector.add($2.claimConstruct);
		}
	;

name
	: CONSTANT
		{
			//log.info("name -> CONSTANT: "+lexer.yytext());
			$$ = new ParserSClaimVal(lexer.yytext());
		}
	;

agent_argument
	: variable
		{
			//log.info("agent_argument -> variable");
			$$ = $1;
		}
	;

agent_argument_list
	: agent_argument
		{
			//log.info("agent_argument_list -> agent_argument");
			
			//register the language variables in the list of agent parameters
			Vector<ClaimConstruct> languageParameters = new Vector<ClaimConstruct>();
			languageParameters.add(new ClaimVariable("this"));
			languageParameters.add(new ClaimVariable("parent",true));
			
			$$ = new ParserSClaimVal(new Vector<ClaimConstruct>(languageParameters));
			$$.claimConstructVector.add($1.claimConstruct);
		}
	| agent_argument_list agent_argument
		{
			//log.info("agent_argument_list -> agent_argument_list agent_argument");
			$$ = $1;
			
			ClaimVariable currentVariable = (ClaimVariable) $2.claimConstruct;
			
			if($$.claimConstructVector.contains(currentVariable)==false)
			{
				ClaimVariable complementaryVariable = currentVariable.getComplement(); //variable complementary in what concerns the affectability
				if($$.claimConstructVector.contains(complementaryVariable)==false)
					$$.claimConstructVector.add(currentVariable);
				else {
					String msg = new String("The variable: "+currentVariable.getName()+" is used both as affectable and as not affectable. ");
					if(currentVariable.getName().equals(new String("parent")))
						yyerror(msg+"The \"parent\" variable belongs to the language and it could be only affectable.");
					else if(currentVariable.getName().equals(new String("this")))
						yyerror(msg+"The \"this\" variable belongs to the language and it could be only not affectable.");
					else
						yyerror(msg);
				}
			}
		}
	;

behavior_type
	: INITIAL
		{
			//log.info("behavior_type -> INITIAL");
			$$ = new ParserSClaimVal(ClaimBehaviorType.INITIAL);
		}
	| REACTIVE
		{
			//log.info("behavior_type -> REACTIVE");
			$$ = new ParserSClaimVal(ClaimBehaviorType.REACTIVE);
		}
	| CYCLIC
		{
			//log.info("behavior_type -> CYCLIC");
			$$ = new ParserSClaimVal(ClaimBehaviorType.CYCLIC);
		}
	| PROACTIVE
		{
			//log.info("behavior_type -> PROACTIVE");
			$$ = new ParserSClaimVal(ClaimBehaviorType.PROACTIVE);
		}
	;

message_structure
	/*: '(' STRUCT ')'
		{
			//log.info("message_structure -> '(' STRUCT ')'");
			$$ = new ParserSClaimVal(new ClaimStructure(null));
		}
	|*/	: '(' STRUCT MESSAGE structure_field_list ')'
		{
			//log.info("message_structure -> '(' STRUCT MESSAGE structure_field_list ')'");
			Vector<ClaimConstruct> structFields = new Vector<ClaimConstruct>();
			structFields.add(new ClaimValue(new String("message")));
			structFields.addAll($4.claimConstructVector);
			$$ = new ParserSClaimVal(new ClaimStructure(structFields));
		}
		| '(' MESSAGE structure_field_list ')'
		{
			//log.info("message_structure -> '(' MESSAGE structure_field_list ')'");
			Vector<ClaimConstruct> structFields = new Vector<ClaimConstruct>();
			structFields.add(new ClaimValue(new String("message")));
			structFields.addAll($3.claimConstructVector);
			$$ = new ParserSClaimVal(new ClaimStructure(structFields));
		}
	;

message_argument /*Argument of a message primitive.*/
	: claim_variable
		{
			//log.info("message_argument -> variable");
			$$ = $1;
		}
	| constant
		{
			//log.info("message_argument -> constant: "+lexer.yytext());
			$$ = $1;
		}
	| message_structure
		{
			//log.info("message_argument -> message_structure");
			$$ = $1;
		}
	;

message_argument_list /*Argument list of a message primitive.*/
	: message_argument
		{
			//log.info("argument_list -> message_argument");
			$$ = new ParserSClaimVal(new Vector<ClaimConstruct>());
			$$.claimConstructVector.add($1.claimConstruct);
		}
	| message_argument_list message_argument
		{
			//log.info("argument_list -> message_argument_list message_argument");
			$$ = $1;
			$$.claimConstructVector.add($2.claimConstruct);
		}
	;

send_function
	: '(' SEND message_argument_list ')'
		{
			//log.info("send_function -> '(' SEND message_argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.SEND ,new String("send"),$3.claimConstructVector));
		}
	;

receive_function
	: '(' RECEIVE message_argument_list ')'
		{
			//log.info("receive_function -> '(' RECEIVE message_argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.RECEIVE ,new String("receive"),$3.claimConstructVector));
		}
	;

input_function
	: '(' INPUT argument_list ')'
		{
			//log.info("in_function -> '(' INPUT argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.INPUT ,new String("input"),$3.claimConstructVector));
		}
	;

output_function
	: '(' OUTPUT argument_list ')'
		{
			//log.info("output_function -> '(' OUTPUT argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.OUTPUT ,new String("output"),$3.claimConstructVector));
		}
	;

print_function
	: '(' PRINT argument_list ')'
		{
			//log.info("print_function -> '(' PRINT argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.PRINT ,new String("print"),$3.claimConstructVector));
		}
	;

in_function
	: '(' IN argument_list ')'
		{
			//log.info("in_function -> '(' IN argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.IN ,new String("in"),$3.claimConstructVector));
		}
	;

out_function
	: '(' OUT argument_list ')'
		{
			//log.info("out_function -> '(' OUT argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.OUT ,new String("out"),$3.claimConstructVector));
		}
	;

open_function
	: '(' OPEN argument_list ')'
		{
			//log.info("open_function -> '(' OPEN argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.OPEN ,new String("open"),$3.claimConstructVector));
		}
	;

acid_function
	: '(' ACID')'
		{
			//log.info("acid_function -> '(' ACID argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.ACID ,new String("acid"),null));
		}
	;

new_function
	: '(' NEW argument_list ')'
		{
			//log.info("new_function -> '(' NEW argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.NEW ,new String("new"),$3.claimConstructVector));
		}
	;

addK_function
	: '(' ADDK argument_list ')'
		{
			//log.info("addK_function -> '(' ADDK argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.ADDK ,new String("addK"),$3.claimConstructVector));
		}
	;

readK_function
	: '(' READK argument_list ')'
		{
			//log.info("readK_function -> '(' READK argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.READK ,new String("readK"),$3.claimConstructVector));
		}
	;

removeK_function
	: '(' REMOVEK argument_list ')'
		{
			//log.info("removeK_function -> '(' REMOVEK argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.REMOVEK ,new String("removeK"),$3.claimConstructVector));
		}
	;

wait_function
	: '(' WAIT argument_list ')'
		{
			//log.info("wait_function -> '(' WAIT argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.WAIT ,new String("wait"),$3.claimConstructVector));
		}
	;

language_function //that can appear anywhere inside the behavior
	: send_function
		{
			//log.info("language_function -> send_function");
			$$ = $1;
		}
/* receive_function
		{
			//log.info("language_function -> receive_function");
			$$ = $1;
		} */
	| output_function
		{
			//log.info("language_function -> output_function");
			$$ = $1;
		}
	| print_function
		{
			//log.info("language_function -> print_function");
			$$ = $1;
		}
	| in_function
		{
			//log.info("language_function -> in_function");
			$$ = $1;
		}
	| out_function
		{
			//log.info("language_function -> out_function");
			$$ = $1;
		}
	| open_function
		{
			//log.info("language_function -> open_function");
			$$ = $1;
		}
	| acid_function
		{
			//log.info("language_function -> acid_function");
			$$ = $1;
		}
	| new_function
		{
			//log.info("language_function -> new_function");
			$$ = $1;
		}
	| addK_function
		{
			//log.info("language_function -> addk_function");
			$$ = $1;
		}
	| readK_function
		{
			//log.info("language_function -> readk_function");
			$$ = $1;
		}
	| removeK_function
		{
			//log.info("language_function -> removek_function");
			$$ = $1;
		}
	| wait_function
		{
			//log.info("language_function -> wait_function");
			$$ = $1;
		}
	;

function
	: '(' name ')'
		{
			//log.info("function -> '(' name ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall(ClaimFunctionType.JAVA,$2.sval,null));
		}
	| '(' name argument_list ')'
		{
			//log.info("function -> '(' name argument_list ')'");
			$$ = new ParserSClaimVal(new ClaimFunctionCall(ClaimFunctionType.JAVA,$2.sval,$3.claimConstructVector));
		}
	;
	
goal
	: '(' AGOAL name argument ACHIEVE proposition_list ')'
		{
			//log.info("goal -> '(' AGOAL name proposition_list ')'");
			//$$ = new ParserSClaimVal(new ClaimaGoal(ClaimConstructType.AGOAL, $3.sval, $6.claimConstructVector, $4.ival));
		}
	| '(' MGOAL name priority MAINTAIN proposition_list TARGET proposition_list ')'
		{
			//$$ = new ParserSClaimVal(new ClaimmGoal(ClaimConstructType.MGOAL, $3.sval, $6.claimConstructVector, $8.claimConstructVector, Integer.parseInt(((ClaimValue) $4.obj).toString())));	
		}
	| '(' MGOAL name priority MAINTAIN proposition_list ')'
		{
			//$$ = $$ = new ParserSClaimVal(new ClaimmGoal(ClaimConstructType.MGOAL, $3.sval, $6.claimConstructVector, null, Integer.parseInt(((ClaimValue) $4.obj).toString())));
		}
	| '(' PGOAL name priority ACTION proposition ')'
		{
			//$$ = new ParserSClaimVal(new ClaimpGoal(ClaimConstructType.PGOAL, $3.sval, $6.claimConstruct, Integer.parseInt(((ClaimValue) $4.obj).toString())));
		}
	;
	
proposition_list
	: proposition
		{
			//log.info("proposition_list -> proposition");
			$$ = new ParserSClaimVal(new Vector<ClaimConstruct>());
			$$.claimConstructVector.add($1.claimConstruct);
		}
	| proposition_list proposition
		{
			//log.info("proposition_list -> proposition_list proposition");
			$$ = $1;
			$$.claimConstructVector.add($2.claimConstruct);
		}
	;
	
proposition
	: constant
		{
			//log.info("proposition -> constant ");
			$$ = $1;
		}
	;
	
priority
	: constant
		{
			//log.info("priority -> constant");
			$$ = $1;
		}
	;

	
valid_condition
	: readK_function
		{
			//log.info("valid_condition -> readK_function");
			$$ = $1;
		}
	| function
		{
			//log.info("valid_condition -> function");
			$$ = $1;
		}
	;

condition
	: '(' CONDITION valid_condition ')'
		{
			//log.info("condition -> '(' CONDITION valid_condition ')'");
			$$ = new ParserSClaimVal(new ClaimCondition((ClaimFunctionCall) $3.claimConstruct));
		}
	;

if_stmt
	: '(' IF valid_condition THEN behavior_content_list ')'
		{
			//log.info("if_stmt -> '(' IF if_valid_condition THEN behavior_content_list ')'");
			$$ = new ParserSClaimVal(new ClaimIf((ClaimFunctionCall) $3.claimConstruct,$5.claimConstructVector, null));
		}
	| '(' IF valid_condition THEN behavior_content_list ELSE behavior_content_list ')'
		{
			//log.info("if_stmt -> '(' IF if_valid_condition THEN behavior_content_list ELSE behavior_content_list ')'");
			$$ = new ParserSClaimVal(new ClaimIf((ClaimFunctionCall) $3.claimConstruct,$5.claimConstructVector,$7.claimConstructVector));
		}
	;

forAllK
	: '(' FORALLK structure ')'
		{
			//log.info("forAllK -> '(' FORALLK structure ')'");
			if (!verifyVariablesInStructure((ClaimStructure) $3.claimConstruct))
				yywarn("there is no variable in the structure of forAllK");
			$$ = new ParserSClaimVal(new ClaimForAllK((ClaimStructure) $3.claimConstruct, null));
		}
	| '(' FORALLK structure behavior_content_list ')'
		{
			//log.info("forAllK -> '(' FORALLK structure behavior_content_list ')'");
			if (!verifyVariablesInStructure((ClaimStructure) $3.claimConstruct))
				yywarn("there is no variable in the structure of forAllK");
			$$ = new ParserSClaimVal(new ClaimForAllK((ClaimStructure) $3.claimConstruct, $4.claimConstructVector));
		}
	;

while
	: '(' WHILE function ')'
		{
			//log.info("while -> '(' WHILE function ')'");
			$$ = new ParserSClaimVal(new ClaimWhile((ClaimFunctionCall) $3.claimConstruct, null));
		}
	| '(' WHILE function behavior_content_list ')'
		{
			//log.info("while -> '(' WHILE function behavior_content_list ')'");
			$$ = new ParserSClaimVal(new ClaimWhile((ClaimFunctionCall) $3.claimConstruct, $4.claimConstructVector));
		}
	;

behavior_content_header
	: receive_function
		{
			//log.info("behavior_content_header -> receive_function");
			$$ = $1;
		}
	| input_function
		{
			//log.info("behavior_content_header -> receive_function");
			$$ = $1;
		}
	| condition
		{
			//log.info("behavior_content_header -> condition");
			$$ = $1;
		}
	;

behavior_content_header_list
	: behavior_content_header
		{
			//log.info("behavior_content_header_list -> behavior_content_header");
			$$ = new ParserSClaimVal(new Vector<ClaimConstruct>());
			$$.claimConstructVector.add($1.claimConstruct);
		}
	| behavior_content_header_list behavior_content_header
		{
			//log.info("behavior_content_header_list -> behavior_content_header_list behavior_content_header");
			$$ = $1;
			$$.claimConstructVector.add($2.claimConstruct);
		}
	;


behavior_content
	: language_function
		{
			//log.info("behavior_content -> language_function");
			$$ = $1;
		}
	| function
		{
			//log.info("behavior_content -> function");
			$$ = $1;
		}
	| if_stmt
		{
			//log.info("behavior_content -> if_stmt");
			$$ = $1;
		}
	| forAllK
		{
			//log.info("behavior_content -> forAllK");
			$$ = $1;
		}
	| while
		{
			//log.info("behavior_content -> while");
			$$ = $1;
		}
	| goal
		{
			$$ = $1;
		}
	;
	
behavior_content_list
	: behavior_content
		{
			//log.info("behavior_content_list -> behavior_content");
			$$ = new ParserSClaimVal(new Vector<ClaimConstruct>());
			$$.claimConstructVector.add($1.claimConstruct);
		}
	| behavior_content_list behavior_content
		{
			//log.info("behavior_content_list -> behavior_content_list behavior_content");
			$$ = $1;
			$$.claimConstructVector.add($2.claimConstruct);
		}
	;

behavior
/*	: '(' behavior_type name ')'
		{
			//log.info("behavior -> '(' behavior_type name ')'");
			$$ = new ParserSClaimVal(new ClaimBehaviorDefinition($3.sval, $2.claimBehaviorType, null));
		}
	|*/: '(' behavior_type name behavior_content_list ')'
		{
			//log.info("behavior -> '(' behavior_type name behavior_content_list ')'");
			$$ = new ParserSClaimVal(new ClaimBehaviorDefinition($3.sval, $2.claimBehaviorType, $4.claimConstructVector));
		}
	| '(' behavior_type name behavior_content_header_list ')'
		{
			//log.info("behavior -> '(' behavior_type name behavior_content_header_list ')'");
			$$ = new ParserSClaimVal(new ClaimBehaviorDefinition($3.sval, $2.claimBehaviorType, $4.claimConstructVector));
		}
	| '(' behavior_type name behavior_content_header_list behavior_content_list ')'
		{
			//log.info("behavior -> '(' behavior_type name behavior_content_header behavior_content_list ')'");
			Boolean concatenateSucceeded = $4.claimConstructVector.addAll($5.claimConstructVector);
			if(concatenateSucceeded)
				$$ = new ParserSClaimVal(new ClaimBehaviorDefinition($3.sval, $2.claimBehaviorType, $4.claimConstructVector));
			else
				log.error("error while concatenating the vectors of statements, while creating the behavior");
		}
	;

behavior_list
	: behavior
		{
			//log.info("behavior_list -> behavior");
			$$ = new ParserSClaimVal(new Vector<ClaimConstruct>());
			$$.claimConstructVector.add($1.claimConstruct);
		}
	| behavior_list behavior
		{
			//log.info("behavior_list -> behavior_list behavior");
			$$ = $1;
			$$.claimConstructVector.add($2.claimConstruct);
		}
	;

behaviors_declaration
	: '(' BEHAVIOR behavior_list ')'
		{
			//log.info("behavior_declaration -> '(' BEHAVIOR behavior_list ')'");
			$$ = $3;
		}
	;

agent_specification
	: '(' AGENT name agent_argument_list behaviors_declaration ')'
		{
			//log.info("agent_specification -> '(' AGENT name agent_argument_list behaviors_declaration ')'");
			parsedAgent = new ClaimAgentDefinition($3.sval, 
					new Vector<ClaimConstruct>(val_peek(2).claimConstructVector),
					new Vector<ClaimBehaviorDefinition>(
							Arrays.asList($5.claimConstructVector.toArray(new ClaimBehaviorDefinition [0]))
					));
			//Set the references of the contained behaviors to this agent:
			for(ClaimBehaviorDefinition currentBehavior:parsedAgent.getBehaviors())
				currentBehavior.setMyAgent(parsedAgent);
		}
	| '(' AGENT name behaviors_declaration ')'
		{
			//log.info("agent_specification -> '(' AGENT name behaviors_declaration ')'");
			
			//register the language variables in the list of agent parameters
			Vector<ClaimConstruct> languageParameters = new Vector<ClaimConstruct>();
			languageParameters.add(new ClaimVariable("this"));
			languageParameters.add(new ClaimVariable("parent",true));
			
			parsedAgent = new ClaimAgentDefinition($3.sval,languageParameters,
					new Vector<ClaimBehaviorDefinition>(
							Arrays.asList($4.claimConstructVector.toArray(new ClaimBehaviorDefinition [0]))
					));
			//Set the references of the contained behaviors to this agent:
			for(ClaimBehaviorDefinition currentBehavior:parsedAgent.getBehaviors())
				currentBehavior.setMyAgent(parsedAgent);
		}
	;

%%
private static String unitName = "parser";

/** the logger */
public Logger log = (UnitComponentExt) new UnitComponentExt().setUnitName(unitName).setLoggerType(
						PlatformUtils.platformLogType());

/** a reference to the agent structure returned by the parser */
public ClaimAgentDefinition parsedAgent;

/** a reference to the lexer object */
private Yylex lexer;

/** interface to the lexer */
private int yylex () {
  int yyl_return = -1;
  try {
    yyl_return = lexer.yylex();
  }
  catch (IOException e) {
    System.err.println("IO error :"+e);
  }
  return yyl_return;
}

/**
 * Verifies if the structure given as argument, or any of its substructures, has at least one variable.
 * This function is useful in the case of statements like ForAllk, where passing a structure 
 * without any variable doesn't make sense.
 * 
 * @param structure - the structure to be verified
 */
public boolean verifyVariablesInStructure(ClaimStructure structure)
{
	for (ClaimConstruct currentField:structure.getFields())
	{
		switch(currentField.getType())
		{
		case VARIABLE:
			return true;
		case STRUCTURE:
			if(verifyVariablesInStructure((ClaimStructure) currentField))
				return true;
			break;
		default:
			break;	
		}
	}
	return false;
}

/** error reporting */
public void yyerror (String error) {
  log.error("Error "/*+lexer.getLine()*/+": " + error);
  System.exit(1);
}

/** warning reporting */
public void yywarn (String warning) {
  log.warn("Warning "/*+lexer.getLine()*/+": " + warning);
}

/** constructor which receives a String with the file name and path as argument 
 * @throws FileNotFoundException */
public ParserSClaim(String filePathAndName) {
  try {
	if (filePathAndName==null)
		throw new Exception();
	lexer = new Yylex(new FileReader(filePathAndName), this);
  } catch (FileNotFoundException e) {
	log.error("The file specified as argument could not be opened. Make sure that you have correctly written the name and the path!");
	Logging.exitLogger(unitName);
  }
  catch (Exception e)
  {
	log.error("The name of the file to be parsed was not specified.");
    Logging.exitLogger(unitName);
  }
}

/** a way to use the parser - main function 
 * @throws FileNotFoundException */
public static void main(String args[]) {
  ParserSClaim yyparser;
  if(args.length>0)
  {
	yyparser = new ParserSClaim(args[0]);
    yyparser.parse();
  }
  else
  {
	System.out.println("No argument was specified. The file name to be parsed together with its path are needed.");
  }
}

/** 
 * a way to use the parser - inside the code
 */
public ClaimAgentDefinition parse() {
  int parsingResult = yyparse();

  if(parsingResult == 0) {
    log.info("Parsing successfully finished! The agent with the class \""+parsedAgent.getClassName()+"\" is ready to be run.");
    Logging.exitLogger(unitName);
    return parsedAgent;
  }
  else {
    Logging.exitLogger(unitName);
    return null;
  }
}