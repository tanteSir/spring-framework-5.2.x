package com.spring.handler;


import org.springframework.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;

/**
 * @ClassName : MyInvocationHandler
 * @Description :
 * @Author : Lin.cx
 * @Date: 2020-07-03 10:14
 */
public class MyInvocationHandler implements InvocationHandler {

	Object target;

	public MyInvocationHandler(Object target){
		this.target = target;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("代理了噢");
		return method.invoke(target, args);
	}
}
