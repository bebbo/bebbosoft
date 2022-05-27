// class: de.bb.bejy.mail.spf.SpfParser
/*
rule: VCHAR ::= [^ /];
rule: SP ::= [ ];
rule: DIGIT ::= [0-9];
rule: PREFIX ::= [-+?~];
rule: ALPHA ::= [a-zA-Z];
rule: ALPHADIGITEXT ::= [a-zA-Z0-9-_.];
rule: DELIMITER ::= [-.+,/_=];
rule: Name ::= $NAME ALPHA ALPHADIGITEXT* $NAME_END;
rule: Version ::= "v=spf" DIGIT;
rule: Directive ::= PREFIX $PREFIX Name Mechanism;
rule: Mechanism ::= ( ':' MacroString)?               
              ('/' $MASK DIGIT+ $MASK_END)? 
              $MECHANISM;
rule: Modifier ::= '=' MacroString $MODIFIER;
rule: SPF ::= Version $VERSION (SP (Directive | Name (Modifier | Mechanism)))* SP*;
rule: MacroString ::= $MACRO ( '%%' $PERCENT 
                | '%_' $UNDERSCORE
                | '%-' $MINUS
                | '%{' ALPHA $TRANSFORM Transformer $DELIMITER (DELIMITER)* $TRANSFORM_END '}'
                | VCHAR $VCHAR
                | .
                )*;
rule: Transformer ::= DIGIT* $TRANSFORM_VAL ('r' $REVERSE)?;
*/
// replacing MacroString__6 with MacroString__7
// replacing MacroString__7 with DELIMITER
// replacing MacroString__9 with .
// replacing Mechanism__1 with Mechanism__2
// replacing Mechanism__3 with Mechanism__4
// replacing SPF__1 with SPF__2
// replacing SPF__4 with Directive
// replacing SPF__7 with Modifier
// replacing SPF__8 with Mechanism
// replacing Transformer__1 with Transformer__2
package de.bb.bejy.mail.spf;

public class SpfParser extends de.bb.bex2.Parser {
  public final static int ID_DELIMITER = 1;
  public final static int ID_MACRO = 2;
  public final static int ID_MASK = 3;
  public final static int ID_MASK_END = 4;
  public final static int ID_MECHANISM = 5;
  public final static int ID_MINUS = 6;
  public final static int ID_MODIFIER = 7;
  public final static int ID_NAME = 8;
  public final static int ID_NAME_END = 9;
  public final static int ID_PERCENT = 10;
  public final static int ID_PREFIX = 11;
  public final static int ID_REVERSE = 12;
  public final static int ID_TRANSFORM = 13;
  public final static int ID_TRANSFORM_END = 14;
  public final static int ID_TRANSFORM_VAL = 15;
  public final static int ID_UNDERSCORE = 16;
  public final static int ID_VCHAR = 17;
  public final static int ID_VERSION = 18;
  public final static int ID_LEFT = 19;

  public SpfParser(de.bb.bex2.Scanner scanner, de.bb.bex2.Context ctx) {
    super(scanner, ctx);
  }

  private final static String data_W_v_x003dspf = "v=spf";
  private final static String data_W__x0025_x0025 = "%%";
  private final static String data_W__x0025_x002d = "%-";
  private final static String data_W__x0025_ = "%_";
  private final static String data_W__x0025_x007b = "%{";
  private final static String data_W__x002f = "/";
  private final static String data_W__x003a = ":";
  private final static String data_W__x003d = "=";
  private final static String data_W_r = "r";
  private final static String data_W_l = "l";
  private final static String data_W__x007d = "}";
  private final static String data_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private final static String data_ALPHADIGITEXT = "-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
  private final static String data_DELIMITER = "+,-./=_";
  private final static String data_DIGIT = "0123456789";
  private final static String data_PREFIX = "+-?~";
  private final static String data_SP = " ";
  private final static String data_VCHAR = " /";

  private
  boolean is_W_v_x003dspf()
  {
    return scanner.isWord(data_W_v_x003dspf);
  }

  private
  boolean is_W__x0025_x0025()
  {
    return scanner.isWord(data_W__x0025_x0025);
  }

  private
  boolean is_W__x0025_x002d()
  {
    return scanner.isWord(data_W__x0025_x002d);
  }

  private
  boolean is_W__x0025_()
  {
    return scanner.isWord(data_W__x0025_);
  }

  private
  boolean is_W__x0025_x007b()
  {
    return scanner.isWord(data_W__x0025_x007b);
  }

  private
  boolean is_W__x002f()
  {
    return scanner.isWord(data_W__x002f);
  }

  private
  boolean is_W__x003a()
  {
    return scanner.isWord(data_W__x003a);
  }

  private
  boolean is_W__x003d()
  {
    return scanner.isWord(data_W__x003d);
  }

  private
  boolean is_W_r()
  {
    return scanner.isWord(data_W_r);
  }

  private
  boolean is_W_l()
  {
    return scanner.isWord(data_W_l);
  }

  private
  boolean is_W__x007d()
  {
    return scanner.isWord(data_W__x007d);
  }

  private
  boolean is_ALPHA()
  {
    if (data_ALPHA.indexOf(scanner.peek()) >= 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  private
  boolean is_ALPHADIGITEXT()
  {
    if (data_ALPHADIGITEXT.indexOf(scanner.peek()) >= 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  private
  boolean is_DELIMITER()
  {
    if (data_DELIMITER.indexOf(scanner.peek()) >= 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  private
  boolean is_DIGIT()
  {
    if (data_DIGIT.indexOf(scanner.peek()) >= 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  private
  boolean is_Directive() throws de.bb.bex2.ParseException
  {
    if (is_PREFIX()) {
      if (context.notify(ID_PREFIX)) {
        if (is_Name()) {
          if (is_Mechanism()) {
            return true;
          }
        }
      }
      throw new de.bb.bex2.ParseException("Directive");//3
    }
    return false;
  }

  public
  boolean is_MacroString() throws de.bb.bex2.ParseException
  {
    if (context.notify(ID_MACRO)) {
      while (is_MacroString__1()) {}
      return true;
    }
    return false;
  }

  private
  boolean is_MacroString__1() throws de.bb.bex2.ParseException
  {
    if (is_MacroString__2()) {
      return true;
    }
    if (is_MacroString__3()) {
      return true;
    }
    if (is_MacroString__4()) {
      return true;
    }
    if (is_MacroString__5()) {
      return true;
    }
    if (is_MacroString__8()) {
      return true;
    }
    return false;
  }

  private
  boolean is_MacroString__2() throws de.bb.bex2.ParseException
  {
    if (is_W__x0025_x0025()) {
      if (context.notify(ID_PERCENT)) {
        return true;
      }
      throw new de.bb.bex2.ParseException("MacroString__2");//1
    }
    return false;
  }

  private
  boolean is_MacroString__3() throws de.bb.bex2.ParseException
  {
    if (is_W__x0025_()) {
      if (context.notify(ID_UNDERSCORE)) {
        return true;
      }
      throw new de.bb.bex2.ParseException("MacroString__3");//1
    }
    return false;
  }

  private
  boolean is_MacroString__4() throws de.bb.bex2.ParseException
  {
    if (is_W__x0025_x002d()) {
      if (context.notify(ID_MINUS)) {
        return true;
      }
      throw new de.bb.bex2.ParseException("MacroString__4");//1
    }
    return false;
  }

  private
  boolean is_MacroString__5() throws de.bb.bex2.ParseException
  {
    if (is_W__x0025_x007b()) {
      if (is_ALPHA()) {
        if (context.notify(ID_TRANSFORM)) {
          if (is_Transformer()) {
            if (context.notify(ID_DELIMITER)) {
              while (is_DELIMITER()) {}
              if (context.notify(ID_TRANSFORM_END)) {
                if (is_W__x007d()) {
                  return true;
                }
              }
            }
          }
        }
      }
      throw new de.bb.bex2.ParseException("MacroString__5");//6
    }
    return false;
  }

  private
  boolean is_MacroString__8() throws de.bb.bex2.ParseException
  {
    if (is_VCHAR()) {
      if (context.notify(ID_VCHAR)) {
        return true;
      }
      throw new de.bb.bex2.ParseException("MacroString__8");//1
    }
    return false;
  }

  private
  boolean is_Mechanism() throws de.bb.bex2.ParseException
  {
    is_Mechanism__2();
    is_Mechanism__4();
    if (context.notify(ID_MECHANISM)) {
      return true;
    }
    return false;
  }

  private
  boolean is_Mechanism__2() throws de.bb.bex2.ParseException
  {
    if (is_W__x003a()) {
      if (is_MacroString()) {
        return true;
      }
      throw new de.bb.bex2.ParseException("Mechanism__2");//1
    }
    return false;
  }

  private
  boolean is_Mechanism__4() throws de.bb.bex2.ParseException
  {
    if (is_W__x002f()) {
      if (context.notify(ID_MASK)) {
        if (is_DIGIT()) {
while (is_DIGIT()) {}
          if (context.notify(ID_MASK_END)) {
            return true;
          }
        }
      }
      throw new de.bb.bex2.ParseException("Mechanism__4");//3
    }
    return false;
  }

  private
  boolean is_Modifier() throws de.bb.bex2.ParseException
  {
    if (is_W__x003d()) {
      if (is_MacroString()) {
        if (context.notify(ID_MODIFIER)) {
          return true;
        }
      }
      throw new de.bb.bex2.ParseException("Modifier");//2
    }
    return false;
  }

  private
  boolean is_Name() throws de.bb.bex2.ParseException
  {
    if (context.notify(ID_NAME)) {
      if (is_ALPHA()) {
        while (is_ALPHADIGITEXT()) {}
        if (context.notify(ID_NAME_END)) {
          return true;
        }
      }
    }
    return false;
  }

  private
  boolean is_PREFIX()
  {
    if (data_PREFIX.indexOf(scanner.peek()) >= 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  private
  boolean is_SP()
  {
    if (data_SP.indexOf(scanner.peek()) >= 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  public
  boolean is_SPF() throws de.bb.bex2.ParseException
  {
    if (is_Version()) {
      if (context.notify(ID_VERSION)) {
        while (is_SPF__2()) {}
        while (is_SP()) {}
        return true;
      }
      throw new de.bb.bex2.ParseException("SPF");//1
    }
    return false;
  }

  private
  boolean is_SPF__2() throws de.bb.bex2.ParseException
  {
    if (is_SP()) {
      if (is_SPF__3()) {
        return true;
      }
      throw new de.bb.bex2.ParseException("SPF__2");//1
    }
    return false;
  }

  private
  boolean is_SPF__3() throws de.bb.bex2.ParseException
  {
    if (is_Directive()) {
      return true;
    }
    if (is_SPF__5()) {
      return true;
    }
    throw new de.bb.bex2.ParseException("SPF__3");
  }

  private
  boolean is_SPF__5() throws de.bb.bex2.ParseException
  {
    if (is_Name()) {
      if (is_SPF__6()) {
        return true;
      }
      throw new de.bb.bex2.ParseException("SPF__5");//1
    }
    return false;
  }

  private
  boolean is_SPF__6() throws de.bb.bex2.ParseException
  {
    if (is_Modifier()) {
      return true;
    }
    if (is_Mechanism()) {
      return true;
    }
    throw new de.bb.bex2.ParseException("SPF__6");
  }

  private
  boolean is_Transformer() throws de.bb.bex2.ParseException
  {
    while (is_DIGIT()) {}
    if (context.notify(ID_TRANSFORM_VAL)) {
      is_Transformer__2();
      return true;
    }
    return false;
  }

  private
  boolean is_Transformer__2() throws de.bb.bex2.ParseException
  {
    if (is_W_r()) {
      if (context.notify(ID_REVERSE)) {
        return true;
      }
      throw new de.bb.bex2.ParseException("Transformer__2");//1
    }
    if (is_W_l()) {
        if (context.notify(ID_LEFT)) {
            return true;
          }
       throw new de.bb.bex2.ParseException("Transformer__2");//1
    }
    return false;
  }

  private
  boolean is_VCHAR()
  {
    if (scanner.peek() >= 0 && data_VCHAR.indexOf(scanner.peek()) < 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  private
  boolean is_Version() throws de.bb.bex2.ParseException
  {
    if (is_W_v_x003dspf()) {
      if (is_DIGIT()) {
        return true;
      }
      throw new de.bb.bex2.ParseException("Version");//1
    }
    return false;
  }

}

