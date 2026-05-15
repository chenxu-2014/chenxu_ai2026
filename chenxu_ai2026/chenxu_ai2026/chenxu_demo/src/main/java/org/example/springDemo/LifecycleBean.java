package org.example.springDemo;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component  // 交给Spring管理
public class LifecycleBean implements BeanNameAware, BeanFactoryAware,
        InitializingBean, DisposableBean {

    private String name;
    private BeanFactory beanFactory;

    public LifecycleBean() {
        System.out.println("【1】构造方法：实例化 Bean");
    }

    // 通过 setter 注入属性（示例）
    public void setName(String name) {
        System.out.println("【2】属性注入：setName()");
        this.name = name;
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("【3】BeanNameAware：setBeanName() - " + name);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        System.out.println("【4】BeanFactoryAware：setBeanFactory()");
        this.beanFactory = beanFactory;
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("【5】@PostConstruct：自定义初始化方法（注解）");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("【6】InitializingBean：afterPropertiesSet()");
    }

    // 通过 @Bean 的 initMethod 属性指定，这里用注解配置，在配置类中设置
    public void customInitMethod() {
        System.out.println("【7】init-method：自定义初始化方法（XML或@Bean的initMethod）");
    }

    // 业务方法
    public void doSomething() {
        System.out.println("【8】业务使用：doSomething() - name=" + name);
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("【9】@PreDestroy：自定义销毁方法（注解）");
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("【10】DisposableBean：destroy()");
    }

    public void customDestroyMethod() {
        System.out.println("【11】destroy-method：自定义销毁方法");
    }
}