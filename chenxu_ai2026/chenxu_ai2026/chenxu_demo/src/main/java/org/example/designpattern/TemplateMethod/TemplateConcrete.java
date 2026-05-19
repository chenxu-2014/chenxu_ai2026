package org.example.designpattern.TemplateMethod;

public class TemplateConcrete extends Template {

    @Override
    public void print() {
        System.out.println("这是子类的实现");
    }
}