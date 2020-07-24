package spring.annotation;

import org.springframework.context.annotation.Import;
import spring.imports.MyImportSelector;

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
