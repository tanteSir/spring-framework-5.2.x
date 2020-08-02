package com.lcx.dao.impl;

import com.lcx.dao.IndexDao;
import com.lcx.service.CyclicDependenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @ClassName : IndexDaoImpl
 * @Description :
 * @Author : Lin.cx
 * @Date: 2020-07-08 22:33
 */
@Repository("indexDao")
public class IndexDaoImpl implements IndexDao {

	@Autowired
	private CyclicDependenceService cyclicDependenceService;

	public IndexDaoImpl(){
		System.out.println("indexDaoImpl -- init");
	}
}
