package com.lcx.service.impl;

import com.lcx.service.IndexService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @ClassName : IndexServiceImpl
 * @Description :
 * @Author : Lin.cx
 * @Date: 2020-06-21 16:28
 */
@Service("indexService")
public class IndexServiceImpl implements IndexService {

	@PostConstruct
	public void init(){
		System.out.println("Hello Spring!");
	}

	@Override
	public void print() {
		System.out.println("打印方法");
	}
}
