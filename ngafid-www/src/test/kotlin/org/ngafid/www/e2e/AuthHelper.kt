package org.ngafid.www.e2e

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions

object AuthHelper {

    fun login(base: TestBase, email: String, password: String) {
        val driver = base.driver
        val wait = base.wait
        driver.get(base.baseUrl)
        wait.until(
            ExpectedConditions.elementToBeClickable(By.linkText("Login"))
        ).click()
        val modal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-dialog"))
        )
        modal.findElement(By.id("loginEmail")).sendKeys(email)
        modal.findElement(By.id("loginPassword")).sendKeys(password)
        modal.findElement(By.cssSelector("button[type='submit']")).click()
        wait.until(ExpectedConditions.invisibilityOf(modal))
    }
 }z