package com.lcx.controller;

import com.lcx.service.IndexService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import spring.config.MyConfig;

/**
 * @ClassName : MyController
 * @Description :
 * @Author : Lin.cx
 * @Date: 2020-06-21 16:27
 */
public class MyController {

	public static void main(String[] args) {
		// 准备spring所有环境
		// 准备工程 = DefaultListableBeanFactory
		// 实例化一个 bdReader和一个 scanner
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
		annotationConfigApplicationContext.register(MyConfig.class);
		// 添加自定义的处理器
		//annotationConfigApplicationContext.addBeanFactoryPostProcessor(new MyBeanFactoryProcessor());
		// 初始化spring环境
		annotationConfigApplicationContext.refresh();
		/*IndexService indexService = (IndexService) annotationConfigApplicationContext.getBean("indexService");
		indexService.print();*/


		/**
		 * 到这里已经完成了扫描，但是完成这个扫描的并不是AnnotationConfigApplicationContext 里的scanner
		 */

		/*IndexService indexService = annotationConfigApplicationContext.getBean(IndexService.class);
		IndexService indexService1 = annotationConfigApplicationContext.getBean(IndexService.class);
		System.out.println(indexService.hashCode()+"-----------------"+indexService1.hashCode());
		indexService.print();*/

		//IndexDaoImpl2 bean = annotationConfigApplicationContext.getBean(IndexDaoImpl2.class);

		/*// 模拟 CGLIB动态代理
		Enhancer enhancer = new Enhancer();
		// 增强父类，因为这是继承实现的
		enhancer.setSuperclass(IndexDao.class);
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		// 过滤方法，不能每次调用都去new
		enhancer.setCallback(new MyMethodCallBack());
		IndexDao indexDao = (IndexDao) enhancer.create();*/


	}
}
