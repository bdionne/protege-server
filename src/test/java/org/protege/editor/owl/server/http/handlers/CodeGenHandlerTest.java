package org.protege.editor.owl.server.handlers;

import junit.framework.TestCase;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.protege.editor.owl.server.http.handlers.CodeGenHandler;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class CodeGenHandlerTest extends TestCase {

    @Test
    public void generateNTokens() {
        List<String> codes = CodeGenHandler.generateCodes(10, 5, "p", "s", "_");
        List<String> expected = Arrays.asList("p_10_s",
                                              "p_11_s",
                                              "p_12_s",
                                              "p_13_s",
                                              "p_14_s");
        assertThat(codes, is(expected));
    }
}
