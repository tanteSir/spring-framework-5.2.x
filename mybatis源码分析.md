# Mybatis源码分析



## 一、mybatis缓存各种问题

#### 1、一级缓存有效在spring中为什么会失效？

```
	因为一级缓存是基于 sqlSession设置的，mybatis为 spring提供了一个 spring-mybatis包，里面提供了一个SqlSessionTemplate类，它在容器启动时被注入到 mapper，代替了 mybatis默认使用的 defaultSqlSession。
	sqlSessionTemplate当中所有查询等方法都通过代理对象进行，而这个代理对象增强的方法就是执行查询后关闭 session功能，因此一级缓存会失效
	而且一级缓存也是个很鸡肋的东西，它只针对特定线程，是单线程的，其他线程的相同访问是不会生效的
```

#### 2、mybatis与 spring整合，为什么要关闭 session？

```
	因为 session是 mybatis的，mybatis想关就关，而 spring不暴露出 api就不能关闭，所以这里不关闭后面就不能关闭了。
    如果把关闭这个的控制权交给开发者，那不一定每个人都会关掉它，而且代码要多一层依赖关系，索性不如 mybatis自己关
    事务跟session无关，mybatis默认没有事务，事务都是由spring管理
```

#### 3、关于缓存

```
一级缓存很鸡肋：
	1、不能跟 spring结合；
	2、缓存只针对特定线程，并且是单线程的，其他线程相同的访问，缓存不会生效

二级缓存：
	1、基于空间，针对全体进程共享缓存
	2、@CacheNameSpace开启
	3、有很大的坑：基于命名空间缓存的，不同命名空间缓存不同，并且不支持分布式
	
综上，mybatis缓存都不好用，所以一般用 redis缓存
```



## 二、mybatis日志系统

#### 1、怎么打印 mybatis日志？

```
1、xml配置
2、sql操作前，指定 logFactory的日志依赖(加在 spring初始化之前)，然后在 log4j.properties中修改配置
3、用 ibatis的 configuration.setlogImpl(log4jImpl.class)，然后 	
	sqlSessionFactoryBean.setConfiguration(configuration)
	
流程：
	mybatis --> javaUtilLog --> jul
	
	遇到的一个坑：在别的项目里引用了slf4j，这个项目没有引用，但是idea认为它引用的到，导致在logFactory引用不到，mybatis在编译的时候会引入slf4j 的jar包但是打包的时候给剃掉了，造成打了断点也进不去的报错
```

#### 2、与 spring5整合，日志出现的问题

```
首先场景是这样的
	mybatis + log4j ====== 有日志输出
	mybatis + spring4 + log4j ====== 日志输出
	mybatis + spring5 + log4j ====== 没有日志输出
	
为什么呢？？？
	首先先清楚spring5日志新特性：
		1.spring4使用原生JCL(这里我称之为 old JCL)；
		2.spring5使用重写的JCL(我称之为 spring JCL)
	下面开始回答问题
	因为 mybatis底层默认使用 slf4j，spring底层默认使用 JCL, slf4j初始化后可以直接绑定 log4j并且打印日志，但它与 spring结合后，logFactory会默认采用 JCL打印日志

那么这个问题可以理解为，
	old JCL + log4j	========= 日志输出
	spring JCL + log4j ====== 没有日志输出

为什么呢？？？
	因为 spring JCL默认采用 JUL打印日志
那么为什么用 JUL就不能打印日志呢？？？
	因为 JUL的日志级别默认只打印 INFO以上的，而我们项目中日志级别是在 INFO之下的，所以不能打印
	而且蛋疼的是， JUL日志级别没办法改，只能扩展 mybatis

那么怎么扩展 mybatis？？？
	1.实现 org.apache.ibatis.logging.Log，改写里面 isDebugEnable方法，返回恒为true
	2. 指定LogFactory使用自己实现的 Log => LogFactory.useCustomLogging(MyLog.class)
	
```

## 三、mybatis流程分析

```
	首先 mybatis源码分两种情况：
		1.单独的 mybatis
		2.与 spring整合的 mybatis
	
	这两种情况下的源码分析会有所不同，如果是 spring-mybatis模式，那 mybatis就是从 spring初始化开始

    mybatis -- sqlSession -- defaultSqlSession -- defaultSqlSession.select -- sql
	spring-mybatis -- sqlSession -- sqlSessionTemplate -- sqlSessionTemplate.select -- proxy.invoke -- select -- sql

```

#### 1、spring跟 mybatis整合的流程

```
SpringBean实例化之前：
	1.通过mapperScan扫描，扫描出来后放入bd中
	2.把mapper变成 FactoryBean, MapperFactoryBeanDefinition
	3.为BeanDefinition添加一个构造方法的值，因为 Mybatis的MapperFactoryBean有一个有参构造方法，
spring在实例化这个对象的时候需要一个构造方法的值，这个值是一个class，后面spring在实例化过程中根据这个class返回我们的代理对象
		   
        
SpringBean实例化之中和之后：
	1、mybatis使用 spring的初始方法扩展点来完成 mapper信息的初始化，如sql语句初始化。
	这里 spring的扩展点主要是 afterPropertiesSet。当对象被实例化之后，因为它实例化的是 MapperFactoryBean，MapperFactoryBean实现 InitializingBean接口，这个接口调用 afterPropertiesSet方法，会再调用子类的 checkDao()，checkDao()里面有行代码是 configuration.addMapper(this.mapperInterface)，里面传参是当前接口。在这个 addMapper()里面会解析mapper接口里面所有方法、传参，最后注册到 configuration中
	我们理解为就是一个 mapper，又可以理解为它就是获得自己的信息，然后缓存起来放到一个 map中 	
调用链：
	org.apache.ibatis.session.Configuration#mappedStatements
	org.springframework.dao.support.DaoSupport#afterPropertiesSet
	org.mybatis.spring.mapper.MapperFactoryBean#checkDaoConfig
	org.apache.ibatis.session.Configuration#addMapper
	org.apache.ibatis.binding.MapperRegistry#addMapper
	org.apache.ibatis.builder.annotation.MapperAnnotationBuilder#parse
	
	mybatis开发了一个类，继承了DaoSupport并重写了里面的 checkDaoConfig，这个是个抽象方法，会调用子类的这个方法
```



#### 2、spring-mybatis的关联点有哪些？

```
1、@MapperScan

2、@Bean sqlSessionFactoryBean

	如果精通spring的应该知道，@MapperScan 的源码就是用 spring中的 Import和 ImportBeanDefinitionRegistrar技术来对 spring进行扩展。
	再对比 @BeanSqlSessionFactoryBean就会知道 spring会首先执行 ImportBeanDefinitionRegistrar当中的 registerBeanDefinition方法。
```

#### 3、mybatis如何解析  mapper？有几种方式？

```
4种	url、resource、class、package	
```

#### 4、spring-mybatis自动装配

```
首先了解知识点：
	1.自动装配用 set(xxx) ，mybatis这里是 byType
	2.byName时，有set的话，会先寻找 set属性的名字，没有 set的话 byName会直接寻找属性的名字 
	3.mapper.xml信息是初始化 SqlSessionFactoryBean时读取并存放的
    4.看源码的时候要注意：popolateBean 设置bean属性的
	
	问题来了，为什么 mybatis与spring结合，自动装配 SqlSession时默认用byType，并且用 set方式注入sqlSessionFactory，而不直接定义一个 sqlSessionFactory，然后用@AutoWired？
	因为用 @Autowired就要依赖spring，不能解耦
	
JD面试题：方法名为什么要跟 statement的 id相同？
	因为 sql语句是根据 命名空间和 方法名获取到的，如果方法名跟 mapper里面的 id不一致的话，就会得不到 sql

为什么 mybatis中的 configuration会包含 statementId?
	Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>("Mapped Statements ...");
	里面的 MappedStatement包含所有 mapper信息
	spring初始化时，会读取 mapper所有信息，放到这个 mappedStatements中
	statement  由包名+类名+方法名组成
	
mybatis在 spring中自动装配的属性有哪些?
	mapperInterface	---class
	addToConfig	---被包含
	setSqlSessionFactory
	setSqlSessionTemplate
	
	自动装配的时候，如果属性是 class、Date、String、Url、Uri等简单类型，或没有 set方法，或已经自行装配的，则直接忽略。
```

