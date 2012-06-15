// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000B4ECFCB2E292A4CCCCB2150FD4CCE457ECFC84D2A4DCB21592E2D4ACA4D4E2929C7D079CF4E2DC50A0924A86A245B2824D20005BFCC22123000000

package LBJ2.nlp.coref;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.learn.*;
import LBJ2.nlp.coherence.*;
import LBJ2.parse.*;


public class MakeCoherent$subjectto extends ParameterizedConstraint
{
  public MakeCoherent$subjectto() { super("LBJ2.nlp.coref.MakeCoherent$subjectto"); }

  public String getInputType() { return "LBJ2.nlp.coref.Document"; }

  public String discreteValue(Object __example)
  {
    if (!(__example instanceof Document))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Constraint 'MakeCoherent$subjectto(Document)' defined on line 9 of coherentCoref.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    Document d = (Document) __example;



    return "true";
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Document))
      {
        System.err.println("Classifier 'MakeCoherent$subjectto(Document)' defined on line 9 of coherentCoref.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "MakeCoherent$subjectto".hashCode(); }
  public boolean equals(Object o) { return o instanceof MakeCoherent$subjectto; }

  public FirstOrderConstraint makeConstraint(Object __example)
  {
    if (!(__example instanceof Document))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Constraint 'MakeCoherent$subjectto(Document)' defined on line 9 of coherentCoref.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    Document d = (Document) __example;
    FirstOrderConstraint __result = new FirstOrderConstant(true);



    return __result;
  }
}

