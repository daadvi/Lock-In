package com.example

import com.example.service.BankingSecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankingSecurityTest {

    @Test
    fun testCaixaPackagesAreRecognizedAndExempted() {
        // Main Caixa Econômica Federal application
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("br.com.gabba.Caixa"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("br.com.gabba.caixa"))

        // Caixa Tem & Social Benefits
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("br.gov.caixa.tem"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("br.gov.caixa.fgts.trabalhador"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("br.gov.caixa.atendimentocidadao"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("br.gov.caixa.cartoes"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("br.gov.caixa.habitacao"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("br.gov.caixa.ib"))
    }

    @Test
    fun testMajorBrazilianBanksRecognized() {
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("com.nu.production"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("com.itau"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("com.itau.iti"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("com.bradesco"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("br.com.bb.android"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("com.santander.app"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("br.com.intermedium"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("com.c6bank.app"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("com.mercadopago.wallet"))
        assertTrue(BankingSecurityHelper.isBankingOrFinancialApp("com.picpay"))
    }

    @Test
    fun testNonBankingAppsNotRecognized() {
        assertFalse(BankingSecurityHelper.isBankingOrFinancialApp("com.instagram.android"))
        assertFalse(BankingSecurityHelper.isBankingOrFinancialApp("com.google.android.youtube"))
        assertFalse(BankingSecurityHelper.isBankingOrFinancialApp("com.zhiliaoapp.musically"))
        assertFalse(BankingSecurityHelper.isBankingOrFinancialApp("com.whatsapp"))
        assertFalse(BankingSecurityHelper.isBankingOrFinancialApp("com.twitter.android"))
    }

    @Test
    fun testFriendlyBankNameForCaixa() {
        assertEquals("Caixa Econômica Federal", BankingSecurityHelper.getFriendlyBankName("br.com.gabba.Caixa"))
        assertEquals("Caixa Econômica Federal", BankingSecurityHelper.getFriendlyBankName("br.gov.caixa.tem"))
    }
}
