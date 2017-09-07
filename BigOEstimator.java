import assignment1.SortTools;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * This class is intended to test the runtime of methods.
 * <p>
 * I wrote this as a quick sanity check before submitting my class assignments that have a time complexity requirement.
 */

public class BigOEstimator {
    public static void main(String[] args) throws Exception {
        // Example Usage
        VariableInput arrayInput = new VariableIntArrayInput();
        BigOEstimator test = new BigOEstimator(Arrays.class, "sort", arrayInput);
        test.setTrialRules(100000,2,10, 5, BigOEstimator.MULTIPLY);
        test.runTest();
    }

    private Class<?> testClass;
    private Object testInstance;
    private Method testMethod;
    private int varInputIndex = -1;
    private Object[] argValues;

    // Enumeration of possible growth types of n.
    final public static int ADD = 0;
    final public static int MULTIPLY = 1;

    private int numOfTrials = 6;
    private int delta = 2;
    private int start = 10;
    private int operation = 1;
    private int repeatTrials = 1;

    public BigOEstimator(Class c, String methodName, Object... argValues)
            throws ClassNotFoundException, NoSuchMethodException {
        int varInputCount = 0;
        for (int i = 0; i < argValues.length; i++) {
            if (argValues[i] instanceof VariableInput) {
                varInputCount++;
                varInputIndex = i;
            }
        }
        if (varInputIndex == -1) {
            throw new RuntimeException(String.format(
                    "Arguments must contain exactly 1 instance of VariableInput, your constructor contained %d.",
                    varInputCount));
        }
        this.argValues = argValues;
        testClass = c;
        try {
            testMethod = testClass.getMethod(methodName, getTypes(false));
        }
        catch (NoSuchMethodException e) {
            try {
                testMethod = testClass.getMethod(methodName, getTypes(true));
            }
            catch (NoSuchFieldException | IllegalAccessException e1){
                throw e;
            }
        }
        catch (NoSuchFieldException | IllegalAccessException e1){
            e1.printStackTrace(); // Should never happen.
        }

    }

    public BigOEstimator(Object instance, String methodName, Object... argValues)
            throws ClassNotFoundException, NoSuchMethodException {
        this(instance.getClass(), methodName, argValues);
        this.testInstance = instance;
    }

    public void setTrialRules(int start, int delta, int numOfTrials, int repeatTrials, int operation){
        this.start = start;
        this.delta = delta;
        this.numOfTrials = numOfTrials;
        this.repeatTrials = repeatTrials;
        this.operation = operation;
    }

    public int[][] runTest() {
        int[] complexities = new int[numOfTrials];
        int[] timeTaken = new int[numOfTrials];

        complexities[0] = start;
        for (int i = 1; i < numOfTrials; i++) {
            switch(operation){
                case ADD: complexities[i] = complexities[i-1] + delta; break;
                case MULTIPLY: complexities[i] = complexities[i-1] * delta; break;
                default: // TODO: Complain to User
            }

        }
        int nWidth = printHeading(complexities);
        printDash(nWidth + 12);
        for (int i = 0; i < numOfTrials; i++) {
            int average = 0;
            try {
                for (int j = 0; j < repeatTrials; j++) {
                    average += execute(complexities[i]);
                }
                average /= repeatTrials;
                timeTaken[i] = average;
                printRow(nWidth, complexities[i], timeTaken[i]);
            } catch (Exception e) {
                timeTaken[i] = -1;
            }
        }
        printDash(nWidth + 12);
        return new int[][]{complexities, timeTaken};
    }

    private Class<?>[] getTypes(boolean unbox) throws NoSuchFieldException, IllegalAccessException {
        Class<?>[] types = new Class[argValues.length];
        for (int i = 0; i < argValues.length; i++) {
            Class<?> type = argValues[i].getClass();
            if (VariableInput.class.isAssignableFrom(type)) {
                types[i] = ((VariableInput) argValues[i]).getType();
                if (unbox) {
                    types[i] = (Class)types[i].getField("TYPE").get(null);
                }
            } else if (type.equals(SpecialRule.class)) {
                types[i] = ((SpecialRule) argValues[i]).getResultType();
            } else {
                types[i] = type.getClass();
            }
        }
        return types;
    }

    private int execute(int complexity) throws InvocationTargetException, IllegalAccessException {
        long currentTime = System.currentTimeMillis();
        testMethod.invoke(testInstance, processArgs(complexity));
        return (int) (System.currentTimeMillis() - currentTime);
    }

    private Object[] processArgs(int complexity) {
        Object[] processedArgs = argValues.clone();
        processedArgs[varInputIndex] = ((VariableInput) processedArgs[varInputIndex]).generate(complexity);
        for (int i = 0; i < processedArgs.length; i++) {
            if (processedArgs[i] instanceof SpecialRule){
                processedArgs[i] = ((SpecialRule) processedArgs[i]).apply(processedArgs[varInputIndex]);
            }
        }
        return processedArgs;
    }

    private static int printHeading(int[] complexities) {
        String nText = "n";
        String timeText = "time (ms)";
        int nMaxCharCount = nText.length();
        int timeMaxCharCount = timeText.length()+2;
        for (int i = 0; i < complexities.length; i++) {
            String n = Integer.toString(complexities[i]);
            if (n.length() > nMaxCharCount) {
                nMaxCharCount = n.length();
            }
        }
        int rowWidth = nMaxCharCount + 1 + timeMaxCharCount;
        char[] header = new char[rowWidth];
        header[nMaxCharCount] = '|';
        arrayReplace(header, nMaxCharCount/2 - nText.length()/2, nText);
        arrayReplace(header, nMaxCharCount + 1 + (timeMaxCharCount/2-timeText.length()/2), timeText);
        for (char c : header) {
            System.out.print(c == 0 ? ' ' : c);
        }
        System.out.println();
        return nMaxCharCount;
    }


    private static void printRow(int nWidth, int n, int time){
        char[] row = new char[nWidth + 12];
        String nText = Integer.toString(n);
        arrayReplace(row, nWidth-nText.length(), nText);
        String timeText = Integer.toString(time);
        arrayReplace(row, nWidth + 12 - timeText.length(), timeText);
        row[nWidth] = '|';
        for (char c : row) {
            System.out.print(c == 0 ? ' ' : c);
        }
        System.out.println();
    }

    private  static void printDash(int width){
        for (int i = 0; i < width; i++) {
            System.out.print('-');
        }
        System.out.println();
    }

    private static void arrayReplace(char[] array, int start, String text){
        for (int i = 0; i < text.length(); i++) {
            array[start+i] = text.charAt(i);
        }
    }
}

class SpecialRule {
    private VariableInput instance;
    private Method m;
    private Class resultType;
    public SpecialRule(VariableInput instance){
        this.instance = instance;
        try {
            this.m =  instance.getClass().getMethod("rule", instance.getType());
        } catch (NoSuchMethodException e) {
            // TODO: Complain to user
            e.printStackTrace();
        }
        resultType = m.getReturnType();
    }
    public Object apply(Object variedInput){
        try {
            return m.invoke(instance, variedInput);
        } catch (IllegalAccessException | InvocationTargetException e) {
            // TODO: Complain to user
            e.printStackTrace();
            return null;
        }
    }

    public Class getResultType() {
        return resultType;
    }
}

abstract class VariableInput {
    protected final Class type;
    protected String[] tags;

    VariableInput(String... tags) {
        this.tags = tags;
        Class type;
        try {
            type = this.getClass().getMethod("generate", int.class).getReturnType();
        } catch (NoSuchMethodException e) {
            type = null; // Should never happen.
        }
        this.type = type;
    }

    public Class getType() {
        return type;
    }

    public abstract Object generate(int complexity);

    public SpecialRule getRule(){
        return new SpecialRule(this);

    }
}

class VariableIntArrayInput extends VariableInput {
    VariableIntArrayInput(String... tags) {
        super(tags);
    }

    public int[] generate(int complexity) {
        int[] testArray = new int[complexity];
        for (int i = 0; i < complexity; i++) {
            testArray[i] = (int)(Math.random()*100);
        }
        return testArray;
    }

    public int rule(int[] a){
        return a.length;
    }
}

class VariableIntInput extends VariableInput {
    VariableIntInput(String... tags) {
        super(tags);
    }

    public Integer generate(int complexity) {
        return complexity;
    }
}

class VariableLongInput extends VariableInput {
    VariableLongInput(String... tags) {
        super(tags);
    }

    public Long generate(int complexity) {
        return (long) complexity;
    }
}