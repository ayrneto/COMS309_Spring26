package onetoone.realtime_chat;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Makes uploaded files downloadable at GET /uploads/<filename>.
 *
 * The "file:uploads/" prefix tells Spring to look in the "uploads" folder
 * relative to the directory where the JAR is running — the same folder
 * where ChatController writes files.
 *
 * Example: a file saved to  uploads/1712345678000_doc.pdf
 *          is served at     GET /uploads/1712345678000_doc.pdf
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}