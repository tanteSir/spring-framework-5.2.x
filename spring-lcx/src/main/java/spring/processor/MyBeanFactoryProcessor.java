package spring.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @ClassName : MyBeanFactoryProcessor
 * @Description : BeanFactoryPostProcessor 对bean 扩展的五点之一
 * 		这里修改bean的作用域，若把@Component注释掉，就不会扩展了
 *
 * @Author : Lin.cx
 * @Date: 2020-06-21 17:31
 */
//@Component
public class MyBeanFactoryProcessor implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinition definition = beanFactory.getBeanDefinition("indexService");
		definition.setScope("prototype");
	}
}
