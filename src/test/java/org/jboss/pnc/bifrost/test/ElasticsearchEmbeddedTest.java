package org.jboss.pnc.bifrost.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Target({ TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Tag("elasticsearch-test")
@Test
public @interface ElasticsearchEmbeddedTest {
}
