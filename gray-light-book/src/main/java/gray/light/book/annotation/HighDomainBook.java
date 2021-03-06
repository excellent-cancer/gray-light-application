package gray.light.book.annotation;

import gray.light.book.config.BookAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 自动配置Book领域
 *
 * @author XyParaCrim
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(BookAutoConfiguration.class)
public @interface HighDomainBook {
}
