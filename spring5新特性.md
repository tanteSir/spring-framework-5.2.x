### spring5新特性

spring5新特性主要有

1、JDK最少要1.8

​		

2、Core框架修订

​		利用了 jdk1.8的 default方法，以及可选的声明，如：@Nullable 和 @NotNull，将运行期的bug放到编译器检查

3、Kotlin函数式编程

4、响应式编程模型



而这里主要讲的是 spring5的日志新特性

##### spring5日志系统

```
	spring4使用原生 JCL从 log4j、JUL里获取存在的日志，其中底层默认使用 log4j

​	spring5 重写了JCL源码，里面用switch case做日志选用的判断，判断机制是用 class.forName。首选是log4j2，如果用class.forName得不到，就用slf4j

​	JCL可以选择 log4j 或 JUL打印日志

​	JUL在 logFactory中取出来是 jdk14Logger@...

​	底层实现逻辑是，通过一个数组的顺序，先后判断能否找到具体的实现，如果找到了就退出循环并拿来使用

	现在 slf4j更加优秀，需要桥接器，绑定其他的日志框架，加载maven 依赖里，有些桥接器会包含实现类jar包，有些没有，所以有些要添加实现依赖有些不需要
	
	slf4j只是一个日志标准，并不是日志系统的具体实现
```

##### 遇到的坑

```
	桥接后可能引起内循环报错，细节待补充
```

