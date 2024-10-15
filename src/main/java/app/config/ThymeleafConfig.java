package app.config;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class ThymeleafConfig {
    public static TemplateEngine templateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();

        // Sæt prefix til templates-mappen i resources
        templateResolver.setPrefix("/templates/"); // Tilføj en skråstreg for at angive rodmappen
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML"); // Anbefales at angive
        templateResolver.setCharacterEncoding("UTF-8"); // Anbefales for at undgå tegnproblemer

        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }
}
