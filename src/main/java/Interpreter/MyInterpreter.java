package Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyInterpreter {

    private static final String VARIABLE_REGEX = "\\$\\w+";
    private static final String VALUE_REGEX = "(-?\\$\\w+|-?\\d+)";
    private static final String OPERATION_REGEX = "(\\-|\\+)";
    private static final String LITERAL_REGEX = "\"[^\"]+\"";
    private static final String PRINT_REGEX = "(\"[^\"]+\"|-?\\$\\w+|-?\\d+|-?\\d+)";

    private Map<String, Integer> userVariables = new HashMap<>();

    public void executeProgram(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                interpret(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Instruction getInstruction(String line) throws UnsupportedOperationException {
        if (line.isEmpty() || line.startsWith("#")) {
            return Instruction.SKIP;
        } else if (line.startsWith("set")) {
            return Instruction.ASSIGNMENT;
        } else if (line.startsWith("print")) {
            return Instruction.CONCLUSION;
        } else {
            throw new UnsupportedOperationException("Unsupported command: " + line);
        }
    }

    private void interpret(String line) {
        Instruction instruction = getInstruction(line);

        switch (instruction) {
            case SKIP:
                return;
            case ASSIGNMENT:
                assignValue(line);
                break;
            case CONCLUSION:
                outputToConsole(line);
                break;
        }
    }

    public String findByRegex(String value, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            return matcher.group();
        }
        return value;
    }

    private List<String> findAllByRegex(String value, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        List<String> results = new ArrayList<>();
        while (matcher.find()) {
            results.add(matcher.group());
        }
        return results;
    }

    public void assignValue(String line) {
        String[] parts = line.split("\\s*=\\s*");
        String variable = findByRegex(parts[0], VARIABLE_REGEX);
        String expression = parts[1].replaceAll("\\s+", "");

        int result = evaluateExpression(expression);

        userVariables.put(variable.substring(1), result);
    }

    private int evaluateExpression(String expression) {
        int result = 0;
        int currentNumber = 0;
        char operation = '+';

        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);

            if (Character.isDigit(ch)) {
                currentNumber = currentNumber * 10 + (ch - '0');
            } else if (ch == '+' || ch == '-') {
                result = performOperation(result, currentNumber, operation);
                operation = ch;
                currentNumber = 0;
            } else if (ch == '$') {
                String variableName = findByRegex(expression.substring(i), VARIABLE_REGEX);
                int variableValue = getValue(variableName);
                result = performOperation(result, variableValue, operation);
                operation = '+';
                i += variableName.length() - 1;
            }
        }

        result = performOperation(result, currentNumber, operation);

        return result;
    }

    private int performOperation(int operand1, int operand2, char operation) {
        if (operation == '+') {
            return operand1 + operand2;
        } else {
            return operand1 - operand2;
        }
    }




    private void outputToConsole(String line) {
        List<String> values = findAllByRegex(line, PRINT_REGEX);
        StringBuilder outputString = new StringBuilder();

        for (String value : values) {
            if (value.matches(LITERAL_REGEX)) {
                value = value.replace("\"", "");
            } else if (value.matches(VARIABLE_REGEX)) {
                value = String.valueOf(getValue(value));
            }

            outputString.append(value);
        }

        System.out.println(outputString.toString());
    }

    private int getValue(String variable) {
        String variableName = variable.substring(1);
        if (userVariables.containsKey(variableName)) {
            return userVariables.get(variableName);
        } else {
            throw new IllegalArgumentException("Variable not found: " + variableName);
        }
    }

    public static void main(String[] args) {
        MyInterpreter interpreter = new MyInterpreter();
        File file = new File("/Users/as.selezneva/repos/Interpreter/src/main/resources/script.txt");
        interpreter.executeProgram(file);
    }
}
