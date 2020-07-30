package com.spring.factory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;

/**
 * @ClassName : MyFactory
 * @Description : BeanFactoryPostProcessor 可以得到 spring内部所有的bd
 * 				  用途：对特定的 bd修改属性
 * @Author : Lin.cx
 * @Date: 2020-07-30 22:28
 */
@Component
public class MyFactory implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		//BeanDefinition indexService = beanFactory.getBeanDefinition("indexService");
		/**
		 * 这里为什么要用 GenericBeanDefinition呢？
		 * 因为它是 BD的子类，有更多的方法可以调用
		 */
		GenericBeanDefinition indexService = (GenericBeanDefinition) beanFactory.getBeanDefinition("indexService");
		// getConstructorArgumentValues 得到构造器的所有参数
		// addGenericArgumentValue 这里可以把字符串转为对象
		indexService.getConstructorArgumentValues().addGenericArgumentValue("com.lcx.dao.IndexDao");

	}
}
