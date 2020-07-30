package com.spring.config;

import com.lcx.dao.impl.IndexDaoImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName : MyConfig
 * @Description :
 * @Author : Lin.cx
 * @Date: 2020-06-21 16:26
 */
@ComponentScan("com")
/**
 * @Configuration这个注解的目的：
 * 为spring中的MyConfig创建cglib动态代理类
 * 这个代理类会实现BeanAware接口，它会往里面自动注入一个当前环境的BeanFactory
 * 当我们调用方法的时候，有一个 methodInvoke方法过滤器，它首先会取判断这个类对象是不是第一次得到
 * 如果是第一次得到，就会调用new方法来创建
 * 如果不是第一次得到，那会通过beanFactory返回对象给他
 * FactoryBean的getObject接口，里面会再封装一层代理，返回这个代理对象
 */
@Configuration
// @AopEnable
// 加上这个可以进行AOP代理
// 作用：往spring的后置处理器中添加一个处理器，能够处理 spring bean使原生对象变成代理对象
// @EnableAspectJAutoProxy
public class MyConfig {

	/**
	 * 如果这个方法是static修饰的，那就会创建两遍
	 * 这是因为spring底层创建bean的机制不同
	 * @return
	 */
	@Bean
	public IndexDaoImpl indexDao(){
		return new IndexDaoImpl();
	}

	/*@Bean
	public IndexDaoImpl2 indexDao2(){
		//indexDao();
		return new IndexDaoImpl2();
	}*/
}
