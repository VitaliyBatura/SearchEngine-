package searchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import searchengine.services.LemmaFinder;

import java.io.IOException;

@Configuration
public class ConfigPool {

    @Bean
    public ForkJoinPoolFactoryBean forkJoinPoolFactoryBean() {
        ForkJoinPoolFactoryBean poolFactoryBean = new ForkJoinPoolFactoryBean();
        return poolFactoryBean;
    }

    @Bean
    public ThreadPoolTaskExecutor threadExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setDaemon(true);
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.setMaxPoolSize(20);
        threadPoolTaskExecutor.setQueueCapacity(10);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean
    public LemmaFinder lemmaFinderBean() throws IOException {
        return LemmaFinder.getInstance();
    }
}
