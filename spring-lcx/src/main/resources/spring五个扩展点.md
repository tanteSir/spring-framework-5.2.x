1、BeanPostProcessor			    
    插手Bean实例化过程、实例化之后，在bean放到bean容器管理之前处理
    经典场景：@PostConstruct、AOP

2、BeanFactoryPostProcessor			
    springBean被容器重任意一个bean被实例化之前来回调它的方法，针对beanFactory来建设
    经典场景：ConfigurationClassPostProcessor #postProcessBeanFactory 针对配置类加上cglib代理

3、BeanDefinitionRegistryPostProcessor		
    BeanFactoryPostProcessor的子类。在BeanFactoryPostProcessor之前执行，why？
    spring底层源码决定的，是先遍历BeanDefinitionRegistryPostProcessor(自定义的 -> 手动add、系统内部的，自定义先执行)
	经典场景：spring内部提供一个ConfigurationClassPostProcessor类 实现了BeanDefinitionRegistryPostProcessor这个接口
             其中有个回调方法，完成了spring核心的功能：扫描里面的类、解析XML、解析import、解析配置类并判断是否为完整						的配置类

4、ImportSelector				
    通过selectImports()方法，返回一个类名(全名)，由spring自己把它变成bd，从而动态添加bd(这个bd是死的)
	DeferredImportSelector	延迟加载
	经典场景：动态扫描

5、ImportBeanDefinitionRegistrar		
    registerBeanDefinitions()方法可以得到 BeanDefinitionRegistry，所以可以动态添加bd。selector能做的事情，registrar都能做，反之则不可
    经典场景：Mybatis的mapperScan