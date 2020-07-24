package spring.imports;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import spring.aop.AopDemo;

import java.util.function.Predicate;

/**
 * @ClassName : MyImportSelector
 * @Description :
 * @Author : Lin.cx
 * @Date: 2020-07-05 19:59
 */
public class MyImportSelector implements ImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		System.out.println("MyImport selectImports");
		return new String[]{AopDemo.class.getName()};
	}

	@Override
	public Predicate<String> getExclusionFilter() {
		return null;
	}
}
