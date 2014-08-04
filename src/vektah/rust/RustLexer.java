/* The following code was generated by JFlex 1.4.3 on 04.08.14 20:05 */

package vektah.rust;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.psi.TokenType;
import vektah.rust.psi.RustTokens;


/**
 * This class is a scanner generated by 
 * <a href="http://www.jflex.de/">JFlex</a> 1.4.3
 * on 04.08.14 20:05 from the specification file
 * <tt>F:/consulo-rust/src/vektah/rust/RustLexer.flex</tt>
 */
class RustLexer implements FlexLexer {
  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int IN_BLOCK_COMMENT = 2;
  public static final int IN_RAW_STRING = 4;
  public static final int YYINITIAL = 0;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = { 
     0,  0,  1,  1,  2, 2
  };

  /** 
   * Translates characters to character classes
   */
  private static final String ZZ_CMAP_PACKED = 
    "\11\0\1\1\1\62\2\61\1\10\22\0\1\17\1\56\1\5\1\63"+
    "\1\77\1\74\1\70\1\6\1\102\1\103\1\60\1\30\1\106\1\67"+
    "\1\32\1\57\1\12\1\22\1\25\1\24\1\26\1\36\1\23\1\36"+
    "\1\21\1\3\1\73\1\107\1\66\1\65\1\64\1\16\1\76\4\4"+
    "\1\27\1\4\16\2\1\15\5\2\1\100\1\7\1\101\1\72\1\31"+
    "\1\16\1\37\1\34\1\44\1\52\1\42\1\33\1\2\1\51\1\20"+
    "\1\2\1\43\1\46\1\47\1\11\1\35\1\50\1\2\1\41\1\40"+
    "\1\45\1\14\1\53\1\55\1\13\1\54\1\2\1\104\1\71\1\105"+
    "\1\75\6\0\1\61\u1fa2\0\2\61\udfd6\0";

  /** 
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\3\0\1\1\1\2\1\3\1\4\1\1\1\5\1\4"+
    "\2\3\1\6\1\7\14\3\1\10\1\11\1\12\1\13"+
    "\1\14\1\15\1\16\1\17\1\20\1\21\1\22\1\23"+
    "\1\24\1\25\1\26\1\27\1\30\1\31\1\32\1\33"+
    "\1\34\1\35\1\36\1\37\3\40\1\41\1\42\1\4"+
    "\2\0\1\4\1\0\1\43\7\0\2\3\1\44\1\45"+
    "\1\3\1\46\1\47\4\3\1\50\3\3\1\51\1\3"+
    "\1\0\17\3\1\52\1\53\1\54\1\0\1\55\1\56"+
    "\1\57\1\60\1\0\1\61\1\62\1\63\1\64\1\4"+
    "\3\0\1\4\1\0\1\65\1\4\1\65\3\0\1\66"+
    "\3\0\1\67\1\70\1\71\1\3\1\72\1\3\1\73"+
    "\1\74\1\3\1\75\5\3\1\76\12\3\1\77\1\100"+
    "\1\101\2\3\1\102\3\3\1\53\1\103\1\53\1\104"+
    "\1\0\1\105\1\106\6\0\1\67\1\70\1\71\1\3"+
    "\1\107\3\3\1\110\3\3\1\111\1\3\1\112\2\3"+
    "\1\113\1\3\1\114\1\115\2\3\1\116\1\117\1\3"+
    "\3\120\1\121\2\0\1\67\3\0\1\70\3\0\1\71"+
    "\3\0\1\3\1\122\1\123\1\124\5\3\1\125\1\126"+
    "\1\3\1\127\1\130\2\0\1\131\1\132\1\133\1\134"+
    "\1\135\4\3\1\136\4\3\1\137";

  private static int [] zzUnpackAction() {
    int [] result = new int[253];
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
    "\0\0\0\110\0\220\0\330\0\u0120\0\u0168\0\u01b0\0\u01f8"+
    "\0\u0240\0\u0288\0\u02d0\0\u0318\0\330\0\u0360\0\u03a8\0\u03f0"+
    "\0\u0438\0\u0480\0\u04c8\0\u0510\0\u0558\0\u05a0\0\u05e8\0\u0630"+
    "\0\u0678\0\u06c0\0\u0708\0\u0750\0\330\0\330\0\u0798\0\u07e0"+
    "\0\u0828\0\u0870\0\330\0\330\0\330\0\u08b8\0\330\0\330"+
    "\0\330\0\330\0\330\0\330\0\330\0\330\0\330\0\330"+
    "\0\330\0\330\0\u0900\0\u0948\0\u0990\0\330\0\u09d8\0\u0a20"+
    "\0\u0a68\0\u0ab0\0\u0af8\0\u01f8\0\330\0\u0b40\0\u0b88\0\u0bd0"+
    "\0\u0c18\0\u0c60\0\u0ca8\0\u0cf0\0\u0d38\0\u0d80\0\u0168\0\u0168"+
    "\0\u0dc8\0\u0e10\0\u0168\0\u0e58\0\u0ea0\0\u0ee8\0\u0f30\0\u0168"+
    "\0\u0f78\0\u0fc0\0\u1008\0\330\0\u1050\0\u1098\0\u10e0\0\u1128"+
    "\0\u1170\0\u11b8\0\u1200\0\u1248\0\u1290\0\u12d8\0\u1320\0\u1368"+
    "\0\u13b0\0\u13f8\0\u1440\0\u1488\0\u14d0\0\330\0\u1518\0\u1560"+
    "\0\u15a8\0\330\0\330\0\330\0\330\0\u15f0\0\330\0\330"+
    "\0\330\0\330\0\330\0\u1638\0\u1680\0\u16c8\0\u1710\0\u1758"+
    "\0\330\0\u17a0\0\u17a0\0\u17e8\0\u1830\0\u1878\0\330\0\u18c0"+
    "\0\u1908\0\u1950\0\u1998\0\u19e0\0\u1a28\0\u1a70\0\u0168\0\u1ab8"+
    "\0\330\0\u0168\0\u1b00\0\u0168\0\u1b48\0\u1b90\0\u1bd8\0\u1c20"+
    "\0\u1c68\0\u0168\0\u1cb0\0\u1cf8\0\u1d40\0\u1d88\0\u1dd0\0\u1e18"+
    "\0\u1e60\0\u1ea8\0\u1ef0\0\u1f38\0\u0168\0\u0168\0\u0168\0\u1f80"+
    "\0\u1fc8\0\u0168\0\u2010\0\u2058\0\u20a0\0\u20e8\0\u2130\0\u2178"+
    "\0\330\0\u21c0\0\330\0\330\0\u2208\0\u2250\0\u2298\0\u22e0"+
    "\0\u2328\0\u2370\0\u23b8\0\u2400\0\u2448\0\u2490\0\u0168\0\u24d8"+
    "\0\u2520\0\u2568\0\u0168\0\u25b0\0\u25f8\0\u2640\0\u0168\0\u2688"+
    "\0\u0168\0\u26d0\0\u2718\0\u0168\0\u2760\0\u0168\0\u0168\0\u27a8"+
    "\0\u27f0\0\u0168\0\u0168\0\u2838\0\u2880\0\u2130\0\330\0\330"+
    "\0\u28c8\0\u2910\0\330\0\u2958\0\u29a0\0\u29e8\0\330\0\u2a30"+
    "\0\u2a78\0\u2ac0\0\330\0\u2b08\0\u2b50\0\u2b98\0\u2be0\0\u0168"+
    "\0\u0168\0\u0168\0\u2c28\0\u2c70\0\u2cb8\0\u2d00\0\u2d48\0\u0168"+
    "\0\u0168\0\u2d90\0\u0168\0\u0168\0\u2dd8\0\u2e20\0\u0168\0\u0168"+
    "\0\u0168\0\u0168\0\u0168\0\u2e68\0\u2eb0\0\u2ef8\0\u2f40\0\u0168"+
    "\0\u2f88\0\u2fd0\0\u3018\0\u3060\0\330";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[253];
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
    "\1\4\1\5\1\6\1\7\1\6\1\10\1\11\1\4"+
    "\1\5\1\6\1\12\1\6\1\13\1\6\1\4\1\5"+
    "\1\14\6\7\1\6\1\15\1\6\1\16\1\17\1\20"+
    "\1\6\1\7\1\21\1\22\1\23\1\24\1\6\1\25"+
    "\1\26\1\27\1\30\1\31\4\6\1\32\1\33\1\34"+
    "\1\35\1\4\1\5\1\36\1\37\1\40\1\41\1\42"+
    "\1\43\1\44\1\45\1\46\1\47\1\50\1\51\1\52"+
    "\1\53\1\54\1\55\1\56\1\57\1\60\1\61\1\62"+
    "\57\63\1\64\1\65\27\63\5\66\1\67\102\66\111\0"+
    "\1\5\6\0\1\5\6\0\1\5\42\0\1\5\27\0"+
    "\3\6\4\0\5\6\2\0\10\6\1\0\1\6\1\0"+
    "\23\6\35\0\1\7\6\0\1\7\1\0\1\70\3\0"+
    "\1\70\6\7\1\71\1\0\1\7\1\72\1\73\2\0"+
    "\1\7\3\0\1\71\45\0\5\74\1\75\1\74\1\76"+
    "\100\74\2\77\4\100\1\0\1\101\1\77\50\100\2\77"+
    "\25\100\3\0\1\7\6\0\1\7\1\102\1\70\3\0"+
    "\1\70\6\7\1\71\1\0\1\7\1\72\1\73\1\103"+
    "\1\104\1\7\3\0\1\71\47\0\3\6\4\0\1\105"+
    "\4\6\2\0\10\6\1\0\1\6\1\0\5\6\1\106"+
    "\15\6\34\0\3\6\4\0\1\107\4\6\2\0\10\6"+
    "\1\0\1\6\1\0\1\110\13\6\1\111\6\6\64\0"+
    "\1\112\57\0\3\6\4\0\1\113\4\6\2\0\10\6"+
    "\1\0\1\6\1\0\2\6\1\114\1\6\1\115\16\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\2\6\1\116\3\6\1\117\14\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\5\6"+
    "\1\120\15\6\34\0\3\6\4\0\3\6\1\121\1\6"+
    "\2\0\10\6\1\0\1\6\1\0\7\6\1\122\2\6"+
    "\1\123\10\6\34\0\3\6\1\124\3\0\5\6\2\0"+
    "\10\6\1\0\1\6\1\0\7\6\1\125\13\6\5\0"+
    "\1\126\26\0\3\6\4\0\1\127\1\6\1\130\2\6"+
    "\2\0\10\6\1\0\1\6\1\0\13\6\1\131\7\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\2\6\1\132\3\6\1\133\14\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\6\6"+
    "\1\134\12\6\1\135\1\6\34\0\3\6\4\0\5\6"+
    "\2\0\10\6\1\0\1\6\1\0\2\6\1\136\4\6"+
    "\1\137\13\6\34\0\3\6\4\0\3\6\1\140\1\6"+
    "\2\0\10\6\1\0\1\6\1\0\2\6\1\141\1\6"+
    "\1\142\16\6\34\0\3\6\4\0\3\6\1\143\1\6"+
    "\2\0\10\6\1\0\1\6\1\0\6\6\1\144\14\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\16\6\1\145\4\6\117\0\1\146\101\0\1\147"+
    "\1\150\113\0\1\151\1\152\106\0\1\153\1\154\107\0"+
    "\1\155\1\156\105\0\1\157\116\0\1\160\14\0\57\63"+
    "\2\0\27\63\60\0\1\161\106\0\1\162\113\0\1\67"+
    "\45\0\1\163\1\164\1\165\1\166\66\0\1\167\6\0"+
    "\1\167\6\0\6\167\1\0\1\170\1\167\4\0\1\167"+
    "\30\0\1\170\20\0\3\171\1\172\6\171\1\172\6\171"+
    "\6\172\2\171\1\173\1\0\3\171\1\172\3\171\1\0"+
    "\45\171\23\0\1\165\1\166\70\0\6\74\1\174\1\175"+
    "\1\176\23\0\1\74\3\0\1\74\14\0\1\74\25\0"+
    "\2\100\4\0\1\177\1\0\1\100\50\0\2\100\33\0"+
    "\1\177\107\0\5\100\1\200\1\201\1\202\23\0\1\100"+
    "\3\0\1\100\14\0\1\100\30\0\2\203\5\0\1\203"+
    "\6\0\7\203\1\0\1\203\1\0\2\203\1\0\2\203"+
    "\2\0\1\203\1\0\1\203\5\0\1\203\47\0\1\204"+
    "\7\0\1\204\6\0\1\204\70\0\1\205\7\0\5\205"+
    "\2\0\1\205\4\0\1\205\53\0\3\6\4\0\5\6"+
    "\2\0\10\6\1\0\1\6\1\0\5\6\1\206\15\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\7\6\1\207\13\6\34\0\3\6\4\0\5\6"+
    "\2\0\10\6\1\0\1\6\1\0\15\6\1\210\5\6"+
    "\64\0\1\211\57\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\6\6\1\212\14\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\13\6"+
    "\1\213\7\6\34\0\3\6\4\0\2\6\1\214\2\6"+
    "\2\0\10\6\1\0\1\6\1\0\23\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\7\6"+
    "\1\215\13\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\15\6\1\216\5\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\13\6"+
    "\1\217\7\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\4\6\1\220\1\6\1\221\14\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\1\222\11\6\1\223\10\6\37\0\1\124\55\0"+
    "\1\126\26\0\3\6\4\0\3\6\1\224\1\6\2\0"+
    "\10\6\1\0\1\6\1\0\23\6\34\0\3\6\4\0"+
    "\5\6\2\0\10\6\1\0\1\6\1\0\12\6\1\225"+
    "\10\6\34\0\3\6\4\0\5\6\2\0\10\6\1\0"+
    "\1\6\1\0\5\6\1\226\15\6\34\0\3\6\4\0"+
    "\1\227\4\6\2\0\10\6\1\0\1\6\1\0\23\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\4\6\1\230\16\6\34\0\3\6\4\0\3\6"+
    "\1\231\1\6\2\0\10\6\1\0\1\6\1\0\4\6"+
    "\1\232\16\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\15\6\1\233\5\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\2\6"+
    "\1\234\20\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\12\6\1\235\10\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\12\6"+
    "\1\236\10\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\17\6\1\237\3\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\11\6"+
    "\1\240\1\241\10\6\34\0\3\6\4\0\5\6\2\0"+
    "\10\6\1\0\1\6\1\0\1\6\1\242\21\6\34\0"+
    "\3\6\4\0\5\6\2\0\1\243\7\6\1\0\1\6"+
    "\1\0\2\6\1\244\20\6\34\0\3\6\4\0\5\6"+
    "\2\0\1\245\7\6\1\0\1\6\1\0\23\6\32\0"+
    "\10\246\1\0\45\246\1\247\1\250\2\246\1\0\25\246"+
    "\56\0\1\251\1\0\1\252\114\0\1\253\107\0\1\254"+
    "\45\0\1\163\112\0\1\163\106\0\1\163\65\0\1\167"+
    "\6\0\1\167\6\0\6\167\2\0\1\167\1\0\1\73"+
    "\2\0\1\167\54\0\1\167\6\0\1\167\6\0\6\167"+
    "\2\0\1\167\4\0\1\167\54\0\1\172\6\0\1\172"+
    "\6\0\6\172\1\71\1\0\1\172\1\0\1\73\2\0"+
    "\1\172\3\0\1\71\50\0\2\255\5\0\1\255\6\0"+
    "\7\255\3\0\2\255\1\0\2\255\2\0\1\255\1\0"+
    "\1\255\5\0\1\255\40\0\2\256\5\0\1\256\6\0"+
    "\7\256\3\0\2\256\1\0\2\256\2\0\1\256\1\0"+
    "\1\256\5\0\1\256\40\0\2\257\5\0\1\257\6\0"+
    "\7\257\3\0\2\257\1\0\2\257\2\0\1\257\1\0"+
    "\1\257\5\0\1\257\40\0\2\260\5\0\1\260\6\0"+
    "\7\260\3\0\2\260\1\0\2\260\2\0\1\260\1\0"+
    "\1\260\5\0\1\260\40\0\2\261\5\0\1\261\6\0"+
    "\7\261\3\0\2\261\1\0\2\261\2\0\1\261\1\0"+
    "\1\261\5\0\1\261\40\0\2\262\5\0\1\262\6\0"+
    "\7\262\3\0\2\262\1\0\2\262\2\0\1\262\1\0"+
    "\1\262\5\0\1\262\40\0\2\203\5\0\1\203\1\0"+
    "\1\263\3\0\1\263\7\203\1\0\1\203\1\0\2\203"+
    "\1\0\2\203\2\0\1\203\1\0\1\203\5\0\1\203"+
    "\47\0\1\204\1\0\1\264\3\0\1\264\1\0\1\204"+
    "\6\0\1\204\70\0\1\205\1\0\1\265\3\0\1\265"+
    "\1\0\5\205\2\0\1\205\4\0\1\205\53\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\4\6"+
    "\1\266\16\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\13\6\1\267\7\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\5\6"+
    "\1\270\15\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\4\6\1\271\16\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\7\6"+
    "\1\272\13\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\1\273\22\6\34\0\3\6\4\0"+
    "\5\6\2\0\10\6\1\0\1\6\1\0\12\6\1\274"+
    "\10\6\34\0\3\6\4\0\3\6\1\275\1\6\2\0"+
    "\10\6\1\0\1\6\1\0\23\6\34\0\3\6\4\0"+
    "\3\6\1\276\1\6\2\0\10\6\1\0\1\6\1\0"+
    "\23\6\34\0\3\6\4\0\5\6\2\0\10\6\1\0"+
    "\1\6\1\0\14\6\1\277\6\6\34\0\3\6\4\0"+
    "\5\6\2\0\10\6\1\0\1\6\1\0\7\6\1\300"+
    "\13\6\34\0\3\6\4\0\5\6\2\0\10\6\1\0"+
    "\1\6\1\0\7\6\1\301\13\6\34\0\3\6\4\0"+
    "\5\6\2\0\10\6\1\0\1\6\1\0\12\6\1\302"+
    "\10\6\34\0\3\6\4\0\5\6\2\0\10\6\1\0"+
    "\1\6\1\0\12\6\1\303\10\6\34\0\3\6\4\0"+
    "\5\6\2\0\10\6\1\0\1\6\1\0\7\6\1\304"+
    "\13\6\34\0\3\6\4\0\5\6\2\0\1\305\7\6"+
    "\1\0\1\6\1\0\23\6\34\0\3\6\4\0\5\6"+
    "\2\0\10\6\1\0\1\6\1\0\7\6\1\306\13\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\15\6\1\307\5\6\34\0\3\6\4\0\5\6"+
    "\2\0\10\6\1\0\1\6\1\0\6\6\1\310\14\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\11\6\1\311\11\6\34\0\3\6\4\0\5\6"+
    "\2\0\10\6\1\0\1\6\1\0\20\6\1\312\2\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\11\6\1\313\11\6\34\0\3\6\4\0\5\6"+
    "\2\0\10\6\1\0\1\6\1\0\13\6\1\314\7\6"+
    "\32\0\10\246\1\0\51\246\1\0\25\246\10\247\1\0"+
    "\51\247\1\0\35\247\1\315\46\247\1\246\1\247\1\316"+
    "\1\317\25\247\57\251\1\320\1\0\27\251\3\0\2\74"+
    "\5\0\1\74\6\0\7\74\3\0\2\74\1\0\2\74"+
    "\2\0\1\74\1\0\1\74\5\0\1\74\40\0\2\174"+
    "\5\0\1\174\6\0\7\174\3\0\2\174\1\0\2\174"+
    "\2\0\1\174\1\0\1\174\5\0\1\174\40\0\2\321"+
    "\5\0\1\321\6\0\7\321\3\0\2\321\1\0\2\321"+
    "\2\0\1\321\1\0\1\321\5\0\1\321\40\0\2\100"+
    "\5\0\1\100\6\0\7\100\3\0\2\100\1\0\2\100"+
    "\2\0\1\100\1\0\1\100\5\0\1\100\40\0\2\200"+
    "\5\0\1\200\6\0\7\200\3\0\2\200\1\0\2\200"+
    "\2\0\1\200\1\0\1\200\5\0\1\200\40\0\2\322"+
    "\5\0\1\322\6\0\7\322\3\0\2\322\1\0\2\322"+
    "\2\0\1\322\1\0\1\322\5\0\1\322\56\0\1\323"+
    "\1\324\1\325\1\326\104\0\1\327\1\330\1\331\1\332"+
    "\104\0\1\333\1\334\1\335\1\336\65\0\3\6\4\0"+
    "\5\6\2\0\10\6\1\0\1\6\1\0\1\337\22\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\7\6\1\340\13\6\34\0\3\6\4\0\5\6"+
    "\2\0\10\6\1\0\1\6\1\0\10\6\1\341\12\6"+
    "\34\0\3\6\4\0\5\6\2\0\10\6\1\0\1\6"+
    "\1\0\6\6\1\342\14\6\34\0\3\6\4\0\5\6"+
    "\2\0\1\343\7\6\1\0\1\6\1\0\23\6\34\0"+
    "\3\6\4\0\5\6\2\0\10\6\1\0\1\6\1\0"+
    "\11\6\1\344\11\6\34\0\3\6\4\0\5\6\2\0"+
    "\10\6\1\0\1\6\1\0\6\6\1\345\14\6\34\0"+
    "\3\6\4\0\5\6\2\0\10\6\1\0\1\6\1\0"+
    "\6\6\1\346\14\6\34\0\3\6\4\0\5\6\2\0"+
    "\1\347\7\6\1\0\1\6\1\0\23\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\7\6"+
    "\1\350\13\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\12\6\1\351\10\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\2\6"+
    "\1\352\20\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\16\6\1\353\4\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\7\6"+
    "\1\354\13\6\114\0\1\317\30\0\2\355\5\0\1\355"+
    "\6\0\7\355\3\0\2\355\1\0\2\355\2\0\1\355"+
    "\1\0\1\355\5\0\1\355\40\0\2\356\5\0\1\356"+
    "\6\0\7\356\3\0\2\356\1\0\2\356\2\0\1\356"+
    "\1\0\1\356\5\0\1\356\60\0\1\323\112\0\1\323"+
    "\106\0\1\323\105\0\1\327\112\0\1\327\106\0\1\327"+
    "\105\0\1\333\112\0\1\333\106\0\1\333\64\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\7\6"+
    "\1\357\13\6\34\0\3\6\4\0\5\6\2\0\10\6"+
    "\1\0\1\6\1\0\11\6\1\360\11\6\34\0\3\6"+
    "\4\0\5\6\2\0\10\6\1\0\1\6\1\0\12\6"+
    "\1\361\10\6\34\0\3\6\4\0\1\362\4\6\2\0"+
    "\10\6\1\0\1\6\1\0\23\6\34\0\3\6\4\0"+
    "\1\363\4\6\2\0\10\6\1\0\1\6\1\0\23\6"+
    "\34\0\3\6\4\0\1\364\4\6\2\0\10\6\1\0"+
    "\1\6\1\0\23\6\34\0\3\6\4\0\5\6\2\0"+
    "\10\6\1\0\1\365\1\0\23\6\35\0\2\175\5\0"+
    "\1\175\6\0\7\175\3\0\2\175\1\0\2\175\2\0"+
    "\1\175\1\0\1\175\5\0\1\175\40\0\2\201\5\0"+
    "\1\201\6\0\7\201\3\0\2\201\1\0\2\201\2\0"+
    "\1\201\1\0\1\201\5\0\1\201\37\0\3\6\4\0"+
    "\3\6\1\366\1\6\2\0\10\6\1\0\1\6\1\0"+
    "\23\6\34\0\3\6\4\0\5\6\2\0\10\6\1\0"+
    "\1\6\1\0\6\6\1\367\14\6\34\0\3\6\4\0"+
    "\5\6\2\0\10\6\1\0\1\6\1\0\7\6\1\370"+
    "\13\6\34\0\3\6\4\0\3\6\1\371\1\6\2\0"+
    "\10\6\1\0\1\6\1\0\23\6\34\0\3\6\4\0"+
    "\5\6\2\0\10\6\1\0\1\6\1\0\13\6\1\372"+
    "\7\6\34\0\3\6\4\0\5\6\2\0\10\6\1\0"+
    "\1\6\1\0\7\6\1\373\13\6\34\0\3\6\4\0"+
    "\5\6\2\0\10\6\1\0\1\6\1\0\5\6\1\374"+
    "\15\6\34\0\3\6\4\0\5\6\2\0\10\6\1\0"+
    "\1\6\1\0\23\6\1\375\31\0";

  private static int [] zzUnpackTrans() {
    int [] result = new int[12456];
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
  private static final char[] EMPTY_BUFFER = new char[0];
  private static final int YYEOF = -1;
  private static java.io.Reader zzReader = null; // Fake

  /* error messages for the codes above */
  private static final String ZZ_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\3\0\1\11\10\1\1\11\17\1\2\11\4\1\3\11"+
    "\1\1\14\11\3\1\1\11\2\1\2\0\1\1\1\0"+
    "\1\11\7\0\17\1\1\11\1\1\1\0\17\1\1\11"+
    "\2\1\1\0\4\11\1\0\5\11\3\0\1\1\1\0"+
    "\1\11\2\1\3\0\1\11\3\0\6\1\1\11\37\1"+
    "\1\11\1\0\2\11\6\0\34\1\2\11\2\0\1\11"+
    "\3\0\1\11\3\0\1\11\3\0\16\1\2\0\16\1"+
    "\1\11";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[253];
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

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private CharSequence zzBuffer = "";

  /** this buffer may contains the current text array to be matched when it is cheap to acquire it */
  private char[] zzBufferArray;

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the textposition at the last state to be included in yytext */
  private int zzPushbackPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /**
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /* user code: */
	private int start_comment;
	private int start_raw_string;
	private int raw_string_hashes;
	private int comment_depth;
	private boolean doc_comment;


  RustLexer(java.io.Reader in) {
    this.zzReader = in;
  }

  /**
   * Creates a new scanner.
   * There is also java.io.Reader version of this constructor.
   *
   * @param   in  the java.io.Inputstream to read input from.
   */
  RustLexer(java.io.InputStream in) {
    this(new java.io.InputStreamReader(in));
  }

  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    char [] map = new char[0x10000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 172) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }

  public final int getTokenStart(){
    return zzStartRead;
  }

  public final int getTokenEnd(){
    return getTokenStart() + yylength();
  }

  public void reset(CharSequence buffer, int start, int end,int initialState){
    zzBuffer = buffer;
    zzBufferArray = com.intellij.util.text.CharArrayUtil.fromSequenceWithoutCopying(buffer);
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzPushbackPos = 0;
    zzAtEOF  = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }

  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
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
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
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
    return zzBufferArray != null ? zzBufferArray[zzStartRead+pos]:zzBuffer.charAt(zzStartRead+pos);
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
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public IElementType advance() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;
    char[] zzBufferArrayL = zzBufferArray;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

      zzState = ZZ_LEXSTATE[zzLexicalState];


      zzForAction: {
        while (true) {

          if (zzCurrentPosL < zzEndReadL)
            zzInput = zzBufferL.charAt(zzCurrentPosL++);
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
              zzInput = zzBufferL.charAt(zzCurrentPosL++);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          int zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
        case 25: 
          { yybegin(YYINITIAL); return RustTokens.CLOSE_SQUARE_BRACKET;
          }
        case 96: break;
        case 46: 
          { yybegin(YYINITIAL); return RustTokens.FAT_ARROW;
          }
        case 97: break;
        case 20: 
          { yybegin(YYINITIAL); return RustTokens.REMAINDER;
          }
        case 98: break;
        case 11: 
          { yybegin(YYINITIAL); return RustTokens.HASH;
          }
        case 99: break;
        case 19: 
          { yybegin(YYINITIAL); return RustTokens.COLON;
          }
        case 100: break;
        case 52: 
          { if (--comment_depth == 0) {
			yybegin(YYINITIAL);
			zzStartRead = start_comment;
			return doc_comment ? RustTokens.BLOCK_DOC_COMMENT : RustTokens.BLOCK_COMMENT;
		} else {
			yybegin(IN_BLOCK_COMMENT);
		}
          }
        case 101: break;
        case 92: 
          { yybegin(YYINITIAL); return RustTokens.KW_RETURN;
          }
        case 102: break;
        case 9: 
          { yybegin(YYINITIAL); return RustTokens.DIVIDE;
          }
        case 103: break;
        case 88: 
          { yybegin(YYINITIAL); return RustTokens.KW_WHILE;
          }
        case 104: break;
        case 71: 
          { yybegin(YYINITIAL); return RustTokens.KW_IMPL;
          }
        case 105: break;
        case 72: 
          { yybegin(YYINITIAL); return RustTokens.KW_SELF;
          }
        case 106: break;
        case 22: 
          { yybegin(YYINITIAL); return RustTokens.AT;
          }
        case 107: break;
        case 75: 
          { yybegin(YYINITIAL); return RustTokens.KW_TRUE;
          }
        case 108: break;
        case 58: 
          { yybegin(YYINITIAL); return RustTokens.KW_USE;
          }
        case 109: break;
        case 43: 
          { yybegin(YYINITIAL); return RustTokens.LINE_COMMENT;
          }
        case 110: break;
        case 91: 
          { yybegin(YYINITIAL); return RustTokens.KW_STRUCT;
          }
        case 111: break;
        case 13: 
          { yybegin(YYINITIAL); return RustTokens.ASSIGN;
          }
        case 112: break;
        case 83: 
          { yybegin(YYINITIAL); return RustTokens.KW_BREAK;
          }
        case 113: break;
        case 2: 
          { yybegin(YYINITIAL); return TokenType.WHITE_SPACE;
          }
        case 114: break;
        case 86: 
          { yybegin(YYINITIAL); return RustTokens.KW_TRAIT;
          }
        case 115: break;
        case 81: 
          // lookahead expression with fixed base length
          zzMarkedPos = zzStartRead + 3;
          { yybegin(IN_BLOCK_COMMENT); start_comment = zzStartRead; doc_comment = true; comment_depth = 1;
          }
        case 116: break;
        case 68: 
          { yybegin(IN_BLOCK_COMMENT); start_comment = zzStartRead; doc_comment = true; comment_depth = 1;
          }
        case 117: break;
        case 65: 
          { yybegin(YYINITIAL); return RustTokens.KW_MOD;
          }
        case 118: break;
        case 30: 
          { yybegin(YYINITIAL); return RustTokens.COMMA;
          }
        case 119: break;
        case 45: 
          { yybegin(YYINITIAL); return RustTokens.GREATER_THAN_OR_EQUAL;
          }
        case 120: break;
        case 51: 
          { yybegin(IN_BLOCK_COMMENT); ++comment_depth;
          }
        case 121: break;
        case 26: 
          { yybegin(YYINITIAL); return RustTokens.OPEN_PAREN;
          }
        case 122: break;
        case 84: 
          { yybegin(YYINITIAL); return RustTokens.KW_SUPER;
          }
        case 123: break;
        case 24: 
          { yybegin(YYINITIAL); return RustTokens.OPEN_SQUARE_BRACKET;
          }
        case 124: break;
        case 70: 
          { yybegin(YYINITIAL); return RustTokens.ASSIGN_LEFT_SHIFT;
          }
        case 125: break;
        case 54: 
          { yybegin(YYINITIAL); return RustTokens.CHAR_LIT;
          }
        case 126: break;
        case 62: 
          { yybegin(YYINITIAL); return RustTokens.KW_REF;
          }
        case 127: break;
        case 35: 
          { yybegin(YYINITIAL); return RustTokens.STRING_LIT;
          }
        case 128: break;
        case 4: 
          { yybegin(YYINITIAL); return RustTokens.DEC_LIT;
          }
        case 129: break;
        case 53: 
          // lookahead expression with fixed lookahead length
          yypushback(1);
          { yybegin(YYINITIAL); return RustTokens.DEC_LIT;
          }
        case 130: break;
        case 48: 
          { yybegin(YYINITIAL); return RustTokens.LESS_THAN_OR_EQUAL;
          }
        case 131: break;
        case 79: 
          { yybegin(YYINITIAL); return RustTokens.KW_PROC;
          }
        case 132: break;
        case 80: 
          // lookahead expression with fixed base length
          zzMarkedPos = zzStartRead + 3;
          { yybegin(YYINITIAL); return RustTokens.LINE_DOC_COMMENT;
          }
        case 133: break;
        case 67: 
          { yybegin(YYINITIAL); return RustTokens.LINE_DOC_COMMENT;
          }
        case 134: break;
        case 66: 
          { yybegin(YYINITIAL); return RustTokens.KW_PUB;
          }
        case 135: break;
        case 64: 
          { yybegin(YYINITIAL); return RustTokens.KW_MUT;
          }
        case 136: break;
        case 89: 
          { yybegin(YYINITIAL); return RustTokens.KW_UNSAFE;
          }
        case 137: break;
        case 56: 
          { yybegin(YYINITIAL); return RustTokens.BIN_LIT;
          }
        case 138: break;
        case 36: 
          { yybegin(YYINITIAL); return RustTokens.KW_IN;
          }
        case 139: break;
        case 76: 
          { yybegin(YYINITIAL); return RustTokens.KW_TYPE;
          }
        case 140: break;
        case 59: 
          { yybegin(YYINITIAL); return RustTokens.TRIPLE_DOT;
          }
        case 141: break;
        case 69: 
          { yybegin(YYINITIAL); return RustTokens.ASSIGN_RIGHT_SHIFT;
          }
        case 142: break;
        case 78: 
          { yybegin(YYINITIAL); return RustTokens.KW_PRIV;
          }
        case 143: break;
        case 3: 
          { yybegin(YYINITIAL); return RustTokens.IDENTIFIER;
          }
        case 144: break;
        case 31: 
          { yybegin(YYINITIAL); return RustTokens.SEMICOLON;
          }
        case 145: break;
        case 57: 
          { yybegin(YYINITIAL); return RustTokens.OCT_LIT;
          }
        case 146: break;
        case 73: 
          { yybegin(YYINITIAL); return RustTokens.KW_ENUM;
          }
        case 147: break;
        case 44: 
          { yybegin(IN_BLOCK_COMMENT); start_comment = zzStartRead; doc_comment = false; comment_depth = 1;
          }
        case 148: break;
        case 39: 
          { yybegin(YYINITIAL); return RustTokens.KW_FN;
          }
        case 149: break;
        case 38: 
          { yybegin(YYINITIAL); return RustTokens.DOUBLE_DOT;
          }
        case 150: break;
        case 55: 
          { yybegin(YYINITIAL); return RustTokens.HEX_LIT;
          }
        case 151: break;
        case 49: 
          { yybegin(YYINITIAL); return RustTokens.THIN_ARROW;
          }
        case 152: break;
        case 17: 
          { yybegin(YYINITIAL); return RustTokens.BITWISE_OR;
          }
        case 153: break;
        case 82: 
          { yybegin(YYINITIAL); return RustTokens.KW_FALSE;
          }
        case 154: break;
        case 85: 
          { yybegin(YYINITIAL); return RustTokens.KW_CRATE;
          }
        case 155: break;
        case 16: 
          { yybegin(YYINITIAL); return RustTokens.BITWISE_AND;
          }
        case 156: break;
        case 14: 
          { yybegin(YYINITIAL); return RustTokens.LESS_THAN;
          }
        case 157: break;
        case 95: 
          { yybegin(YYINITIAL); return RustTokens.KW_MACRO_RULES;
          }
        case 158: break;
        case 32: 
          { yybegin(IN_BLOCK_COMMENT);
          }
        case 159: break;
        case 33: 
          { yybegin(IN_RAW_STRING);
          }
        case 160: break;
        case 7: 
          { yybegin(YYINITIAL); return RustTokens.DOT;
          }
        case 161: break;
        case 29: 
          { yybegin(YYINITIAL); return RustTokens.CLOSE_BRACE;
          }
        case 162: break;
        case 12: 
          { yybegin(YYINITIAL); return RustTokens.GREATER_THAN;
          }
        case 163: break;
        case 61: 
          { yybegin(YYINITIAL); return RustTokens.KW_BOX;
          }
        case 164: break;
        case 10: 
          { yybegin(YYINITIAL); return RustTokens.MULTIPLY;
          }
        case 165: break;
        case 34: 
          { if (yytext().length() >= raw_string_hashes) {
			// Greedily ate too many #'s ... lets rewind a sec.
			if (yytext().length() > raw_string_hashes) {
				yypushback(yytext().length() - raw_string_hashes);
			}
			yybegin(YYINITIAL);
			zzStartRead = start_raw_string;
			return RustTokens.RAW_STRING_LIT;
		} else {
			yybegin(IN_RAW_STRING);
		}
          }
        case 166: break;
        case 37: 
          { yybegin(YYINITIAL); return RustTokens.KW_IF;
          }
        case 167: break;
        case 60: 
          { yybegin(YYINITIAL); return RustTokens.KW_FOR;
          }
        case 168: break;
        case 15: 
          { yybegin(YYINITIAL); return RustTokens.MINUS;
          }
        case 169: break;
        case 50: 
          { yybegin(YYINITIAL); return RustTokens.DOUBLE_COLON;
          }
        case 170: break;
        case 94: 
          { yybegin(YYINITIAL); return RustTokens.KW_CONTINUE;
          }
        case 171: break;
        case 42: 
          { yybegin(YYINITIAL); return RustTokens.NOT_EQUAL;
          }
        case 172: break;
        case 74: 
          { yybegin(YYINITIAL); return RustTokens.KW_ELSE;
          }
        case 173: break;
        case 27: 
          { yybegin(YYINITIAL); return RustTokens.CLOSE_PAREN;
          }
        case 174: break;
        case 93: 
          { yybegin(YYINITIAL); return RustTokens.KW_EXTERN;
          }
        case 175: break;
        case 90: 
          { yybegin(YYINITIAL); return RustTokens.KW_STATIC;
          }
        case 176: break;
        case 41: 
          { yybegin(IN_RAW_STRING); start_raw_string = zzStartRead; raw_string_hashes = yytext().length() - 1;
          }
        case 177: break;
        case 40: 
          { yybegin(YYINITIAL); return RustTokens.KW_AS;
          }
        case 178: break;
        case 18: 
          { yybegin(YYINITIAL); return RustTokens.BITWISE_XOR;
          }
        case 179: break;
        case 5: 
          { yybegin(YYINITIAL); return RustTokens.SINGLE_QUOTE;
          }
        case 180: break;
        case 63: 
          { yybegin(YYINITIAL); return RustTokens.KW_LET;
          }
        case 181: break;
        case 8: 
          { yybegin(YYINITIAL); return RustTokens.NOT;
          }
        case 182: break;
        case 87: 
          { yybegin(YYINITIAL); return RustTokens.KW_MATCH;
          }
        case 183: break;
        case 1: 
          { yybegin(YYINITIAL); return TokenType.BAD_CHARACTER;
          }
        case 184: break;
        case 21: 
          { yybegin(YYINITIAL); return RustTokens.BOX;
          }
        case 185: break;
        case 23: 
          { yybegin(YYINITIAL); return RustTokens.DOLLAR;
          }
        case 186: break;
        case 28: 
          { yybegin(YYINITIAL); return RustTokens.OPEN_BRACE;
          }
        case 187: break;
        case 6: 
          { yybegin(YYINITIAL); return RustTokens.PLUS;
          }
        case 188: break;
        case 77: 
          { yybegin(YYINITIAL); return RustTokens.KW_LOOP;
          }
        case 189: break;
        case 47: 
          { yybegin(YYINITIAL); return RustTokens.EQUAL;
          }
        case 190: break;
        default:
          if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
            zzAtEOF = true;
            switch (zzLexicalState) {
            case IN_BLOCK_COMMENT: {
              yybegin(YYINITIAL); zzStartRead = start_comment; return RustTokens.BLOCK_COMMENT;
            }
            case 254: break;
            case IN_RAW_STRING: {
              yybegin(YYINITIAL); zzStartRead = start_raw_string; return RustTokens.RAW_STRING_LIT;
            }
            case 255: break;
            default:
            return null;
            }
          }
          else {
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
