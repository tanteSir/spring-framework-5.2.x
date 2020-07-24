package spring.aop;

import com.lcx.dao.IndexDao;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.stereotype.Component;
import spring.handler.MyInvocationHandler;


/**
 * @ClassName : AopDemo
 * @Description :
 * @Author : Lin.cx
 * @Date: 2020-07-05 20:04
 */
@Component
public class AopDemo implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("AopDemo => postProcessBeforeInitialization :"+beanName);
		if (beanName.equals("indexDao")){
			bean = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{IndexDao.class}, new MyInvocationHandler(bean));
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return null;
	}
}
