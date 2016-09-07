// class: de.bb.jpa.QLParser
// use: de.bb.bex2.xml.Char for Char
// use: de.bb.bex2.xml.NameChar for NameChar
// use: de.bb.bex2.xml.Letter for Letter
// rule: SPACE ::= #x20 | #x0D | #x0A | #x09;
// rule: S ::= SPACE;
// rule: HEX ::= [0-9A-Fa-f];
// rule: IDENTCHARSTART ::= [a-zA-Z_$];
// rule: IDENTCHAR ::= [a-zA-Z0-9_$];
// rule: String ::= '"' stringEnd;
// rule: stringEnd ::= '"' | '\\u' HEX HEX HEX HEX stringEnd | '\\' ESC stringEnd | Char stringEnd;
// rule: Identification_variable ::= IDENTCHARSTART IDENTCHAR*;
// rule: QL ::= select_statement | update_statement | delete_statement;
// rule: select_statement ::= select_clause from_clause where_clause? groupby_clause? having_clause? orderby_clause?;
// rule: update_statement ::= update_clause where_clause?;
// rule: delete_statement ::= delete_clause where_clause?;
// rule: delete_clause ::= "DELETE" S+ "FROM" S+ abstract_schema_name (("AS" S+)? Identification_variable)?;
// rule: from_clause ::= "FROM" S+ identification_variable_declaration (S* "," S* (identification_variable_declaration | collection_member_declaration))*;
// rule: groupby_clause ::= "GROUP" S+ "BY" S+ groupby_item ("," S* groupby_item)*;
// rule: having_clause ::= "HAVING" S+ conditional_expression;
// rule: orderby_clause ::= "ORDER" S+ "BY" S+ orderby_item ("," S* orderby_item)*;
// rule: select_clause ::= "SELECT" S+ ("DISTINCT" S+)? select_expression ("," S* select_expression)*;
// rule: where_clause ::= "WHERE" S+ conditional_expression;
// rule: abstract_schema_name ::= (String | Identification_variable);
// rule: identification_variable_declaration ::= range_variable_declaration ( join | fetch_join )*;
// rule: range_variable_declaration ::= abstract_schema_name S+ ("AS" S+)? Identification_variable;
// rule: join ::= join_spec path_expression S+ ("AS" S+)? Identification_variable;
// rule: fetch_join ::= join_spec S+ "FETCH" S+ path_expression;
// rule: join_spec ::= ("LEFT" S+ (("OUTER" | "INNER") S+)? )? "JOIN" S+;
// rule: path_expression ::= Identification_variable ("." S* Identification_variable)+ S*;
// rule: opt_path_expression ::= path_expression;
// rule: collection_member_declaration ::= "IN" S* "(" S* path_expression ")" S* ("AS" S+)? Identification_variable;
// rule: groupby_item ::= opt_path_expression;
// rule: orderby_item ::= path_expression (("ASC" | "DESC" ) S*)?;
// rule: select_expression ::= aggregate_expression | "OBJECT" S* "(" S* Identification_variable ")" S* | constructor_expression | opt_path_expression;
// rule: constructor_expression ::= "NEW" S+ constructor_name "(" S* constructor_item ("," S* constructor_item)* ")" S*;
// rule: constructor_name ::= Identification_variable S* ("." S* Identification_variable S*)*;
// rule: constructor_item ::= path_expression;
// rule: aggregate_expression ::= ( "AVG" | "MAX" | "MIN" | "SUM" ) S* "(" S* ("DISTINCT" S+)? path_expression ")" S* |
//           "COUNT" S* "(" S* ("DISTINCT" S+)? opt_path_expression ")" S*;
// rule: update_clause ::= "UPDATE" S+ abstract_schema_name (("AS" S+)? Identification_variable)?
//                 "SET" S+ update_item ("," S* update_item)*;
// rule: update_item ::= path_expression "=" S* new_value;
// rule: new_value ::= simple_arithmetic_expression | primary | simple_entity_expression | "NULL" S*;
// rule: conditional_expression ::= ("NOT" S+)? ( "(" S* conditional_expression S* ")" S* | simple_cond_expression ) 
//         ("OR" (S+ conditional_expression | "(" S* conditional_expression S* ")"))? 
//         ("AND" (S+ conditional_expression | "(" S* conditional_expression S* ")"))?;
// rule: simple_cond_expression ::= comparison_expression | between_expression |like_expression | in_expression | null_comparison_expression 
//               | empty_collection_comparison_expression |collection_member_expression | exists_expression;
// rule: comparison_expression ::= expression comparison_operator (expression | all_or_any_expression);
// rule: comparison_operator ::= '=' | '>=' | '<=' | '<>' | '>' | '<';
// replacing QL__1 with select_statement
// replacing QL__2 with update_statement
// replacing QL__3 with delete_statement
// replacing S with SPACE
// replacing abstract_schema_name__2 with String
// replacing abstract_schema_name__3 with Identification_variable
// replacing aggregate_expression__10 with aggregate_expression__11
// replacing aggregate_expression__2 with "AVG"
// replacing aggregate_expression__3 with "MAX"
// replacing aggregate_expression__4 with "MIN"
// replacing aggregate_expression__5 with "SUM"
// replacing aggregate_expression__6 with aggregate_expression__7
// replacing collection_member_declaration__1 with collection_member_declaration__2
// replacing comparison_expression__2 with expression
// replacing comparison_expression__3 with all_or_any_expression
// replacing comparison_operator__1 with '='
// replacing comparison_operator__2 with '>='
// replacing comparison_operator__3 with '<='
// replacing comparison_operator__4 with '<>'
// replacing comparison_operator__5 with '>'
// replacing comparison_operator__6 with '<'
// replacing conditional_expression__1 with conditional_expression__2
// replacing conditional_expression__11 with conditional_expression__12
// replacing conditional_expression__5 with simple_cond_expression
// replacing conditional_expression__6 with conditional_expression__7
// replacing constructor_expression__1 with constructor_expression__2
// replacing constructor_name__1 with constructor_name__2
// replacing delete_clause__1 with delete_clause__2
// replacing delete_clause__3 with delete_clause__4
// replacing from_clause__1 with from_clause__2
// replacing from_clause__4 with identification_variable_declaration
// replacing from_clause__5 with collection_member_declaration
// replacing groupby_clause__1 with groupby_clause__2
// replacing identification_variable_declaration__2 with join
// replacing identification_variable_declaration__3 with fetch_join
// replacing join__1 with join__2
// replacing join_spec__1 with join_spec__2
// replacing join_spec__3 with join_spec__4
// replacing join_spec__6 with "OUTER"
// replacing join_spec__7 with "INNER"
// replacing new_value__1 with simple_arithmetic_expression
// replacing new_value__2 with primary
// replacing new_value__3 with simple_entity_expression
// replacing orderby_clause__1 with orderby_clause__2
// replacing orderby_item__1 with orderby_item__2
// replacing orderby_item__4 with "ASC"
// replacing orderby_item__5 with "DESC"
// replacing path_expression__1 with path_expression__2
// replacing range_variable_declaration__1 with range_variable_declaration__2
// replacing select_clause__1 with select_clause__2
// replacing select_clause__3 with select_clause__4
// replacing select_expression__1 with aggregate_expression
// replacing select_expression__3 with constructor_expression
// replacing select_expression__4 with opt_path_expression
// replacing simple_cond_expression__1 with comparison_expression
// replacing simple_cond_expression__2 with between_expression
// replacing simple_cond_expression__3 with like_expression
// replacing simple_cond_expression__4 with in_expression
// replacing simple_cond_expression__5 with null_comparison_expression
// replacing simple_cond_expression__6 with empty_collection_comparison_expression
// replacing simple_cond_expression__7 with collection_member_expression
// replacing simple_cond_expression__8 with exists_expression
// replacing stringEnd__1 with '"'
// replacing stringEnd__3 with '\' ESC stringEnd | Char stringEnd
// replacing update_clause__1 with update_clause__2
// replacing update_clause__3 with update_clause__4
// replacing update_clause__5 with update_clause__6
package de.bb.jpa;

public final class QLParser extends de.bb.bex2.Parser {

  public QLParser(de.bb.bex2.Scanner scanner, de.bb.bex2.Context ctx) {
    super(scanner, ctx);
    current = new de.bb.bex2.ParseEntry(scanner, -1, 0, scanner.getLength());

  }

  private final static char [] data_W__x0028 = {'(',};
  private final static char [] data_W__x0029 = {')',};
  private final static char [] data_W__x002c = {',',};
  private final static char [] data_W__x002e = {'.',};
  private final static char [] data_W__x003d = {'=',};
  private final static char [] data_W_AND = {'A','N','D',};
  private final static char [] data_W_AS = {'A','S',};
  private final static char [] data_W_ASC = {'A','S','C',};
  private final static char [] data_W_AVG = {'A','V','G',};
  private final static char [] data_W_BY = {'B','Y',};
  private final static char [] data_W_COUNT = {'C','O','U','N','T',};
  private final static char [] data_W_DELETE = {'D','E','L','E','T','E',};
  private final static char [] data_W_DESC = {'D','E','S','C',};
  private final static char [] data_W_DISTINCT = {'D','I','S','T','I','N','C','T',};
  private final static char [] data_W_FETCH = {'F','E','T','C','H',};
  private final static char [] data_W_FROM = {'F','R','O','M',};
  private final static char [] data_W_GROUP = {'G','R','O','U','P',};
  private final static char [] data_W_HAVING = {'H','A','V','I','N','G',};
  private final static char [] data_W_IN = {'I','N',};
  private final static char [] data_W_INNER = {'I','N','N','E','R',};
  private final static char [] data_W_JOIN = {'J','O','I','N',};
  private final static char [] data_W_LEFT = {'L','E','F','T',};
  private final static char [] data_W_MAX = {'M','A','X',};
  private final static char [] data_W_MIN = {'M','I','N',};
  private final static char [] data_W_NEW = {'N','E','W',};
  private final static char [] data_W_NOT = {'N','O','T',};
  private final static char [] data_W_NULL = {'N','U','L','L',};
  private final static char [] data_W_OBJECT = {'O','B','J','E','C','T',};
  private final static char [] data_W_OR = {'O','R',};
  private final static char [] data_W_ORDER = {'O','R','D','E','R',};
  private final static char [] data_W_OUTER = {'O','U','T','E','R',};
  private final static char [] data_W_SELECT = {'S','E','L','E','C','T',};
  private final static char [] data_W_SET = {'S','E','T',};
  private final static char [] data_W_SUM = {'S','U','M',};
  private final static char [] data_W_UPDATE = {'U','P','D','A','T','E',};
  private final static char [] data_W_WHERE = {'W','H','E','R','E',};
  private final static char [] data_W__x0022 = {'"',};
  private final static char [] data_W__x003c = {'<',};
  private final static char [] data_W__x003c_x003d = {'<','=',};
  private final static char [] data_W__x003c_x003e = {'<','>',};
  private final static char [] data_W__x003d = {'=',};
  private final static char [] data_W__x003e = {'>',};
  private final static char [] data_W__x003e_x003d = {'>','=',};
  private final static char [] data_W__x005c_x0027_x0020ESC_x0020stringEnd_x0020_x007c_x0020Char_x0020stringEn = {'\\','\'',' ','E','S','C',' ','s','t','r','i','n','g','E','n','d',' ','|',' ','C','h','a','r',' ','s','t','r','i','n','g','E','n',};
  private final static char [] data_W__x005cu = {'\\','u',};
  private final static String data_HEX = "0123456789ABCDEFabcdef";
  private final static String data_IDENTCHAR = "$0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
  private final static String data_IDENTCHARSTART = "$ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
  private final static String data_SPACE = "\u0009\n\r ";

  private
  boolean is_HEX()
  {
    if (data_HEX.indexOf(scanner.peek()) >= 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  private
  boolean is_IDENTCHAR()
  {
    if (data_IDENTCHAR.indexOf(scanner.peek()) >= 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  private
  boolean is_IDENTCHARSTART()
  {
    if (data_IDENTCHARSTART.indexOf(scanner.peek()) >= 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  private
  boolean is_Identification_variable() throws de.bb.bex2.ParseException
  {
    if (is_IDENTCHARSTART()) {
      while (is_IDENTCHAR()) {/**/}
      return true;
    }
    return false;
  }

  public
  boolean is_QL() throws de.bb.bex2.ParseException
  {
    if (is_select_statement()) {
      return true;
    }
    if (is_update_statement()) {
      return true;
    }
    if (is_delete_statement()) {
      return true;
    }
    return false;
  }

  private
  boolean is_SPACE()
  {
    if (data_SPACE.indexOf(scanner.peek()) >= 0) {
      scanner.move(1);
      return true;
    }
    return false;
  }

  private
  boolean is_String() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x0022)) {
      if (is_stringEnd()) {
        return true;
      }
      syntaxError("String");//1
    }
    return false;
  }

  private
  boolean is_abstract_schema_name() throws de.bb.bex2.ParseException
  {
    abstract_schema_name = current = new de.bb.bex2.ParseEntry(current, scanner, TK_ABSTRACT_SCHEMA_NAME, scanner.getPosition());
    if (is_abstract_schema_name__1()) {
  current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry abstract_schema_name;
  public de.bb.bex2.ParseEntry getAbstract_schema_name() { return abstract_schema_name; }
  public final static int TK_ABSTRACT_SCHEMA_NAME = 1;

  private
  boolean is_abstract_schema_name__1() throws de.bb.bex2.ParseException
  {
    if (is_String()) {
      return true;
    }
    if (is_Identification_variable()) {
      return true;
    }
    return false;
  }

  private
  boolean is_aggregate_expression() throws de.bb.bex2.ParseException
  {
    aggregate_expression = current = new de.bb.bex2.ParseEntry(current, scanner, TK_AGGREGATE_EXPRESSION, scanner.getPosition());
    if (is_aggregate_expression__8()) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
    if (is_aggregate_expression__9()) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry aggregate_expression;
  public de.bb.bex2.ParseEntry getAggregate_expression() { return aggregate_expression; }
  public final static int TK_AGGREGATE_EXPRESSION = 2;

  private
  boolean is_aggregate_expression__1() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_AVG)) {
      return true;
    }
    if (scanner.isWord(data_W_MAX)) {
      return true;
    }
    if (scanner.isWord(data_W_MIN)) {
      return true;
    }
    if (scanner.isWord(data_W_SUM)) {
      return true;
    }
    return false;
  }

  private
  boolean is_aggregate_expression__11() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_DISTINCT)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("aggregate_expression__11");//1
    }
    return false;
  }

  private
  boolean is_aggregate_expression__7() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_DISTINCT)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("aggregate_expression__7");//1
    }
    return false;
  }

  private
  boolean is_aggregate_expression__8() throws de.bb.bex2.ParseException
  {
    if (is_aggregate_expression__1()) {
      while (is_SPACE()) {/**/}
      if (scanner.isWord(data_W__x0028)) {
        while (is_SPACE()) {/**/}
        is_aggregate_expression__7();
        if (is_path_expression()) {
          if (scanner.isWord(data_W__x0029)) {
            while (is_SPACE()) {/**/}
            return true;
          }
        }
      }
      syntaxError("aggregate_expression__8");//3
    }
    return false;
  }

  private
  boolean is_aggregate_expression__9() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_COUNT)) {
      while (is_SPACE()) {/**/}
      if (scanner.isWord(data_W__x0028)) {
        while (is_SPACE()) {/**/}
        is_aggregate_expression__11();
        if (is_opt_path_expression()) {
          if (scanner.isWord(data_W__x0029)) {
            while (is_SPACE()) {/**/}
            return true;
          }
        }
      }
      syntaxError("aggregate_expression__9");//3
    }
    return false;
  }

  private
  boolean is_collection_member_declaration() throws de.bb.bex2.ParseException
  {
    collection_member_declaration = current = new de.bb.bex2.ParseEntry(current, scanner, TK_COLLECTION_MEMBER_DECLARATION, scanner.getPosition());
    if (scanner.isWord(data_W_IN)) {
      while (is_SPACE()) {/**/}
      if (scanner.isWord(data_W__x0028)) {
        while (is_SPACE()) {/**/}
        if (is_path_expression()) {
          if (scanner.isWord(data_W__x0029)) {
            while (is_SPACE()) {/**/}
            is_collection_member_declaration__2();
            if (is_Identification_variable()) {
          current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
              return true;
            }
          }
        }
      }
      syntaxError("collection_member_declaration");//4
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry collection_member_declaration;
  public de.bb.bex2.ParseEntry getCollection_member_declaration() { return collection_member_declaration; }
  public final static int TK_COLLECTION_MEMBER_DECLARATION = 3;

  private
  boolean is_collection_member_declaration__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_AS)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("collection_member_declaration__2");//1
    }
    return false;
  }

  private
  boolean is_comparison_expression() throws de.bb.bex2.ParseException
  {
    comparison_expression = current = new de.bb.bex2.ParseEntry(current, scanner, TK_COMPARISON_EXPRESSION, scanner.getPosition());
    if (is_comparison_operator()) {
      if (is_comparison_expression__1()) {
    current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
        return true;
      }
      syntaxError("comparison_expression");//1
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry comparison_expression;
  public de.bb.bex2.ParseEntry getComparison_expression() { return comparison_expression; }
  public final static int TK_COMPARISON_EXPRESSION = 4;

  private
  boolean is_comparison_expression__1() throws de.bb.bex2.ParseException
  {
    return false;
  }

  private
  boolean is_comparison_operator() throws de.bb.bex2.ParseException
  {
    comparison_operator = current = new de.bb.bex2.ParseEntry(current, scanner, TK_COMPARISON_OPERATOR, scanner.getPosition());
    if (scanner.isWord(data_W__x003d)) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
    if (scanner.isWord(data_W__x003e_x003d)) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
    if (scanner.isWord(data_W__x003c_x003d)) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
    if (scanner.isWord(data_W__x003c_x003e)) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
    if (scanner.isWord(data_W__x003e)) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
    if (scanner.isWord(data_W__x003c)) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry comparison_operator;
  public de.bb.bex2.ParseEntry getComparison_operator() { return comparison_operator; }
  public final static int TK_COMPARISON_OPERATOR = 5;

  private
  boolean is_conditional_expression() throws de.bb.bex2.ParseException
  {
    conditional_expression = current = new de.bb.bex2.ParseEntry(current, scanner, TK_CONDITIONAL_EXPRESSION, scanner.getPosition());
    is_conditional_expression__2();
    if (is_conditional_expression__3()) {
      is_conditional_expression__7();
      is_conditional_expression__12();
  current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry conditional_expression;
  public de.bb.bex2.ParseEntry getConditional_expression() { return conditional_expression; }
  public final static int TK_CONDITIONAL_EXPRESSION = 6;

  private
  boolean is_conditional_expression__10() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x0028)) {
      while (is_SPACE()) {/**/}
      if (is_conditional_expression()) {
        while (is_SPACE()) {/**/}
        if (scanner.isWord(data_W__x0029)) {
          return true;
        }
      }
      syntaxError("conditional_expression__10");//2
    }
    return false;
  }

  private
  boolean is_conditional_expression__12() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_AND)) {
      if (is_conditional_expression__13()) {
        return true;
      }
      syntaxError("conditional_expression__12");//1
    }
    return false;
  }

  private
  boolean is_conditional_expression__13() throws de.bb.bex2.ParseException
  {
    if (is_conditional_expression__14()) {
      return true;
    }
    if (is_conditional_expression__15()) {
      return true;
    }
    return false;
  }

  private
  boolean is_conditional_expression__14() throws de.bb.bex2.ParseException
  {
    if (is_SPACE()) {
      while (is_SPACE()) {/**/}
      if (is_conditional_expression()) {
        return true;
      }
    }
    return false;
  }

  private
  boolean is_conditional_expression__15() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x0028)) {
      while (is_SPACE()) {/**/}
      if (is_conditional_expression()) {
        while (is_SPACE()) {/**/}
        if (scanner.isWord(data_W__x0029)) {
          return true;
        }
      }
      syntaxError("conditional_expression__15");//2
    }
    return false;
  }

  private
  boolean is_conditional_expression__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_NOT)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("conditional_expression__2");//1
    }
    return false;
  }

  private
  boolean is_conditional_expression__3() throws de.bb.bex2.ParseException
  {
    if (is_conditional_expression__4()) {
      return true;
    }
    if (is_simple_cond_expression()) {
      return true;
    }
    return false;
  }

  private
  boolean is_conditional_expression__4() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x0028)) {
      while (is_SPACE()) {/**/}
      if (is_conditional_expression()) {
        while (is_SPACE()) {/**/}
        if (scanner.isWord(data_W__x0029)) {
          while (is_SPACE()) {/**/}
          return true;
        }
      }
      syntaxError("conditional_expression__4");//2
    }
    return false;
  }

  private
  boolean is_conditional_expression__7() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_OR)) {
      if (is_conditional_expression__8()) {
        return true;
      }
      syntaxError("conditional_expression__7");//1
    }
    return false;
  }

  private
  boolean is_conditional_expression__8() throws de.bb.bex2.ParseException
  {
    if (is_conditional_expression__9()) {
      return true;
    }
    if (is_conditional_expression__10()) {
      return true;
    }
    return false;
  }

  private
  boolean is_conditional_expression__9() throws de.bb.bex2.ParseException
  {
    if (is_SPACE()) {
      while (is_SPACE()) {/**/}
      if (is_conditional_expression()) {
        return true;
      }
    }
    return false;
  }

  private
  boolean is_constructor_expression() throws de.bb.bex2.ParseException
  {
    constructor_expression = current = new de.bb.bex2.ParseEntry(current, scanner, TK_CONSTRUCTOR_EXPRESSION, scanner.getPosition());
    if (scanner.isWord(data_W_NEW)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        if (is_constructor_name()) {
          if (scanner.isWord(data_W__x0028)) {
            while (is_SPACE()) {/**/}
            if (is_constructor_item()) {
              while (is_constructor_expression__2()) {/**/}
              if (scanner.isWord(data_W__x0029)) {
                while (is_SPACE()) {/**/}
            current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
                return true;
              }
            }
          }
        }
      }
      syntaxError("constructor_expression");//5
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry constructor_expression;
  public de.bb.bex2.ParseEntry getConstructor_expression() { return constructor_expression; }
  public final static int TK_CONSTRUCTOR_EXPRESSION = 7;

  private
  boolean is_constructor_expression__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x002c)) {
      while (is_SPACE()) {/**/}
      if (is_constructor_item()) {
        return true;
      }
      syntaxError("constructor_expression__2");//1
    }
    return false;
  }

  private
  boolean is_constructor_item() throws de.bb.bex2.ParseException
  {
    constructor_item = current = new de.bb.bex2.ParseEntry(current, scanner, TK_CONSTRUCTOR_ITEM, scanner.getPosition());
    if (is_path_expression()) {
  current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry constructor_item;
  public de.bb.bex2.ParseEntry getConstructor_item() { return constructor_item; }
  public final static int TK_CONSTRUCTOR_ITEM = 8;

  private
  boolean is_constructor_name() throws de.bb.bex2.ParseException
  {
    constructor_name = current = new de.bb.bex2.ParseEntry(current, scanner, TK_CONSTRUCTOR_NAME, scanner.getPosition());
    if (is_Identification_variable()) {
      while (is_SPACE()) {/**/}
      while (is_constructor_name__2()) {/**/}
  current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry constructor_name;
  public de.bb.bex2.ParseEntry getConstructor_name() { return constructor_name; }
  public final static int TK_CONSTRUCTOR_NAME = 9;

  private
  boolean is_constructor_name__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x002e)) {
      while (is_SPACE()) {/**/}
      if (is_Identification_variable()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("constructor_name__2");//1
    }
    return false;
  }

  private
  boolean is_delete_clause() throws de.bb.bex2.ParseException
  {
    delete_clause = current = new de.bb.bex2.ParseEntry(current, scanner, TK_DELETE_CLAUSE, scanner.getPosition());
    if (scanner.isWord(data_W_DELETE)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        if (scanner.isWord(data_W_FROM)) {
          if (is_SPACE()) {
            while (is_SPACE()) {/**/}
            if (is_abstract_schema_name()) {
              is_delete_clause__2();
          current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
              return true;
            }
          }
        }
      }
      syntaxError("delete_clause");//4
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry delete_clause;
  public de.bb.bex2.ParseEntry getDelete_clause() { return delete_clause; }
  public final static int TK_DELETE_CLAUSE = 10;

  private
  boolean is_delete_clause__2() throws de.bb.bex2.ParseException
  {
    is_delete_clause__4();
    if (is_Identification_variable()) {
      return true;
    }
    return false;
  }

  private
  boolean is_delete_clause__4() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_AS)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("delete_clause__4");//1
    }
    return false;
  }

  private
  boolean is_delete_statement() throws de.bb.bex2.ParseException
  {
    delete_statement = current = new de.bb.bex2.ParseEntry(current, scanner, TK_DELETE_STATEMENT, scanner.getPosition());
    if (is_delete_clause()) {
      is_where_clause();
  current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry delete_statement;
  public de.bb.bex2.ParseEntry getDelete_statement() { return delete_statement; }
  public final static int TK_DELETE_STATEMENT = 11;

  private
  boolean is_fetch_join() throws de.bb.bex2.ParseException
  {
    fetch_join = current = new de.bb.bex2.ParseEntry(current, scanner, TK_FETCH_JOIN, scanner.getPosition());
    if (is_join_spec()) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        if (scanner.isWord(data_W_FETCH)) {
          if (is_SPACE()) {
            while (is_SPACE()) {/**/}
            if (is_path_expression()) {
          current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
              return true;
            }
          }
        }
      }
      syntaxError("fetch_join");//4
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry fetch_join;
  public de.bb.bex2.ParseEntry getFetch_join() { return fetch_join; }
  public final static int TK_FETCH_JOIN = 12;

  private
  boolean is_from_clause() throws de.bb.bex2.ParseException
  {
    from_clause = current = new de.bb.bex2.ParseEntry(current, scanner, TK_FROM_CLAUSE, scanner.getPosition());
    if (scanner.isWord(data_W_FROM)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        if (is_identification_variable_declaration()) {
          while (is_from_clause__2()) {/**/}
      current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
          return true;
        }
      }
      syntaxError("from_clause");//2
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry from_clause;
  public de.bb.bex2.ParseEntry getFrom_clause() { return from_clause; }
  public final static int TK_FROM_CLAUSE = 13;

  private
  boolean is_from_clause__2() throws de.bb.bex2.ParseException
  {
    while (is_SPACE()) {/**/}
    if (scanner.isWord(data_W__x002c)) {
      while (is_SPACE()) {/**/}
      if (is_from_clause__3()) {
        return true;
      }
      syntaxError("from_clause__2");//1
    }
    return false;
  }

  private
  boolean is_from_clause__3() throws de.bb.bex2.ParseException
  {
    if (is_identification_variable_declaration()) {
      return true;
    }
    if (is_collection_member_declaration()) {
      return true;
    }
    return false;
  }

  private
  boolean is_groupby_clause() throws de.bb.bex2.ParseException
  {
    groupby_clause = current = new de.bb.bex2.ParseEntry(current, scanner, TK_GROUPBY_CLAUSE, scanner.getPosition());
    if (scanner.isWord(data_W_GROUP)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        if (scanner.isWord(data_W_BY)) {
          if (is_SPACE()) {
            while (is_SPACE()) {/**/}
            if (is_groupby_item()) {
              while (is_groupby_clause__2()) {/**/}
          current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
              return true;
            }
          }
        }
      }
      syntaxError("groupby_clause");//4
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry groupby_clause;
  public de.bb.bex2.ParseEntry getGroupby_clause() { return groupby_clause; }
  public final static int TK_GROUPBY_CLAUSE = 14;

  private
  boolean is_groupby_clause__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x002c)) {
      while (is_SPACE()) {/**/}
      if (is_groupby_item()) {
        return true;
      }
      syntaxError("groupby_clause__2");//1
    }
    return false;
  }

  private
  boolean is_groupby_item() throws de.bb.bex2.ParseException
  {
    groupby_item = current = new de.bb.bex2.ParseEntry(current, scanner, TK_GROUPBY_ITEM, scanner.getPosition());
    if (is_opt_path_expression()) {
  current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry groupby_item;
  public de.bb.bex2.ParseEntry getGroupby_item() { return groupby_item; }
  public final static int TK_GROUPBY_ITEM = 15;

  private
  boolean is_having_clause() throws de.bb.bex2.ParseException
  {
    having_clause = current = new de.bb.bex2.ParseEntry(current, scanner, TK_HAVING_CLAUSE, scanner.getPosition());
    if (scanner.isWord(data_W_HAVING)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        if (is_conditional_expression()) {
      current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
          return true;
        }
      }
      syntaxError("having_clause");//2
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry having_clause;
  public de.bb.bex2.ParseEntry getHaving_clause() { return having_clause; }
  public final static int TK_HAVING_CLAUSE = 16;

  private
  boolean is_identification_variable_declaration() throws de.bb.bex2.ParseException
  {
    identification_variable_declaration = current = new de.bb.bex2.ParseEntry(current, scanner, TK_IDENTIFICATION_VARIABLE_DECLARATION, scanner.getPosition());
    if (is_range_variable_declaration()) {
      while (is_identification_variable_declaration__1()) {/**/}
  current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry identification_variable_declaration;
  public de.bb.bex2.ParseEntry getIdentification_variable_declaration() { return identification_variable_declaration; }
  public final static int TK_IDENTIFICATION_VARIABLE_DECLARATION = 17;

  private
  boolean is_identification_variable_declaration__1() throws de.bb.bex2.ParseException
  {
    if (is_join()) {
      return true;
    }
    if (is_fetch_join()) {
      return true;
    }
    return false;
  }

  private
  boolean is_join() throws de.bb.bex2.ParseException
  {
    join = current = new de.bb.bex2.ParseEntry(current, scanner, TK_JOIN, scanner.getPosition());
    if (is_join_spec()) {
      if (is_path_expression()) {
        if (is_SPACE()) {
          while (is_SPACE()) {/**/}
          is_join__2();
          if (is_Identification_variable()) {
        current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
            return true;
          }
        }
      }
      syntaxError("join");//3
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry join;
  public de.bb.bex2.ParseEntry getJoin() { return join; }
  public final static int TK_JOIN = 18;

  private
  boolean is_join__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_AS)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("join__2");//1
    }
    return false;
  }

  private
  boolean is_join_spec() throws de.bb.bex2.ParseException
  {
    join_spec = current = new de.bb.bex2.ParseEntry(current, scanner, TK_JOIN_SPEC, scanner.getPosition());
    is_join_spec__2();
    if (scanner.isWord(data_W_JOIN)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
    current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
        return true;
      }
      syntaxError("join_spec");//1
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry join_spec;
  public de.bb.bex2.ParseEntry getJoin_spec() { return join_spec; }
  public final static int TK_JOIN_SPEC = 19;

  private
  boolean is_join_spec__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_LEFT)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        is_join_spec__4();
        return true;
      }
      syntaxError("join_spec__2");//1
    }
    return false;
  }

  private
  boolean is_join_spec__4() throws de.bb.bex2.ParseException
  {
    if (is_join_spec__5()) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("join_spec__4");//1
    }
    return false;
  }

  private
  boolean is_join_spec__5() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_OUTER)) {
      return true;
    }
    if (scanner.isWord(data_W_INNER)) {
      return true;
    }
    return false;
  }

  private
  boolean is_new_value() throws de.bb.bex2.ParseException
  {
    new_value = current = new de.bb.bex2.ParseEntry(current, scanner, TK_NEW_VALUE, scanner.getPosition());
    if (is_new_value__4()) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry new_value;
  public de.bb.bex2.ParseEntry getNew_value() { return new_value; }
  public final static int TK_NEW_VALUE = 20;

  private
  boolean is_new_value__4() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_NULL)) {
      while (is_SPACE()) {/**/}
      return true;
    }
    return false;
  }

  private
  boolean is_opt_path_expression() throws de.bb.bex2.ParseException
  {
    opt_path_expression = current = new de.bb.bex2.ParseEntry(current, scanner, TK_OPT_PATH_EXPRESSION, scanner.getPosition());
    if (is_path_expression()) {
  current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry opt_path_expression;
  public de.bb.bex2.ParseEntry getOpt_path_expression() { return opt_path_expression; }
  public final static int TK_OPT_PATH_EXPRESSION = 21;

  private
  boolean is_orderby_clause() throws de.bb.bex2.ParseException
  {
    orderby_clause = current = new de.bb.bex2.ParseEntry(current, scanner, TK_ORDERBY_CLAUSE, scanner.getPosition());
    if (scanner.isWord(data_W_ORDER)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        if (scanner.isWord(data_W_BY)) {
          if (is_SPACE()) {
            while (is_SPACE()) {/**/}
            if (is_orderby_item()) {
              while (is_orderby_clause__2()) {/**/}
          current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
              return true;
            }
          }
        }
      }
      syntaxError("orderby_clause");//4
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry orderby_clause;
  public de.bb.bex2.ParseEntry getOrderby_clause() { return orderby_clause; }
  public final static int TK_ORDERBY_CLAUSE = 22;

  private
  boolean is_orderby_clause__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x002c)) {
      while (is_SPACE()) {/**/}
      if (is_orderby_item()) {
        return true;
      }
      syntaxError("orderby_clause__2");//1
    }
    return false;
  }

  private
  boolean is_orderby_item() throws de.bb.bex2.ParseException
  {
    orderby_item = current = new de.bb.bex2.ParseEntry(current, scanner, TK_ORDERBY_ITEM, scanner.getPosition());
    if (is_path_expression()) {
      is_orderby_item__2();
  current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry orderby_item;
  public de.bb.bex2.ParseEntry getOrderby_item() { return orderby_item; }
  public final static int TK_ORDERBY_ITEM = 23;

  private
  boolean is_orderby_item__2() throws de.bb.bex2.ParseException
  {
    if (is_orderby_item__3()) {
      while (is_SPACE()) {/**/}
      return true;
    }
    return false;
  }

  private
  boolean is_orderby_item__3() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_ASC)) {
      return true;
    }
    if (scanner.isWord(data_W_DESC)) {
      return true;
    }
    return false;
  }

  private
  boolean is_path_expression() throws de.bb.bex2.ParseException
  {
    path_expression = current = new de.bb.bex2.ParseEntry(current, scanner, TK_PATH_EXPRESSION, scanner.getPosition());
    if (is_Identification_variable()) {
      if (is_path_expression__2()) {
        while (is_path_expression__2()) {/**/}
        while (is_SPACE()) {/**/}
    current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
        return true;
      }
      syntaxError("path_expression");//1
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry path_expression;
  public de.bb.bex2.ParseEntry getPath_expression() { return path_expression; }
  public final static int TK_PATH_EXPRESSION = 24;

  private
  boolean is_path_expression__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x002e)) {
      while (is_SPACE()) {/**/}
      if (is_Identification_variable()) {
        return true;
      }
      syntaxError("path_expression__2");//1
    }
    return false;
  }

  private
  boolean is_range_variable_declaration() throws de.bb.bex2.ParseException
  {
    range_variable_declaration = current = new de.bb.bex2.ParseEntry(current, scanner, TK_RANGE_VARIABLE_DECLARATION, scanner.getPosition());
    if (is_abstract_schema_name()) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        is_range_variable_declaration__2();
        if (is_Identification_variable()) {
      current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
          return true;
        }
      }
      syntaxError("range_variable_declaration");//2
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry range_variable_declaration;
  public de.bb.bex2.ParseEntry getRange_variable_declaration() { return range_variable_declaration; }
  public final static int TK_RANGE_VARIABLE_DECLARATION = 25;

  private
  boolean is_range_variable_declaration__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_AS)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("range_variable_declaration__2");//1
    }
    return false;
  }

  private
  boolean is_select_clause() throws de.bb.bex2.ParseException
  {
    select_clause = current = new de.bb.bex2.ParseEntry(current, scanner, TK_SELECT_CLAUSE, scanner.getPosition());
    if (scanner.isWord(data_W_SELECT)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        is_select_clause__2();
        if (is_select_expression()) {
          while (is_select_clause__4()) {/**/}
      current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
          return true;
        }
      }
      syntaxError("select_clause");//2
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry select_clause;
  public de.bb.bex2.ParseEntry getSelect_clause() { return select_clause; }
  public final static int TK_SELECT_CLAUSE = 26;

  private
  boolean is_select_clause__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_DISTINCT)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("select_clause__2");//1
    }
    return false;
  }

  private
  boolean is_select_clause__4() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x002c)) {
      while (is_SPACE()) {/**/}
      if (is_select_expression()) {
        return true;
      }
      syntaxError("select_clause__4");//1
    }
    return false;
  }

  private
  boolean is_select_expression() throws de.bb.bex2.ParseException
  {
    select_expression = current = new de.bb.bex2.ParseEntry(current, scanner, TK_SELECT_EXPRESSION, scanner.getPosition());
    if (is_aggregate_expression()) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
    if (is_select_expression__2()) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
    if (is_constructor_expression()) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
    if (is_opt_path_expression()) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry select_expression;
  public de.bb.bex2.ParseEntry getSelect_expression() { return select_expression; }
  public final static int TK_SELECT_EXPRESSION = 27;

  private
  boolean is_select_expression__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_OBJECT)) {
      while (is_SPACE()) {/**/}
      if (scanner.isWord(data_W__x0028)) {
        while (is_SPACE()) {/**/}
        if (is_Identification_variable()) {
          if (scanner.isWord(data_W__x0029)) {
            while (is_SPACE()) {/**/}
            return true;
          }
        }
      }
      syntaxError("select_expression__2");//3
    }
    return false;
  }

  private
  boolean is_select_statement() throws de.bb.bex2.ParseException
  {
    select_statement = current = new de.bb.bex2.ParseEntry(current, scanner, TK_SELECT_STATEMENT, scanner.getPosition());
    if (is_select_clause()) {
      if (is_from_clause()) {
        is_where_clause();
        is_groupby_clause();
        is_having_clause();
        is_orderby_clause();
    current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
        return true;
      }
      syntaxError("select_statement");//1
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry select_statement;
  public de.bb.bex2.ParseEntry getSelect_statement() { return select_statement; }
  public final static int TK_SELECT_STATEMENT = 28;

  private
  boolean is_simple_cond_expression() throws de.bb.bex2.ParseException
  {
    simple_cond_expression = current = new de.bb.bex2.ParseEntry(current, scanner, TK_SIMPLE_COND_EXPRESSION, scanner.getPosition());
    if (is_comparison_expression()) {
current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry simple_cond_expression;
  public de.bb.bex2.ParseEntry getSimple_cond_expression() { return simple_cond_expression; }
  public final static int TK_SIMPLE_COND_EXPRESSION = 29;

  private
  boolean is_stringEnd() throws de.bb.bex2.ParseException
  {
   for(;;) {
    if (scanner.isWord(data_W__x0022)) {
      return true;
    }
    if (is_stringEnd__2()) {
      continue;
    }
    if (scanner.isWord(data_W__x005c_x0027_x0020ESC_x0020stringEnd_x0020_x007c_x0020Char_x0020stringEn)) {
      return true;
    }
    break;
   }
    return false;
  }

  private
  boolean is_stringEnd__2() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x005cu)) {
      if (is_HEX()) {
        if (is_HEX()) {
          if (is_HEX()) {
            if (is_HEX()) {
              return true;
            }
          }
        }
      }
      syntaxError("stringEnd__2");//4
    }
    return false;
  }

  private
  boolean is_update_clause() throws de.bb.bex2.ParseException
  {
    update_clause = current = new de.bb.bex2.ParseEntry(current, scanner, TK_UPDATE_CLAUSE, scanner.getPosition());
    if (scanner.isWord(data_W_UPDATE)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        if (is_abstract_schema_name()) {
          is_update_clause__2();
          if (scanner.isWord(data_W_SET)) {
            if (is_SPACE()) {
              while (is_SPACE()) {/**/}
              if (is_update_item()) {
                while (is_update_clause__6()) {/**/}
            current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
                return true;
              }
            }
          }
        }
      }
      syntaxError("update_clause");//5
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry update_clause;
  public de.bb.bex2.ParseEntry getUpdate_clause() { return update_clause; }
  public final static int TK_UPDATE_CLAUSE = 30;

  private
  boolean is_update_clause__2() throws de.bb.bex2.ParseException
  {
    is_update_clause__4();
    if (is_Identification_variable()) {
      return true;
    }
    return false;
  }

  private
  boolean is_update_clause__4() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W_AS)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        return true;
      }
      syntaxError("update_clause__4");//1
    }
    return false;
  }

  private
  boolean is_update_clause__6() throws de.bb.bex2.ParseException
  {
    if (scanner.isWord(data_W__x002c)) {
      while (is_SPACE()) {/**/}
      if (is_update_item()) {
        return true;
      }
      syntaxError("update_clause__6");//1
    }
    return false;
  }

  private
  boolean is_update_item() throws de.bb.bex2.ParseException
  {
    update_item = current = new de.bb.bex2.ParseEntry(current, scanner, TK_UPDATE_ITEM, scanner.getPosition());
    if (is_path_expression()) {
      if (scanner.isWord(data_W__x003d)) {
        while (is_SPACE()) {/**/}
        if (is_new_value()) {
      current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
          return true;
        }
      }
      syntaxError("update_item");//2
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry update_item;
  public de.bb.bex2.ParseEntry getUpdate_item() { return update_item; }
  public final static int TK_UPDATE_ITEM = 31;

  private
  boolean is_update_statement() throws de.bb.bex2.ParseException
  {
    update_statement = current = new de.bb.bex2.ParseEntry(current, scanner, TK_UPDATE_STATEMENT, scanner.getPosition());
    if (is_update_clause()) {
      is_where_clause();
  current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
      return true;
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry update_statement;
  public de.bb.bex2.ParseEntry getUpdate_statement() { return update_statement; }
  public final static int TK_UPDATE_STATEMENT = 32;

  private
  boolean is_where_clause() throws de.bb.bex2.ParseException
  {
    where_clause = current = new de.bb.bex2.ParseEntry(current, scanner, TK_WHERE_CLAUSE, scanner.getPosition());
    if (scanner.isWord(data_W_WHERE)) {
      if (is_SPACE()) {
        while (is_SPACE()) {/**/}
        if (is_conditional_expression()) {
      current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();
          return true;
        }
      }
      syntaxError("where_clause");//2
    }
current = current.getParent();
    return false;
  }
  private de.bb.bex2.ParseEntry where_clause;
  public de.bb.bex2.ParseEntry getWhere_clause() { return where_clause; }
  public final static int TK_WHERE_CLAUSE = 33;

  public final static int TK_USERDEFINED = 34;
}

