package LBJ2;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.lang.reflect.*;
import LBJ2.IR.*;
import LBJ2.classify.*;
import LBJ2.learn.*;
import LBJ2.parse.*;
import LBJ2.util.StudentT;


/**
  * After code has been generated with {@link TranslateToJava}, this pass
  * trains any classifiers for which training was indicated.
  *
  * @see    LBJ2.TranslateToJava
  * @author Nick Rizzolo
 **/
public class Train extends Pass
{
  /**
    * Any code that's interested can check this variable to see if training is
    * currently taking place.
   **/
  public static boolean TRAINING = false;


  /**
    * Generates a <code>String</code> containing the name of the specified
    * <code>Throwable</code> and its stack trace.
    *
    * @param t  <code>Throwable</code>.
    * @return   The generated message.
   **/
  private static String stackTrace(Throwable t)
  {
    String message = "  " + t + "\n";
    StackTraceElement[] elements = t.getStackTrace();
    if (elements.length == 0) message += "    no stack trace available\n";
    for (int i = 0; i < elements.length; ++i)
      message += "    " + elements[i] + "\n";
    return message;
  }


  /**
    * Run the <code>javac</code> compiler with the specified arguments in
    * addition to those specified on the command line.
    *
    * @param arguments  The arguments to send to <code>javac</code>.
    * @return           <code>true</code> iff errors were encountered.
   **/
  public static boolean runJavac(String arguments)
  {
    Process javac = null;
    String pathArguments = "-classpath \"" + Main.classPath + "\" -sourcepath \""
                           + Main.sourcePath + "\"";

    if (Main.generatedSourceDirectory != null)
    {
      String gsd = Main.generatedSourceDirectory;
      int packageIndex = -1;
      if (!AST.globalSymbolTable.getPackage().equals(""))
        packageIndex =
          gsd.lastIndexOf(File.separator + AST.globalSymbolTable.getPackage()
                                           .replace('.', File.separatorChar));
      if (packageIndex != -1) gsd = gsd.substring(0, packageIndex);
      pathArguments += File.pathSeparator + gsd;
    }

    if (Main.classPackageDirectory != null)
      pathArguments += " -d " + Main.classPackageDirectory;

    String command = Configuration.javac + " " + Main.javacArguments + " "
                     + pathArguments + " " + arguments;

    try { javac = Runtime.getRuntime().exec(command); }
    catch (Exception e)
    {
      System.err.println("Failed to execute 'javac': " + e);
      System.exit(1);
    }

    BufferedReader error =
      new BufferedReader(new InputStreamReader(javac.getErrorStream()));
    try
    {
      for (String line = error.readLine(); line != null;
           line = error.readLine())
        System.out.println(line);
    }
    catch (Exception e)
    {
      System.err.println("Error reading STDERR from 'javac': " + e);
      System.exit(1);
    }

    int exit = 0;
    try { exit = javac.waitFor(); }
    catch (Exception e)
    {
      System.err.println("Error waiting for 'javac' to terminate: " + e);
      System.exit(1);
    }

    return exit != 0;
  }


  /**
    * Progress output will be printed every <code>progressOutput</code>
    * examples.
   **/
  protected int progressOutput;
  /**
    * Set to <code>true</code> iff there existed a
    * {@link LearningClassifierExpression} for which new code was generated.
   **/
  protected boolean newCode;
  /**
    * An array of the training threads, which is never modified after it is
    * constructed.
   **/
  protected TrainingThread[] threads;
  /** A map of all the training threads indexed by the name of the learner. */
  protected HashMap threadMap;
  /**
    * The keys of this map are the names of learners; the values are
    * <code>LinkedList</code>s of the names of the learners that the learner
    * named by the key depends on.
   **/
  protected HashMap learnerDependencies;


  /**
    * Instantiates a pass that runs on an entire {@link AST}.
    *
    * @param ast    The program to run this pass on.
    * @param output Progress output will be printed every <code>output</code>
    *               examples.
   **/
  public Train(AST ast, int output)
  {
    super(ast);
    progressOutput = output;
  }


  /**
    * Adds an edge from dependor to dependency in the
    * {@link #learnerDependencies} graph.  If <code>dependency</code> is
    * <code>null</code>, no new list item is added, but the
    * <code>HashSet</code> associated with <code>dependor</code> is still
    * created if it didn't already exist.
    *
    * @param dependor   The name of the node doing the depending.
    * @param dependency The name of the node depended on.
   **/
  private void addDependency(String dependor, String dependency)
  {
    HashSet dependencies = (HashSet) learnerDependencies.get(dependor);

    if (dependencies == null)
    {
      dependencies = new HashSet();
      learnerDependencies.put(dependor, dependencies);
    }

    if (dependency != null) dependencies.add(dependency);
  }


  /**
    * This method initializes the {@link #learnerDependencies} graph such
    * that the entry for each learner contains the names of all learners that
    * depend on it, except that cycles are broken by preferring that learners
    * appearing earlier in the source get trained first.
   **/
  protected void fillLearnerDependorsDAG()
  {
    threads =
      (TrainingThread[]) threadMap.values().toArray(new TrainingThread[0]);
    Arrays.sort(threads,
                new Comparator()
                {
                  public int compare(Object o1, Object o2)
                  {
                    TrainingThread t1 = (TrainingThread) o1;
                    TrainingThread t2 = (TrainingThread) o2;
                    return t2.byteOffset - t1.byteOffset;
                  }
                });

    for (int i = 0; i < threads.length - 1; ++i)
      for (int j = i + 1; j < threads.length; ++j)
      {
        if (SemanticAnalysis.isDependentOn(threads[i].getName(),
                                           threads[j].getName()))
          addDependency(threads[i].getName(), threads[j].getName());
        else if (SemanticAnalysis.isDependentOn(threads[j].getName(),
                                                threads[i].getName()))
          addDependency(threads[j].getName(), threads[i].getName());
      }
  }


  /**
    * This method updates the {@link #learnerDependencies} graph by removing
    * the specified name from every dependencies list, and then starts every
    * thread that has no more dependencies.
    *
    * @param name The name of a learner whose training has completed.
   **/
  protected void executeReadyThreads(String name)
  {
    LinkedList ready = new LinkedList();

    synchronized (learnerDependencies)
    {
      for (Iterator I = learnerDependencies.entrySet().iterator();
           I.hasNext(); )
      {
        Map.Entry e = (Map.Entry) I.next();
        HashSet dependencies = (HashSet) e.getValue();
        dependencies.remove(name);
        if (dependencies.size() == 0) ready.add(e.getKey());
      }
    }

    for (Iterator I = ready.iterator(); I.hasNext(); )
    {
      TrainingThread thread = null;

      synchronized (threadMap)
      {
        thread = (TrainingThread) threadMap.remove(I.next());
      }

      if (thread != null)
      {
        thread.start();

        if (!Main.concurrentTraining)
        {
          try { thread.join(); }
          catch (InterruptedException e)
          {
            System.err.println("LBJ ERROR: Training of " + thread.getName()
                               + " has been interrupted.");
            fatalError = true;
          }
        }
      }
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ast  The node to process.
   **/
  public void run(AST ast)
  {
    if (RevisionAnalysis.noChanges) return;
    TRAINING = true;
    threadMap = new HashMap();
    learnerDependencies = new HashMap();

    String files = "";
    for (Iterator I = Main.fileNames.iterator(); I.hasNext(); )
      files += " " + I.next();

    System.out.println("Compiling generated code");
    if (runJavac(files)) return;

    runOnChildren(ast);

    fillLearnerDependorsDAG();
    executeReadyThreads(null);

    for (int i = 0; i < threads.length; ++i)
    {
      try { threads[i].join(); }
      catch (InterruptedException e)
      {
        System.err.println("LBJ ERROR: Training of " + threads[i].getName()
                           + " has been interrupted.");
        fatalError = true;
      }
    }

    if (!fatalError && newCode)
    {
      System.out.println("Compiling generated code");
      runJavac(files);
    }

    TRAINING = false;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param lce  The node to process.
   **/
  public void run(LearningClassifierExpression lce)
  {
    runOnChildren(lce);

    String lceName = lce.name.toString();
    if (lce.parser != null
          && RevisionAnalysis.revisionStatus.get(lceName)
             .equals(RevisionAnalysis.UNAFFECTED)
        || lce.parser == null
           && !RevisionAnalysis.revisionStatus.get(lceName)
               .equals(RevisionAnalysis.REVISED))
      return;

    newCode = true;

    // First, instantiate the learner.
    Class learnerClass = ast.symbolTable.classForName(lceName);
    if (learnerClass == null)
    {
      reportError(lce.line,
                  "Could not locate class for classifier '" + lceName + "'.");
      return;
    }

    Learner learner = null;
    try { learner = (Learner) learnerClass.newInstance(); }
    catch (Exception e)
    {
      reportError(lce.line,
                  "Could not instantiate learner '" + lceName + "': " + e);
      return;
    }

    // Get the unique instance.
    Method getInstance = null;
    try
    {
      getInstance =
        learnerClass.getDeclaredMethod("getInstance", new Class[0]);
    }
    catch (Exception e)
    {
      reportError(lce.line, "Could not access method '" + lceName
                            + ".getInstance()': " + e);
      return;
    }

    try { learner = (Learner) getInstance.invoke(null, null); }
    catch (Exception e)
    {
      reportError(lce.line,
                  "Could not get unique instance of '" + lce.parser.name
                  + "': " + e + ", caused by");
      System.err.print(stackTrace(e.getCause()));
      return;
    }

    if (learner == null)
    {
      System.err.println("Could not get unique instance of '" + lceName
                         + "'.");
      System.exit(1);
    }

    Parser parser = null;
    if (lce.parser != null)
    {
      // Next, instantiate the parser.
      Method getParser = null;
      try
      {
        getParser = learnerClass.getDeclaredMethod("getParser", new Class[0]);
      }
      catch (Exception e)
      {
        reportError(lce.line, "Could not access method '" + lceName
                              + ".getParser()': " + e);
        return;
      }

      try { parser = (Parser) getParser.invoke(null, null); }
      catch (Exception e)
      {
        reportError(lce.line,
                    "Could not instantiate parser '" + lce.parser.name + "': "
                    + e + ", caused by");
        Throwable cause = e.getCause();
        System.err.print(stackTrace(cause));

        if (cause instanceof ExceptionInInitializerError)
        {
          System.err.println("... caused by");
          System.err.print(
              stackTrace(((ExceptionInInitializerError) cause).getCause()));
        }

        return;
      }
    }

    TrainingThread thread =
      new TrainingThread(lceName, lce.byteOffset, learnerClass, learner,
                         parser, lce);
    threadMap.put(lceName, thread);
    addDependency(lceName, null);
  }


  /**
    * Runs this pass on all nodes of the indicated type.  There's no reason to
    * traverse children of {@link CodedClassifier}s, so this method exists
    * simply to stop that from happening.
    *
    * @param cc The node to process.
   **/
  public void run(CodedClassifier cc) { }


  /**
    * Runs this pass on all nodes of the indicated type.  There's no reason to
    * traverse children of {@link ConstraintDeclaration}s, so this method
    * exists simply to stop that from happening.
    *
    * @param cd The node to process.
   **/
  public void run(ConstraintDeclaration cd) { }


  /**
    * Runs this pass on all nodes of the indicated type.  There's no reason to
    * traverse children of {@link InferenceDeclaration}s, so this method
    * exists simply to stop that from happening.
    *
    * @param id The node to process.
   **/
  public void run(InferenceDeclaration id) { }


  /**
    * This class contains the code that trains a learning classifier.  It is a
    * subclass of <code>Thread</code> so that it may be executed concurrently.
    *
    * @author Nick Rizzolo
   **/
  protected class TrainingThread extends Thread
  {
    /** The byte offset at which the learner appeared. */
    public int byteOffset;
    /** The class of the learner to train. */
    protected Class learnerClass;
    /** The learner to train. */
    protected Learner learner;
    /** The parser from which training objects are obtained. */
    protected Parser parser;
    /** The expression that specified the learner. */
    protected LearningClassifierExpression lce;


    /**
      * Initializing constructor.
      *
      * @param n    The name of the learner.
      * @param b    The byte offset at which the learner appeared.
      * @param c    The class of the learner.
      * @param l    The learner.
      * @param p    The parser.
      * @param lce  The expression that specified the learner.
     **/
    public TrainingThread(String n, int b, Class c, Learner l, Parser p,
                          LearningClassifierExpression lce)
    {
      super(n);
      byteOffset = b;
      learnerClass = c;
      learner = l;
      parser = p;
      this.lce = lce;
    }


    /** Performs the training and then generates the new code. */
    public void run()
    {
      if (parser != null)
      {
        // if a progressOutput value is specified in the lce, override that
        // which was provided via command line. If neither are provided,
        // default is 0.
        if (lce.progressOutput != null)
        {
          int p = Integer.parseInt(lce.progressOutput.value);
          progressOutput = p;
        }

        int examples = 0;

        // declare these outside of the conditional so they can be referenced
        // later
        // store the old labeler and extractor
        Classifier labeler = learner.getLabeler();
        Parser oldparser = parser;

        System.out.println("Training " + getName());

        // ----------------- Feature Vector Pre-Extraction -----------------
        boolean preExtract =
          lce.preExtract != null
          && Boolean.valueOf(lce.preExtract.value).booleanValue();
        if (preExtract)
        {
          String exampleFilePath = getName() + ".ex";
          if (Main.generatedSourceDirectory != null)
            exampleFilePath = Main.generatedSourceDirectory + File.separator
                              + exampleFilePath;
          File exampleFile = new File(exampleFilePath);

          if (lce.rounds != null && exampleFile.exists()
              && !exampleFile.delete())
          {
            System.err.println("LBJ ERROR: Can't delete " + exampleFile);
            System.exit(1);
          }

          DataOutputStream dos = null;
          try
          {
            dos =
              new DataOutputStream(
                  new FileOutputStream(exampleFilePath, true));
          }
          catch (Exception e)
          {
            System.err.println("Can't create feature vector stream for '"
                               + getName() + "': " + e);
            System.exit(1);
          }

          LinkedHashMap lexicon = new LinkedHashMap();
          int nextFeatureIndex = 0;
          Classifier extractor = learner.getExtractor();

          for (Object example = parser.next(); example != null;
               example = parser.next())
          {
            if (progressOutput > 0 && examples % progressOutput == 0)
              System.out.println("  " + getName() + ", pre-extract: "
                                 + examples + " examples at " + new Date());

            try
            {
              // if we run into a fold separator, write a -1
              if (example == FoldSeparator.separator)
              {
                dos.writeInt(-1);
              }
              // otherwise, act normally
              else
              {
                ++examples;
                FeatureVector vector = extractor.classify(example);
                vector.addLabels(labeler.classify(example));

                dos.writeInt(vector.labels.size());
                for (Iterator I = vector.labels.iterator(); I.hasNext(); )
                {
                  Feature f = (Feature) I.next();
                  Integer index = (Integer) lexicon.get(f);

                  if (index == null)
                  {
                    f.intern();
                    index = new Integer(nextFeatureIndex++);
                    lexicon.put(f, index);
                  }

                  dos.writeInt(index.intValue());
                }

                dos.writeInt(vector.features.size());
                for (Iterator I = vector.features.iterator(); I.hasNext(); )
                {
                  Feature f = (Feature) I.next();
                  Integer index = (Integer) lexicon.get(f);

                  if (index == null)
                  {
                    f.intern();
                    index = new Integer(nextFeatureIndex++);
                    lexicon.put(f, index);
                  }

                  dos.writeInt(index.intValue());
                }
              }
            }
            catch (Exception e)
            {
              System.err.println("Can't write example for '" + getName()
                                 + "': " + e);
              e.printStackTrace();
              System.exit(1);
            }
          }

          try { dos.close(); }
          catch (Exception e)
          {
            System.err.println("Can't close feature vector stream for '"
                               + getName() + "': " + e);
            System.exit(1);
          }

          if (progressOutput > 0)
            System.out.println("  " + getName() + ", pre-extract: " + examples
                               + " examples processed at " + new Date());

          ObjectOutputStream oos = null;
          String lexiconFilePath = getName() + ".lex";
          if (Main.generatedSourceDirectory != null)
            lexiconFilePath = Main.generatedSourceDirectory + File.separator
                              + lexiconFilePath;

          try
          {
            oos =
              new ObjectOutputStream(new FileOutputStream(lexiconFilePath));
          }
          catch (Exception e)
          {
            System.err.println("Can't create lexicon stream for '" + getName()
                               + "': " + e);
            System.exit(1);
          }

          Feature[] lexiconArray =
            (Feature[]) lexicon.keySet().toArray(new Feature[0]);
          try { oos.writeObject(lexiconArray); }
          catch (Exception e)
          {
            System.err.println("Can't write lexicon for '" + getName() + "': "
                               + e);
            System.exit(1);
          }

          try { oos.close(); }
          catch (Exception e)
          {
            System.err.println("Can't close lexicon stream for '"
                + getName() + "': " + e);
            System.exit(1);
          }

          // Setting up the learner to pull from the file
          final String returnType = lce.labeler.returnType.getTypeName();
          final String[] allowable =
            new String[lce.labeler.returnType.values.size()];

          ConstantList.ConstantListIterator I =
            lce.labeler.returnType.values.listIterator();
          for (int i = 0; I.hasNext(); ++i)
            allowable[i] = I.nextItem().value;

          if (labeler.getOutputType().equals("discrete"))
            learner.setLabeler(
                new LabelVectorReturner()
                {
                  public String getOutputType() { return returnType; }
                  public String[] allowableValues() { return allowable; }
                  public String discreteValue(Object e)
                  {
                    return ((DiscreteFeature) classify(e).firstFeature())
                           .getValue();
                  }
                });
          else if (labeler.getOutputType().equals("real"))
            learner.setLabeler(
                new LabelVectorReturner()
                {
                  public String getOutputType() { return returnType; }
                  public String[] allowableValues() { return allowable; }
                  public double realValue(Object e)
                  {
                    return ((RealFeature) classify(e).firstFeature())
                           .getValue();
                  }
                });
          else if (labeler.getOutputType().equals("discrete[]"))
            learner.setLabeler(
                new LabelVectorReturner()
                {
                  public String getOutputType() { return returnType; }
                  public String[] allowableValues() { return allowable; }
                  public String[] discreteValueArray(Object e)
                  {
                    return classify(e).discreteValueArray();
                  }
                });
          else if (labeler.getOutputType().equals("real[]"))
            learner.setLabeler(
                new LabelVectorReturner()
                {
                  public String getOutputType() { return returnType; }
                  public String[] allowableValues() { return allowable; }
                  public double[] realValueArray(Object e)
                  {
                    return classify(e).realValueArray();
                  }
                });
          else
            learner.setLabeler(
                new LabelVectorReturner()
                {
                  public String getOutputType() { return returnType; }
                  public String[] allowableValues() { return allowable; }
                });

          // set the parser to be a FeatureVectorParser, so we can train from
          // the data we just extracted
          parser = new FeatureVectorParser(exampleFilePath, lexiconArray);
        }


        // ------------------- Cross Validation -----------------------------
        if (lce.K != null)
        {
          int k = Integer.parseInt(lce.K.value);

          if (k > 1 || lce.splitStrategy == FoldParser.SplitStrategy.manual)
          {
            FoldParser.SplitStrategy splitStrategy = lce.splitStrategy;

            System.out.print("  " + getName() + ": Cross Validation: ");
            if (k != -1) System.out.print("k = " + k + ", ");
            System.out.print("Split = " + splitStrategy.getName());

            int rounds = 1;
            if (lce.rounds != null)
              rounds = Integer.parseInt(lce.rounds.value);
            if (rounds != 1) System.out.print(", Rounds = " + rounds);
            System.out.println();

            FoldParser foldParser = null;
            // If we pre-extracted, we know how many examples there are
            // already; otherwise FoldParser will have to compute it.
            if (preExtract)
              foldParser =
                new FoldParser(parser, k, splitStrategy, 0, false, examples);
            else
              foldParser = new FoldParser(parser, k, splitStrategy, 0, false);
            k = foldParser.getK();

            // An array to store the results of k-fold testing
            double[] results = new double[k];

            Classifier oracle = learner.getLabeler();
            TestingMetric testingMetric = null;

            if (lce.testingMetric != null)
            {
              // i basically copied this section from where Nick gets an
              // instance of Parser

              Method getTestingMetric = null;
              try
              {
                getTestingMetric =
                  learnerClass.getDeclaredMethod("getTestingMetric",
                                                 new Class[0]);
              }
              catch (Exception e)
              {
                reportError(lce.line,
                            "Could not access method'" + getName()
                            + ".getTestingMetric()': " + e);
                return;
              }

              try
              {
                testingMetric =
                  (TestingMetric) getTestingMetric.invoke(null, null);
              }
              catch (Exception e)
              {
                reportError(lce.line,
                            "Could not instantiate testing metric '"
                            + lce.parser.name + "': " + e + ", caused by");
                System.err.print(stackTrace(e.getCause()));
                return;
              }
            }
            else
            {
              // if no user-implemented testing metric was specified,
              // default to the built-in accuracy metric
              testingMetric = new Accuracy();
            }

            boolean usingAccuracy = testingMetric instanceof Accuracy;
            String metricName = testingMetric.getClass().getName();
            while (metricName.indexOf('.') != -1)
              metricName = metricName.substring(metricName.indexOf('.') + 1);

            // Go through the example file K times, learning a different
            // subset each time.
            for (int i = 0; i < k; foldParser.setPivot(++i))
            {
              // train on all subsets except the i'th
              System.out.println("  " + getName()
                                 + ": training against subset " + i);

              // go through training once per round
              for (int r = 0; r < rounds; ++r)
              {
                examples = 0;

                for (Object example = foldParser.next(); example != null;
                     example = foldParser.next(), ++examples)
                {
                  if (progressOutput > 0 && examples % progressOutput == 0)
                    System.out.println(
                        "  " + getName() + ", round " + r + ", " + examples
                        + " examples processed at " + new Date());
                  learner.learn(example);
                }

                if (progressOutput > 0)
                  System.out.println("  " + getName() + ", " + examples
                                     + " examples processed at "
                                     + new Date());

                foldParser.reset();
              }

              learner.doneLearning();
              foldParser.setFromPivot(true);
              results[i] = testingMetric.test(oracle, learner, foldParser);

              System.out.print("  " + getName() + ": subset " + i);
              double rounded = results[i];
              if (usingAccuracy) rounded *= 100;
              rounded = Math.round(rounded * 100000) / 100000.0;
              System.out.print(" " + metricName + ": " + rounded);
              if (usingAccuracy) System.out.print("%");
              System.out.println();

              foldParser.setFromPivot(false);
              learner.forget();
            }


            // x: the mean of the testing results
            double x = 0.0;
            for (int i = 0; i < k; ++i)
              x += results[i];
            x /= (double) k;

            // s: the standard deviation of the testing results
            double s = 0.0;
            for (int i = 0; i < k; ++i)
              s += (results[i] - x) * (results[i] - x);
            s /= (double) (k - 1);
            s = Math.sqrt(s);

            // sem: estimated standard error of the mean
            double sem = s / Math.sqrt(k);

            // alpha: unconfidence level
            double alpha = Double.parseDouble(lce.alpha.value);

            double t = StudentT.tTable(k - 1, alpha);
            double halfInterval = t * sem;

            if (usingAccuracy)
            {
              x *= 100;
              halfInterval *= 100;
            }

            halfInterval = Math.round(halfInterval * 100000) / 100000.0;
            x = Math.round(x * 100000) / 100000.0;

            System.out.print("  " + getName() + ": " + (100 * (1 - alpha))
                             + "% confidence interval: " + x);
            if (usingAccuracy) System.out.print("%");
            System.out.print(" +/- " + halfInterval);
            if (usingAccuracy) System.out.print("%");
            System.out.println();

            parser.reset();

            System.out.println("  " + getName()
                               + ": training on entire training set");
          } // end of 'if k > 1' section
        }

        // ------------------- Final Training --------------------------------
        if (lce.rounds == null)
        {
          examples = 0;

          for (Object example = parser.next(); example != null;
               example = parser.next())
          {
            if (progressOutput > 0 && examples % progressOutput == 0)
              System.out.println("  " + getName() + ": " + examples
                                 + " examples processed at " + new Date());

            if (example != FoldSeparator.separator)
            {
              learner.learn(example);
              ++examples;
            }
          }

          if (progressOutput > 0)
            System.out.println("  " + getName() + ": " + examples
                               + " examples processed at " + new Date());
        }
        else
        {
          int rounds = Integer.parseInt(lce.rounds.value);

          for (int i = 1; i <= rounds; ++i)
          {
            examples = 0;

            for (Object example = parser.next(); example != null;
                 example = parser.next())
            {
              if (progressOutput > 0 && examples % progressOutput == 0)
                System.out.println(
                    "  " + getName() + ", round " + i + ": " + examples
                    + " examples processed at " + new Date());

              if (example != FoldSeparator.separator)
              {
                learner.learn(example);
                ++examples;
              }
            }

            if (progressOutput > 0)
              System.out.println("  " + getName() + ", round " + i + ": "
                                 + examples + " examples processed at "
                                 + new Date());
            parser.reset();
          }
        }

        // Resetting the labeler and parser
        if (preExtract)
        {
          learner.setLabeler(labeler);
          parser = oldparser;
        }

        learner.doneLearning();
      }

      String lcFilePath = getName() + ".lc";
      if (Main.classDirectory != null)
        lcFilePath = Main.classDirectory + File.separator + lcFilePath;

      // Write the unique instance.
      ObjectOutputStream oos = null;
      try
      {
        oos = new ObjectOutputStream(
                new GZIPOutputStream(new FileOutputStream(lcFilePath)));
      }
      catch (Exception e)
      {
        System.err.println("Can't create object stream for '" + getName()
                           + "': " + e);
        System.exit(1);
      }

      try { oos.writeObject(learner); }
      catch (Exception e)
      {
        System.err.println("Can't write to object stream for '" + getName()
                           + "': " + e);
        System.exit(1);
      }

      try { oos.close(); }
      catch (Exception e)
      {
        System.err.println("Can't close object stream for '" + getName()
                           + "': " + e);
        System.exit(1);
      }

      Field fieldIsTraining = null;
      try { fieldIsTraining = learnerClass.getField("isTraining"); }
      catch (Exception e)
      {
        System.err.println("Can't access " + learnerClass
                           + "'s isTraining field: " + e);
        System.exit(1);
      }

      try { fieldIsTraining.setBoolean(learner, false); }
      catch (Exception e)
      {
        System.err.println("Can't set " + learnerClass
                           + "'s isTraining field: " + e);
        System.exit(1);
      }

      // Write the new code.
      PrintStream out = TranslateToJava.open(lce);
      if (out == null) return;

      out.println(TranslateToJava.disclaimer);
      out.print("// ");
      TranslateToJava.compressAndPrint(lce.shallow(), out);
      out.println("\n");

      ast.symbolTable.generateHeader(out);

      if (lce.cacheIn != null)
      {
        String field = lce.cacheIn.toString();
        boolean cachedInMap = field.equals(ClassifierAssignment.mapCache);
        if (cachedInMap) out.println("import java.util.WeakHashMap;");
      }

      out.println("\n");
      if (lce.comment != null) out.println(lce.comment);

      out.println("\n\npublic class " + getName() + " extends "
                  + lce.learnerName);
      out.println("{");
      out.println("  public static boolean isTraining = false;");
      out.println("  private static java.net.URL lcFilePath;");
      out.println("  private static " + getName() + " instance;");
      out.println("  public static " + getName() + " getInstance()");
      out.println("  {");
      out.println("    if (instance == null)");
      out.println("      instance = (" + lce.name + ") "
                  + "Classifier.binaryRead(lcFilePath, \"" + lce.name
                  + "\");");
      out.println("    return instance;");
      out.println("  }\n");

      out.println("  static");
      out.println("  {");
      out.println("    lcFilePath = " + getName() + ".class.getResource(\""
                  + getName() + ".lc\");\n");

      out.println("    if (lcFilePath == null)");
      out.println("    {");
      out.println("      System.err.println(\"ERROR: Can't locate "
                  + getName() + ".lc in the class path.\");");
      out.println("      System.exit(1);");
      out.println("    }");
      out.println("  }\n");

      out.println("  public void save()");
      out.println("  {");
      out.println("    if (instance == null) return;\n");

      out.println("    if (lcFilePath.toString().indexOf(\".jar!\" + "
                  + "java.io.File.separator) != -1)");
      out.println("    {");
      out.println("      System.err.println(\"WARNING: " + getName() + ".lc "
                  + "is part of a jar file.  It will be written to the "
                  + "current directory.  Use 'jar -u' to update the jar "
                  + "file.  To avoid seeing this message in the future, "
                  + "unpack the jar file and put the unpacked files on your "
                  + "class path instead.\");");
      out.println(
          "      instance.binaryWrite(System.getProperty(\"user.dir\") + "
          + "java.io.File.separator + \"" + getName() + ".lc\", \""
          + getName() + "\");");
      out.println("    }");
      out.println("    else instance.binaryWrite(lcFilePath.getPath(), \""
                  + getName() + "\");");
      out.println("  }\n");

      TranslateToJava.generateLearnerBody(out, lce, true);

      out.println("}\n");
      out.close();

      executeReadyThreads(getName());
    }
  }
}

