// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000592914B43C040158FFAC4B7AD5A526FC985F2D22A011A0E5DBC29C447262BB2BBB1B2A8FFDD9963D4B01F0D093406FD7F66ED033583B12A734632CDB3F50EECB3BEA3B939D775B57547D22B25A68F10972C237A011E3C3E729BE2C31B84C5006D0617F71482D91C9CD94F4D0E21ABB6A9C0A27E1494762E7A556CFBB582390C83AD20384195C284A7F046D742BE309D6FDEF1DE37FE15470F4D2E915FF2A3FE56A953B5F5136FFCA869053BB8A205342C9D132A499F766557E145F84F4A86C9549CE325C495E0D285FF505D742983F086A45A398E277B74FB1318B604A92CCFD07E0F2730751A4DE41630635D40E3C5DC6892991DF4167A5666E7EA16D777E5DE0BC804FD28DC25859008F5CBDC03E3A9618CE8DDA16B62A9D83F859ABC36AD912FAAD5E92C9E34C88EB3CB8370E30AE569310EFEF002761B7D738300000

package LBJ2.nlp.coref;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.learn.*;
import LBJ2.parse.*;
import java.util.*;


public class ForcePronounLink extends ParameterizedConstraint
{
  private static final DataCoref __DataCoref = new DataCoref();

  public ForcePronounLink() { super("LBJ2.nlp.coref.ForcePronounLink"); }

  public String getInputType() { return "LBJ2.nlp.coref.Document"; }

  public String discreteValue(Object __example)
  {
    if (!(__example instanceof Document))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Constraint 'ForcePronounLink(Document)' defined on line 56 of dataCoref.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    Document d = (Document) __example;

    LinkedList previousMentions = new LinkedList();
    Document.Mention previous = null;
    for (int i = 0; i < d.sentences(); ++i)
    {
      for (int j = 0; j < d.mentionsInSentence(i); ++j)
      {
        Document.Mention current = d.getMention(i, j);
        if (previous != null)
        {
          previousMentions = (LinkedList) previousMentions.clone();
          previousMentions.add(previous);
        }
        {
          boolean LBJ2$constraint$result$0;
          {
            boolean LBJ2$constraint$result$1;
            {
              boolean LBJ2$constraint$result$2;
              {
                boolean LBJ2$constraint$result$3;
                {
                  boolean LBJ2$constraint$result$4;
                  {
                    boolean LBJ2$constraint$result$5;
                    {
                      boolean LBJ2$constraint$result$6;
                      {
                        boolean LBJ2$constraint$result$7;
                        {
                          boolean LBJ2$constraint$result$8;
                          LBJ2$constraint$result$8 = ("" + (current.getHead().toLowerCase())).equals("" + ("he"));
                          if (!LBJ2$constraint$result$8)
                            LBJ2$constraint$result$7 = ("" + (current.getHead().toLowerCase())).equals("" + ("him"));
                          else LBJ2$constraint$result$7 = true;
                        }
                        if (!LBJ2$constraint$result$7)
                          LBJ2$constraint$result$6 = ("" + (current.getHead().toLowerCase())).equals("" + ("himself"));
                        else LBJ2$constraint$result$6 = true;
                      }
                      if (!LBJ2$constraint$result$6)
                        LBJ2$constraint$result$5 = ("" + (current.getHead().toLowerCase())).equals("" + ("his"));
                      else LBJ2$constraint$result$5 = true;
                    }
                    if (!LBJ2$constraint$result$5)
                      LBJ2$constraint$result$4 = ("" + (current.getHead().toLowerCase())).equals("" + ("she"));
                    else LBJ2$constraint$result$4 = true;
                  }
                  if (!LBJ2$constraint$result$4)
                    LBJ2$constraint$result$3 = ("" + (current.getHead().toLowerCase())).equals("" + ("her"));
                  else LBJ2$constraint$result$3 = true;
                }
                if (!LBJ2$constraint$result$3)
                  LBJ2$constraint$result$2 = ("" + (current.getHead().toLowerCase())).equals("" + ("herself"));
                else LBJ2$constraint$result$2 = true;
              }
              if (!LBJ2$constraint$result$2)
                LBJ2$constraint$result$1 = ("" + (current.getHead().toLowerCase())).equals("" + ("hers"));
              else LBJ2$constraint$result$1 = true;
            }
            if (LBJ2$constraint$result$1)
              {
                boolean LBJ2$constraint$result$9;
                LBJ2$constraint$result$9 = !("" + (previousMentions.size())).equals("" + (0));
                if (LBJ2$constraint$result$9)
                  {
                    LBJ2$constraint$result$0 = false;
                    for (java.util.Iterator __I0 = (previousMentions).iterator(); __I0.hasNext() && !LBJ2$constraint$result$0; )
                    {
                      Document.Mention m = (Document.Mention) __I0.next();
                      LBJ2$constraint$result$0 = ("" + (__DataCoref.discreteValue((Object) d.getMentionPair(m, current)))).equals("" + (true));
                    }
                  }
                else LBJ2$constraint$result$0 = true;
              }
            else LBJ2$constraint$result$0 = true;
          }
          if (!LBJ2$constraint$result$0) return "false";
        }
        previous = current;
      }
    }

    return "true";
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Document))
      {
        System.err.println("Classifier 'ForcePronounLink(Document)' defined on line 56 of dataCoref.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "ForcePronounLink".hashCode(); }
  public boolean equals(Object o) { return o instanceof ForcePronounLink; }

  public FirstOrderConstraint makeConstraint(Object __example)
  {
    if (!(__example instanceof Document))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Constraint 'ForcePronounLink(Document)' defined on line 56 of dataCoref.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    Document d = (Document) __example;
    FirstOrderConstraint __result = new FirstOrderConstant(true);

    LinkedList previousMentions = new LinkedList();
    Document.Mention previous = null;
    for (int i = 0; i < d.sentences(); ++i)
    {
      for (int j = 0; j < d.mentionsInSentence(i); ++j)
      {
        Document.Mention current = d.getMention(i, j);
        if (previous != null)
        {
          previousMentions = (LinkedList) previousMentions.clone();
          previousMentions.add(previous);
        }
        {
          Object[] LBJ$constraint$context = new Object[3];
          LBJ$constraint$context[0] = d;
          LBJ$constraint$context[1] = current;
          LBJ$constraint$context[2] = previousMentions;
          FirstOrderConstraint LBJ2$constraint$result$0 = null;
          {
            FirstOrderConstraint LBJ2$constraint$result$1 = null;
            {
              FirstOrderConstraint LBJ2$constraint$result$2 = null;
              {
                FirstOrderConstraint LBJ2$constraint$result$3 = null;
                {
                  FirstOrderConstraint LBJ2$constraint$result$4 = null;
                  {
                    FirstOrderConstraint LBJ2$constraint$result$5 = null;
                    {
                      FirstOrderConstraint LBJ2$constraint$result$6 = null;
                      {
                        FirstOrderConstraint LBJ2$constraint$result$7 = null;
                        {
                          FirstOrderConstraint LBJ2$constraint$result$8 = null;
                          LBJ2$constraint$result$8 = new FirstOrderConstant(("" + (current.getHead().toLowerCase())).equals("" + ("he")));
                          FirstOrderConstraint LBJ2$constraint$result$9 = null;
                          LBJ2$constraint$result$9 = new FirstOrderConstant(("" + (current.getHead().toLowerCase())).equals("" + ("him")));
                          LBJ2$constraint$result$7 = new FirstOrderDisjunction(LBJ2$constraint$result$8, LBJ2$constraint$result$9);
                        }
                        FirstOrderConstraint LBJ2$constraint$result$10 = null;
                        LBJ2$constraint$result$10 = new FirstOrderConstant(("" + (current.getHead().toLowerCase())).equals("" + ("himself")));
                        LBJ2$constraint$result$6 = new FirstOrderDisjunction(LBJ2$constraint$result$7, LBJ2$constraint$result$10);
                      }
                      FirstOrderConstraint LBJ2$constraint$result$11 = null;
                      LBJ2$constraint$result$11 = new FirstOrderConstant(("" + (current.getHead().toLowerCase())).equals("" + ("his")));
                      LBJ2$constraint$result$5 = new FirstOrderDisjunction(LBJ2$constraint$result$6, LBJ2$constraint$result$11);
                    }
                    FirstOrderConstraint LBJ2$constraint$result$12 = null;
                    LBJ2$constraint$result$12 = new FirstOrderConstant(("" + (current.getHead().toLowerCase())).equals("" + ("she")));
                    LBJ2$constraint$result$4 = new FirstOrderDisjunction(LBJ2$constraint$result$5, LBJ2$constraint$result$12);
                  }
                  FirstOrderConstraint LBJ2$constraint$result$13 = null;
                  LBJ2$constraint$result$13 = new FirstOrderConstant(("" + (current.getHead().toLowerCase())).equals("" + ("her")));
                  LBJ2$constraint$result$3 = new FirstOrderDisjunction(LBJ2$constraint$result$4, LBJ2$constraint$result$13);
                }
                FirstOrderConstraint LBJ2$constraint$result$14 = null;
                LBJ2$constraint$result$14 = new FirstOrderConstant(("" + (current.getHead().toLowerCase())).equals("" + ("herself")));
                LBJ2$constraint$result$2 = new FirstOrderDisjunction(LBJ2$constraint$result$3, LBJ2$constraint$result$14);
              }
              FirstOrderConstraint LBJ2$constraint$result$15 = null;
              LBJ2$constraint$result$15 = new FirstOrderConstant(("" + (current.getHead().toLowerCase())).equals("" + ("hers")));
              LBJ2$constraint$result$1 = new FirstOrderDisjunction(LBJ2$constraint$result$2, LBJ2$constraint$result$15);
            }
            FirstOrderConstraint LBJ2$constraint$result$16 = null;
            {
              FirstOrderConstraint LBJ2$constraint$result$17 = null;
              LBJ2$constraint$result$17 = new FirstOrderConstant(!("" + (previousMentions.size())).equals("" + (0)));
              FirstOrderConstraint LBJ2$constraint$result$18 = null;
              {
                FirstOrderConstraint LBJ2$constraint$result$19 = null;
                {
                  EqualityArgumentReplacer LBJ$EAR =
                    new EqualityArgumentReplacer(LBJ$constraint$context, true)
                    {
                      public Object getLeftObject()
                      {
                        Document d = (Document) context[0];
                        Document.Mention current = (Document.Mention) context[1];
                        Document.Mention m = (Document.Mention) quantificationVariables.get(0);
                        return d.getMentionPair(m, current);
                      }
                    };
                  LBJ2$constraint$result$19 = new FirstOrderEqualityWithValue(true, new FirstOrderVariable(__DataCoref, null), "" + (true), LBJ$EAR);
                }
                LBJ2$constraint$result$18 = new ExistentialQuantifier("m", previousMentions, LBJ2$constraint$result$19);
              }
              LBJ2$constraint$result$16 = new FirstOrderImplication(LBJ2$constraint$result$17, LBJ2$constraint$result$18);
            }
            LBJ2$constraint$result$0 = new FirstOrderImplication(LBJ2$constraint$result$1, LBJ2$constraint$result$16);
          }
          __result = new FirstOrderConjunction(__result, LBJ2$constraint$result$0);
        }
        previous = current;
      }
    }

    return __result;
  }
}

