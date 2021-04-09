/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
//### This file created by BYACC 1.8(/Java extension  1.15)
//### Java capabilities added 7 Jan 97, Bob Jamison
//### Updated : 27 Nov 97  -- Bob Jamison, Joe Nieten
//###           01 Jan 98  -- Bob Jamison -- fixed generic semantic constructor
//###           01 Jun 99  -- Bob Jamison -- added Runnable support
//###           06 Aug 00  -- Bob Jamison -- made state variables class-global
//###           03 Jan 01  -- Bob Jamison -- improved flags, tracing
//###           16 May 01  -- Bob Jamison -- added custom stack sizing
//###           04 Mar 02  -- Yuval Oren  -- improved java performance, added options
//###           14 Mar 02  -- Tomas Hurka -- -d support, static initializer workaround
//### Please send bug reports to tom@hukatronic.cz
//### static char yysccsid[] = "@(#)yaccpar	1.8 (Berkeley) 01/20/90";



package net.xqhs.flash.sclaim.parser.generation;



//#line 5 "parser.y"
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.sclaim.constructs.ClaimAgentDefinition;
import net.xqhs.flash.sclaim.constructs.ClaimBehaviorDefinition;
import net.xqhs.flash.sclaim.constructs.ClaimBehaviorType;
import net.xqhs.flash.sclaim.constructs.ClaimCondition;
import net.xqhs.flash.sclaim.constructs.ClaimConstruct;
import net.xqhs.flash.sclaim.constructs.ClaimForAllK;
import net.xqhs.flash.sclaim.constructs.ClaimFunctionCall;
import net.xqhs.flash.sclaim.constructs.ClaimFunctionType;
import net.xqhs.flash.sclaim.constructs.ClaimIf;
import net.xqhs.flash.sclaim.constructs.ClaimStructure;
import net.xqhs.flash.sclaim.constructs.ClaimValue;
import net.xqhs.flash.sclaim.constructs.ClaimVariable;
import net.xqhs.flash.sclaim.constructs.ClaimWhile;
import net.xqhs.util.logging.LoggerClassic;
import net.xqhs.util.logging.UnitComponentExt;




public class ParserSClaim
{

boolean yydebug;        //do I want debug output?
int yynerrs;            //number of errors so far
int yyerrflag;          //was there an error?
int yychar;             //the current working character

//########## MESSAGES ##########
//###############################################################
// method: debug
//###############################################################
void debug(String msg)
{
  if (yydebug)
    System.out.println(msg);
}

//########## STATE STACK ##########
final static int YYSTACKSIZE = 500;  //maximum stack size
int statestk[] = new int[YYSTACKSIZE]; //state stack
int stateptr;
int stateptrmax;                     //highest index of stackptr
int statemax;                        //state when highest index reached
//###############################################################
// methods: state stack push,pop,drop,peek
//###############################################################
final void state_push(int state)
{
  try {
		stateptr++;
		statestk[stateptr]=state;
	 }
	 catch (ArrayIndexOutOfBoundsException e) {
     int oldsize = statestk.length;
     int newsize = oldsize * 2;
     int[] newstack = new int[newsize];
     System.arraycopy(statestk,0,newstack,0,oldsize);
     statestk = newstack;
     statestk[stateptr]=state;
  }
}
final int state_pop()
{
  return statestk[stateptr--];
}
final void state_drop(int cnt)
{
  stateptr -= cnt; 
}
final int state_peek(int relative)
{
  return statestk[stateptr-relative];
}
//###############################################################
// method: init_stacks : allocate and prepare stacks
//###############################################################
final boolean init_stacks()
{
  stateptr = -1;
  val_init();
  return true;
}
//###############################################################
// method: dump_stacks : show n levels of the stacks
//###############################################################
void dump_stacks(int count)
{
int i;
  System.out.println("=index==state====value=     s:"+stateptr+"  v:"+valptr);
  for (i=0;i<count;i++)
    System.out.println(" "+i+"    "+statestk[i]+"      "+valstk[i]);
  System.out.println("======================");
}


//########## SEMANTIC VALUES ##########
//## **user defined:ParserSClaimVal
String   yytext;//user variable to return contextual strings
ParserSClaimVal yyval; //used to return semantic vals from action routines
ParserSClaimVal yylval;//the 'lval' (result) I got from yylex()
ParserSClaimVal valstk[] = new ParserSClaimVal[YYSTACKSIZE];
int valptr;
//###############################################################
// methods: value stack push,pop,drop,peek.
//###############################################################
final void val_init()
{
  yyval=new ParserSClaimVal();
  yylval=new ParserSClaimVal();
  valptr=-1;
}
final void val_push(ParserSClaimVal val)
{
  try {
    valptr++;
    valstk[valptr]=val;
  }
  catch (ArrayIndexOutOfBoundsException e) {
    int oldsize = valstk.length;
    int newsize = oldsize*2;
    ParserSClaimVal[] newstack = new ParserSClaimVal[newsize];
    System.arraycopy(valstk,0,newstack,0,oldsize);
    valstk = newstack;
    valstk[valptr]=val;
  }
}
final ParserSClaimVal val_pop()
{
  return valstk[valptr--];
}
final void val_drop(int cnt)
{
  valptr -= cnt;
}
final ParserSClaimVal val_peek(int relative)
{
  return valstk[valptr-relative];
}
final ParserSClaimVal dup_yyval(ParserSClaimVal val)
{
  return val;
}
//#### end semantic value section ####
public final static short VARIABLE=257;
public final static short AFFECTABLE_VARIABLE=258;
public final static short CONSTANT=259;
public final static short STRING_LITERAL=260;
public final static short THIS=261;
public final static short PARENT=262;
public final static short STRUCT=263;
public final static short AGENT=264;
public final static short BEHAVIOR=265;
public final static short INITIAL=266;
public final static short REACTIVE=267;
public final static short CYCLIC=268;
public final static short PROACTIVE=269;
public final static short RECEIVE=270;
public final static short SEND=271;
public final static short MESSAGE=272;
public final static short CONDITION=273;
public final static short FORALLK=274;
public final static short WHILE=275;
public final static short ADDK=276;
public final static short READK=277;
public final static short REMOVEK=278;
public final static short IF=279;
public final static short THEN=280;
public final static short ELSE=281;
public final static short INPUT=282;
public final static short OUTPUT=283;
public final static short PRINT=284;
public final static short IN=285;
public final static short OUT=286;
public final static short OPEN=287;
public final static short ACID=288;
public final static short NEW=289;
public final static short WAIT=290;
public final static short AGOAL=291;
public final static short MGOAL=292;
public final static short PGOAL=293;
public final static short ACHIEVE=294;
public final static short ACTION=295;
public final static short MAINTAIN=296;
public final static short TARGET=297;
public final static short YYERRCODE=256;
final static short yylhs[] = {                           -1,
    1,    1,    2,    2,    2,    3,    3,    4,    4,    4,
    6,    6,    5,    7,    7,    7,    7,    7,   10,   10,
   11,   12,   13,   13,   14,   14,   14,   14,   15,   15,
   16,   16,   16,   17,   17,   18,   19,   20,   21,   22,
   23,   24,   25,   26,   27,   28,    9,   29,   30,   31,
   31,   31,   31,   31,   31,   31,   31,   31,   31,   31,
   31,    8,    8,   32,   32,   32,   32,   33,   33,   35,
   34,   36,   36,   37,   38,   38,   40,   40,   41,   41,
   42,   42,   42,   43,   43,   44,   44,   44,   44,   44,
   44,   39,   39,   45,   45,   45,   46,   46,   47,    0,
    0,
};
final static short yylen[] = {                            2,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    2,    4,    1,    1,    1,    1,    1,    1,    2,
    1,    1,    1,    2,    1,    1,    1,    1,    5,    4,
    1,    1,    1,    1,    2,    4,    4,    4,    4,    4,
    4,    4,    4,    3,    4,    4,    4,    4,    4,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    3,    4,    7,    9,    7,    7,    1,    2,    1,
    1,    1,    1,    4,    6,    8,    4,    5,    4,    5,
    1,    1,    1,    1,    2,    1,    1,    1,    1,    1,
    1,    1,    2,    5,    5,    6,    1,    2,    4,    6,
    5,
};
final static short yydefred[] = {                         0,
    0,    0,    0,   21,    0,    1,    2,    0,   22,   23,
    0,    0,    0,   24,    0,  101,    0,   97,    0,  100,
   25,   26,   27,   28,    0,   99,   98,    0,    0,   87,
   59,   50,   81,   82,   51,   52,   53,   54,   55,   56,
   57,   58,   60,   61,   86,   91,   83,   88,    0,   89,
   90,   84,    0,   92,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,   94,   93,   95,
    0,   85,    7,    6,    4,    5,    0,    3,   31,   32,
   33,   34,    0,    0,    0,   73,   72,    0,    0,    0,
    0,    0,    0,   14,   15,   16,   19,   17,   18,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,   44,
    0,    0,    0,    0,    0,   62,    0,   96,    0,    0,
   37,   35,   36,   74,    0,   77,    0,   79,    0,   46,
   20,   47,   48,    0,   38,   39,   40,   41,   42,   43,
   45,   49,    0,   71,    0,    0,   63,    0,    8,    9,
   11,   10,    0,    0,   78,   80,    0,    0,    0,    0,
    0,   30,   12,   13,    0,   75,   70,    0,   68,    0,
    0,   29,    0,   64,   69,    0,   66,   67,   76,    0,
   65,
};
final static short yydgoto[] = {                          2,
   88,  104,  105,  161,  106,  163,  107,  108,  109,  110,
   76,   10,   11,   25,   91,   92,   93,   32,   33,   34,
   35,   36,   37,   38,   39,   40,   41,   42,   43,   44,
   45,   46,  178,  155,  179,   98,   47,   48,   49,   50,
   51,   52,   53,   54,   18,   19,   12,
};
final static short yysindex[] = {                       -33,
 -252,    0, -222,    0,   97,    0,    0, -216,    0,    0,
   97,   15,   18,    0,   46,    0, -187,    0,  -23,    0,
    0,    0,    0,    0, -222,    0,    0,   54,  -77,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    5,    0,
    0,    0,   23,    0,   79,   79,   60,   63,   69,   85,
   85,   85,   60,   85,   85,   85,   85,   85,   85,   71,
   85,   85, -222, -222, -222,  -37,  129,    0,    0,    0,
   27,    0,    0,    0,    0,    0, -233,    0,    0,    0,
    0,    0,  -31,  -25, -253,    0,    0,   86, -131,   35,
 -222,   45, -234,    0,    0,    0,    0,    0,    0,  -14,
   -8,    1, -147,    7,   13,   19,   25,   31,   37,    0,
   43,   49,   85, -168, -168,    0,   55,    0, -127,   91,
    0,    0,    0,    0,   91,    0,   57,    0,   77,    0,
    0,    0,    0,  101,    0,    0,    0,    0,    0,    0,
    0,    0, -148,    0, -144, -142,    0,   91,    0,    0,
    0,    0,   61,   67,    0,    0,  -39, -168, -168, -168,
   73,    0,    0,    0,  101,    0,    0,  -21,    0,  -41,
  113,    0,   83,    0,    0, -168,    0,    0,    0,  -19,
    0,
};
final static short yyrindex[] = {                         0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,
};
final static short yygindex[] = {                         0,
   39,  -42,  -20, -102,  -24, -130,  248,  328,  329,  112,
  117,  144,    0,    0,    0,   42,  103,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0, -146,   32, -159,   98,    0,    0,  -45,    0,
    0,  109,    0,  -11,  145,    0,  152,
};
final static int YYTABLESIZE=512;
static short yytable[];
static { yytable();}
static void yytable(){
yytable = new short[]{                        187,
   77,  176,  103,  126,  164,    4,    1,   81,   87,  131,
  181,    3,   89,   89,   87,  133,   17,   26,  185,  184,
  185,  191,  180,   61,    4,  103,  140,  171,  135,  129,
  185,  103,  142,  100,   90,   90,    4,   79,  130,  190,
  103,  143,   61,    9,   77,   78,  103,  145,   13,    9,
   89,   89,  103,  146,  137,   16,  139,   17,  103,  147,
  173,  173,   29,   80,  103,  148,   77,  128,  173,   79,
  103,  149,   90,   90,   77,  136,  103,  150,   21,   22,
   23,   24,  103,  151,   77,  138,   20,  159,  103,  152,
   83,   84,  159,   29,  103,  157,   77,  165,  167,   95,
   99,  172,   99,  154,  154,  162,   99,  174,  101,  160,
  162,  120,   99,  182,  160,  159,   77,  166,   87,    5,
  159,  159,   77,  189,  103,   79,  134,   79,  159,  183,
   99,  135,  144,  162,  132,  132,    8,  160,  162,  162,
   77,   28,  160,  160,  158,  168,  162,  177,  177,  177,
  160,  169,  170,  188,   14,   79,  156,  177,   94,  177,
  113,   82,   15,   27,    0,  177,    0,    0,    0,  177,
    0,   79,  111,  112,    0,  114,  115,  116,  117,  118,
  119,    4,  121,  122,    0,    0,    0,  127,    0,  123,
  124,  125,   55,   56,    0,   57,   58,   59,   60,   61,
   62,   63,    0,    0,   64,   65,   66,   67,   68,   69,
   70,   71,   72,   73,   74,   75,    0,   83,   84,    6,
    7,   83,   84,   85,   86,    6,    7,   83,   84,   85,
   86,    6,    7,   83,   84,   85,   86,   83,   84,   83,
   84,  175,    6,    7,   83,   84,   85,   86,    6,    7,
   83,   84,   85,   86,    0,  186,    0,    6,    7,   83,
   84,   85,   86,    6,    7,   83,   84,   85,   86,    6,
    7,   83,   84,   85,   86,    6,    7,   83,   84,   85,
   86,    6,    7,   83,   84,   85,   86,    6,    7,   83,
   84,   85,   86,    6,    7,   83,   84,   85,   86,    6,
    7,   83,   84,   85,   86,    6,    7,   83,   84,   85,
   86,    6,    7,   83,   84,   85,   86,    6,    7,   83,
   84,   85,   86,    6,    7,   83,   84,   85,   86,    6,
    7,   83,   84,   85,   86,    6,    7,   83,   84,   85,
   86,    6,    7,   83,   84,   85,   86,    6,    7,   83,
   84,   85,   86,    6,    7,   30,   31,  141,  141,  141,
    0,  141,  141,  141,  141,  141,  141,    0,  141,  141,
  153,    0,    0,    0,  141,    0,   30,   31,    0,    0,
   30,   31,    0,    0,   96,   97,  102,    4,    0,    0,
   96,   97,    0,    0,    0,    0,    0,    0,    0,   56,
    0,    0,   58,   59,   60,   61,   62,   63,   30,   31,
    0,   65,   66,   67,   68,   69,   70,   71,   72,   73,
   74,   75,    0,    0,    0,    0,    0,   30,   31,   30,
   31,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,   30,   31,   30,   31,    0,    0,
    0,   30,   31,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,   30,   31,    0,    0,    0,    0,
    0,    0,   30,   31,    0,    0,    0,    0,    0,    0,
   30,   31,
};
}
static short yycheck[];
static { yycheck(); }
static void yycheck() {
yycheck = new short[] {                         41,
   40,   41,   40,   41,  135,  259,   40,   53,   40,   41,
  170,  264,   55,   56,   40,   41,   40,   41,  178,   41,
  180,   41,  169,  277,  259,   40,   41,  158,  263,  263,
  190,   40,   41,   58,   55,   56,  259,   49,  272,  186,
   40,   41,  277,    5,   40,   41,   40,   41,  265,   11,
   93,   94,   40,   41,  100,   41,  102,   40,   40,   41,
  163,  164,   40,   41,   40,   41,   40,   41,  171,   81,
   40,   41,   93,   94,   40,   41,   40,   41,  266,  267,
  268,  269,   40,   41,   40,   41,   41,  130,   40,   41,
  259,  260,  135,   40,   40,   41,   40,   41,  144,   40,
   40,   41,   40,  124,  125,  130,   40,   41,   40,  130,
  135,   41,   40,   41,  135,  158,   40,   41,   40,    3,
  163,  164,   40,   41,   40,  137,   41,  139,  171,  175,
   40,  263,  280,  158,   93,   94,   40,  158,  163,  164,
   40,   25,  163,  164,  272,  294,  171,  168,  169,  170,
  171,  296,  295,   41,   11,  167,  125,  178,   56,  180,
   63,   53,   11,   19,   -1,  186,   -1,   -1,   -1,  190,
   -1,  183,   61,   62,   -1,   64,   65,   66,   67,   68,
   69,  259,   71,   72,   -1,   -1,   -1,   76,   -1,   73,
   74,   75,  270,  271,   -1,  273,  274,  275,  276,  277,
  278,  279,   -1,   -1,  282,  283,  284,  285,  286,  287,
  288,  289,  290,  291,  292,  293,   -1,  259,  260,  257,
  258,  259,  260,  261,  262,  257,  258,  259,  260,  261,
  262,  257,  258,  259,  260,  261,  262,  259,  260,  259,
  260,  281,  257,  258,  259,  260,  261,  262,  257,  258,
  259,  260,  261,  262,   -1,  297,   -1,  257,  258,  259,
  260,  261,  262,  257,  258,  259,  260,  261,  262,  257,
  258,  259,  260,  261,  262,  257,  258,  259,  260,  261,
  262,  257,  258,  259,  260,  261,  262,  257,  258,  259,
  260,  261,  262,  257,  258,  259,  260,  261,  262,  257,
  258,  259,  260,  261,  262,  257,  258,  259,  260,  261,
  262,  257,  258,  259,  260,  261,  262,  257,  258,  259,
  260,  261,  262,  257,  258,  259,  260,  261,  262,  257,
  258,  259,  260,  261,  262,  257,  258,  259,  260,  261,
  262,  257,  258,  259,  260,  261,  262,  257,  258,  259,
  260,  261,  262,  257,  258,   28,   28,  110,  111,  112,
   -1,  114,  115,  116,  117,  118,  119,   -1,  121,  122,
  123,   -1,   -1,   -1,  127,   -1,   49,   49,   -1,   -1,
   53,   53,   -1,   -1,   57,   57,   59,  259,   -1,   -1,
   63,   63,   -1,   -1,   -1,   -1,   -1,   -1,   -1,  271,
   -1,   -1,  274,  275,  276,  277,  278,  279,   81,   81,
   -1,  283,  284,  285,  286,  287,  288,  289,  290,  291,
  292,  293,   -1,   -1,   -1,   -1,   -1,  100,  100,  102,
  102,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
   -1,   -1,   -1,   -1,  137,  137,  139,  139,   -1,   -1,
   -1,  144,  144,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
   -1,   -1,   -1,   -1,  167,  167,   -1,   -1,   -1,   -1,
   -1,   -1,  175,  175,   -1,   -1,   -1,   -1,   -1,   -1,
  183,  183,
};
}
final static short YYFINAL=2;
final static short YYMAXTOKEN=297;
final static String yyname[] = {
"end-of-file",null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,"'('","')'",null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,"VARIABLE","AFFECTABLE_VARIABLE","CONSTANT",
"STRING_LITERAL","THIS","PARENT","STRUCT","AGENT","BEHAVIOR","INITIAL",
"REACTIVE","CYCLIC","PROACTIVE","RECEIVE","SEND","MESSAGE","CONDITION",
"FORALLK","WHILE","ADDK","READK","REMOVEK","IF","THEN","ELSE","INPUT","OUTPUT",
"PRINT","IN","OUT","OPEN","ACID","NEW","WAIT","AGOAL","MGOAL","PGOAL","ACHIEVE",
"ACTION","MAINTAIN","TARGET",
};
final static String yyrule[] = {
"$accept : agent_specification",
"variable : VARIABLE",
"variable : AFFECTABLE_VARIABLE",
"claim_variable : variable",
"claim_variable : THIS",
"claim_variable : PARENT",
"constant : STRING_LITERAL",
"constant : CONSTANT",
"structure_field : claim_variable",
"structure_field : constant",
"structure_field : structure",
"structure_field_list : structure_field",
"structure_field_list : structure_field_list structure_field",
"structure : '(' STRUCT structure_field_list ')'",
"argument : claim_variable",
"argument : constant",
"argument : structure",
"argument : function",
"argument : readK_function",
"argument_list : argument",
"argument_list : argument_list argument",
"name : CONSTANT",
"agent_argument : variable",
"agent_argument_list : agent_argument",
"agent_argument_list : agent_argument_list agent_argument",
"behavior_type : INITIAL",
"behavior_type : REACTIVE",
"behavior_type : CYCLIC",
"behavior_type : PROACTIVE",
"message_structure : '(' STRUCT MESSAGE structure_field_list ')'",
"message_structure : '(' MESSAGE structure_field_list ')'",
"message_argument : claim_variable",
"message_argument : constant",
"message_argument : message_structure",
"message_argument_list : message_argument",
"message_argument_list : message_argument_list message_argument",
"send_function : '(' SEND message_argument_list ')'",
"receive_function : '(' RECEIVE message_argument_list ')'",
"input_function : '(' INPUT argument_list ')'",
"output_function : '(' OUTPUT argument_list ')'",
"print_function : '(' PRINT argument_list ')'",
"in_function : '(' IN argument_list ')'",
"out_function : '(' OUT argument_list ')'",
"open_function : '(' OPEN argument_list ')'",
"acid_function : '(' ACID ')'",
"new_function : '(' NEW argument_list ')'",
"addK_function : '(' ADDK argument_list ')'",
"readK_function : '(' READK argument_list ')'",
"removeK_function : '(' REMOVEK argument_list ')'",
"wait_function : '(' WAIT argument_list ')'",
"language_function : send_function",
"language_function : output_function",
"language_function : print_function",
"language_function : in_function",
"language_function : out_function",
"language_function : open_function",
"language_function : acid_function",
"language_function : new_function",
"language_function : addK_function",
"language_function : readK_function",
"language_function : removeK_function",
"language_function : wait_function",
"function : '(' name ')'",
"function : '(' name argument_list ')'",
"goal : '(' AGOAL name argument ACHIEVE proposition_list ')'",
"goal : '(' MGOAL name priority MAINTAIN proposition_list TARGET proposition_list ')'",
"goal : '(' MGOAL name priority MAINTAIN proposition_list ')'",
"goal : '(' PGOAL name priority ACTION proposition ')'",
"proposition_list : proposition",
"proposition_list : proposition_list proposition",
"proposition : constant",
"priority : constant",
"valid_condition : readK_function",
"valid_condition : function",
"condition : '(' CONDITION valid_condition ')'",
"if_stmt : '(' IF valid_condition THEN behavior_content_list ')'",
"if_stmt : '(' IF valid_condition THEN behavior_content_list ELSE behavior_content_list ')'",
"forAllK : '(' FORALLK structure ')'",
"forAllK : '(' FORALLK structure behavior_content_list ')'",
"while : '(' WHILE function ')'",
"while : '(' WHILE function behavior_content_list ')'",
"behavior_content_header : receive_function",
"behavior_content_header : input_function",
"behavior_content_header : condition",
"behavior_content_header_list : behavior_content_header",
"behavior_content_header_list : behavior_content_header_list behavior_content_header",
"behavior_content : language_function",
"behavior_content : function",
"behavior_content : if_stmt",
"behavior_content : forAllK",
"behavior_content : while",
"behavior_content : goal",
"behavior_content_list : behavior_content",
"behavior_content_list : behavior_content_list behavior_content",
"behavior : '(' behavior_type name behavior_content_list ')'",
"behavior : '(' behavior_type name behavior_content_header_list ')'",
"behavior : '(' behavior_type name behavior_content_header_list behavior_content_list ')'",
"behavior_list : behavior",
"behavior_list : behavior_list behavior",
"behaviors_declaration : '(' BEHAVIOR behavior_list ')'",
"agent_specification : '(' AGENT name agent_argument_list behaviors_declaration ')'",
"agent_specification : '(' AGENT name behaviors_declaration ')'",
};

//#line 776 "parser.y"
private static String unitName = "parser";

/** the logger */
public LoggerClassic log = (LoggerClassic) new UnitComponentExt(unitName)
		.setLoggerType(PlatformUtils.platformLogType());

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
	// FIXME: need log exit.
  }
  catch (Exception e)
  {
	log.error("The name of the file to be parsed was not specified.");
	// FIXME: need log exit.
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
	// FIXME: need log exit.
    return parsedAgent;
  }
  else {
		// FIXME: need log exit.
    return null;
  }
}
//#line 606 "ParserSClaim.java"
//###############################################################
// method: yylexdebug : check lexer state
//###############################################################
void yylexdebug(int state,int ch)
{
String s=null;
  if (ch < 0) ch=0;
  if (ch <= YYMAXTOKEN) //check index bounds
     s = yyname[ch];    //now get it
  if (s==null)
    s = "illegal-symbol";
  debug("state "+state+", reading "+ch+" ("+s+")");
}





//The following are now global, to aid in error reporting
int yyn;       //next next thing to do
int yym;       //
int yystate;   //current parsing state from state table
String yys;    //current token string


//###############################################################
// method: yyparse : parse input and execute indicated items
//###############################################################
int yyparse()
{
boolean doaction;
  init_stacks();
  yynerrs = 0;
  yyerrflag = 0;
  yychar = -1;          //impossible char forces a read
  yystate=0;            //initial state
  state_push(yystate);  //save it
  val_push(yylval);     //save empty value
  while (true) //until parsing is done, either correctly, or w/error
    {
    doaction=true;
    if (yydebug) debug("loop"); 
    //#### NEXT ACTION (from reduction table)
    for (yyn=yydefred[yystate];yyn==0;yyn=yydefred[yystate])
      {
      if (yydebug) debug("yyn:"+yyn+"  state:"+yystate+"  yychar:"+yychar);
      if (yychar < 0)      //we want a char?
        {
        yychar = yylex();  //get next token
        if (yydebug) debug(" next yychar:"+yychar);
        //#### ERROR CHECK ####
        if (yychar < 0)    //it it didn't work/error
          {
          yychar = 0;      //change it to default string (no -1!)
          if (yydebug)
            yylexdebug(yystate,yychar);
          }
        }//yychar<0
      yyn = yysindex[yystate];  //get amount to shift by (shift index)
      if ((yyn != 0) && (yyn += yychar) >= 0 &&
          yyn <= YYTABLESIZE && yycheck[yyn] == yychar)
        {
        if (yydebug)
          debug("state "+yystate+", shifting to state "+yytable[yyn]);
        //#### NEXT STATE ####
        yystate = yytable[yyn];//we are in a new state
        state_push(yystate);   //save it
        val_push(yylval);      //push our lval as the input for next rule
        yychar = -1;           //since we have 'eaten' a token, say we need another
        if (yyerrflag > 0)     //have we recovered an error?
           --yyerrflag;        //give ourselves credit
        doaction=false;        //but don't process yet
        break;   //quit the yyn=0 loop
        }

    yyn = yyrindex[yystate];  //reduce
    if ((yyn !=0 ) && (yyn += yychar) >= 0 &&
            yyn <= YYTABLESIZE && yycheck[yyn] == yychar)
      {   //we reduced!
      if (yydebug) debug("reduce");
      yyn = yytable[yyn];
      doaction=true; //get ready to execute
      break;         //drop down to actions
      }
    else //ERROR RECOVERY
      {
      if (yyerrflag==0)
        {
        yyerror("syntax error");
        yynerrs++;
        }
      if (yyerrflag < 3) //low error count?
        {
        yyerrflag = 3;
        while (true)   //do until break
          {
          if (stateptr<0)   //check for under & overflow here
            {
            yyerror("stack underflow. aborting...");  //note lower case 's'
            return 1;
            }
          yyn = yysindex[state_peek(0)];
          if ((yyn != 0) && (yyn += YYERRCODE) >= 0 &&
                    yyn <= YYTABLESIZE && yycheck[yyn] == YYERRCODE)
            {
            if (yydebug)
              debug("state "+state_peek(0)+", error recovery shifting to state "+yytable[yyn]+" ");
            yystate = yytable[yyn];
            state_push(yystate);
            val_push(yylval);
            doaction=false;
            break;
            }
          else
            {
            if (yydebug)
              debug("error recovery discarding state "+state_peek(0)+" ");
            if (stateptr<0)   //check for under & overflow here
              {
              yyerror("Stack underflow. aborting...");  //capital 'S'
              return 1;
              }
            state_pop();
            val_pop();
            }
          }
        }
      else            //discard this token
        {
        if (yychar == 0)
          return 1; //yyabort
        if (yydebug)
          {
          yys = null;
          if (yychar <= YYMAXTOKEN) yys = yyname[yychar];
          if (yys == null) yys = "illegal-symbol";
          debug("state "+yystate+", error recovery discards token "+yychar+" ("+yys+")");
          }
        yychar = -1;  //read another
        }
      }//end error recovery
    }//yyn=0 loop
    if (!doaction)   //any reason not to proceed?
      continue;      //skip action
    yym = yylen[yyn];          //get count of terminals on rhs
    if (yydebug)
      debug("state "+yystate+", reducing "+yym+" by rule "+yyn+" ("+yyrule[yyn]+")");
    if (yym>0)                 //if count of rhs not 'nil'
      yyval = val_peek(yym-1); //get current semantic value
    yyval = dup_yyval(yyval); //duplicate yyval if ParserVal is used as semantic value
    switch(yyn)
      {
//########## USER-SUPPLIED ACTIONS ##########
case 1:
//#line 37 "parser.y"
{
			/*log.info("variable -> VARIABLE:"+lexer.yytext().substring(1));*/
			yyval = new ParserSClaimVal(new ClaimVariable(lexer.yytext().substring(1)));
		}
break;
case 2:
//#line 42 "parser.y"
{
			/*log.info("variable -> AFFECTABLE_VARIABLE:"+lexer.yytext().substring(2));*/
			yyval = new ParserSClaimVal(new ClaimVariable(lexer.yytext().substring(2),true));
		}
break;
case 3:
//#line 49 "parser.y"
{
			/*log.info("claim_variable -> variable");*/
			yyval = val_peek(0);
		}
break;
case 4:
//#line 54 "parser.y"
{
			/*log.info("claim_variable -> THIS");*/
			yyval = new ParserSClaimVal(new ClaimVariable("this"));
		}
break;
case 5:
//#line 59 "parser.y"
{
			/*log.info("claim_variable -> PARENT");*/
			yyval = new ParserSClaimVal(new ClaimVariable("parent",true));
		}
break;
case 6:
//#line 67 "parser.y"
{
			/*//log.info("constant -> STRING_LITERAL:"+lexer.yytext());*/
			String content = lexer.yytext();
			yyval = new ParserSClaimVal(new ClaimValue(content.substring(1,content.length()-1)));
		}
break;
case 7:
//#line 73 "parser.y"
{
			/*log.info("constant -> CONSTANT: "+lexer.yytext());*/
			yyval = new ParserSClaimVal(new ClaimValue(lexer.yytext()));
		}
break;
case 8:
//#line 82 "parser.y"
{
			/*log.info("structure_field -> claim_variable");*/
			yyval = val_peek(0);
		}
break;
case 9:
//#line 87 "parser.y"
{
			/*log.info("structure_field -> constant: "+lexer.yytext());*/
			yyval = val_peek(0);
		}
break;
case 10:
//#line 92 "parser.y"
{
			/*log.info("structure_field -> structure");*/
			yyval = val_peek(0);
		}
break;
case 11:
//#line 100 "parser.y"
{
			/*log.info("structure_field_list -> structure_field");*/
			yyval = new ParserSClaimVal(new Vector<ClaimConstruct>());
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 12:
//#line 106 "parser.y"
{
			/*log.info("structure_field_list -> structure_field_list structure_field");*/
			yyval = val_peek(1);
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 13:
//#line 120 "parser.y"
{
			/*log.info("structure -> '(' STRUCT structure_field_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimStructure(val_peek(1).claimConstructVector));
		}
break;
case 14:
//#line 128 "parser.y"
{
			/*log.info("argument -> claim_variable");*/
			yyval = val_peek(0);
		}
break;
case 15:
//#line 133 "parser.y"
{
			/*log.info("argument -> constant: "+lexer.yytext());*/
			yyval = val_peek(0);
		}
break;
case 16:
//#line 138 "parser.y"
{
			/*log.info("argument -> structure");*/
			yyval = val_peek(0);
		}
break;
case 17:
//#line 143 "parser.y"
{
			/*log.info("argument -> functions");*/
			yyval = val_peek(0);
		}
break;
case 18:
//#line 148 "parser.y"
{
			/*log.info("argument -> readK_function");*/
			yyval = val_peek(0);
		}
break;
case 19:
//#line 156 "parser.y"
{
			/*log.info("argument_list -> argument");*/
			yyval = new ParserSClaimVal(new Vector<ClaimConstruct>());
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 20:
//#line 162 "parser.y"
{
			/*log.info("argument_list -> argument_list argument");*/
			yyval = val_peek(1);
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 21:
//#line 171 "parser.y"
{
			/*log.info("name -> CONSTANT: "+lexer.yytext());*/
			yyval = new ParserSClaimVal(lexer.yytext());
		}
break;
case 22:
//#line 179 "parser.y"
{
			/*log.info("agent_argument -> variable");*/
			yyval = val_peek(0);
		}
break;
case 23:
//#line 187 "parser.y"
{
			/*log.info("agent_argument_list -> agent_argument");*/
			
			/*register the language variables in the list of agent parameters*/
			Vector<ClaimConstruct> languageParameters = new Vector<ClaimConstruct>();
			languageParameters.add(new ClaimVariable("this"));
			languageParameters.add(new ClaimVariable("parent",true));
			
			yyval = new ParserSClaimVal(new Vector<ClaimConstruct>(languageParameters));
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 24:
//#line 199 "parser.y"
{
			/*log.info("agent_argument_list -> agent_argument_list agent_argument");*/
			yyval = val_peek(1);
			
			ClaimVariable currentVariable = (ClaimVariable) val_peek(0).claimConstruct;
			
			if(yyval.claimConstructVector.contains(currentVariable)==false)
			{
				ClaimVariable complementaryVariable = currentVariable.getComplement(); /*variable complementary in what concerns the affectability*/
				if(yyval.claimConstructVector.contains(complementaryVariable)==false)
					yyval.claimConstructVector.add(currentVariable);
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
break;
case 25:
//#line 225 "parser.y"
{
			/*log.info("behavior_type -> INITIAL");*/
			yyval = new ParserSClaimVal(ClaimBehaviorType.INITIAL);
		}
break;
case 26:
//#line 230 "parser.y"
{
			/*log.info("behavior_type -> REACTIVE");*/
			yyval = new ParserSClaimVal(ClaimBehaviorType.REACTIVE);
		}
break;
case 27:
//#line 235 "parser.y"
{
			/*log.info("behavior_type -> CYCLIC");*/
			yyval = new ParserSClaimVal(ClaimBehaviorType.CYCLIC);
		}
break;
case 28:
//#line 240 "parser.y"
{
			/*log.info("behavior_type -> PROACTIVE");*/
			yyval = new ParserSClaimVal(ClaimBehaviorType.PROACTIVE);
		}
break;
case 29:
//#line 253 "parser.y"
{
			/*log.info("message_structure -> '(' STRUCT MESSAGE structure_field_list ')'");*/
			Vector<ClaimConstruct> structFields = new Vector<ClaimConstruct>();
			structFields.add(new ClaimValue(new String("message")));
			structFields.addAll(val_peek(1).claimConstructVector);
			yyval = new ParserSClaimVal(new ClaimStructure(structFields));
		}
break;
case 30:
//#line 261 "parser.y"
{
			/*log.info("message_structure -> '(' MESSAGE structure_field_list ')'");*/
			Vector<ClaimConstruct> structFields = new Vector<ClaimConstruct>();
			structFields.add(new ClaimValue(new String("message")));
			structFields.addAll(val_peek(1).claimConstructVector);
			yyval = new ParserSClaimVal(new ClaimStructure(structFields));
		}
break;
case 31:
//#line 272 "parser.y"
{
			/*log.info("message_argument -> variable");*/
			yyval = val_peek(0);
		}
break;
case 32:
//#line 277 "parser.y"
{
			/*log.info("message_argument -> constant: "+lexer.yytext());*/
			yyval = val_peek(0);
		}
break;
case 33:
//#line 282 "parser.y"
{
			/*log.info("message_argument -> message_structure");*/
			yyval = val_peek(0);
		}
break;
case 34:
//#line 290 "parser.y"
{
			/*log.info("argument_list -> message_argument");*/
			yyval = new ParserSClaimVal(new Vector<ClaimConstruct>());
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 35:
//#line 296 "parser.y"
{
			/*log.info("argument_list -> message_argument_list message_argument");*/
			yyval = val_peek(1);
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 36:
//#line 305 "parser.y"
{
			/*log.info("send_function -> '(' SEND message_argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.SEND ,new String("send"),val_peek(1).claimConstructVector));
		}
break;
case 37:
//#line 313 "parser.y"
{
			/*log.info("receive_function -> '(' RECEIVE message_argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.RECEIVE ,new String("receive"),val_peek(1).claimConstructVector));
		}
break;
case 38:
//#line 321 "parser.y"
{
			/*log.info("in_function -> '(' INPUT argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.INPUT ,new String("input"),val_peek(1).claimConstructVector));
		}
break;
case 39:
//#line 329 "parser.y"
{
			/*log.info("output_function -> '(' OUTPUT argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.OUTPUT ,new String("output"),val_peek(1).claimConstructVector));
		}
break;
case 40:
//#line 337 "parser.y"
{
			/*log.info("print_function -> '(' PRINT argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.PRINT ,new String("print"),val_peek(1).claimConstructVector));
		}
break;
case 41:
//#line 345 "parser.y"
{
			/*log.info("in_function -> '(' IN argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.IN ,new String("in"),val_peek(1).claimConstructVector));
		}
break;
case 42:
//#line 353 "parser.y"
{
			/*log.info("out_function -> '(' OUT argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.OUT ,new String("out"),val_peek(1).claimConstructVector));
		}
break;
case 43:
//#line 361 "parser.y"
{
			/*log.info("open_function -> '(' OPEN argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.OPEN ,new String("open"),val_peek(1).claimConstructVector));
		}
break;
case 44:
//#line 369 "parser.y"
{
			/*log.info("acid_function -> '(' ACID argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.ACID ,new String("acid"),null));
		}
break;
case 45:
//#line 377 "parser.y"
{
			/*log.info("new_function -> '(' NEW argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.NEW ,new String("new"),val_peek(1).claimConstructVector));
		}
break;
case 46:
//#line 385 "parser.y"
{
			/*log.info("addK_function -> '(' ADDK argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.ADDK ,new String("addK"),val_peek(1).claimConstructVector));
		}
break;
case 47:
//#line 393 "parser.y"
{
			/*log.info("readK_function -> '(' READK argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.READK ,new String("readK"),val_peek(1).claimConstructVector));
		}
break;
case 48:
//#line 401 "parser.y"
{
			/*log.info("removeK_function -> '(' REMOVEK argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.REMOVEK ,new String("removeK"),val_peek(1).claimConstructVector));
		}
break;
case 49:
//#line 409 "parser.y"
{
			/*log.info("wait_function -> '(' WAIT argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall( ClaimFunctionType.WAIT ,new String("wait"),val_peek(1).claimConstructVector));
		}
break;
case 50:
//#line 417 "parser.y"
{
			/*log.info("language_function -> send_function");*/
			yyval = val_peek(0);
		}
break;
case 51:
//#line 427 "parser.y"
{
			/*log.info("language_function -> output_function");*/
			yyval = val_peek(0);
		}
break;
case 52:
//#line 432 "parser.y"
{
			/*log.info("language_function -> print_function");*/
			yyval = val_peek(0);
		}
break;
case 53:
//#line 437 "parser.y"
{
			/*log.info("language_function -> in_function");*/
			yyval = val_peek(0);
		}
break;
case 54:
//#line 442 "parser.y"
{
			/*log.info("language_function -> out_function");*/
			yyval = val_peek(0);
		}
break;
case 55:
//#line 447 "parser.y"
{
			/*log.info("language_function -> open_function");*/
			yyval = val_peek(0);
		}
break;
case 56:
//#line 452 "parser.y"
{
			/*log.info("language_function -> acid_function");*/
			yyval = val_peek(0);
		}
break;
case 57:
//#line 457 "parser.y"
{
			/*log.info("language_function -> new_function");*/
			yyval = val_peek(0);
		}
break;
case 58:
//#line 462 "parser.y"
{
			/*log.info("language_function -> addk_function");*/
			yyval = val_peek(0);
		}
break;
case 59:
//#line 467 "parser.y"
{
			/*log.info("language_function -> readk_function");*/
			yyval = val_peek(0);
		}
break;
case 60:
//#line 472 "parser.y"
{
			/*log.info("language_function -> removek_function");*/
			yyval = val_peek(0);
		}
break;
case 61:
//#line 477 "parser.y"
{
			/*log.info("language_function -> wait_function");*/
			yyval = val_peek(0);
		}
break;
case 62:
//#line 485 "parser.y"
{
			/*log.info("function -> '(' name ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall(ClaimFunctionType.JAVA,val_peek(1).sval,null));
		}
break;
case 63:
//#line 490 "parser.y"
{
			/*log.info("function -> '(' name argument_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimFunctionCall(ClaimFunctionType.JAVA,val_peek(2).sval,val_peek(1).claimConstructVector));
		}
break;
case 64:
//#line 498 "parser.y"
{
			/*log.info("goal -> '(' AGOAL name proposition_list ')'");*/
			/*$$ = new ParserSClaimVal(new ClaimaGoal(ClaimConstructType.AGOAL, $3.sval, $6.claimConstructVector, $4.ival));*/
		}
break;
case 65:
//#line 503 "parser.y"
{
			/*$$ = new ParserSClaimVal(new ClaimmGoal(ClaimConstructType.MGOAL, $3.sval, $6.claimConstructVector, $8.claimConstructVector, Integer.parseInt(((ClaimValue) $4.obj).toString())));	*/
		}
break;
case 66:
//#line 507 "parser.y"
{
			/*$$ = $$ = new ParserSClaimVal(new ClaimmGoal(ClaimConstructType.MGOAL, $3.sval, $6.claimConstructVector, null, Integer.parseInt(((ClaimValue) $4.obj).toString())));*/
		}
break;
case 67:
//#line 511 "parser.y"
{
			/*$$ = new ParserSClaimVal(new ClaimpGoal(ClaimConstructType.PGOAL, $3.sval, $6.claimConstruct, Integer.parseInt(((ClaimValue) $4.obj).toString())));*/
		}
break;
case 68:
//#line 518 "parser.y"
{
			/*log.info("proposition_list -> proposition");*/
			yyval = new ParserSClaimVal(new Vector<ClaimConstruct>());
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 69:
//#line 524 "parser.y"
{
			/*log.info("proposition_list -> proposition_list proposition");*/
			yyval = val_peek(1);
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 70:
//#line 533 "parser.y"
{
			/*log.info("proposition -> constant ");*/
			yyval = val_peek(0);
		}
break;
case 71:
//#line 541 "parser.y"
{
			/*log.info("priority -> constant");*/
			yyval = val_peek(0);
		}
break;
case 72:
//#line 550 "parser.y"
{
			/*log.info("valid_condition -> readK_function");*/
			yyval = val_peek(0);
		}
break;
case 73:
//#line 555 "parser.y"
{
			/*log.info("valid_condition -> function");*/
			yyval = val_peek(0);
		}
break;
case 74:
//#line 563 "parser.y"
{
			/*log.info("condition -> '(' CONDITION valid_condition ')'");*/
			yyval = new ParserSClaimVal(new ClaimCondition((ClaimFunctionCall) val_peek(1).claimConstruct));
		}
break;
case 75:
//#line 571 "parser.y"
{
			/*log.info("if_stmt -> '(' IF if_valid_condition THEN behavior_content_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimIf((ClaimFunctionCall) val_peek(3).claimConstruct,val_peek(1).claimConstructVector, null));
		}
break;
case 76:
//#line 576 "parser.y"
{
			/*log.info("if_stmt -> '(' IF if_valid_condition THEN behavior_content_list ELSE behavior_content_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimIf((ClaimFunctionCall) val_peek(5).claimConstruct,val_peek(3).claimConstructVector,val_peek(1).claimConstructVector));
		}
break;
case 77:
//#line 584 "parser.y"
{
			/*log.info("forAllK -> '(' FORALLK structure ')'");*/
			if (!verifyVariablesInStructure((ClaimStructure) val_peek(1).claimConstruct))
				yywarn("there is no variable in the structure of forAllK");
			yyval = new ParserSClaimVal(new ClaimForAllK((ClaimStructure) val_peek(1).claimConstruct, null));
		}
break;
case 78:
//#line 591 "parser.y"
{
			/*log.info("forAllK -> '(' FORALLK structure behavior_content_list ')'");*/
			if (!verifyVariablesInStructure((ClaimStructure) val_peek(2).claimConstruct))
				yywarn("there is no variable in the structure of forAllK");
			yyval = new ParserSClaimVal(new ClaimForAllK((ClaimStructure) val_peek(2).claimConstruct, val_peek(1).claimConstructVector));
		}
break;
case 79:
//#line 601 "parser.y"
{
			/*log.info("while -> '(' WHILE function ')'");*/
			yyval = new ParserSClaimVal(new ClaimWhile((ClaimFunctionCall) val_peek(1).claimConstruct, null));
		}
break;
case 80:
//#line 606 "parser.y"
{
			/*log.info("while -> '(' WHILE function behavior_content_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimWhile((ClaimFunctionCall) val_peek(2).claimConstruct, val_peek(1).claimConstructVector));
		}
break;
case 81:
//#line 614 "parser.y"
{
			/*log.info("behavior_content_header -> receive_function");*/
			yyval = val_peek(0);
		}
break;
case 82:
//#line 619 "parser.y"
{
			/*log.info("behavior_content_header -> receive_function");*/
			yyval = val_peek(0);
		}
break;
case 83:
//#line 624 "parser.y"
{
			/*log.info("behavior_content_header -> condition");*/
			yyval = val_peek(0);
		}
break;
case 84:
//#line 632 "parser.y"
{
			/*log.info("behavior_content_header_list -> behavior_content_header");*/
			yyval = new ParserSClaimVal(new Vector<ClaimConstruct>());
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 85:
//#line 638 "parser.y"
{
			/*log.info("behavior_content_header_list -> behavior_content_header_list behavior_content_header");*/
			yyval = val_peek(1);
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 86:
//#line 648 "parser.y"
{
			/*log.info("behavior_content -> language_function");*/
			yyval = val_peek(0);
		}
break;
case 87:
//#line 653 "parser.y"
{
			/*log.info("behavior_content -> function");*/
			yyval = val_peek(0);
		}
break;
case 88:
//#line 658 "parser.y"
{
			/*log.info("behavior_content -> if_stmt");*/
			yyval = val_peek(0);
		}
break;
case 89:
//#line 663 "parser.y"
{
			/*log.info("behavior_content -> forAllK");*/
			yyval = val_peek(0);
		}
break;
case 90:
//#line 668 "parser.y"
{
			/*log.info("behavior_content -> while");*/
			yyval = val_peek(0);
		}
break;
case 91:
//#line 673 "parser.y"
{
			yyval = val_peek(0);
		}
break;
case 92:
//#line 680 "parser.y"
{
			/*log.info("behavior_content_list -> behavior_content");*/
			yyval = new ParserSClaimVal(new Vector<ClaimConstruct>());
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 93:
//#line 686 "parser.y"
{
			/*log.info("behavior_content_list -> behavior_content_list behavior_content");*/
			yyval = val_peek(1);
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 94:
//#line 700 "parser.y"
{
			/*log.info("behavior -> '(' behavior_type name behavior_content_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimBehaviorDefinition(val_peek(2).sval, val_peek(3).claimBehaviorType, val_peek(1).claimConstructVector));
		}
break;
case 95:
//#line 705 "parser.y"
{
			/*log.info("behavior -> '(' behavior_type name behavior_content_header_list ')'");*/
			yyval = new ParserSClaimVal(new ClaimBehaviorDefinition(val_peek(2).sval, val_peek(3).claimBehaviorType, val_peek(1).claimConstructVector));
		}
break;
case 96:
//#line 710 "parser.y"
{
			/*log.info("behavior -> '(' behavior_type name behavior_content_header behavior_content_list ')'");*/
			Boolean concatenateSucceeded = val_peek(2).claimConstructVector.addAll(val_peek(1).claimConstructVector);
			if(concatenateSucceeded)
				yyval = new ParserSClaimVal(new ClaimBehaviorDefinition(val_peek(3).sval, val_peek(4).claimBehaviorType, val_peek(2).claimConstructVector));
			else
				log.error("error while concatenating the vectors of statements, while creating the behavior");
		}
break;
case 97:
//#line 722 "parser.y"
{
			/*log.info("behavior_list -> behavior");*/
			yyval = new ParserSClaimVal(new Vector<ClaimConstruct>());
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 98:
//#line 728 "parser.y"
{
			/*log.info("behavior_list -> behavior_list behavior");*/
			yyval = val_peek(1);
			yyval.claimConstructVector.add(val_peek(0).claimConstruct);
		}
break;
case 99:
//#line 737 "parser.y"
{
			/*log.info("behavior_declaration -> '(' BEHAVIOR behavior_list ')'");*/
			yyval = val_peek(1);
		}
break;
case 100:
//#line 745 "parser.y"
{
			/*log.info("agent_specification -> '(' AGENT name agent_argument_list behaviors_declaration ')'");*/
			parsedAgent = new ClaimAgentDefinition(val_peek(3).sval, 
					new Vector<ClaimConstruct>(val_peek(2).claimConstructVector),
					new Vector<ClaimBehaviorDefinition>(
							Arrays.asList(val_peek(1).claimConstructVector.toArray(new ClaimBehaviorDefinition [0]))
					));
			/*Set the references of the contained behaviors to this agent:*/
			for(ClaimBehaviorDefinition currentBehavior:parsedAgent.getBehaviors())
				currentBehavior.setMyAgent(parsedAgent);
		}
break;
case 101:
//#line 757 "parser.y"
{
			/*log.info("agent_specification -> '(' AGENT name behaviors_declaration ')'");*/
			
			/*register the language variables in the list of agent parameters*/
			Vector<ClaimConstruct> languageParameters = new Vector<ClaimConstruct>();
			languageParameters.add(new ClaimVariable("this"));
			languageParameters.add(new ClaimVariable("parent",true));
			
			parsedAgent = new ClaimAgentDefinition(val_peek(2).sval,languageParameters,
					new Vector<ClaimBehaviorDefinition>(
							Arrays.asList(val_peek(1).claimConstructVector.toArray(new ClaimBehaviorDefinition [0]))
					));
			/*Set the references of the contained behaviors to this agent:*/
			for(ClaimBehaviorDefinition currentBehavior:parsedAgent.getBehaviors())
				currentBehavior.setMyAgent(parsedAgent);
		}
break;
//#line 1531 "ParserSClaim.java"
//########## END OF USER-SUPPLIED ACTIONS ##########
    }//switch
    //#### Now let's reduce... ####
    if (yydebug) debug("reduce");
    state_drop(yym);             //we just reduced yylen states
    yystate = state_peek(0);     //get new state
    val_drop(yym);               //corresponding value drop
    yym = yylhs[yyn];            //select next TERMINAL(on lhs)
    if (yystate == 0 && yym == 0)//done? 'rest' state and at first TERMINAL
      {
      if (yydebug) debug("After reduction, shifting from state 0 to state "+YYFINAL+"");
      yystate = YYFINAL;         //explicitly say we're done
      state_push(YYFINAL);       //and save it
      val_push(yyval);           //also save the semantic value of parsing
      if (yychar < 0)            //we want another character?
        {
        yychar = yylex();        //get next character
        if (yychar<0) yychar=0;  //clean, if necessary
        if (yydebug)
          yylexdebug(yystate,yychar);
        }
      if (yychar == 0)          //Good exit (if lex returns 0 ;-)
         break;                 //quit the loop--all DONE
      }//if yystate
    else                        //else not done yet
      {                         //get next state and push, for next yydefred[]
      yyn = yygindex[yym];      //find out where to go
      if ((yyn != 0) && (yyn += yystate) >= 0 &&
            yyn <= YYTABLESIZE && yycheck[yyn] == yystate)
        yystate = yytable[yyn]; //get new state
      else
        yystate = yydgoto[yym]; //else go to new defred
      if (yydebug) debug("after reduction, shifting from state "+state_peek(0)+" to state "+yystate+"");
      state_push(yystate);     //going again, so push state & val...
      val_push(yyval);         //for next action
      }
    }//main loop
  return 0;//yyaccept!!
}
//## end of method parse() ######################################



//## run() --- for Thread #######################################
/**
 * A default run method, used for operating this parser
 * object in the background.  It is intended for extending Thread
 * or implementing Runnable.  Turn off with -Jnorun .
 */
public void run()
{
  yyparse();
}
//## end of method run() ########################################



//## Constructors ###############################################
/**
 * Default constructor.  Turn off with -Jnoconstruct .

 */
public ParserSClaim()
{
  //nothing to do
}


/**
 * Create a parser, setting the debug to true or false.
 * @param debugMe true for debugging, false for no debug.
 */
public ParserSClaim(boolean debugMe)
{
  yydebug=debugMe;
}
//###############################################################



}
//################### END OF CLASS ##############################
