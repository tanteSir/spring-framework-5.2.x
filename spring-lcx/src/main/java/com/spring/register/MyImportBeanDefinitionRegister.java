package com.spring.register;

import com.lcx.service.IndexService;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @ClassName : MyImportBeanDefinitionRegister
 * @Description :
 * @Author : Lin.cx
 * @Date: 2020-07-04 15:21
 */
public class MyImportBeanDefinitionRegister implements ImportBeanDefinitionRegistrar {


	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		// 扫描所有接口，得到类对象

		// 把对象转换为bd
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(IndexService.class);
		GenericBeanDefinition beanDefinition = (GenericBeanDefinition) builder.getBeanDefinition();
		beanDefinition.setBeanClass(FactoryBean.class);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("indexService");
		registry.registerBeanDefinition("indexService", beanDefinition);
	}
}
