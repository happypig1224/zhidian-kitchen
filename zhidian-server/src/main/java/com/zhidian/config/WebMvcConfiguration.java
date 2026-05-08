package com.zhidian.config;

import com.zhidian.interceptor.JwtTokenAdminInterceptor;
import com.zhidian.interceptor.JwtTokenUserInterceptor;
import com.zhidian.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

/**
 * 配置类，注册 web 层相关组件
 */
@Configuration
@Slf4j
@EnableAsync  // 启用异步任务支持
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册自定义拦截器
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");

        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login")
                .excludePathPatterns("/user/shop/status")
                .excludePathPatterns("/user/seckillVoucher/queryVoucher");
    }

    @Bean
    public Docket docket1(){
        log.info("准备生成接口文档...");
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("知味小厨项目接口文档")
                .version("2.0")
                .description("知味小厨项目接口文档")
                .build();

        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .groupName("管理端接口")
                .apiInfo(apiInfo)
                .select()
                //指定生成接口需要扫描的包
                .apis(RequestHandlerSelectors.basePackage("com.zhidian.controller.admin"))
                .paths(PathSelectors.any())
                .build();

        return docket;
    }

    @Bean
    public Docket docket2(){
        log.info("准备生成接口文档...");
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("知味小厨项目接口文档")
                .version("2.0")
                .description("知味小厨项目接口文档")
                .build();

        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .groupName("用户端接口")
                .apiInfo(apiInfo)
                .select()
                //指定生成接口需要扫描的包
                .apis(RequestHandlerSelectors.basePackage("com.zhidian.controller.user"))
                .paths(PathSelectors.any())
                .build();

        return docket;
    }

    /**
     * 设置静态资源映射，主要是访问接口文档（html、js、css）
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }

    /**
     * 配置 Spring MVC 的异步任务执行器
     * @return AsyncTaskExecutor
     */
    @Bean(name = "mvcTaskExecutor")
    public AsyncTaskExecutor mvcTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);           // 核心线程数 (增加，支持更多并发)
        executor.setMaxPoolSize(100);           // 最大线程数 (增加，应对流量高峰)
        executor.setQueueCapacity(500);         // 队列容量 (增加，缓冲更多请求)
        executor.setKeepAliveSeconds(60);       // 空闲线程存活时间
        executor.setThreadNamePrefix("mvc-async-"); // 线程名前缀
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略
        executor.setWaitForTasksToCompleteOnShutdown(true); // 关闭时等待任务完成
        executor.setAwaitTerminationSeconds(30); // 等待终止时间
        executor.initialize();
        log.info("MVC 异步任务执行器配置完成，核心线程：20, 最大线程：100, 队列容量：500");
        return executor;
    }
    
    /**
     * 配置异步支持，指定使用自定义的 AsyncTaskExecutor
     * @param config
     */
    @Override
    public void configureAsyncSupport(org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer config) {
        config.setTaskExecutor(mvcTaskExecutor());
        log.info("已配置 Spring MVC 异步任务执行器");
    }
    
    /**
     * 扩展 Spring MVC 框架的消息转化器
     * @param converters
     */
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");
        //创建一个消息转换器对象
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        //需要为消息转换器设置一个对象转换器，对象转换器可以将 Java 对象序列化为 json 数据
        converter.setObjectMapper(new JacksonObjectMapper());
        //将自己的消息转化器加入容器中
        converters.add(0,converter);
    }
}
