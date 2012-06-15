// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D615DDA6380341E7593DE554A5467D57EE66DB9147DAC6D7180A17C17A725262D136CEDD77E8D8650787119C7FF4A4B4DB77A19C3C1DE1D251EC29D0476B1F74DED691A33C07590CF0C109E3D4570CED3C5C99B2AD0DFB2388CE2093099FA91345291C8AE432B62590B34BD6605B570A42E19FAE123E3E11AA4B76AB1A2D4F262B9D0A46F4CC6E6CC66066713EF58ED3A641E0A96443B8CF238372B62769E781F1FA51E61A16516D0A6A68BAB5541B9F76BAABF0D46187A56B69C847F502ABAA6A806CF77C642D6ECFD71611CA670BE2EDE4B68CF9464BE6BD5EE8E009EED5747F40AB99FCE66E3B7DE5F3B576A653FDD58647AABDE8D12912BDBB062B9F345445AC2F7F70EC85AD3623200000

package LBJ2.nlp.coref;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.learn.*;
import LBJ2.parse.*;
import java.util.*;


public class NoNonPronounToPronoun extends ParameterizedConstraint
{
  private static final DataCoref __DataCoref = new DataCoref();

  public NoNonPronounToPronoun() { super("LBJ2.nlp.coref.NoNonPronounToPronoun"); }

  public String getInputType() { return "LBJ2.nlp.coref.Document"; }

  public String discreteValue(Object __example)
  {
    if (!(__example instanceof Document))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Constraint 'NoNonPronounToPronoun(Document)' defined on line 30 of dataCoref.lbj received '" + type + "' as input.");
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
            LBJ2$constraint$result$1 = !("" + (current.getType())).equals("" + ("PRO"));
            if (LBJ2$constraint$result$1)
              {
                LBJ2$constraint$result$0 = true;
                for (java.util.Iterator __I0 = (previousMentions).iterator(); __I0.hasNext() && LBJ2$constraint$result$0; )
                {
                  Document.Mention m = (Document.Mention) __I0.next();
                  {
                    boolean LBJ2$constraint$result$2;
                    LBJ2$constraint$result$2 = ("" + (m.getType())).equals("" + ("PRO"));
                    if (LBJ2$constraint$result$2)
                      LBJ2$constraint$result$0 = !("" + (__DataCoref.discreteValue((Object) d.getMentionPair(m, current)))).equals("" + (true));
                    else LBJ2$constraint$result$0 = true;
                  }
                }
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
        System.err.println("Classifier 'NoNonPronounToPronoun(Document)' defined on line 30 of dataCoref.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "NoNonPronounToPronoun".hashCode(); }
  public boolean equals(Object o) { return o instanceof NoNonPronounToPronoun; }

  public FirstOrderConstraint makeConstraint(Object __example)
  {
    if (!(__example instanceof Document))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Constraint 'NoNonPronounToPronoun(Document)' defined on line 30 of dataCoref.lbj received '" + type + "' as input.");
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
            LBJ2$constraint$result$1 = new FirstOrderConstant(!("" + (current.getType())).equals("" + ("PRO")));
            FirstOrderConstraint LBJ2$constraint$result$2 = null;
            {
              FirstOrderConstraint LBJ2$constraint$result$3 = null;
              {
                FirstOrderConstraint LBJ2$constraint$result$4 = null;
                {
                  EqualityArgumentReplacer LBJ$EAR =
                    new EqualityArgumentReplacer(LBJ$constraint$context, true)
                    {
                      public String getLeftValue()
                      {
                        Document.Mention m = (Document.Mention) quantificationVariables.get(0);
                        return "" + (m.getType());
                      }
                    };
                  LBJ2$constraint$result$4 = new FirstOrderEqualityTwoValues(true, null, "" + ("PRO"), LBJ$EAR);
                }
                FirstOrderConstraint LBJ2$constraint$result$5 = null;
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
                  LBJ2$constraint$result$5 = new FirstOrderEqualityWithValue(false, new FirstOrderVariable(__DataCoref, null), "" + (true), LBJ$EAR);
                }
                LBJ2$constraint$result$3 = new FirstOrderImplication(LBJ2$constraint$result$4, LBJ2$constraint$result$5);
              }
              LBJ2$constraint$result$2 = new UniversalQuantifier("m", previousMentions, LBJ2$constraint$result$3);
            }
            LBJ2$constraint$result$0 = new FirstOrderImplication(LBJ2$constraint$result$1, LBJ2$constraint$result$2);
          }
          __result = new FirstOrderConjunction(__result, LBJ2$constraint$result$0);
        }
        previous = current;
      }
    }

    return __result;
  }
}

