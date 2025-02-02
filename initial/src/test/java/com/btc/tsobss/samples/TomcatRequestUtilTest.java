package com.btc.tsobss.samples;

import org.apache.tomcat.util.http.RequestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TomcatRequestUtilTest {

    @Test
    public void testNormalize() {
        Assertions.assertEquals("/my/sample/path", RequestUtil.normalize("/my/sample/./path"));
        Assertions.assertEquals("/my/path", RequestUtil.normalize("/my/sample/../path"));
        Assertions.assertNull(RequestUtil.normalize("/../my/path"));
    }
}
