package org.ngafid.core.accounts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;

/**
 * Unit tests for AccountException class.
 * Tests exception creation, message handling, and title functionality.
 */
public class AccountExceptionTest {

    @Test
    @DisplayName("Should create AccountException with title and message")
    public void testConstructorWithTitleAndMessage() {
        String title = "Authentication Error";
        String message = "Invalid credentials provided";
        
        AccountException exception = new AccountException(title, message);
        
        assertNotNull(exception);
        assertEquals(title, exception.getTitle());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create AccountException with title, message, and cause")
    public void testConstructorWithTitleMessageAndCause() {
        String title = "Database Error";
        String message = "Failed to connect to database";
        Throwable cause = new RuntimeException("Connection timeout");
        
        AccountException exception = new AccountException(title, message, cause);
        
        assertNotNull(exception);
        assertEquals(title, exception.getTitle());
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should handle null title")
    public void testConstructorWithNullTitle() {
        String title = null;
        String message = "Some error occurred";
        
        AccountException exception = new AccountException(title, message);
        
        assertNotNull(exception);
        assertNull(exception.getTitle());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("Should handle null message")
    public void testConstructorWithNullMessage() {
        String title = "Error Title";
        String message = null;
        
        AccountException exception = new AccountException(title, message);
        
        assertNotNull(exception);
        assertEquals(title, exception.getTitle());
        assertNull(exception.getMessage());
    }

    @Test
    @DisplayName("Should handle null title and message")
    public void testConstructorWithNullTitleAndMessage() {
        String title = null;
        String message = null;
        
        AccountException exception = new AccountException(title, message);
        
        assertNotNull(exception);
        assertNull(exception.getTitle());
        assertNull(exception.getMessage());
    }

    @Test
    @DisplayName("Should handle null title with cause")
    public void testConstructorWithNullTitleAndCause() {
        String title = null;
        String message = "Error with cause";
        Throwable cause = new IllegalArgumentException("Invalid argument");
        
        AccountException exception = new AccountException(title, message, cause);
        
        assertNotNull(exception);
        assertNull(exception.getTitle());
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should handle null message with cause")
    public void testConstructorWithNullMessageAndCause() {
        String title = "Error Title";
        String message = null;
        Throwable cause = new SQLException("Database error");
        
        AccountException exception = new AccountException(title, message, cause);
        
        assertNotNull(exception);
        assertEquals(title, exception.getTitle());
        assertNull(exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should handle null cause")
    public void testConstructorWithNullCause() {
        String title = "Error Title";
        String message = "Error message";
        Throwable cause = null;
        
        AccountException exception = new AccountException(title, message, cause);
        
        assertNotNull(exception);
        assertEquals(title, exception.getTitle());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should handle all null parameters")
    public void testConstructorWithAllNullParameters() {
        String title = null;
        String message = null;
        Throwable cause = null;
        
        AccountException exception = new AccountException(title, message, cause);
        
        assertNotNull(exception);
        assertNull(exception.getTitle());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should handle empty title and message")
    public void testConstructorWithEmptyTitleAndMessage() {
        String title = "";
        String message = "";
        
        AccountException exception = new AccountException(title, message);
        
        assertNotNull(exception);
        assertEquals("", exception.getTitle());
        assertEquals("", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle whitespace title and message")
    public void testConstructorWithWhitespaceTitleAndMessage() {
        String title = "   ";
        String message = "\t\n";
        
        AccountException exception = new AccountException(title, message);
        
        assertNotNull(exception);
        assertEquals("   ", exception.getTitle());
        assertEquals("\t\n", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle long title and message")
    public void testConstructorWithLongTitleAndMessage() {
        String title = "Very Long Error Title That Exceeds Normal Length";
        String message = "This is a very long error message that contains detailed information about what went wrong and how to fix it. It includes multiple sentences and provides comprehensive context for the error.";
        
        AccountException exception = new AccountException(title, message);
        
        assertNotNull(exception);
        assertEquals(title, exception.getTitle());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("Should handle special characters in title and message")
    public void testConstructorWithSpecialCharacters() {
        String title = "Error: @#$%^&*()_+-=[]{}|;':\",./<>?";
        String message = "Error with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        
        AccountException exception = new AccountException(title, message);
        
        assertNotNull(exception);
        assertEquals(title, exception.getTitle());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("Should handle Unicode characters in title and message")
    public void testConstructorWithUnicodeCharacters() {
        String title = "错误标题";
        String message = "错误消息：数据库连接失败";
        
        AccountException exception = new AccountException(title, message);
        
        assertNotNull(exception);
        assertEquals(title, exception.getTitle());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("Should handle different exception types as cause")
    public void testConstructorWithDifferentExceptionTypes() {
        String title = "Various Error Types";
        
        // Test with RuntimeException
        RuntimeException runtimeException = new RuntimeException("Runtime error");
        AccountException exception1 = new AccountException(title, "Runtime error", runtimeException);
        assertEquals(runtimeException, exception1.getCause());
        
        // Test with IllegalArgumentException
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Illegal argument");
        AccountException exception2 = new AccountException(title, "Illegal argument error", illegalArgumentException);
        assertEquals(illegalArgumentException, exception2.getCause());
        
        // Test with SQLException
        SQLException sqlException = new SQLException("Database error");
        AccountException exception3 = new AccountException(title, "Database error", sqlException);
        assertEquals(sqlException, exception3.getCause());
    }

    @Test
    @DisplayName("Should maintain title immutability")
    public void testTitleImmutability() {
        String title = "Original Title";
        String message = "Test message";
        
        AccountException exception = new AccountException(title, message);
        String retrievedTitle = exception.getTitle();
        
        // The retrieved title should be the same reference (not a copy)
        assertSame(title, retrievedTitle);
        
        // Modifying the original title should not affect the exception's title
        // (though this is more about defensive programming)
        assertEquals("Original Title", exception.getTitle());
    }

    @Test
    @DisplayName("Should handle nested exceptions as cause")
    public void testConstructorWithNestedExceptions() {
        String title = "Nested Exception Test";
        String message = "Outer exception";
        
        // Create a nested exception chain
        RuntimeException innerException = new RuntimeException("Inner exception");
        SQLException middleException = new SQLException("Middle exception", innerException);
        IllegalArgumentException outerException = new IllegalArgumentException("Outer exception", middleException);
        
        AccountException exception = new AccountException(title, message, outerException);
        
        assertNotNull(exception);
        assertEquals(title, exception.getTitle());
        assertEquals(message, exception.getMessage());
        assertEquals(outerException, exception.getCause());
        
        // Verify the exception chain
        assertEquals(middleException, exception.getCause().getCause());
        assertEquals(innerException, exception.getCause().getCause().getCause());
    }

    @Test
    @DisplayName("Should handle AccountException as cause")
    public void testConstructorWithAccountExceptionAsCause() {
        String title = "Outer Account Exception";
        String message = "Outer message";
        String innerTitle = "Inner Account Exception";
        String innerMessage = "Inner message";
        
        AccountException innerException = new AccountException(innerTitle, innerMessage);
        AccountException outerException = new AccountException(title, message, innerException);
        
        assertNotNull(outerException);
        assertEquals(title, outerException.getTitle());
        assertEquals(message, outerException.getMessage());
        assertEquals(innerException, outerException.getCause());
        
        // Verify the inner exception properties
        assertEquals(innerTitle, ((AccountException) outerException.getCause()).getTitle());
        assertEquals(innerMessage, outerException.getCause().getMessage());
    }
}
