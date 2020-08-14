### 关于依赖注入

```
怎么寻找依赖对象？
	1、byName	2、byType

怎么填充属性？
	0、反射  field.set	1、构造器		2、反射 set方法	这里的序号是 autowireMode的值
	

@Autowired	使用byType，找不到就 byName，找到后默认用反射 field.set的方式完成填充
		如果在 set方法上加 @Autowired，那就用 set方法填充

```



#### 源码解析零碎的知识点笔记

##### 1、redis与 spring怎么结合？

```
	Redis 中 @EnableRedisHttpSession 里面 import进了 RedisHttpSessionConfiguration类，然后这个 configuration类实现了 ImportAware
	ImportAware 里面有 setImportMetadata()，可以获取对应类上的所有注解与属性
	
	ImportAwareBeanPostProcessor  是个内部类，对以后看redis源码有很大帮助
```

##### 2、部分后置处理器作用

```
BeanPostProcessorChecker  			检查 Bean有没有被后置处理器执行 如果没被执行的话，就会报错
CommonAnnotationBeanPostProcessor	处理公共注解，如 @Autowired
RequiredAnnotationBeanPostProcessor	处理@Require，生成Bean时某属性必须存在

后置处理器有些会存在 bdMap，有些创建后用完就没用了所以不会进去
```

##### 3、AspectJ与AOP

```
	spring aop --jdk cglib 都是动态织入
	Aspectj  都是静态织入  在编译器织入
	
	AOP什么时候被加载进后置处理器？
	通过 @EnableAspectJAutoProxy
	
	ConversionService  用来转换类型的
```

##### 4、Spring IOC怎么理解？

```
微观上：仅仅是一个 ConcurrentHashMap，存放对象的名字和对象的实例
宏观上：是整个 Spring环境，包括 Bean工厂、后置处理器、读取器、注册器等等
```

##### 5、spring创建过程最开始的那几个对象，为什么不能再 context创建完成后直接放到 IOC的 currentHashMap中去，是技术上无法实现，还是破坏了封装性，或者破坏了 spring的设计思想？

```
	spring把 bean放到容器中要进行很多处理，这个是必须的，包括通过 BeanFactoryPostProcessor获取所有的beanDefinition并且进行改变等等，
	如果直接放进去就破坏了 spring的设计思想
```

##### 6、spring后置处理器为啥用 list数据结构？为什么不用 set 或者 map，如果我有1000000个类自己实现了后置处理器的接口，这个list就这么一个个去对比？

```
	区别不大，并且是遍历的，list比map快
```

##### 7、spring决定使用构造处理器的小知识

```
// 由后置处理器决定返回哪些构造方法
	Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
	spring为什么要得到构造方法？？  
	因为要去new对象；如果用的是默认构造方法，spring则认为没有构造方法 
```

##### 8、@MapperScan() 的原理是什么？

```
	作用：扫描接口，使其变为对象，且在 spring管理中
	spring在解析 @MapperScan时，会解析 @Import上的 MapperScannerRegistrar，这个类实现了 ImportBeanDefinitionRegistrar，而实现的这个类可以把 spring里面的 BeanDifinitionMap暴露出去，让生成的 Mybatis bean 注册到当中

	
	MapperSannerRegistrar 里面有一个registerBeanDefinition()，这个方法是在所有bean执行之前执行的
```

##### 9、Spring里的 full lite标识

```
	full：加了@Configuration 	表示是一个全配置类
	lite：加了@Import 			表示是个部分配置类
```

##### 10、@Configuration 加与不加的区别	

```
	加了会进行cglib动态代理，关于 cglib的流程
	
	appconfig-> cglib-> class-> bd-> bean 

	methodIntercept
	cglib -> Enhancer
	
	两个@Bean 一个static，判断调用几次场景     xml测试场景

	具体信息待补充
```

##### 11、三种 import的使用

```
	普通类							扫描完成后注册
	
	1.importSelector				  扫描出来先放到configurationClasses变量，后面解析出来进行注册  loadBeanDefinition
	
	2.importBeanDefinitionRegistrar	  扫描出来先放到importBeanDefinitionRegistrars变量，后面解析出来进行注册
	
	3.import普通类						扫描出来先放到configurationClasses变量，然后直接注册
```

##### 12、bean初始化过程？

```
大体流程
	new -> beanPostProcessor -> doCreateBean() -> beanWarpper -> 通过一系列的后置处理器完成bean的初始化
```

##### 13、自动装配 NO 跟 ByType有什么区别？

```
byType 是用 set方式注入
spring自动装配模型 ！= 自动装配的技术
NO == byType技术
spring默认 NO

伪代码如下：
Lcx lcx{
  
  @Autowired  
  Test test;

  mode = no{
     test直接忽略
  }
  mode = no1{
     test通过类型自动装配  但 它!= byType
  }

}
```

##### 14、spring的 FactoryMethod模拟场景

```
两个@Bean 一个static，判断调用几次场景     xml测试场景

具体待补充
```

```java
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
		IndexService indexService = (IndexService) annotationConfigApplicationContext.getBean("indexService");
		indexService.print();


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
```

