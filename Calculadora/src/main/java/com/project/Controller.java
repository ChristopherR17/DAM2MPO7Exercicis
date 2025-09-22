package com.project;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.event.ActionEvent;

public class Controller {

    @FXML
    private Button buttonAdd, buttonRest, buttonMulti, buttonDiv, buttonResult, buttonClear;

    @FXML
    private Button num9, num8, num7, num6, num5, num4, num3, num2, num1, num0, dot;

    @FXML
    private Text textCounter;

    private StringBuilder input = new StringBuilder();
    private double firstOperand = 0;
    private String operator = "";

    @FXML
    private void actionNums(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        input.append(clicked.getText());
        textCounter.setText(input.toString());
    }

    @FXML
    private void actionAdd(ActionEvent event) {
        setOperator("+");
    }

    @FXML
    private void actionRest(ActionEvent event) {
        setOperator("-");
    }

    @FXML
    private void actionMulti(ActionEvent event) {
        setOperator("*");
    }

    @FXML
    private void actionDiv(ActionEvent event) {
        setOperator("/");
    }

    @FXML
    private void actionResult(ActionEvent event) {
        if (!operator.isEmpty() && input.length() > 0) {
            double secondOperand = Double.parseDouble(input.toString());
            double result = 0;
            switch (operator) {
                case "+": result = firstOperand + secondOperand; break;
                case "-": result = firstOperand - secondOperand; break;
                case "*": result = firstOperand * secondOperand; break;
                case "/": {
                    if (secondOperand != 0) {
                        result = firstOperand / secondOperand;
                    } else {
                        textCounter.setText("Error");
                        return;
                    }
                    break;
                }
            }
            textCounter.setText(String.valueOf(result));
            input.setLength(0);
            operator = "";
        }
    }

    private void setOperator(String op) {
        if (input.length() > 0) {
            firstOperand = Double.parseDouble(input.toString());
            operator = op;
            input.setLength(0);
            textCounter.setText("");
        }
    }

    @FXML
    private void actionClear(ActionEvent event) {
        input.setLength(0);
        operator = "";
        firstOperand = 0;
        textCounter.setText("0");
    }
}