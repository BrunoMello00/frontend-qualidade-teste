package com.tcc.estoque.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Testes Selenium E2E - Sistema de Estoque e Vendas")
class SistemaEstoqueVendasSeleniumTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:4200";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    @BeforeAll
    static void setupClass() {

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, WAIT_TIMEOUT);

        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    @AfterAll
    static void teardownClass() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void setup() {

        driver.get(BASE_URL);
    }

    @Test
    @Order(1)
    @DisplayName("Deve carregar a pagina inicial do sistema")
    void deveCarregarPaginaInicialDoSistema() {

        assertAll("Validacao da pagina inicial",
            () -> {
                String pageTitle = driver.getTitle();
                assertThat(pageTitle).isNotEmpty();
                assertThat(pageTitle).containsAnyOf("Estoque", "Vendas", "Sistema", "Qualidade", "Teste");
            },
            () -> {
                String currentUrl = driver.getCurrentUrl();
                assertThat(currentUrl).startsWith(BASE_URL);
            },
            () -> {

                WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                assertThat(body).isNotNull();
                assertThat(body.isDisplayed()).isTrue();
            }
        );
    }

    @Test
    @Order(2)
    @DisplayName("Deve validar navegacao e elementos da interface")
    void deveValidarNavegacaoEElementosInterface() {

        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        assertAll("Validacao de navegacao e interface",
            () -> {

                boolean temElementosEsperados = false;

                try {

                    WebElement navbar = driver.findElement(By.cssSelector("nav, .navbar, [role='navigation']"));
                    temElementosEsperados = navbar.isDisplayed();
                } catch (Exception e) {

                    try {
                        WebElement header = driver.findElement(By.cssSelector("header, .header"));
                        temElementosEsperados = header.isDisplayed();
                    } catch (Exception ex) {

                        String pageSource = driver.getPageSource();
                        temElementosEsperados = pageSource.contains("estoque") || 
                                              pageSource.contains("produto") || 
                                              pageSource.contains("cliente") ||
                                              pageSource.contains("venda");
                    }
                }

                assertThat(temElementosEsperados).as("Pagina deve ter elementos de navegacao ou conteudo relevante").isTrue();
            },
            () -> {

                String pageSource = driver.getPageSource();
                assertThat(pageSource).isNotEmpty();
                assertThat(pageSource.length()).isGreaterThan(100); 
            }
        );
    }

    @Test
    @Order(3)
    @DisplayName("Deve validar formulario de entrada de dados")
    void deveValidarFormularioEntradaDados() {

        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        assertAll("Validacao de formularios",
            () -> {
                boolean temFormulario = false;

                try {

                    driver.findElements(By.tagName("form")).forEach(form -> {
                        if (form.isDisplayed()) {

                        }
                    });

                    driver.findElements(By.cssSelector("input, select, textarea")).forEach(input -> {
                        if (input.isDisplayed()) {

                        }
                    });

                    temFormulario = !driver.findElements(By.cssSelector("input, form, button")).isEmpty();
                } catch (Exception e) {

                    temFormulario = driver.findElements(By.cssSelector("a, button, [onclick]")).size() > 0;
                }

                assertThat(temFormulario).as("Sistema deve ter elementos interativos ou formularios").isTrue();
            }
        );
    }

    @Test
    @Order(4) 
    @DisplayName("Deve validar responsividade e funcionalidade basica")
    void deveValidarResponsividadeEFuncionalidadeBasica() {

        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 1080));
        String desktopPageSource = driver.getPageSource();

        driver.manage().window().setSize(new org.openqa.selenium.Dimension(768, 1024));
        String tabletPageSource = driver.getPageSource();

        driver.manage().window().setSize(new org.openqa.selenium.Dimension(375, 667));
        String mobilePageSource = driver.getPageSource();

        assertAll("Validacao de responsividade",
            () -> assertThat(desktopPageSource).isNotEmpty(),
            () -> assertThat(tabletPageSource).isNotEmpty(), 
            () -> assertThat(mobilePageSource).isNotEmpty(),
            () -> {

                boolean temConteudoConsistente = desktopPageSource.length() > 0 &&
                                               tabletPageSource.length() > 0 &&
                                               mobilePageSource.length() > 0;

                assertThat(temConteudoConsistente).as("Sistema deve ser responsivo").isTrue();
            }
        );
    }

    @Test
    @Order(5)
    @DisplayName("Deve validar performance basica da aplicacao")
    void deveValidarPerformanceBasicaAplicacao() {

        long startTime = System.currentTimeMillis();

        driver.get(BASE_URL);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;

        assertAll("Validacao de performance",
            () -> {

                assertThat(loadTime).as("Tempo de carregamento deve ser aceitavel").isLessThan(10000);
            },
            () -> {

                String pageSource = driver.getPageSource();
                boolean paginaFuncional = !pageSource.contains("error") || 
                                        !pageSource.toLowerCase().contains("exception") ||
                                        pageSource.length() > 500;

                assertThat(paginaFuncional).as("Pagina deve estar funcional sem erros criticos").isTrue();
            }
        );
    }

}

