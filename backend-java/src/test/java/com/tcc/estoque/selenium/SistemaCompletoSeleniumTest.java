package com.tcc.estoque.selenium;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SistemaCompletoSeleniumTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port;
        
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    void testAutenticacaoEControleAcesso() {
        long inicioTeste = System.currentTimeMillis();

        
        driver.get(baseUrl + "/dashboard");
        
        wait.until(ExpectedConditions.urlContains("/login"));
        assertThat(driver.getCurrentUrl()).contains("/login");

        
        WebElement emailField = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@type='email' or @placeholder='Email' or @id='email']")));
        WebElement passwordField = driver.findElement(
                By.xpath("//input[@type='password' or @placeholder='Senha' or @id='password']"));
        WebElement loginButton = driver.findElement(
                By.xpath("//button[contains(text(), 'Login') or contains(text(), 'Entrar')]"));

        emailField.sendKeys("admin@invalido.com");
        passwordField.sendKeys("senhaErrada");
        loginButton.click();

        WebElement errorMessage = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'inválid') or contains(text(), 'erro') or contains(@class, 'error')]")));
        assertThat(errorMessage.isDisplayed()).isTrue();

        
        emailField.clear();
        passwordField.clear();
        emailField.sendKeys("admin@sistema.com");
        passwordField.sendKeys("admin123");
        loginButton.click();

        wait.until(ExpectedConditions.urlContains("/dashboard"));
        assertThat(driver.getCurrentUrl()).contains("/dashboard");

        long tempoLogin = System.currentTimeMillis() - inicioTeste;
        
        
        assertAll("Validações de autenticação",
                () -> assertThat(tempoLogin).isLessThan(5000), 
                () -> assertThat(driver.getTitle()).isNotEmpty(),
                () -> assertThat(driver.findElement(By.xpath("//*[contains(text(), 'Dashboard') or contains(text(), 'Início')]")).isDisplayed()).isTrue()
        );
    }

    @Test
    @Order(2)
    void testGestaoEstoqueInterfaceResponsiva() {
        realizarLogin();

        long inicioTeste = System.currentTimeMillis();

        
        WebElement menuProdutos = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(text(), 'Produtos') or contains(@href, '/produtos')]")));
        menuProdutos.click();

        wait.until(ExpectedConditions.urlContains("/produtos"));

        
        WebElement btnNovoProduto = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Novo') or contains(text(), 'Adicionar') or contains(text(), 'Criar')]")));
        btnNovoProduto.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@placeholder='Nome do produto' or @name='nome' or @id='nome']")))
                .sendKeys("Produto Selenium Test");

        driver.findElement(By.xpath("//input[@placeholder='Descrição' or @name='descricao' or @id='descricao']"))
                .sendKeys("Produto criado via teste Selenium");

        driver.findElement(By.xpath("//input[@placeholder='Preço' or @name='preco' or @id='preco']"))
                .sendKeys("99.90");

        driver.findElement(By.xpath("//input[@placeholder='Estoque' or @name='estoque' or @id='estoque' or @name='quantidade']"))
                .sendKeys("100");

        WebElement btnSalvar = driver.findElement(
                By.xpath("//button[contains(text(), 'Salvar') or contains(text(), 'Criar') or @type='submit']"));
        btnSalvar.click();

        
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'sucesso')]")),
                ExpectedConditions.urlContains("/produtos")
        ));

        
        
        driver.manage().window().setSize(new Dimension(375, 667));
        driver.navigate().refresh();
        
        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Produto Selenium Test')]")));
        
        WebElement produtoMobile = driver.findElement(
                By.xpath("//*[contains(text(), 'Produto Selenium Test')]"));
        assertThat(produtoMobile.isDisplayed()).isTrue();

        
        driver.manage().window().setSize(new Dimension(1920, 1080));

        
        
        driver.navigate().to(baseUrl + "/estoque/movimentacoes");
        wait.until(ExpectedConditions.urlContains("/movimentacoes"));

        WebElement btnNovaMovimentacao = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Nova') or contains(text(), 'Adicionar')]")));
        btnNovaMovimentacao.click();

        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='produto' or @id='produto']")));
        
        Select selectProduto = new Select(driver.findElement(
                By.xpath("//select[@name='produto' or @id='produto']")));
        selectProduto.selectByVisibleText("Produto Selenium Test");

        Select selectTipo = new Select(driver.findElement(
                By.xpath("//select[@name='tipo' or @id='tipo']")));
        selectTipo.selectByValue("ENTRADA");

        driver.findElement(By.xpath("//input[@name='quantidade' or @id='quantidade']"))
                .sendKeys("50");

        driver.findElement(By.xpath("//input[@name='motivo' or @id='motivo']"))
                .sendKeys("Reposição de estoque - Teste Selenium");

        WebElement btnSalvarMovimentacao = driver.findElement(
                By.xpath("//button[contains(text(), 'Salvar') or @type='submit']"));
        btnSalvarMovimentacao.click();

        long tempoGestaoEstoque = System.currentTimeMillis() - inicioTeste;

        
        assertAll("Validações de gestão de estoque",
                () -> assertThat(tempoGestaoEstoque).isLessThan(10000), 
                () -> assertThat(driver.getPageSource()).contains("Produto Selenium Test"),
                () -> assertThat(driver.getCurrentUrl()).contains("/movimentacoes")
        );
    }

    @Test
    @Order(3)
    void testGestaoClientesSistemaPontuacao() {
        realizarLogin();

        long inicioTeste = System.currentTimeMillis();

        
        driver.get(baseUrl + "/clientes");
        wait.until(ExpectedConditions.urlContains("/clientes"));

        WebElement btnNovoCliente = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Novo') or contains(text(), 'Adicionar')]")));
        btnNovoCliente.click();

        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@name='nome' or @id='nome']")))
                .sendKeys("Cliente Selenium VIP");

        driver.findElement(By.xpath("//input[@name='email' or @id='email']"))
                .sendKeys("cliente.selenium@teste.com");

        driver.findElement(By.xpath("//input[@name='telefone' or @id='telefone']"))
                .sendKeys("(21)99999-9999");

        driver.findElement(By.xpath("//input[@name='cpf' or @id='cpf']"))
                .sendKeys("12345678901");

        driver.findElement(By.xpath("//input[@name='endereco' or @id='endereco']"))
                .sendKeys("Rua Selenium, 123 - Teste City");

        WebElement btnSalvarCliente = driver.findElement(
                By.xpath("//button[contains(text(), 'Salvar') or @type='submit']"));
        btnSalvarCliente.click();

        
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'sucesso')]")),
                ExpectedConditions.urlContains("/clientes")
        ));

        
        driver.get(baseUrl + "/pontuacao");
        wait.until(ExpectedConditions.urlContains("/pontuacao"));

        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Cliente Selenium VIP')]")));

        
        WebElement menuCategorias = driver.findElement(
                By.xpath("//a[contains(text(), 'Categorias') or contains(@href, 'categorias')]"));
        menuCategorias.click();

        WebElement btnNovaCategoria = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Nova') or contains(text(), 'Adicionar')]")));
        btnNovaCategoria.click();

        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@name='nome' or @id='nome']")))
                .sendKeys("VIP Selenium");

        driver.findElement(By.xpath("//input[@name='pontosMinimos' or @id='pontosMinimos']"))
                .sendKeys("5000");

        driver.findElement(By.xpath("//input[@name='cor' or @id='cor']"))
                .sendKeys("#FF0000");

        WebElement btnSalvarCategoria = driver.findElement(
                By.xpath("//button[contains(text(), 'Salvar') or @type='submit']"));
        btnSalvarCategoria.click();

        long tempoClientePontuacao = System.currentTimeMillis() - inicioTeste;

        
        assertAll("Validações de cliente e pontuação",
                () -> assertThat(tempoClientePontuacao).isLessThan(15000), 
                () -> assertThat(driver.getPageSource()).contains("Cliente Selenium VIP"),
                () -> assertThat(driver.getPageSource()).contains("VIP Selenium")
        );
    }

    @Test
    @Order(4) 
    void testProcessamentoCompletoVendas() {
        realizarLogin();

        long inicioTeste = System.currentTimeMillis();

        
        driver.get(baseUrl + "/vendas");
        wait.until(ExpectedConditions.urlContains("/vendas"));

        WebElement btnNovaVenda = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Nova') or contains(text(), 'Venda')]")));
        btnNovaVenda.click();

        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='cliente' or @id='cliente']")));
        
        Select selectCliente = new Select(driver.findElement(
                By.xpath("//select[@name='cliente' or @id='cliente']")));
        selectCliente.selectByVisibleText("Cliente Selenium VIP");

        
        WebElement btnAdicionarItem = driver.findElement(
                By.xpath("//button[contains(text(), 'Adicionar') or contains(text(), 'Item')]"));
        btnAdicionarItem.click();

        
        Select selectProduto = new Select(wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='produto' or contains(@name, 'produto')]"))));
        selectProduto.selectByVisibleText("Produto Selenium Test");

        
        driver.findElement(By.xpath("//input[@name='quantidade' or contains(@name, 'quantidade')]"))
                .sendKeys("2");

        
        Select metodoPagamento = new Select(driver.findElement(
                By.xpath("//select[@name='metodoPagamento' or @id='metodoPagamento']")));
        metodoPagamento.selectByValue("DINHEIRO");

        WebElement btnFinalizarVenda = driver.findElement(
                By.xpath("//button[contains(text(), 'Finalizar') or contains(text(), 'Confirmar')]"));
        btnFinalizarVenda.click();

        
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'sucesso')]")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'Venda realizada')]"))
        ));

        
        driver.get(baseUrl + "/vendas");
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Cliente Selenium VIP')]")));

        long tempoVenda = System.currentTimeMillis() - inicioTeste;

        
        assertAll("Validações de venda",
                () -> assertThat(tempoVenda).isLessThan(12000), 
                () -> assertThat(driver.getPageSource()).contains("Cliente Selenium VIP"),
                () -> assertThat(driver.getPageSource()).contains("Produto Selenium Test")
        );
    }

    @Test
    @Order(5)
    void testDashboardPerformanceRelatorios() {
        realizarLogin();

        long inicioTeste = System.currentTimeMillis();

        
        driver.get(baseUrl + "/dashboard");
        wait.until(ExpectedConditions.urlContains("/dashboard"));

        
        List<WebElement> metricas = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.xpath("//div[contains(@class, 'metric') or contains(@class, 'card')]")));
        assertThat(metricas.size()).isGreaterThan(0);

        long tempoDashboard = System.currentTimeMillis() - inicioTeste;

        
        long inicioRelatorios = System.currentTimeMillis();

        
        if (elementExists("//a[contains(text(), 'Relatórios')]")) {
            driver.findElement(By.xpath("//a[contains(text(), 'Relatórios')]")).click();
            wait.until(ExpectedConditions.urlContains("/relatorio"));
        }

        
        if (elementExists("//button[contains(text(), 'Produtos') or contains(text(), 'Gerar')]")) {
            WebElement btnRelatorio = driver.findElement(
                    By.xpath("//button[contains(text(), 'Produtos') or contains(text(), 'Gerar')]"));
            btnRelatorio.click();
            
            
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//table")),
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//canvas"))
            ));
        }

        long tempoRelatorios = System.currentTimeMillis() - inicioRelatorios;

        
        long inicioNavegacao = System.currentTimeMillis();
        
        String[] paginas = {"/produtos", "/clientes", "/vendas", "/estoque", "/dashboard"};
        for (String pagina : paginas) {
            driver.get(baseUrl + pagina);
            wait.until(ExpectedConditions.urlContains(pagina));
        }
        
        long tempoNavegacao = System.currentTimeMillis() - inicioNavegacao;

        
        assertAll("Validações de dashboard e performance",
                () -> assertThat(tempoDashboard).isLessThan(3000),    
                () -> assertThat(tempoRelatorios).isLessThan(5000),   
                () -> assertThat(tempoNavegacao).isLessThan(10000),   
                () -> assertThat(metricas.size()).isGreaterThanOrEqualTo(4), 
                () -> assertThat(driver.getPageSource()).contains("dashboard") 
        );
    }

    @Test
    @Order(6)
    void testUsabilidadeSegurancaAvancada() {
        realizarLogin();

        long inicioTeste = System.currentTimeMillis();

        
        
        driver.get(baseUrl + "/produtos");
        
        List<WebElement> inputs = driver.findElements(By.tagName("input"));
        for (WebElement input : inputs) {
            String type = input.getAttribute("type");
            if (!"hidden".equals(type) && !"submit".equals(type)) {
                boolean temLabel = input.getAttribute("placeholder") != null ||
                                 input.getAttribute("aria-label") != null ||
                                 input.getAttribute("title") != null;
                assertThat(temLabel).as("Input deve ter label/placeholder").isTrue();
            }
        }

        
        
        Dimension[] tamanhosTela = {
                new Dimension(1920, 1080), 
                new Dimension(1024, 768),  
                new Dimension(375, 667),   
                new Dimension(414, 896)    
        };

        for (Dimension tamanho : tamanhosTela) {
            driver.manage().window().setSize(tamanho);
            driver.navigate().refresh();
            
            
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//nav | //button[contains(@class, 'menu')] | //*[contains(@class, 'navbar')]")));
        }

        
        driver.manage().window().setSize(new Dimension(1920, 1080));

        
        
        driver.get(baseUrl + "/admin/configuracoes");
        
        
        String currentUrl = driver.getCurrentUrl();
        boolean acessoControlado = currentUrl.contains("/login") || 
                                  currentUrl.contains("/403") ||
                                  driver.getPageSource().contains("não autorizado") ||
                                  driver.getPageSource().contains("acesso negado");

        
        
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("sessionStorage.setItem('lastActivity', Date.now() - 3600000);"); 

        driver.navigate().refresh();
        
        
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/login"),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'sessão')]"))
        ));

        long tempoTestesSeguranca = System.currentTimeMillis() - inicioTeste;

        
        assertAll("Validações de usabilidade e segurança",
                () -> assertThat(tempoTestesSeguranca).isLessThan(20000), 
                () -> assertThat(inputs.size()).isGreaterThan(0), 
                () -> assertThat(driver.getCurrentUrl()).contains("/login") 
        );
    }

    @Test
    @Order(7)
    void testPerformanceStress() {
        realizarLogin();

        long inicioTeste = System.currentTimeMillis();

        
        String[] paginasParaTeste = {
                "/dashboard",
                "/produtos",
                "/clientes", 
                "/vendas",
                "/estoque",
                "/pontuacao",
                "/relatorios"
        };

        long tempoTotalNavegacao = 0;
        for (String pagina : paginasParaTeste) {
            long inicioPagina = System.currentTimeMillis();
            
            driver.get(baseUrl + pagina);
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains(pagina),
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//main | //div[@class='content'] | //body"))
            ));
            
            long tempoPagina = System.currentTimeMillis() - inicioPagina;
            tempoTotalNavegacao += tempoPagina;
            
            
            assertThat(tempoPagina).as("Página " + pagina + " deve carregar rapidamente").isLessThan(3000);
        }

        
        long inicioOperacoes = System.currentTimeMillis();

        
        driver.get(baseUrl + "/produtos");
        for (int i = 0; i < 5; i++) {
            WebElement campoBusca = driver.findElement(
                    By.xpath("//input[@placeholder='Buscar' or @name='busca' or @type='search']"));
            campoBusca.clear();
            campoBusca.sendKeys("produto " + i);
            
            
            TimeUnit.MILLISECONDS.sleep(500);
        }

        long tempoOperacoes = System.currentTimeMillis() - inicioOperacoes;
        long tempoTotal = System.currentTimeMillis() - inicioTeste;

        
        assertAll("Validações de performance",
                () -> assertThat(tempoTotalNavegacao).isLessThan(15000), 
                () -> assertThat(tempoOperacoes).isLessThan(5000),       
                () -> assertThat(tempoTotal).isLessThan(30000),          
                () -> assertThat(tempoTotalNavegacao / paginasParaTeste.length).isLessThan(2500) 
        );
    }


    private void realizarLogin() {
        driver.get(baseUrl + "/login");
        
        WebElement emailField = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@type='email' or @placeholder='Email' or @id='email']")));
        WebElement passwordField = driver.findElement(
                By.xpath("//input[@type='password' or @placeholder='Senha' or @id='password']"));
        WebElement loginButton = driver.findElement(
                By.xpath("//button[contains(text(), 'Login') or contains(text(), 'Entrar')]"));

        emailField.sendKeys("admin@sistema.com");
        passwordField.sendKeys("admin123");
        loginButton.click();

        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }

    private boolean elementExists(String xpath) {
        try {
            driver.findElement(By.xpath(xpath));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
