package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.sleep;
import static com.codeborne.selenide.Condition.*;
import static org.junit.Assert.*;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;

public class NavisaleUITest {
    private static class PData {
        private String name;
        private String price;
        private int amount;
        private List<String> params = new ArrayList<String>();   

        public PData(String name, String price) {
            this.name = name;
            this.price = price;
            this.amount = 1;
            this.params = new ArrayList<String>();
        }    

        public void setAmount(int newAmount) { amount = newAmount; }
        public void addParam(String value) { params.add(value);  }
        public String getName() { return name; }
        public String getPrice() { return price; }
        public int getAmount() {return amount;}
        public List<String> getParams() { return params; }
    }

    private List<PData> cartProducts = new ArrayList<>();
    
    @BeforeClass
    public static void setUp() {
        Configuration.browser = "chrome";
        Configuration.baseUrl = "https://navisale.ru";
        Configuration.timeout = 9999;
        Configuration.browserSize = "1920x1080";
    }

    @Test
    public void cartVerificationTest(){
        for (int tests = 0; tests < 5; tests++){
            int productsToCheck = 3;
            open("/");
            for (int i = 0; i < productsToCheck; i++){
                addToCart();
            }
            // Открываем корзину
            $("a[data-selector='basket-desktop']").click();
            cartCheck();           
            cartProducts.clear();
            Selenide.clearBrowserCookies();
            Selenide.clearBrowserLocalStorage();
            Selenide.closeWebDriver(); 
            sleep(5000);
        }
    }

    private void addToCart(){
        SelenideElement catalog = $("a.header-rubrics-toggler, [href='/catalog']");
        catalog.click();
        sleep(1000);
        
        // Перемещение по меню в поисках случайного товара из
        ElementsCollection categories = $("ul[class='mega-burger-sidebar-menu']").$$("li");
        System.out.println("Number of main categories: " + categories.size());
        System.out.println("Current page: " + WebDriverRunner.url());
        int randomCategory = new Random().nextInt(categories.size());
        categories.get(randomCategory).click();
        ElementsCollection subcategories = $$("ul.rubrics-catalog__list li");        
        randomCategory = new Random().nextInt(subcategories.size());
        subcategories.get(randomCategory).click();
        ElementsCollection subsubcategories = $$("ul.rubrics-catalog__sub-list li");        
        // Есть подкатегории, в которых нет дальнейшего разделения
        if (subsubcategories.size() > 0){
            randomCategory = new Random().nextInt(subsubcategories.size());
            subsubcategories.get(randomCategory).click();
        }
        ElementsCollection prodGrid = $$("ul.rubrics-items-grid li");
        // Пролистывание страницы до других продуктов занимает время, что триггерит тайм-аут. Для этого ограничение в 20 товаров.   
        randomCategory = new Random().nextInt(Math.min(prodGrid.size(), 20)); 
        prodGrid.get(randomCategory).scrollIntoView(true).shouldBe(visible).shouldBe(clickable).click();

        String productName = $("h1.font-semibold.heading-2").getText();
        System.out.println("Full product name: " + productName);
        System.out.println("Current page: " + WebDriverRunner.url());
        String productPrice = $("div[data-selector='priceArea']").getText();
        System.out.println("Product unit price: " + productPrice);
        PData currentProduct = new PData(productName, productPrice);
        
        // Случайно выбираем параметры заказа на товар
        ElementsCollection options = $$("div[class='min-w-0']"); 
        System.out.println("Number of additional parameters: " + options.size());
        for (int i = 0; i < options.size(); i++){       
            if (options.get(i).findElements(By.tagName("ul")).isEmpty()){
                continue;
            }
            SelenideElement option = options.get(i).$("ul");
            if (option.findElements(By.tagName("li")).isEmpty()){
                continue;
            }
            ElementsCollection types = option.$$("li"); 
            randomCategory = new Random().nextInt(types.size());           
                  
            System.out.println("Value selected: " + types.get(randomCategory).text());
            types.get(randomCategory).click(); 
            
        }

        // Записываем текущие параметры для сверки
        for (int i = 0; i < options.size(); i++){       
            SelenideElement legend = options.get(i).$("legend");
            if (legend.exists()){
                SelenideElement paramName = legend.$("span[class*='whitespace-nowrap']");
                SelenideElement paramValue = legend.$("span[data-selector='options-group:legend-value']");

                System.out.println("Parameter name: " + paramName.text());                
                System.out.println("Selected value: " + paramValue.text());
                System.out.println("Current page: " + WebDriverRunner.url());
                currentProduct.addParam(paramValue.text());
            }
        }

        // Добавляем в корзину
        $("button[data-kit='primary:600']").click();

        int howMuch = new Random().nextInt(98)+1;          
        SelenideElement amount = $("span[data-kit='secondary:600']").$("input");

        // Пример - на сайте некоторые толстовки Out of stock
        amount.should(exist);
        amount.doubleClick();// чтобы выделить
        amount.sendKeys(Integer.toString(howMuch));   
        // Потому что есть лимит по количеству товара и 99
        howMuch = Integer.parseInt(amount.getValue());
        currentProduct.setAmount(howMuch);
        cartProducts.add(currentProduct);
    }

    private void cartCheck(){        
        sleep(5000);
        // В текущей версии UI товары зачем-то бьются на страны без возможности выбрать всё
        // И общее количество товара по стране не показывается без выделения конкретной страны
        ElementsCollection countries = $$("div[class*='rounded-700']");
        
        int realNumberOfItems = 0, allegedNumberOfItems = 0;
        for (int i = 0; i < cartProducts.size(); i++){
            System.out.println("Product: " + cartProducts.get(i).getName() 
                             + " - Priced: " + cartProducts.get(i).getPrice() 
                             + " - Counted: " + cartProducts.get(i).getAmount());
            realNumberOfItems += cartProducts.get(i).getAmount();
        }

        System.out.println("Number of countries: " + countries.size()); 

        int finalNumberOfItems = 0;        
        for (int i = 0; i < countries.size(); i++) {     

            SelenideElement countryLabel = countries.get(i).$("label");
            ElementsCollection countryItems = countries.get(i).$$("li[class*='pt-4 pb-3']");
            System.out.println("Cart entries for country: " + countryItems.size());

            if (countryLabel.text().length() > 0){             
                sleep(1000);    
                if (countryLabel.text().split(" ")[0].contains("Товары"))
                    countryLabel.click();
                
                sleep(5000);    
                System.out.println("Items by country: " + countryLabel.text()); 
                while (countryLabel.text().split(" ")[0].contains("Товары")){
                    countryLabel.click();
                    sleep(5000);   
                }
                allegedNumberOfItems += Integer.parseInt(countryLabel.text().split(" ")[0]);     
                for (int j = 0; j < countryItems.size(); j++){
                    checkProduct(countryItems.get(j));                    
                    sleep(500);    
                }       
            }           
            else {  
                for (int j = 0; j < countryItems.size(); j++){
                    checkProduct(countryItems.get(j));      
                    if (!countryItems.get(j).$("input[type='checkbox']").isSelected())                        
                        countryItems.get(j).$("input[type='checkbox']").click();
                    SelenideElement countField = countryItems.get(j).$("div[class*='quantity']").$("input");
                    System.out.println("Number of item " + countryItems.get(j).text() + ": " + countField.getValue());
                    allegedNumberOfItems += Integer.parseInt(countField.getValue());  
                    // на сайте много времени занимает обновление общего числа товаров
                    sleep(500);    
                }                
            }
            
            SelenideElement finalItemsInCart = $("div[class*='mt-3 px-4']").$("div[class*='control-300']");
            System.out.println("Total number of items: " + finalItemsInCart.getText());

            finalNumberOfItems += Integer.parseInt(finalItemsInCart.text().split(" ")[0]);  
        }   

        System.out.println("Pre-calculated number of items: " + realNumberOfItems);
        System.out.println("Calculated amount of items in the cart: " + allegedNumberOfItems);
        System.out.println("Shown total of items in the cart: " + finalNumberOfItems);

        // 2) проверить количество выбранного товара
        assertTrue(finalNumberOfItems==allegedNumberOfItems); 
        assertTrue(finalNumberOfItems==realNumberOfItems); 
        assertTrue(realNumberOfItems==allegedNumberOfItems); 

        sleep(100);
    }    

    private String priceToNumber(String price){
        return String.join("", price.split(" "));
    }

    private void checkProduct(SelenideElement item){
        SelenideElement name = item.$("a[class*='body-400']");
        SelenideElement params = item.$("div[class*='body-300']");
        SelenideElement priceTag = item.$("span[data-kit='horizontal:400']").$("span[class*='control-400']");

        // Так как в корзине ценник автоматически умножается на число, а на странице товара - нет.
        SelenideElement countField = item.$("div[class*='quantity']").$("input");
        int realPriceTag = Integer.parseInt(priceToNumber(priceTag.text()))/Integer.parseInt(countField.getValue());  

        boolean productPresent = false;
        boolean productPriceCorrect = false;
        
        System.out.println("Cart Product: " + name.text()
                        + " - Cart Priced: " + priceTag.text()                        
                        + " - Cart Counted: " + countField.getValue());
        if (params.exists())
            System.out.println("Cart Product Params: " + params.text());
              
        // 1) проверить что название и параметры в корзине соответствуют добавленному ранее товару 
        // 3) для каждого товара сравнить ценник на странице и в корзине
        for (int k = 0; k < cartProducts.size(); k++){
            if (cartProducts.get(k).getName().contains(name.text())){
                productPresent = true;
                if (priceToNumber(cartProducts.get(k).getPrice()).contains(Integer.toString(realPriceTag))){
                    productPriceCorrect = true;
                    if (params.exists())
                        for (String param : cartProducts.get(k).getParams()){
                            if (!param.isEmpty())
                                params.shouldHave(partialText(param));
                        }
                }
                break;
            }
        }
        assertTrue(productPresent);
        assertTrue(productPriceCorrect);
    }    
}