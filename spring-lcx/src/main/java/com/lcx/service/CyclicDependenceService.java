package com.lcx.service;

import com.lcx.dao.IndexDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @ClassName : CyclicDependenceService
 * @Description :
 * @Author : Lin.cx
 * @Date: 2020-08-02 22:13
 */
@Repository("cyclicService")
public class CyclicDependenceService {

	/**
	 * 测试循环依赖问题
	 */
	@Autowired
	private IndexDao indexDao;
}
