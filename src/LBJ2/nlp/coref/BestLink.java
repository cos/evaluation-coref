// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D605BCA63C0301CF59DED4221C437D8B9B4B9B4121824FB0485BE2BEA5250D3AD349EFB777319D138B8E028D999D999DE28F493A12F91E9135E319FF457C0D5178C31BA1E7046486F44923C522E715829ECC021B01E00E1FB71C05AB5894DD456DCA2167917C61AF011498321F8E1B5EFE90C63989E8EB3C42B463B121FE9993CD893C599EAADFBAF7FAA15475DC02A959F7752649637069E706EA3654B5818554D38A931E3CD22ACA9F7ABAAB715DB2CB9E6C0E152BFA0136DEC61C8FF289CE20F937701AF57C5704EFE196A52A1E862B979011B75BCA1F6682A27BD9A4A6D0BFD34E850BD5EDDB2A2EDCFEF0D6C17CEB4F100000

package LBJ2.nlp.coref;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.learn.*;
import LBJ2.parse.*;
import java.util.*;


public class BestLink extends ParameterizedConstraint
{
  private static final DataCoref __DataCoref = new DataCoref();

  public BestLink() { super("LBJ2.nlp.coref.BestLink"); }

  public String getInputType() { return "LBJ2.nlp.coref.Document"; }

  public String discreteValue(Object __example)
  {
    if (!(__example instanceof Document))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Constraint 'BestLink(Document)' defined on line 6 of dataCoref.lbj received '" + type + "' as input.");
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
            int LBJ$m$0 = 0;
            int LBJ$bound$0 = 1;
            for (java.util.Iterator __I0 = (previousMentions).iterator(); __I0.hasNext() && LBJ$m$0 <= LBJ$bound$0; )
            {
              Document.Mention m = (Document.Mention) __I0.next();
              boolean LBJ2$constraint$result$1;
              LBJ2$constraint$result$1 = ("" + (__DataCoref.discreteValue((Object) d.getMentionPair(m, current)))).equals("" + (true));
              if (LBJ2$constraint$result$1) ++LBJ$m$0;
            }
            LBJ2$constraint$result$0 = LBJ$m$0 <= LBJ$bound$0;
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
        System.err.println("Classifier 'BestLink(Document)' defined on line 6 of dataCoref.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "BestLink".hashCode(); }
  public boolean equals(Object o) { return o instanceof BestLink; }

  public FirstOrderConstraint makeConstraint(Object __example)
  {
    if (!(__example instanceof Document))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Constraint 'BestLink(Document)' defined on line 6 of dataCoref.lbj received '" + type + "' as input.");
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
              LBJ2$constraint$result$1 = new FirstOrderEqualityWithValue(true, new FirstOrderVariable(__DataCoref, null), "" + (true), LBJ$EAR);
            }
            LBJ2$constraint$result$0 = new AtMostQuantifier("m", previousMentions, LBJ2$constraint$result$1, 1);
          }
          __result = new FirstOrderConjunction(__result, LBJ2$constraint$result$0);
        }
        previous = current;
      }
    }

    return __result;
  }
}

