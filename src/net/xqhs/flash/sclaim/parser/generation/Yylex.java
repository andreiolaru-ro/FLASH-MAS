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
/* The following code was generated by JFlex 1.6.1 */

/* Lexer specification to be used with jflex, in order to generate the lexical analyzer of S-Claim.
To generate the Lexer, in Linux, run the script "generateLexer.sh", included in the directory */
package net.xqhs.flash.sclaim.parser.generation;


/**
 * This class is a scanner generated by 
 * <a href="http://www.jflex.de/">JFlex</a> 1.6.1
 * from the specification file <tt>lexer.y</tt>
 */
class Yylex {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = { 
     0, 0
  };

  /** 
   * Translates characters to character classes
   */
  private static final String ZZ_CMAP_PACKED = 
    "\11\0\1\3\1\2\1\11\1\12\1\1\22\0\1\3\1\0\1\7"+
    "\5\0\1\14\1\14\1\5\4\0\1\4\12\6\5\0\1\13\1\0"+
    "\1\44\5\6\1\43\3\6\1\41\17\6\1\0\1\10\2\0\1\6"+
    "\1\0\1\22\1\31\1\27\1\37\1\24\1\36\1\30\1\16\1\17"+
    "\2\6\1\34\1\40\1\25\1\33\1\21\1\6\1\23\1\20\1\15"+
    "\1\26\1\32\1\42\1\6\1\35\1\6\12\0\1\11\u1fa2\0\1\11"+
    "\1\11\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\udfe6\0";

  /** 
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\1\0\1\1\2\2\2\1\1\3\1\4\20\1\1\0"+
    "\1\5\1\1\1\6\1\0\2\1\1\7\1\10\27\1"+
    "\1\0\1\1\1\0\1\1\1\5\1\0\1\11\25\1"+
    "\1\12\4\1\1\13\7\1\1\0\1\2\1\14\1\15"+
    "\4\1\1\16\6\1\1\17\1\1\1\20\5\1\1\21"+
    "\3\1\1\22\6\1\1\23\1\2\2\1\1\24\2\1"+
    "\1\25\1\1\1\26\2\1\1\27\1\30\1\1\1\31"+
    "\11\1\1\32\1\33\1\34\1\1\1\35\1\36\1\1"+
    "\1\37\5\1\1\40\1\1\1\41\3\1\1\42\1\1"+
    "\1\43\1\1\1\44\1\45\2\1\1\46\1\1\1\47"+
    "\1\1\1\50\1\1\1\51\1\52\1\53\1\54";

  private static int [] zzUnpackAction() {
    int [] result = new int[193];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /** 
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\45\0\112\0\157\0\224\0\271\0\336\0\157"+
    "\0\u0103\0\u0128\0\u014d\0\u0172\0\u0197\0\u01bc\0\u01e1\0\u0206"+
    "\0\u022b\0\u0250\0\u0275\0\u029a\0\u02bf\0\u02e4\0\u0309\0\u032e"+
    "\0\u0353\0\271\0\u0378\0\u039d\0\u03c2\0\u03e7\0\u040c\0\u0431"+
    "\0\45\0\u0456\0\u047b\0\u04a0\0\u04c5\0\u04ea\0\u050f\0\u0534"+
    "\0\u0559\0\u057e\0\u05a3\0\u05c8\0\u05ed\0\u0612\0\u0637\0\u065c"+
    "\0\u0681\0\u06a6\0\u06cb\0\u06f0\0\u0715\0\u073a\0\u075f\0\u0784"+
    "\0\u07a9\0\u07ce\0\u07f3\0\u0818\0\u0353\0\u083d\0\u03c2\0\u0862"+
    "\0\u0887\0\u08ac\0\u08d1\0\u08f6\0\u091b\0\u0940\0\u0965\0\u098a"+
    "\0\u09af\0\u09d4\0\u09f9\0\u0a1e\0\u0a43\0\u0a68\0\u0a8d\0\u0ab2"+
    "\0\u0ad7\0\u0afc\0\u0b21\0\u0b46\0\45\0\u0b6b\0\u0b90\0\u0bb5"+
    "\0\u0bda\0\u0bff\0\u0c24\0\u0c49\0\u0c6e\0\u0c93\0\u0cb8\0\u0cdd"+
    "\0\u0d02\0\u0d27\0\u07ce\0\45\0\45\0\u0d4c\0\u0d71\0\u0d96"+
    "\0\u0dbb\0\45\0\u0de0\0\u0e05\0\u0e2a\0\u0e4f\0\u0e74\0\u0e99"+
    "\0\45\0\u0ebe\0\45\0\u0ee3\0\u0f08\0\u0f2d\0\u0f52\0\u0f77"+
    "\0\45\0\u0f9c\0\u0fc1\0\u0fe6\0\45\0\u100b\0\u1030\0\u1055"+
    "\0\u107a\0\u109f\0\u10c4\0\45\0\45\0\u10e9\0\u110e\0\45"+
    "\0\u1133\0\u1158\0\45\0\u117d\0\45\0\u11a2\0\u11c7\0\45"+
    "\0\45\0\u11ec\0\45\0\u1211\0\u1236\0\u125b\0\u1280\0\u12a5"+
    "\0\u12ca\0\u12ef\0\u1314\0\u1339\0\45\0\45\0\45\0\u135e"+
    "\0\45\0\45\0\u1383\0\45\0\u13a8\0\u13cd\0\u13f2\0\u1417"+
    "\0\u143c\0\45\0\u1461\0\45\0\u1486\0\u14ab\0\u14d0\0\45"+
    "\0\u14f5\0\45\0\u151a\0\45\0\45\0\u153f\0\u1564\0\45"+
    "\0\u1589\0\45\0\u15ae\0\45\0\u15d3\0\45\0\45\0\45"+
    "\0\45";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[193];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /** 
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\2\1\3\2\4\1\5\2\2\1\6\2\2\1\4"+
    "\1\7\1\10\1\11\1\2\1\12\1\13\1\14\1\15"+
    "\1\16\1\17\1\20\1\2\1\21\1\2\1\22\1\2"+
    "\1\23\2\2\1\24\1\2\1\25\1\2\1\26\3\2"+
    "\3\0\6\2\3\0\30\2\2\0\1\4\107\0\1\2"+
    "\3\0\1\27\1\30\4\2\3\0\30\2\1\6\3\31"+
    "\3\6\1\32\1\33\1\6\3\31\30\6\6\0\1\34"+
    "\4\0\1\35\1\0\30\34\1\2\3\0\6\2\3\0"+
    "\1\2\1\36\3\2\1\37\23\2\3\0\6\2\3\0"+
    "\10\2\1\40\10\2\1\41\7\2\3\0\6\2\3\0"+
    "\1\42\6\2\1\43\21\2\3\0\6\2\3\0\5\2"+
    "\1\44\1\45\17\2\1\46\2\2\3\0\6\2\3\0"+
    "\12\2\1\47\1\50\6\2\1\51\3\2\1\52\2\2"+
    "\3\0\6\2\3\0\7\2\1\53\21\2\3\0\6\2"+
    "\3\0\17\2\1\54\11\2\3\0\6\2\3\0\7\2"+
    "\1\55\21\2\3\0\6\2\3\0\16\2\1\56\1\2"+
    "\1\57\10\2\3\0\6\2\3\0\7\2\1\60\21\2"+
    "\3\0\6\2\3\0\4\2\1\61\4\2\1\62\17\2"+
    "\3\0\6\2\3\0\16\2\1\63\12\2\3\0\6\2"+
    "\3\0\5\2\1\64\1\2\1\65\16\2\1\66\2\2"+
    "\3\0\6\2\3\0\1\2\1\67\3\2\1\70\22\2"+
    "\1\27\1\3\1\4\1\71\6\27\3\71\30\27\1\72"+
    "\3\73\1\72\1\74\4\72\3\73\30\72\7\31\1\75"+
    "\1\76\34\31\1\6\2\0\1\31\5\6\1\2\1\0"+
    "\2\31\30\6\6\0\1\34\6\0\30\34\6\0\1\77"+
    "\6\0\30\77\1\2\3\0\6\2\3\0\2\2\1\100"+
    "\4\2\1\101\21\2\3\0\6\2\3\0\6\2\1\102"+
    "\22\2\3\0\6\2\3\0\2\2\1\103\1\2\1\104"+
    "\24\2\3\0\6\2\3\0\6\2\1\105\22\2\3\0"+
    "\6\2\3\0\10\2\1\106\20\2\3\0\6\2\3\0"+
    "\6\2\1\107\22\2\3\0\6\2\3\0\2\2\1\110"+
    "\13\2\1\111\12\2\3\0\6\2\3\0\16\2\1\112"+
    "\12\2\3\0\6\2\3\0\1\113\1\114\1\115\26\2"+
    "\3\0\6\2\3\0\7\2\1\116\21\2\3\0\6\2"+
    "\3\0\22\2\1\117\6\2\3\0\6\2\3\0\16\2"+
    "\1\120\12\2\3\0\6\2\3\0\5\2\1\121\4\2"+
    "\1\122\10\2\1\123\5\2\3\0\6\2\3\0\3\2"+
    "\1\124\25\2\3\0\6\2\3\0\25\2\1\125\3\2"+
    "\3\0\6\2\3\0\10\2\1\126\20\2\3\0\6\2"+
    "\3\0\12\2\1\127\16\2\3\0\6\2\3\0\1\2"+
    "\1\130\27\2\3\0\6\2\3\0\7\2\1\131\21\2"+
    "\3\0\6\2\3\0\1\132\30\2\3\0\6\2\3\0"+
    "\6\2\1\133\22\2\3\0\6\2\3\0\2\2\1\134"+
    "\26\2\3\0\6\2\3\0\3\2\1\135\25\2\3\0"+
    "\6\2\3\0\16\2\1\136\12\2\3\0\6\2\3\0"+
    "\2\2\1\137\26\2\3\0\6\2\3\0\2\2\1\140"+
    "\25\2\1\71\1\3\1\4\42\71\1\72\3\73\1\72"+
    "\1\141\4\72\3\73\30\72\5\73\1\142\37\73\1\72"+
    "\3\73\1\143\1\141\4\72\3\73\30\72\1\31\2\0"+
    "\6\31\2\0\32\31\1\2\3\0\6\2\3\0\3\2"+
    "\1\144\25\2\3\0\6\2\3\0\10\2\1\145\20\2"+
    "\3\0\6\2\3\0\13\2\1\146\15\2\3\0\6\2"+
    "\3\0\1\147\30\2\3\0\6\2\3\0\11\2\1\150"+
    "\17\2\3\0\6\2\3\0\11\2\1\151\17\2\3\0"+
    "\6\2\3\0\22\2\1\152\6\2\3\0\6\2\3\0"+
    "\7\2\1\153\21\2\3\0\6\2\3\0\10\2\1\154"+
    "\20\2\3\0\6\2\3\0\5\2\1\155\23\2\3\0"+
    "\6\2\3\0\5\2\1\156\23\2\3\0\6\2\3\0"+
    "\2\2\1\157\26\2\3\0\6\2\3\0\2\2\1\160"+
    "\26\2\3\0\6\2\3\0\22\2\1\161\6\2\3\0"+
    "\6\2\3\0\10\2\1\162\20\2\3\0\6\2\3\0"+
    "\24\2\1\163\4\2\3\0\6\2\3\0\5\2\1\164"+
    "\23\2\3\0\6\2\3\0\12\2\1\165\7\2\1\166"+
    "\6\2\3\0\6\2\3\0\7\2\1\167\21\2\3\0"+
    "\6\2\3\0\16\2\1\170\12\2\3\0\6\2\3\0"+
    "\7\2\1\171\21\2\3\0\6\2\3\0\22\2\1\172"+
    "\6\2\3\0\6\2\3\0\17\2\1\173\11\2\3\0"+
    "\6\2\3\0\5\2\1\174\23\2\3\0\6\2\3\0"+
    "\10\2\1\175\20\2\3\0\6\2\3\0\4\2\1\176"+
    "\24\2\3\0\6\2\3\0\27\2\1\177\1\2\3\0"+
    "\6\2\3\0\10\2\1\200\20\2\3\0\6\2\3\0"+
    "\3\2\1\201\25\2\3\0\6\2\3\0\5\2\1\202"+
    "\23\2\3\0\6\2\3\0\17\2\1\203\11\2\3\0"+
    "\6\2\3\0\1\204\27\2\1\72\3\73\1\205\1\141"+
    "\4\72\3\73\30\72\4\73\1\4\1\142\37\73\1\2"+
    "\3\0\6\2\3\0\7\2\1\206\21\2\3\0\6\2"+
    "\3\0\2\2\1\207\26\2\3\0\6\2\3\0\1\210"+
    "\30\2\3\0\6\2\3\0\12\2\1\211\16\2\3\0"+
    "\6\2\3\0\10\2\1\212\20\2\3\0\6\2\3\0"+
    "\1\213\30\2\3\0\6\2\3\0\12\2\1\214\16\2"+
    "\3\0\6\2\3\0\17\2\1\215\11\2\3\0\6\2"+
    "\3\0\16\2\1\216\12\2\3\0\6\2\3\0\7\2"+
    "\1\217\21\2\3\0\6\2\3\0\1\220\30\2\3\0"+
    "\6\2\3\0\17\2\1\221\11\2\3\0\6\2\3\0"+
    "\1\222\30\2\3\0\6\2\3\0\24\2\1\223\4\2"+
    "\3\0\6\2\3\0\2\2\1\224\26\2\3\0\6\2"+
    "\3\0\15\2\1\225\13\2\3\0\6\2\3\0\2\2"+
    "\1\226\26\2\3\0\6\2\3\0\2\2\1\227\26\2"+
    "\3\0\6\2\3\0\15\2\1\230\13\2\3\0\6\2"+
    "\3\0\11\2\1\231\17\2\3\0\6\2\3\0\17\2"+
    "\1\232\11\2\3\0\6\2\3\0\1\233\30\2\3\0"+
    "\6\2\3\0\5\2\1\234\23\2\3\0\6\2\3\0"+
    "\17\2\1\235\11\2\3\0\6\2\3\0\7\2\1\236"+
    "\21\2\3\0\6\2\3\0\1\237\30\2\3\0\6\2"+
    "\3\0\5\2\1\240\23\2\3\0\6\2\3\0\1\241"+
    "\30\2\3\0\6\2\3\0\1\242\30\2\3\0\6\2"+
    "\3\0\1\243\30\2\3\0\6\2\3\0\10\2\1\244"+
    "\20\2\3\0\6\2\3\0\15\2\1\245\13\2\3\0"+
    "\6\2\3\0\2\2\1\246\26\2\3\0\6\2\3\0"+
    "\15\2\1\247\13\2\3\0\6\2\3\0\7\2\1\250"+
    "\21\2\3\0\6\2\3\0\1\251\30\2\3\0\6\2"+
    "\3\0\12\2\1\252\16\2\3\0\6\2\3\0\2\2"+
    "\1\253\26\2\3\0\6\2\3\0\1\254\30\2\3\0"+
    "\6\2\3\0\17\2\1\255\11\2\3\0\6\2\3\0"+
    "\5\2\1\256\23\2\3\0\6\2\3\0\13\2\1\257"+
    "\15\2\3\0\6\2\3\0\17\2\1\260\11\2\3\0"+
    "\6\2\3\0\2\2\1\261\26\2\3\0\6\2\3\0"+
    "\7\2\1\262\21\2\3\0\6\2\3\0\15\2\1\263"+
    "\13\2\3\0\6\2\3\0\7\2\1\264\21\2\3\0"+
    "\6\2\3\0\24\2\1\265\4\2\3\0\6\2\3\0"+
    "\2\2\1\266\26\2\3\0\6\2\3\0\16\2\1\267"+
    "\12\2\3\0\6\2\3\0\24\2\1\270\4\2\3\0"+
    "\6\2\3\0\2\2\1\271\26\2\3\0\6\2\3\0"+
    "\7\2\1\272\21\2\3\0\6\2\3\0\15\2\1\273"+
    "\13\2\3\0\6\2\3\0\7\2\1\274\21\2\3\0"+
    "\6\2\3\0\16\2\1\275\12\2\3\0\6\2\3\0"+
    "\6\2\1\276\22\2\3\0\6\2\3\0\10\2\1\277"+
    "\20\2\3\0\6\2\3\0\7\2\1\300\21\2\3\0"+
    "\6\2\3\0\10\2\1\301\17\2";

  private static int [] zzUnpackTrans() {
    int [] result = new int[5624];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String ZZ_ERROR_MSG[] = {
    "Unknown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\1\0\2\1\1\11\3\1\1\11\20\1\1\0\3\1"+
    "\1\0\33\1\1\0\1\1\1\0\2\1\1\0\43\1"+
    "\1\0\137\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[193];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** number of newlines encountered up to the start of the matched text */
  private int yyline;

  /** the number of characters up to the start of the matched text */
  private int yychar;

  /**
   * the number of characters from the last newline up to the start of the 
   * matched text
   */
  private int yycolumn;

  /** 
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;
  
  /** 
   * The number of occupied positions in zzBuffer beyond zzEndRead.
   * When a lead/high surrogate has been read from the input stream
   * into the final zzBuffer position, this will have a value of 1;
   * otherwise, it will have a value of 0.
   */
  private int zzFinalHighSurrogate = 0;

  /* user code: */
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


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  Yylex(java.io.Reader in) {
    this.zzReader = in;
  }


  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    char [] map = new char[0x110000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 156) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }


  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   * 
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {

    /* first: make room (if you can) */
    if (zzStartRead > 0) {
      zzEndRead += zzFinalHighSurrogate;
      zzFinalHighSurrogate = 0;
      System.arraycopy(zzBuffer, zzStartRead,
                       zzBuffer, 0,
                       zzEndRead-zzStartRead);

      /* translate stored positions */
      zzEndRead-= zzStartRead;
      zzCurrentPos-= zzStartRead;
      zzMarkedPos-= zzStartRead;
      zzStartRead = 0;
    }

    /* is the buffer big enough? */
    if (zzCurrentPos >= zzBuffer.length - zzFinalHighSurrogate) {
      /* if not: blow it up */
      char newBuffer[] = new char[zzBuffer.length*2];
      System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
      zzBuffer = newBuffer;
      zzEndRead += zzFinalHighSurrogate;
      zzFinalHighSurrogate = 0;
    }

    /* fill the buffer with new input */
    int requested = zzBuffer.length - zzEndRead;
    int numRead = zzReader.read(zzBuffer, zzEndRead, requested);

    /* not supposed to occur according to specification of java.io.Reader */
    if (numRead == 0) {
      throw new java.io.IOException("Reader returned 0 characters. See JFlex examples for workaround.");
    }
    if (numRead > 0) {
      zzEndRead += numRead;
      /* If numRead == requested, we might have requested to few chars to
         encode a full Unicode character. We assume that a Reader would
         otherwise never return half characters. */
      if (numRead == requested) {
        if (Character.isHighSurrogate(zzBuffer[zzEndRead - 1])) {
          --zzEndRead;
          zzFinalHighSurrogate = 1;
        }
      }
      /* potentially more input available */
      return false;
    }

    /* numRead < 0 ==> end of stream */
    return true;
  }

    
  /**
   * Closes the input stream.
   */
  public final void yyclose() throws java.io.IOException {
    zzAtEOF = true;            /* indicate end of file */
    zzEndRead = zzStartRead;  /* invalidate buffer    */

    if (zzReader != null)
      zzReader.close();
  }


  /**
   * Resets the scanner to read from a new input stream.
   * Does not close the old reader.
   *
   * All internal variables are reset, the old input stream 
   * <b>cannot</b> be reused (internal buffer is discarded and lost).
   * Lexical state is set to <tt>ZZ_INITIAL</tt>.
   *
   * Internal scan buffer is resized down to its initial length, if it has grown.
   *
   * @param reader   the new input stream 
   */
  public final void yyreset(java.io.Reader reader) {
    zzReader = reader;
    zzAtBOL  = true;
    zzAtEOF  = false;
    zzEOFDone = false;
    zzEndRead = zzStartRead = 0;
    zzCurrentPos = zzMarkedPos = 0;
    zzFinalHighSurrogate = 0;
    yyline = yychar = yycolumn = 0;
    zzLexicalState = YYINITIAL;
    if (zzBuffer.length > ZZ_BUFFERSIZE)
      zzBuffer = new char[ZZ_BUFFERSIZE];
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final String yytext() {
    return new String( zzBuffer, zzStartRead, zzMarkedPos-zzStartRead );
  }


  /**
   * Returns the character at position <tt>pos</tt> from the 
   * matched text. 
   * 
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch. 
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer[zzStartRead+pos];
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of 
   * yypushback(int) and a match-all fallback rule) this method 
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  } 


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Contains user EOF-code, which will be executed exactly once,
   * when the end of file is reached
   */
  private void zzDoEOF() throws java.io.IOException {
    if (!zzEOFDone) {
      zzEOFDone = true;
      yyclose();
    }
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public int yylex() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    char [] zzBufferL = zzBuffer;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      boolean zzR = false;
      int zzCh;
      int zzCharCount;
      for (zzCurrentPosL = zzStartRead  ;
           zzCurrentPosL < zzMarkedPosL ;
           zzCurrentPosL += zzCharCount ) {
        zzCh = Character.codePointAt(zzBufferL, zzCurrentPosL, zzMarkedPosL);
        zzCharCount = Character.charCount(zzCh);
        switch (zzCh) {
        case '\u000B':
        case '\u000C':
        case '\u0085':
        case '\u2028':
        case '\u2029':
          yyline++;
          yycolumn = 0;
          zzR = false;
          break;
        case '\r':
          yyline++;
          yycolumn = 0;
          zzR = true;
          break;
        case '\n':
          if (zzR)
            zzR = false;
          else {
            yyline++;
            yycolumn = 0;
          }
          break;
        default:
          zzR = false;
          yycolumn += zzCharCount;
        }
      }

      if (zzR) {
        // peek one character ahead if it is \n (if we have counted one line too much)
        boolean zzPeek;
        if (zzMarkedPosL < zzEndReadL)
          zzPeek = zzBufferL[zzMarkedPosL] == '\n';
        else if (zzAtEOF)
          zzPeek = false;
        else {
          boolean eof = zzRefill();
          zzEndReadL = zzEndRead;
          zzMarkedPosL = zzMarkedPos;
          zzBufferL = zzBuffer;
          if (eof) 
            zzPeek = false;
          else 
            zzPeek = zzBufferL[zzMarkedPosL] == '\n';
        }
        if (zzPeek) yyline--;
      }
      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
  
      zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {
    
          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
            zzCurrentPosL += Character.charCount(zzInput);
          }
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
        zzAtEOF = true;
            zzDoEOF();
          { return 0; }
      }
      else {
        switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
          case 1: 
            { /* yyparser.yylval = new ParserSClaimVal(yytext());*/
				return ParserSClaim.CONSTANT;
            }
          case 45: break;
          case 2: 
            { /* ignore */
            }
          case 46: break;
          case 3: 
            { /* ignore bad characters */
            }
          case 47: break;
          case 4: 
            { return (int) yycharat(0);
            }
          case 48: break;
          case 5: 
            { return ParserSClaim.STRING_LITERAL;
            }
          case 49: break;
          case 6: 
            { /* yyparser.yylval = new ParserSClaimVal(yytext().substring(1));*/
				return ParserSClaim.VARIABLE;
            }
          case 50: break;
          case 7: 
            { return ParserSClaim.IN;
            }
          case 51: break;
          case 8: 
            { return ParserSClaim.IF;
            }
          case 52: break;
          case 9: 
            { /* yyparser.yylval = new ParserSClaimVal(yytext().substring(1));*/
				return ParserSClaim.AFFECTABLE_VARIABLE;
            }
          case 53: break;
          case 10: 
            { return ParserSClaim.NEW;
            }
          case 54: break;
          case 11: 
            { return ParserSClaim.OUT;
            }
          case 55: break;
          case 12: 
            { return ParserSClaim.THIS;
            }
          case 56: break;
          case 13: 
            { return ParserSClaim.THEN;
            }
          case 57: break;
          case 14: 
            { return ParserSClaim.SEND;
            }
          case 58: break;
          case 15: 
            { return ParserSClaim.ACID;
            }
          case 59: break;
          case 16: 
            { return ParserSClaim.ADDK;
            }
          case 60: break;
          case 17: 
            { return ParserSClaim.ELSE;
            }
          case 61: break;
          case 18: 
            { return ParserSClaim.OPEN;
            }
          case 62: break;
          case 19: 
            { return ParserSClaim.WAIT;
            }
          case 63: break;
          case 20: 
            { return ParserSClaim.INPUT;
            }
          case 64: break;
          case 21: 
            { return ParserSClaim.PRINT;
            }
          case 65: break;
          case 22: 
            { return ParserSClaim.PGOAL;
            }
          case 66: break;
          case 23: 
            { return ParserSClaim.AGENT;
            }
          case 67: break;
          case 24: 
            { return ParserSClaim.AGOAL;
            }
          case 68: break;
          case 25: 
            { return ParserSClaim.READK;
            }
          case 69: break;
          case 26: 
            { return ParserSClaim.MGOAL;
            }
          case 70: break;
          case 27: 
            { return ParserSClaim.WHILE;
            }
          case 71: break;
          case 28: 
            { return ParserSClaim.TARGET;
            }
          case 72: break;
          case 29: 
            { return ParserSClaim.STRUCT;
            }
          case 73: break;
          case 30: 
            { return ParserSClaim.PARENT;
            }
          case 74: break;
          case 31: 
            { return ParserSClaim.ACTION;
            }
          case 75: break;
          case 32: 
            { return ParserSClaim.CYCLIC;
            }
          case 76: break;
          case 33: 
            { return ParserSClaim.OUTPUT;
            }
          case 77: break;
          case 34: 
            { return ParserSClaim.INITIAL;
            }
          case 78: break;
          case 35: 
            { return ParserSClaim.ACHIEVE;
            }
          case 79: break;
          case 36: 
            { return ParserSClaim.RECEIVE;
            }
          case 80: break;
          case 37: 
            { return ParserSClaim.REMOVEK;
            }
          case 81: break;
          case 38: 
            { return ParserSClaim.FORALLK;
            }
          case 82: break;
          case 39: 
            { return ParserSClaim.MESSAGE;
            }
          case 83: break;
          case 40: 
            { return ParserSClaim.REACTIVE;
            }
          case 84: break;
          case 41: 
            { return ParserSClaim.BEHAVIOR;
            }
          case 85: break;
          case 42: 
            { return ParserSClaim.MAINTAIN;
            }
          case 86: break;
          case 43: 
            { return ParserSClaim.PROACTIVE;
            }
          case 87: break;
          case 44: 
            { return ParserSClaim.CONDITION;
            }
          case 88: break;
          default:
            zzScanError(ZZ_NO_MATCH);
        }
      }
    }
  }


}
