package com.spring.annotation;

import com.spring.imports.MyImportSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @ClassName : AopEnable
 * @Description :
 * @Author : Lin.cx
 * @Date: 2020-07-05 20:33
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(MyImportSelector.class)
public @interface AopEnable {


}
