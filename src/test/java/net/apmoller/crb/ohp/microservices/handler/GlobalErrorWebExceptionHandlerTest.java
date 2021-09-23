package net.apmoller.crb.ohp.microservices.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import kotlin.text.Charsets;
import net.apmoller.crb.ohp.microservices.config.SpringBootComponentsConfig;
import net.apmoller.crb.ohp.microservices.exception.BadRequestException;
import net.apmoller.crb.ohp.microservices.exception.DataNotFoundException;
import net.apmoller.crb.ohp.microservices.exception.ForbiddenException;
import net.apmoller.crb.ohp.microservices.exception.UnauthorizedException;
import net.apmoller.crb.ohp.microservices.util.ExceptionUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalErrorWebExceptionHandlerTest {

    private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ReactiveWebServerFactoryAutoConfiguration.class,
                    HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class,
                    ErrorWebFluxAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class, SpringBootComponentsConfig.class))
            .withPropertyValues("spring.main.web-application-type=reactive", "spring.application.reactive-handlers.enabled=true",
                    "server.port=0")
            .withUserConfiguration(Application.class);

    @Test
    public void unauthorized() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/unauthorized").exchange().expectStatus().isUnauthorized()
                    .expectBody().jsonPath("status").isEqualTo("401")
                    .jsonPath("requestUri").isEqualTo("/unauthorized")
                    .jsonPath("method").isEqualTo("GET")
                    .jsonPath("message").isEqualTo("Unauthorized");
        });
    }

    @Test
    public void forbidden() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/forbidden").exchange().expectStatus().isForbidden()
                    .expectBody().jsonPath("status").isEqualTo("403")
                    .jsonPath("requestUri").isEqualTo("/forbidden")
                    .jsonPath("method").isEqualTo("GET")
                    .jsonPath("message").isEqualTo("Forbidden");
        });
    }

    @Test
    public void dataNotFound() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/dataNotFound").exchange().expectStatus().isNotFound()
                    .expectBody().jsonPath("status").isEqualTo("404")
                    .jsonPath("requestUri").isEqualTo("/dataNotFound")
                    .jsonPath("method").isEqualTo("GET")
                    .jsonPath("message").isEqualTo("Data not found");
        });
    }

    @Test
    public void internalError() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/internalServerError").exchange().expectStatus().value(Matchers.is(500))
                    .expectBody().jsonPath("status").isEqualTo("500")
                    .jsonPath("requestUri").isEqualTo("/internalServerError")
                    .jsonPath("method").isEqualTo("GET")
                    .jsonPath("message").isEqualTo("Internal Server Error");
        });
    }

    @Test
    public void webclientErrorBadRequest() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/webclientErrorBadRequest").exchange().expectStatus().value(Matchers.is(400))
                    .expectBody().jsonPath("status").isEqualTo("400")
                    .jsonPath("requestUri").isEqualTo("/webclientErrorBadRequest")
                    .jsonPath("method").isEqualTo("GET")
                    .jsonPath("message").isEqualTo("400 Missing mandatory param");
        });
    }

    @Test
    public void webclientErrorForbidden() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/webclientErrorForbidden").exchange().expectStatus().value(Matchers.is(403))
                    .expectBody().jsonPath("status").isEqualTo("403")
                    .jsonPath("requestUri").isEqualTo("/webclientErrorForbidden")
                    .jsonPath("method").isEqualTo("GET")
                    .jsonPath("message").isEqualTo("403 Forbidden");
        });
    }

    @Test
    public void webclientErrorUnauthorized() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/webclientErrorUnauthorized").exchange().expectStatus().value(Matchers.is(401))
                    .expectBody().jsonPath("status").isEqualTo("401")
                    .jsonPath("requestUri").isEqualTo("/webclientErrorUnauthorized")
                    .jsonPath("method").isEqualTo("GET")
                    .jsonPath("message").isEqualTo("401 Unauthorized");
        });
    }

    @Test
    public void webclientErrorNotFound() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/webclientErrorNotFound").exchange().expectStatus().value(Matchers.is(404))
                    .expectBody().jsonPath("status").isEqualTo("404")
                    .jsonPath("requestUri").isEqualTo("/webclientErrorNotFound")
                    .jsonPath("method").isEqualTo("GET")
                    .jsonPath("message").isEqualTo("404 Not found");
        });
    }

    @Test
    public void webclientError() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/webclientErrorNotAllowed").exchange().expectStatus().value(Matchers.is(500))
                    .expectBody().jsonPath("status").isEqualTo("500")
                    .jsonPath("requestUri").isEqualTo("/webclientErrorNotAllowed")
                    .jsonPath("method").isEqualTo("GET")
                    .jsonPath("message").isEqualTo("Internal Server Error");
        });
    }

//    @Test
//    public void bindingResultError() {
//        this.contextRunner.run((context) -> {
//            WebTestClient client = WebTestClient.bindToApplicationContext(context)
//                    .build();
//            client.post().uri("/bindException").contentType(MediaType.APPLICATION_JSON)
//                    .syncBody("{}").exchange().expectStatus().isBadRequest().expectBody()
//                    .consumeWith(entityExchangeResult -> entityExchangeResult.ex)
//                    .jsonPath("status").isEqualTo("400")
//                    .jsonPath("message").isEqualTo(ExceptionUtils.VALIDATION_ERROR_DESCRIPTION)
//                    .jsonPath("requestUri").isEqualTo("/bindException")
//                    .jsonPath("method").isEqualTo("POST")
//                    .jsonPath("$.subErrors.length()").isEqualTo(1)
//                    .jsonPath("$.subErrors[0].field").isEqualTo("CarrierCode")
//                    .jsonPath("$.subErrors[0].rejectedValue").isEmpty()
//                    .jsonPath("$.subErrors[0].message").isEqualTo("Invalid Carrier Code");
//
//        } );
//    }

    @Test
    public void badRequest() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/badRequest").exchange().expectStatus()
                    .isBadRequest().expectBody().jsonPath("status")
                    .isEqualTo("400").jsonPath("message")
                    .isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
        });
    }

    @Test
    public void responseStatusException() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/incorrectUri").exchange().expectStatus()
                    .isNotFound().expectBody().jsonPath("status")
                    .isEqualTo("404")
                    .jsonPath("requestUri").isEqualTo("/incorrectUri")
                    .jsonPath("method").isEqualTo("GET")
                    .jsonPath("message").isEqualTo("404 NOT_FOUND");
        });
    }

    @Test
    public void constraintViolationException() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.post().uri("/constraintViolationException").contentType(MediaType.APPLICATION_JSON)
                    .syncBody("{}").exchange().expectStatus().isBadRequest().expectBody()
                    .jsonPath("status").isEqualTo("400")
                    .jsonPath("message").isEqualTo(ExceptionUtils.VALIDATION_ERROR_DESCRIPTION)
                    .jsonPath("requestUri").isEqualTo("/constraintViolationException")
                    .jsonPath("method").isEqualTo("POST")
                    .jsonPath("$.subErrors.length()").isEqualTo(2)
                    .jsonPath("$.subErrors[*].field").value(Matchers.containsInAnyOrder("parameter1", "parameter2"))
                    .jsonPath("$.subErrors[*].rejectedValue").value(Matchers.containsInAnyOrder(null, "AB"))
                    .jsonPath("$.subErrors[*].message").value(Matchers.containsInAnyOrder("must not be null", "must be greater than or equal to 3"));
        });
    }

    @Test
    public void serverWebInputException() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.post().uri("/serverWebInputException")
                    .body(Mono.empty(), String.class)
                    .exchange()
                    .expectStatus().isBadRequest().expectBody()
                    .jsonPath("status").isEqualTo("400")
                    .jsonPath("message").isEqualTo("Bad Request")
                    .jsonPath("requestUri").isEqualTo("/serverWebInputException")
                    .jsonPath("method").isEqualTo("POST");
        });
    }


    @Test
    public void serverWebInputDecodingException() {
        this.contextRunner.run((context) -> {
            WebTestClient client = WebTestClient.bindToApplicationContext(context)
                    .build();
            client.get().uri("/decodingException").exchange().expectStatus()
                    .isBadRequest().expectBody().jsonPath("status")
                    .isEqualTo("400").jsonPath("message")
                    .isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase())
                    .jsonPath("$.subErrors.length()").isEqualTo(1)
                    .jsonPath("$.subErrors[*].field").value(Matchers.contains("some_inputObject.some_inputDate"))
                    .jsonPath("$.subErrors[*].rejectedValue").value(Matchers.contains("2019-199-199"))
                    .jsonPath("$.subErrors[*].message").value(Matchers.contains("Invalid format"));
            ;
        });
    }

    @Configuration
    public static class Application {

        @RestController
        protected static class ErrorController {

            @GetMapping("/")
            public String home() {
                throw new IllegalStateException("Expected!");
            }

            @GetMapping("/unauthorized")
            public String unauthorized() {
                throw new UnauthorizedException("Unauthorized!");
            }

            @GetMapping("/forbidden")
            public String forbidden() {
                throw new ForbiddenException("Forbidden!");
            }

            @GetMapping("/webclientErrorBadRequest")
            public String webclientErrorBadRequest() {
                throw WebClientResponseException.create(400, "Missing mandatory param", HttpHeaders.EMPTY, "dummy".getBytes(), Charsets.UTF_8);
            }

            @GetMapping("/webclientErrorForbidden")
            public String webclientErrorForbidden() {
                throw WebClientResponseException.create(403, "Forbidden", HttpHeaders.EMPTY, "dummy".getBytes(), Charsets.UTF_8);
            }

            @GetMapping("/webclientErrorUnauthorized")
            public String webclientErrorUnauthorized() {
                throw WebClientResponseException.create(401, "Unauthorized", HttpHeaders.EMPTY, "dummy".getBytes(), Charsets.UTF_8);
            }

            @GetMapping("/webclientErrorNotFound")
            public String webclientErrorNotFound() {
                throw WebClientResponseException.create(404, "Not found", HttpHeaders.EMPTY, "dummy".getBytes(), Charsets.UTF_8);
            }

            @GetMapping("/webclientErrorNotAllowed")
            public String webclientErrorNotAllowed() {
                throw WebClientResponseException.create(405, "Method Not Allowed", HttpHeaders.EMPTY, "dummy".getBytes(), Charsets.UTF_8);
            }

            @GetMapping("/internalServerError")
            public String internalError() {
                throw new NullPointerException("Null pointer!");
            }

            @GetMapping("/badRequest")
            public Mono<String> badRequest() {
                return Mono.error(new BadRequestException("some message"));
            }

            @GetMapping("/dataNotFound")
            public Mono<String> dataNotFound() {
                return Mono.error(new DataNotFoundException("some DataNotFoundException"));
            }

            @PostMapping("/bindException")
            public Mono<String> bindException() {
                FieldError fieldError = new FieldError("Code", "CarrierCode", "Invalid Carrier Code");
                BindingResult bindingResult = new BeanPropertyBindingResult("User", "ID");
                bindingResult.addError(fieldError);
                BindException bindException = new BindException(bindingResult);
                return Mono.error(bindException);
            }

            @PostMapping("/constraintViolationException")
            public Mono<String> constraintViolationException() {
                TestObjectWithValidation testClass;
                Set<ConstraintViolation<TestObjectWithValidation>> violations;
                LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
                localValidatorFactoryBean.afterPropertiesSet();
                Validator localValidator = localValidatorFactoryBean.getValidator();
                testClass = new TestObjectWithValidation(null, "AB");
                violations = localValidator.validate(testClass);
                ConstraintViolationException constraintViolationException = new ConstraintViolationException(violations);
                return Mono.error(constraintViolationException);
            }

            @PostMapping("/serverWebInputException")
            public Mono<Void> serverWebInputException(@RequestBody Mono<String> input) {
                return input.then();
            }


            @GetMapping("/decodingException")
            public Mono<String> decodingException() {
                InvalidFormatException invalidFormatException = mock(InvalidFormatException.class);
                List<JsonMappingException.Reference> references = new ArrayList<>();
                references.add(new JsonMappingException.Reference("some_dummyObject", "some_inputObject"));
                references.add(new JsonMappingException.Reference("some_dummyDate", "some_inputDate"));
                when(invalidFormatException.getPath()).thenReturn(references);
                when(invalidFormatException.getValue()).thenReturn("2019-199-199");
                ServerWebInputException serverWebInputException = new ServerWebInputException("ServerWebInputException", null,
                        new DecodingException("DecodingException", invalidFormatException));

                return Mono.error(serverWebInputException);
            }
        }

    }

    /**
     * Simple test class with some properties that are annotated with validation constraints
     */
    public static class TestObjectWithValidation {

        @NotNull
        private String parameter1;

        @Min(value = 3)
        private String parameter2;

        TestObjectWithValidation(@NotNull String parameter1, @Min(value = 3) String parameter2) {
            this.parameter1 = parameter1;
            this.parameter2 = parameter2;
        }

        public void testMethodWithValidation(@NotNull String parameter1, @Pattern(regexp = "[a-zA-Z0-9]{9}") String parameter2) {

        }

    }

}
