package org.ngafid.core.accounts;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AccountException class.
 * Tests exception creation, message handling, and title functionality.
 */
public class AccountExceptionTest {

    /**
     * Verifies the two-argument constructor stores the title and message, exposes them via the getters, and leaves the
     * cause null.
     */
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

    /**
     * Verifies the three-argument constructor stores the title, message, and underlying cause.
     */
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

    /**
     * Verifies a null title is retained as null while the message is still stored.
     */
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

    /**
     * Verifies a null message is retained as null while the title is still stored.
     */
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

    /**
     * Verifies the two-argument constructor accepts a null title and message together, leaving both null.
     */
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

    /**
     * Verifies the three-argument constructor accepts a null title while still storing the message and cause.
     */
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

    /**
     * Verifies the three-argument constructor accepts a null message while still storing the title and cause.
     */
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

    /**
     * Verifies the three-argument constructor accepts a null cause while still storing the title and message.
     */
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

    /**
     * Verifies the three-argument constructor tolerates all-null arguments, leaving title, message, and cause null.
     */
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

    /**
     * Verifies empty-string title and message are stored verbatim (not coerced to null).
     */
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

    /**
     * Verifies whitespace-only title and message are preserved exactly without trimming.
     */
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

    /**
     * Verifies long multi-sentence title and message strings are stored without truncation.
     */
    @Test
    @DisplayName("Should handle long title and message")
    public void testConstructorWithLongTitleAndMessage() {
        String title = "Very Long Error Title That Exceeds Normal Length";
        String message = "This is a very long error message that contains detailed "
                + "information about what went wrong and how to fix it. It "
                + "includes multiple sentences and provides comprehensive "
                + "context for the error.";

        AccountException exception = new AccountException(title, message);

        assertNotNull(exception);
        assertEquals(title, exception.getTitle());
        assertEquals(message, exception.getMessage());
    }

    /**
     * Verifies punctuation and symbol characters in the title and message are preserved verbatim.
     */
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

    /**
     * Verifies non-ASCII (Unicode) title and message text is stored and returned intact.
     */
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

    /**
     * Verifies the cause may be any Throwable subtype (RuntimeException, IllegalArgumentException, SQLException) and is
     * stored unchanged in each case.
     */
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

    /**
     * Verifies {@code getTitle} returns the same string reference that was passed in (no defensive copy), confirming
     * the title is held directly.
     */
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

    /**
     * Verifies a multi-level cause chain passed as the cause is preserved so the full chain is walkable via repeated
     * {@code getCause} calls.
     */
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

    /**
     * Verifies an AccountException may itself be the cause of another AccountException, with the inner instance's title
     * and message remaining accessible through the chain.
     */
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
