package org.example.designpattern.TemplateMethod;

public class TemplateDemo {
    public static void main(String[] args) {
        Template temp = new TemplateConcrete();
        temp.update();
    }
}
